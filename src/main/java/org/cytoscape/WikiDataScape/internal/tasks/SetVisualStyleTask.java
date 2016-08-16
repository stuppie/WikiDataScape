package org.cytoscape.WikiDataScape.internal.tasks;

import java.awt.Color;
import java.awt.Paint;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.cytoscape.WikiDataScape.internal.CyActivator;
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

    public SetVisualStyleTask(CyNetworkView cyView) {
        System.out.println("SetVisualStyleTask init");
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

        vs.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.RED);
        vs.setDefaultValue(BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE, ArrowShapeVisualProperty.ARROW);

        vmmServiceRef.addVisualStyle(vs);

        return vs;
    }
    
    private void updateVisualStyle(VisualStyle vs){
        CyAppAdapter adapter = CyActivator.getCyAppAdapter();
        VisualMappingFunctionFactory vmfDiscreteFactory = adapter.getVisualMappingFunctionDiscreteFactory();
        CyNetworkViewManager cnvm = adapter.getCyNetworkViewManager();
        
        // Here need to add new colors to map to new nodes
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
        System.out.println("createTaskIterator");
        return new TaskIterator(new SetVisualStyleTask(cyView));
    }
}
