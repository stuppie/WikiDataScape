package org.cytoscape.WikiDataScape.internal.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.cytoscape.WikiDataScape.internal.CyActivator;
import org.cytoscape.WikiDataScape.internal.model.Item;
import org.cytoscape.WikiDataScape.internal.model.Property;
import org.cytoscape.WikiDataScape.internal.model.Triple;
import org.cytoscape.WikiDataScape.internal.model.Triples;
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
 * Actually make the new nodes and edges
 * @author gstupp
 */
public class TransformTask_1 extends AbstractTask {

    private final CyNetworkManager netMgr;
    private final CyApplicationManager applicationManager;
    private final CyNetwork myNet;
    private final CyNetworkView myView;
    private Map<String, CyNode> idMap = new HashMap<>();
    private TaskManager taskManager;
    
    HashMap<String, CyNode> nodeWdidMap;
    List<CyNode> newNodes;
    private final Triples triples;

    public TransformTask_1(Triples triples) {
        CyAppAdapter adapter = CyActivator.getCyAppAdapter();
        this.netMgr = adapter.getCyNetworkManager();
        this.applicationManager = adapter.getCyApplicationManager();
        this.triples = triples;
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
        newNodes = new ArrayList<>();
        nodeWdidMap = new HashMap<>();
        List<CyNode> nodeList = myNet.getNodeList();
        for (CyNode node : nodeList) {
            String wdid = myNet.getDefaultNodeTable().getRow(node.getSUID()).get("wdid", String.class);
            nodeWdidMap.put(wdid, node);
        }
        System.out.println("nodeNameMap: " + nodeWdidMap);
        
        makeNetwork();
        updateView(myView);
        System.out.println("done transform task");
    }

    private CyNode makeNodeIfNotExists(String wdid){
        CyNode subjectNode;
        if (nodeWdidMap.containsKey(wdid)) {
            subjectNode = nodeWdidMap.get(wdid);
        } else {
            subjectNode = myNet.addNode();
            newNodes.add(subjectNode);
            nodeWdidMap.put(wdid, subjectNode);
            myNet.getDefaultNodeTable().getRow(subjectNode.getSUID()).set("wdid", wdid);
        }
        return subjectNode;
    }
        
    private CyNode makeNodeIfNotExists(Item subject){
        
        CyNode subjectNode;
        if (nodeWdidMap.containsKey(subject.getWdid())) {
            subjectNode = nodeWdidMap.get(subject.getWdid());
        } else {
            subjectNode = myNet.addNode();
            newNodes.add(subjectNode);
            nodeWdidMap.put(subject.getWdid(), subjectNode);
            myNet.getDefaultNodeTable().getRow(subjectNode.getSUID()).set("name", subject.getName());
            myNet.getDefaultNodeTable().getRow(subjectNode.getSUID()).set("wdid", subject.getWdid());
            myNet.getDefaultNodeTable().getRow(subjectNode.getSUID()).set("type", subject.getType());               
        }
        return subjectNode;
    }
    
    public void makeNetwork() {
        CyNode subjNode;
        CyNode objNode;
        Iterator<Item> subjects = triples.getSubjects().iterator();
        
        // subject, predicate, object
        while (subjects.hasNext()) {
            Item subject = (Item) subjects.next(); // This is the item (node) we are now building direct edges with
            HashSet<Triple> subjectTriples = triples.getSubjectTriples(subject).getTriplesFlat();
            
            subjNode = makeNodeIfNotExists(subject);
            
            for (Triple triple : subjectTriples) {
                Item obj = triple.getObject();
                objNode = makeNodeIfNotExists(obj);
                System.out.println("subjNode: " + subjNode);
                System.out.println("objNode: " + objNode);
                if (!myNet.containsEdge(objNode, subjNode)) {
                    myNet.addEdge(subjNode, objNode, false);
                    Property prop = triple.getPredicate();
                    List<CyEdge> connectingEdgeList = myNet.getConnectingEdgeList(objNode, subjNode, CyEdge.Type.ANY);
                    if (connectingEdgeList.size() != 1){
                        // TODO: This'll fail if we have more than one edge connecting two nodes
                        System.out.println("more than one edge connecting two nodes");
                    }
                    myNet.getDefaultEdgeTable().getRow(connectingEdgeList.get(0).getSUID()).set("interaction", prop.getName());
                    // Name the edge
                    String node1 = myNet.getDefaultNodeTable().getRow(subjNode.getSUID()).get("name", String.class);
                    String node2 = myNet.getDefaultNodeTable().getRow(objNode.getSUID()).get("name", String.class);
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
        return new TaskIterator(new TransformTask_1(triples));
    }
}
