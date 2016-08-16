package org.cytoscape.WikiDataScape.internal.tasks;

import java.awt.Color;
import java.awt.Paint;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.cytoscape.WikiDataScape.internal.CyActivator;
import org.cytoscape.WikiDataScape.internal.RainbowColorMappingGenerator;
import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.NullVisualProperty;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.gui.util.DiscreteMappingGenerator;
import org.cytoscape.view.vizmap.gui.util.PropertySheetUtil;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;

/**
 *
 * @author gstupp
 */
public class SetVisualStyleTask extends AbstractTask {

    public SetVisualStyleTask() {
        System.out.println("SetVisualStyleTask init");
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        System.out.println("SetVisualStyleTask run");
        // https://github.com/christopher-johnson/cyto-rdf/blob/22bc518e70e2e4cca1fb4d19f9be5ef1d974aa76/src/com/generalbioinformatics/cy3/internal/CytoscapeV3Mapper.java
        CyAppAdapter adapter = CyActivator.getCyAppAdapter();
        VisualMappingManager vmmServiceRef = adapter.getVisualMappingManager();
        VisualStyleFactory visualStyleFactory = adapter.getVisualStyleFactory();
        VisualMappingFunctionFactory vmfDiscreteFactory = adapter.getVisualMappingFunctionDiscreteFactory();
        VisualMappingFunctionFactory vmfPassFactory = adapter.getVisualMappingFunctionPassthroughFactory();
        
        CyNetworkViewManager cnvm = adapter.getCyNetworkViewManager();
        
        for (CyNetworkView netView: cnvm.getNetworkViewSet()){
            VisualStyle vs = vmmServiceRef.getCurrentVisualStyle();
            
            VisualMappingFunction<String, String> edgeLabel = vmfPassFactory.createVisualMappingFunction("interaction", String.class, BasicVisualLexicon.EDGE_LABEL);
            vs.addVisualMappingFunction(edgeLabel);

            VisualMappingFunction<String, Paint> nodeColor = vmfDiscreteFactory.createVisualMappingFunction("instance of", String.class, BasicVisualLexicon.NODE_FILL_COLOR);
            
            /* // can't figure this shit out
            RainbowColorMappingGenerator g = new RainbowColorMappingGenerator();
            CyNetwork myNet = netView.getModel();
            CyTable nodeTable = myNet.getDefaultNodeTable();
            Map generateMap = g.generateMap(new HashSet(nodeTable.getColumn("instance of").getValues(String.class)));
            */
            
            vs.addVisualMappingFunction(nodeColor);
            
            vs.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.RED);
            vs.setDefaultValue(BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE, ArrowShapeVisualProperty.ARROW);
            
            //vmmServiceRef.addVisualStyle(vs);
            vmmServiceRef.setCurrentVisualStyle(vs);
        }

    }

    public TaskIterator createTaskIterator() {
        System.out.println("createTaskIterator");
        return new TaskIterator(new SetVisualStyleTask());
    }
}
