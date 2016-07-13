/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cytoscape.myapp.internal;

import java.awt.Color;
import java.awt.Paint;
import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
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
        VisualMappingFunctionFactory vmfFactoryD = adapter.getVisualMappingFunctionDiscreteFactory();
        
        // TODO: I don't know how to get the current network view. This is just getting the first one.
        // and will probably fail if there are no views
        CyNetworkViewManager cnvm = adapter.getCyNetworkViewManager();
        CyNetworkView netView = cnvm.getNetworkViewSet().iterator().next();

        VisualStyle vs = visualStyleFactory.createVisualStyle(vmmServiceRef.getDefaultVisualStyle());
        vs.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.RED);

        DiscreteMapping<String, Paint> colorMapping = (DiscreteMapping<String, Paint>) vmfFactoryD.createVisualMappingFunction("type", String.class, BasicVisualLexicon.NODE_FILL_COLOR);

        colorMapping.putMapValue("protein", new Color(153, 153, 255));
        colorMapping.putMapValue("GO", new Color(1, 255, 1));
        colorMapping.putMapValue("compound", new Color(255, 153, 153));
        colorMapping.putMapValue("domain", new Color(255, 1, 153));
        colorMapping.putMapValue("gene", new Color(153, 1, 153));
        vs.addVisualMappingFunction(colorMapping);

        vmmServiceRef.addVisualStyle(vs);
        vmmServiceRef.setVisualStyle(vs, netView);
        
    }
    
    public TaskIterator createTaskIterator() {
        System.out.println("createTaskIterator");
        return new TaskIterator(new SetVisualStyleTask());
    }
}
