package org.cytoscape.WikiDataScape.internal.tasks;

import java.awt.Color;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.cytoscape.WikiDataScape.internal.CyActivator;
import org.cytoscape.WikiDataScape.internal.RainbowColorMappingGenerator;
import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.gui.util.DiscreteMappingGenerator;
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
        vs.setTitle("WikiDataScape");

        VisualMappingFunction<String, String> edgeLabel = vmfPassFactory.createVisualMappingFunction("interaction", String.class, BasicVisualLexicon.EDGE_LABEL);
        vs.addVisualMappingFunction(edgeLabel);

        DiscreteMapping nodeColor = (DiscreteMapping) vmfDiscreteFactory.createVisualMappingFunction(column, String.class, BasicVisualLexicon.NODE_FILL_COLOR);
        
        CyNetwork myNet = cyView.getModel();
        CyTable nodeTable = myNet.getDefaultNodeTable();
        HashSet<String> attributes = new HashSet<>();
        nodeTable.getColumn(column).getValues(List.class).stream().filter((x) -> (x!=null)).forEach((x) -> {
            attributes.addAll(x);
        });
        if (!attributes.isEmpty()){
            RainbowColorMappingGenerator d = new RainbowColorMappingGenerator();
            Map<String, Color> generateMap = d.generateMap(attributes);
            nodeColor.putAll(generateMap);
            vs.addVisualMappingFunction(nodeColor);
        }

        vs.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.GRAY);
        vs.setDefaultValue(BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE, ArrowShapeVisualProperty.ARROW);

        vmmServiceRef.addVisualStyle(vs);

        return vs;
    }
    
    private void updateVisualStyle(VisualStyle vs){
        CyAppAdapter adapter = CyActivator.getCyAppAdapter();
        VisualMappingFunctionFactory vmfDiscreteFactory = adapter.getVisualMappingFunctionDiscreteFactory();
        CyNetworkViewManager cnvm = adapter.getCyNetworkViewManager();
                
        DiscreteMapping nodeColor = (DiscreteMapping) vmfDiscreteFactory.createVisualMappingFunction(column, String.class, BasicVisualLexicon.NODE_FILL_COLOR);
        
        CyNetwork myNet = cyView.getModel();
        CyTable nodeTable = myNet.getDefaultNodeTable();
        HashSet<String> attributes = new HashSet<>();
        List<List> instances = nodeTable.getColumn(column).getValues(List.class);
        instances.stream().filter((x) -> (x!=null)).forEach((x) -> {
            attributes.addAll(x);
        });
        System.out.println("attributes: " + attributes);
        if (!attributes.isEmpty()){
            RainbowColorMappingGenerator d = new RainbowColorMappingGenerator();
            Map<String, Color> generateMap = d.generateMap(attributes);
            nodeColor.putAll(generateMap);
            vs.addVisualMappingFunction(nodeColor);
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
