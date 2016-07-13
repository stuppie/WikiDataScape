/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cytoscape.myapp.internal;

/**
 *
 * @author gstupp
 */
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.MultiMap;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.view.model.CyNetworkView;

public class TransformTaskFactory extends AbstractTaskFactory {
    
    private final CyNetworkManager netMgr;
    private final CyNetworkFactory cnf;
    private Map<Item, List<String>> test;
    private final CyApplicationManager applicationManager;
    
    public TransformTaskFactory(CyNetworkManager netMgr, CyNetworkFactory cnf, CyApplicationManager applicationManager, Map<Item, List<String>> test){
        this.netMgr = netMgr;
        this.cnf = cnf;
        this.applicationManager = applicationManager;
        this.test = test;
        System.out.println("TransformTaskFactory init");
    }		
    
    @Override
    public TaskIterator createTaskIterator() {
        System.out.println("createTaskIterator");
        return new TaskIterator(new TransformTask(netMgr, cnf, applicationManager, test));
    }
}