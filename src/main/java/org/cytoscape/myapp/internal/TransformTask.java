/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cytoscape.myapp.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.MultiMap;
import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

/**
 *
 * @author gstupp
 */
public class TransformTask extends AbstractTask {

    private final CyNetworkManager netMgr;
    private final CyNetworkFactory cnf;
    private final CyApplicationManager applicationManager;

    private CyNetwork myNet;
    private CyNetworkView myView;
    private Map<String, CyNode> idMap = new HashMap<>();
    private Map<Item, List<String>> test;

    public TransformTask(CyNetworkManager netMgr, CyNetworkFactory cnf, CyApplicationManager applicationManager, Map<Item, List<String>> test) {
        this.netMgr = netMgr;
        this.cnf = cnf;
        this.test = test;
        this.applicationManager = applicationManager;
        this.myView = this.applicationManager.getCurrentNetworkView();
        this.myNet = this.myView.getModel();
        System.out.println("TransformTask");
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        System.out.println("running transform task");
        if (myNet.getDefaultNodeTable().getColumn("id") == null)
            myNet.getDefaultNodeTable().createColumn("id", String.class, true);
        if (myNet.getDefaultNodeTable().getColumn("type") == null)
            myNet.getDefaultNodeTable().createColumn("type", String.class, true);
        makeNetwork();
        updateView(myView);
        System.out.println("done transform task");
    }

    public void makeNetwork() {
        // System.out.println("HELLO: " + CyActivator.getGlobalVar());
        HashMap<String, CyNode> nodeNameMap = new HashMap<String, CyNode>();
        List<CyNode> nodeList = myNet.getNodeList();
        for (CyNode node : nodeList){
            String ip = myNet.getDefaultNodeTable().getRow(node.getSUID()).get("wdid", String.class);
            nodeNameMap.put(ip, node);
        }
        System.out.println("nodeNameMap: " + nodeNameMap);
        
        CyNode protNode;
        Iterator keys = this.test.keySet().iterator();
        while ( keys.hasNext() ){
            Item go = (Item) keys.next();
            List<String> prot = this.test.get(go);
            // go is a go term, prot is a list of proteins its connected to
            CyNode goNode;
            // Check if this node already exists
            if (nodeNameMap.containsKey(go.getWdid()))
                goNode = nodeNameMap.get(go.getWdid());
            else {
                goNode = myNet.addNode();
                myNet.getDefaultNodeTable().getRow(goNode.getSUID()).set("name", go.getName());
                myNet.getDefaultNodeTable().getRow(goNode.getSUID()).set("wdid", go.getWdid());
                myNet.getDefaultNodeTable().getRow(goNode.getSUID()).set("id", go.getId());
                myNet.getDefaultNodeTable().getRow(goNode.getSUID()).set("type", go.getType());
            }
                
            for (String p : prot) {
                protNode = nodeNameMap.get(p);
                System.out.println("protNode: " + protNode);
                System.out.println("goNode: " + goNode);
                if (!myNet.containsEdge(nodeNameMap.get(p), goNode)){
                    myNet.addEdge(nodeNameMap.get(p), goNode, false);
                }
            }
        }
        netMgr.addNetwork(myNet);
    }
    
    public static void updateView(CyNetworkView view){
        CyAppAdapter appAdapter = CyActivator.getCyAppAdapter();
        CyLayoutAlgorithmManager alMan = appAdapter.getCyLayoutAlgorithmManager();
        CyLayoutAlgorithm algor = alMan.getDefaultLayout(); // default grid layout
        TaskIterator itr = algor.createTaskIterator(view,algor.createLayoutContext(),CyLayoutAlgorithm.ALL_NODE_VIEWS,null);
        appAdapter.getTaskManager().execute(itr);// We use the synchronous task manager otherwise the visual style and updateView() may occur before the view is relayed out:
        SynchronousTaskManager<?> synTaskMan = appAdapter.getCyServiceRegistrar().getService(SynchronousTaskManager.class);           
        synTaskMan.execute(itr); 
        view.updateView(); // update view layout part
        appAdapter.getVisualMappingManager().getVisualStyle(view).apply(view); // update view style part
    }
}