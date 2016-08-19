package org.cytoscape.WikiDataScape.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.cytoscape.WikiDataScape.internal.model.Triples;
import org.cytoscape.WikiDataScape.internal.tasks.NodeLookupTask;
import org.cytoscape.app.CyAppAdapter;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.osgi.framework.BundleContext;

import org.cytoscape.application.swing.CyNodeViewContextMenuFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.TaskManager;

import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;

public class CyActivator extends AbstractCyActivator {

    private static CyAppAdapter appAdapter;

    //  a node and if its done the "what links here" query
    public static HashMap<CyNode, Boolean> nodeDoneWhatLinks = new HashMap<>();

    public static Triples triples = new Triples();

    public CyActivator() {
        super();
    }

    @Override
    public void start(BundleContext bc) throws Exception {

        this.appAdapter = getService(bc, CyAppAdapter.class);
        CyApplicationManager cyApplicationManager = getService(bc, CyApplicationManager.class);
        CyNetworkManager cyNetworkManagerServiceRef = getService(bc, CyNetworkManager.class);
        TaskManager taskManager = getService(bc, TaskManager.class);

        // Toolbar menu: item lookup
        ItemLookupMenuAction itemLookupMenuAction = new ItemLookupMenuAction(cyApplicationManager, cyNetworkManagerServiceRef, "WikiData Item Search", ItemLookupDialog.class);
        registerAllServices(bc, itemLookupMenuAction, new Properties());

        // Toolbar menu: ID list lookup
        ItemLookupMenuAction itemListLookupMenuAction = new ItemLookupMenuAction(cyApplicationManager, cyNetworkManagerServiceRef, "WikiData Multi-Item Lookup", ItemListLookupDialog.class);
        registerAllServices(bc, itemListLookupMenuAction, new Properties());

        // Right click menu (node). Lookup
        CyNodeViewContextMenuFactory lookupNodeViewContextMenuFactory = new NodeLookupContextMenuFactory();
        Properties lookupNodeViewContextMenuFactoryProps = new Properties();
        lookupNodeViewContextMenuFactoryProps.put("preferredMenu", "WikiData");
        lookupNodeViewContextMenuFactoryProps.setProperty("title", "wikidata title");
        registerAllServices(bc, lookupNodeViewContextMenuFactory, lookupNodeViewContextMenuFactoryProps);
        
        // Right click menu (node). Props
        CyNodeViewContextMenuFactory propNodeViewContextMenuFactory = new NodePropQueryContextMenuFactory(taskManager);
        Properties propNodeViewContextMenuFactoryProps = new Properties();
        propNodeViewContextMenuFactoryProps.put("preferredMenu", "WikiData");
        propNodeViewContextMenuFactoryProps.setProperty("title", "wikidata title");
        registerAllServices(bc, propNodeViewContextMenuFactory, propNodeViewContextMenuFactoryProps);

        // Right click menu (node). Inverse Props
        CyNodeViewContextMenuFactory propInverseNodeViewContextMenuFactory = new NodeInversePropQueryContextMenuFactory(taskManager);
        Properties propInverseNodeViewContextMenuFactoryProps = new Properties();
        propInverseNodeViewContextMenuFactoryProps.put("preferredMenu", "WikiData");
        propInverseNodeViewContextMenuFactoryProps.setProperty("title", "wikidata title");
        registerAllServices(bc, propInverseNodeViewContextMenuFactory, propInverseNodeViewContextMenuFactoryProps);

        // Right click menu (node). Browse WikiData
        BrowseContextMenu browseContextMenu = new BrowseContextMenu();
        Properties browseContextMenuProps = new Properties();
        browseContextMenuProps.put("preferredMenu", "WikiData");
        registerAllServices(bc, browseContextMenu, browseContextMenuProps);

        System.out.println("WikiDataScape Started Up");
    }

    public static CyAppAdapter getCyAppAdapter() {
        return appAdapter;
    }

    public static void makeNewNodes(String[] wdids) {
        CyAppAdapter adapter = CyActivator.getCyAppAdapter();
        TaskManager taskManager = adapter.getTaskManager();
        CyApplicationManager applicationManager = adapter.getCyApplicationManager();
        CyNetworkView myView = applicationManager.getCurrentNetworkView();
        CyNetwork myNet = myView.getModel();
        CyNetworkManager cyNetworkManager = adapter.getCyNetworkManager();
        CyTable nodeTable = myNet.getDefaultNodeTable();
        if (nodeTable.getColumn("wdid") == null) {
            nodeTable.createColumn("wdid", String.class, true);
        }

        List<CyNode> newNodes = new ArrayList<>();

        HashMap<String, CyNode> nodeNameMap = new HashMap<>();
        List<CyNode> nodeList = myNet.getNodeList();
        for (CyNode node : nodeList) {
            String wdid = myNet.getDefaultNodeTable().getRow(node.getSUID()).get("wdid", String.class);
            nodeNameMap.put(wdid, node);
        }

        for (String wdid : wdids) {
            // Check if this node already exists
            if (!nodeNameMap.containsKey(wdid)) {
                CyNode node = myNet.addNode();
                newNodes.add(node);
                myNet.getDefaultNodeTable().getRow(node.getSUID()).set("wdid", wdid);
            }
        }
        cyNetworkManager.addNetwork(myNet);
        CyActivator.updateView(myView);

        // keep track of new nodes and run the lookup task on them
        if (!newNodes.isEmpty()) {
            NodeLookupTask nodeLookupTask = new NodeLookupTask(newNodes);
            taskManager.execute(nodeLookupTask.createTaskIterator());
        }
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

}
