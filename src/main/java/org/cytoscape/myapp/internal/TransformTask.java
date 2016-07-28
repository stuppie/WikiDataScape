/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cytoscape.myapp.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;

/**
 *
 * @author gstupp
 */
public class TransformTask extends AbstractTask {

    private final CyNetworkManager netMgr;
    private final CyApplicationManager applicationManager;
    private final CyNetwork myNet;
    private final CyNetworkView myView;
    private Map<String, CyNode> idMap = new HashMap<>();
    private Map<Item, List<String>> test;
    private String interaction;
    private TaskManager taskManager;

    public TransformTask(Map<Item, List<String>> test, String interaction) {
        CyAppAdapter adapter = CyActivator.getCyAppAdapter();
        this.netMgr = adapter.getCyNetworkManager();
        this.applicationManager = adapter.getCyApplicationManager();
        this.test = test;
        this.interaction = interaction;
        this.myView = this.applicationManager.getCurrentNetworkView();
        this.myNet = this.myView.getModel();
        System.out.println("TransformTask");
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        System.out.println("running transform task");
        CyAppAdapter adapter = CyActivator.getCyAppAdapter();
        this.taskManager = adapter.getTaskManager();
        if (myNet.getDefaultNodeTable().getColumn("id") == null) {
            myNet.getDefaultNodeTable().createColumn("id", String.class, true);
        }
        if (myNet.getDefaultNodeTable().getColumn("type") == null) {
            myNet.getDefaultNodeTable().createColumn("type", String.class, true);
        }
        makeNetwork();
        updateView(myView);
        System.out.println("done transform task");
    }

    public void makeNetwork() {
        // System.out.println("HELLO: " + CyActivator.getGlobalVar());
        HashMap<String, CyNode> nodeNameMap = new HashMap<String, CyNode>();
        List<CyNode> nodeList = myNet.getNodeList();
        for (CyNode node : nodeList) {
            String ip = myNet.getDefaultNodeTable().getRow(node.getSUID()).get("wdid", String.class);
            nodeNameMap.put(ip, node);
        }
        System.out.println("nodeNameMap: " + nodeNameMap);
        
        List<CyNode> newNodes = new ArrayList<>();
        CyNode protNode;
        Iterator keys = this.test.keySet().iterator();
        while (keys.hasNext()) {
            Item go = (Item) keys.next();
            List<String> prot = this.test.get(go);
            // go is a go term, prot is a list of proteins its connected to
            CyNode goNode;
            // Check if this node already exists
            if (nodeNameMap.containsKey(go.getWdid())) {
                goNode = nodeNameMap.get(go.getWdid());
            } else {
                goNode = myNet.addNode();
                newNodes.add(goNode);
                myNet.getDefaultNodeTable().getRow(goNode.getSUID()).set("name", go.getName());
                myNet.getDefaultNodeTable().getRow(goNode.getSUID()).set("wdid", go.getWdid());
                myNet.getDefaultNodeTable().getRow(goNode.getSUID()).set("id", go.getId());
                myNet.getDefaultNodeTable().getRow(goNode.getSUID()).set("type", go.getType());               
            }

            for (String p : prot) {
                protNode = nodeNameMap.get(p);
                System.out.println("protNode: " + protNode);
                System.out.println("goNode: " + goNode);
                if (!myNet.containsEdge(protNode, goNode)) {
                    myNet.addEdge(protNode, goNode, false);
                    List<CyEdge> connectingEdgeList = myNet.getConnectingEdgeList(protNode, goNode, CyEdge.Type.ANY);
                    if (connectingEdgeList.size() != 1){
                        // TODO: This'll fail if we have more than one edge connecting two nodes
                        System.out.println("more than one edge connecting two nodes");
                    }
                    myNet.getDefaultEdgeTable().getRow(connectingEdgeList.get(0).getSUID()).set("interaction", interaction);
                    // Name the edge
                    String node1 = myNet.getDefaultNodeTable().getRow(protNode.getSUID()).get("name", String.class);
                    String node2 = myNet.getDefaultNodeTable().getRow(goNode.getSUID()).get("name", String.class);
                    String edgeName = node1 + " <-> " + node2;
                    myNet.getDefaultEdgeTable().getRow(connectingEdgeList.get(0).getSUID()).set("name", edgeName);
                }
            }
        }
        netMgr.addNetwork(myNet);
        // keep track of new nodes and run the lookup task on them
        NodeLookupTask nodeLookupTask = new NodeLookupTask(newNodes);
        taskManager.execute(nodeLookupTask.createTaskIterator());
    }

    public static void updateView(CyNetworkView view) {
        CyAppAdapter appAdapter = CyActivator.getCyAppAdapter();
        
        CyLayoutAlgorithmManager alMan = appAdapter.getCyLayoutAlgorithmManager();
        CyLayoutAlgorithm algor = alMan.getDefaultLayout(); // default grid layout
        TaskIterator itr = algor.createTaskIterator(view, algor.createLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS, null);
        appAdapter.getTaskManager().execute(itr);// We use the synchronous task manager otherwise the visual style and updateView() may occur before the view is relayed out:
        
        SynchronousTaskManager<?> synTaskMan = appAdapter.getCyServiceRegistrar().getService(SynchronousTaskManager.class);
        synTaskMan.execute(itr);
        
        view.updateView(); // update view layout part
        appAdapter.getVisualMappingManager().getVisualStyle(view).apply(view); // update view style part
        
    }

    public TaskIterator createTaskIterator() {
        return new TaskIterator(new TransformTask(test, interaction));
    }
}
