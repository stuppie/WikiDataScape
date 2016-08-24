package org.cytoscape.WikiDataScape.internal.tasks;

import java.awt.Color;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.cytoscape.WikiDataScape.internal.CyActivator;
import org.cytoscape.WikiDataScape.internal.RandomColorMappingGenerator;
import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.presentation.property.LineTypeVisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualPropertyDependency;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

/**
 *
 * @author gstupp
 */
public class SetVisualStyleTask extends AbstractTask {

    private final CyNetworkView cyView;
    //private final String column = "subclass of";
    private final String column = "instance of";

    public SetVisualStyleTask(CyNetworkView cyView) {
        this.cyView = cyView;
    }

    private VisualStyle createVisualStyle() {

        CyAppAdapter adapter = CyActivator.getCyAppAdapter();
        VisualMappingManager vmmServiceRef = adapter.getVisualMappingManager();
        VisualStyleFactory visualStyleFactory = adapter.getVisualStyleFactory();
        VisualMappingFunctionFactory vmfDiscreteFactory = adapter.getVisualMappingFunctionDiscreteFactory();
        VisualMappingFunctionFactory vmfPassFactory = adapter.getVisualMappingFunctionPassthroughFactory();

        VisualStyle vs = visualStyleFactory.createVisualStyle(vmmServiceRef.getDefaultVisualStyle());
        /*
        VisualStyle base = vmmServiceRef.getAllVisualStyles().stream().filter(x -> x.getTitle().equals("Marquee")).findFirst().get();
        VisualStyle vs = visualStyleFactory.createVisualStyle(base);
        */
        vs.setTitle("WikiDataScape");

        // edge label: passthrough "interaction" column
        VisualMappingFunction<String, String> edgeLabel = vmfPassFactory.createVisualMappingFunction("interaction", String.class, BasicVisualLexicon.EDGE_LABEL);
        vs.addVisualMappingFunction(edgeLabel);

        // node face color mapping: discrete "instance of" random color
        DiscreteMapping nodeColor = (DiscreteMapping) vmfDiscreteFactory.createVisualMappingFunction(column, String.class, BasicVisualLexicon.NODE_FILL_COLOR);
        CyNetwork myNet = cyView.getModel();
        CyTable nodeTable = myNet.getDefaultNodeTable();
        HashSet<String> attributes = new HashSet<>();
        if (nodeTable.getColumn(column) != null) {
            nodeTable.getColumn(column).getValues(List.class).stream().filter((x) -> (x != null)).forEach((x) -> {
                attributes.addAll(x);
            });
            if (!attributes.isEmpty()) {
                RandomColorMappingGenerator d = new RandomColorMappingGenerator();
                Map<String, Color> generateMap = d.generateMap(attributes);
                nodeColor.putAll(generateMap);
            }
        }
        nodeColor.putMapValue("", Color.GRAY);
        vs.addVisualMappingFunction(nodeColor);

        // edge stroke color: discrete mapping to "instance of" random color
        DiscreteMapping edgeColor = (DiscreteMapping) vmfDiscreteFactory.createVisualMappingFunction("interaction", String.class, BasicVisualLexicon.EDGE_UNSELECTED_PAINT);
        CyTable edgeTable = myNet.getDefaultEdgeTable();
        HashSet<String> interactions = new HashSet<>(edgeTable.getColumn("interaction").getValues(String.class));
        if (!interactions.isEmpty()) {
            RandomColorMappingGenerator d = new RandomColorMappingGenerator();
            Map<String, Color> generateMap = d.generateMap(interactions);
            edgeColor.putAll(generateMap);
        }
        edgeColor.putMapValue("", Color.black);
        vs.addVisualMappingFunction(edgeColor);

        // edge line type mapping a couple major edge types
        DiscreteMapping edgeLineType = (DiscreteMapping) vmfDiscreteFactory.createVisualMappingFunction("interaction", String.class, BasicVisualLexicon.EDGE_LINE_TYPE);
        edgeLineType.putMapValue("subclass of", LineTypeVisualProperty.DASH_DOT);
        edgeLineType.putMapValue("instance of", LineTypeVisualProperty.DOT);
        edgeLineType.putMapValue("has part", LineTypeVisualProperty.EQUAL_DASH);
        vs.addVisualMappingFunction(edgeLineType);

        vs.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.GRAY);
        vs.setDefaultValue(BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE, ArrowShapeVisualProperty.ARROW);
        
        // Enable "Custom Graphics fit to Node" and "Edge color to arrows" dependency
        // Also disable "Lock Node height and width"
        // copied from here, because this isn't documented anywhere
        // https://github.com/idekerlab/dot-app/blob/24e308b9f869abf0e7007f6a508c99e99ebec52d/src/main/java/org/cytoscape/intern/read/DotReaderTask.java
        for (VisualPropertyDependency<?> dep : vs.getAllVisualPropertyDependencies()) {
            if (dep.getIdString().equals("nodeCustomGraphicsSizeSync")
                    || dep.getIdString().equals("arrowColorMatchesEdge")) {
                dep.setDependency(true);
            } else if (dep.getIdString().equals("nodeSizeLocked")) {
                dep.setDependency(false);
            }

        }

        vmmServiceRef.addVisualStyle(vs);

        return vs;
    }

    private void updateVisualStyle(VisualStyle vs) {

        // update node face color
        DiscreteMapping nodeColor = (DiscreteMapping) vs.getVisualMappingFunction(BasicVisualLexicon.NODE_FILL_COLOR);
        CyNetwork myNet = cyView.getModel();
        CyTable nodeTable = myNet.getDefaultNodeTable();
        HashSet<String> attributes = new HashSet<>();
        if (nodeTable.getColumn(column) != null) {
            List<List> instances = nodeTable.getColumn(column).getValues(List.class);
            instances.stream().filter((x) -> (x != null)).forEach((x) -> {
                attributes.addAll(x);
            });
            if (!attributes.isEmpty()) {
                Map currentMap = nodeColor.getAll();
                RandomColorMappingGenerator d = new RandomColorMappingGenerator();
                Map<String, Color> generateMap = d.generateMap(attributes);
                generateMap.putAll(currentMap);
                nodeColor.putAll(generateMap);
            }
        }

        // update edge stroke color
        DiscreteMapping edgeColor = (DiscreteMapping) vs.getVisualMappingFunction(BasicVisualLexicon.EDGE_UNSELECTED_PAINT);
        CyTable edgeTable = myNet.getDefaultEdgeTable();
        HashSet<String> interactions = new HashSet<>(edgeTable.getColumn("interaction").getValues(String.class));
        if (!interactions.isEmpty()) {
            // This reuses the same colors
            Map currentMap = edgeColor.getAll();
            RandomColorMappingGenerator d = new RandomColorMappingGenerator();
            Map<String, Color> generateMap = d.generateMap(interactions);
            generateMap.putAll(currentMap);
            edgeColor.putAll(generateMap);
        }
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        System.out.println("SetVisualStyleTask run");
        CyAppAdapter adapter = CyActivator.getCyAppAdapter();
        VisualMappingManager vmmServiceRef = adapter.getVisualMappingManager();

        // check if the wikidatascape visual style exists.
        // If it doesnt, create it and then set the it for the current view
        Set<String> titles = vmmServiceRef.getAllVisualStyles().stream().map(x -> x.getTitle()).collect(Collectors.toSet());
        if (!titles.contains("WikiDataScape")) {
            VisualStyle vs = createVisualStyle();
            vmmServiceRef.setCurrentVisualStyle(vs);
        } else {
            VisualStyle vs = vmmServiceRef.getAllVisualStyles().stream().filter(x -> x.getTitle().equals("WikiDataScape")).findFirst().get();
            updateVisualStyle(vs);
        }

    }

    public TaskIterator createTaskIterator() {
        return new TaskIterator(new SetVisualStyleTask(cyView));
    }
}
