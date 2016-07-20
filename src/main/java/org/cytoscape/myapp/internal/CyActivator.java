package org.cytoscape.myapp.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.cytoscape.app.CyAppAdapter;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.osgi.framework.BundleContext;

import org.cytoscape.application.swing.CyNodeViewContextMenuFactory;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.work.TaskManager;

import org.cytoscape.service.util.CyServiceRegistrar;

import org.cytoscape.session.CyNetworkNaming;

public class CyActivator extends AbstractCyActivator {

    private static CyAppAdapter appAdapter;
    private static HashMap<CyNode, Set<Property>> nodeProps = new HashMap<>(); //  a node and props for that node

    public CyActivator() {
        super();
    }

    @Override
    public void start(BundleContext bc) throws Exception {

        this.appAdapter = getService(bc, CyAppAdapter.class);
        CyApplicationManager cyApplicationManager = getService(bc, CyApplicationManager.class);
        CySwingApplication cyDesktopService = getService(bc, CySwingApplication.class);
        CyServiceRegistrar cyServiceRegistrar = getService(bc, CyServiceRegistrar.class);
        CyNetworkManager cyNetworkManagerServiceRef = getService(bc, CyNetworkManager.class);
        CyNetworkNaming cyNetworkNamingServiceRef = getService(bc, CyNetworkNaming.class);
        CyNetworkFactory cyNetworkFactoryServiceRef = getService(bc, CyNetworkFactory.class);
        TaskManager taskManager = getService(bc, TaskManager.class);
        CyEventHelper eventHelper = getService(bc, CyEventHelper.class);

        // Toolbar menu
        MenuAction action = new MenuAction(cyApplicationManager, cyNetworkManagerServiceRef, "WikiData");
        Properties properties = new Properties();
        registerAllServices(bc, action, properties);

        // Right click menu (node). Query
        CyNodeViewContextMenuFactory myNodeViewContextMenuFactory = new NodeTemplateQueryContextMenuFactory(cyNetworkManagerServiceRef,
                cyNetworkFactoryServiceRef, taskManager, cyApplicationManager, eventHelper);
        Properties myNodeViewContextMenuFactoryProps = new Properties();
        myNodeViewContextMenuFactoryProps.put("preferredMenu", "WikiData");
        myNodeViewContextMenuFactoryProps.setProperty("title", "wikidata title");
        registerAllServices(bc, myNodeViewContextMenuFactory, myNodeViewContextMenuFactoryProps);

        // Right click menu (node). Lookup
        CyNodeViewContextMenuFactory lookupNodeViewContextMenuFactory = new NodeLookupContextMenuFactory();
        Properties lookupNodeViewContextMenuFactoryProps = new Properties();
        lookupNodeViewContextMenuFactoryProps.put("preferredMenu", "WikiData");
        lookupNodeViewContextMenuFactoryProps.setProperty("title", "wikidata title");
        registerAllServices(bc, lookupNodeViewContextMenuFactory, lookupNodeViewContextMenuFactoryProps);

        // Right click menu (node). Props
        CyNodeViewContextMenuFactory propNodeViewContextMenuFactory = new NodePropQueryContextMenuFactory(cyNetworkManagerServiceRef,
                cyNetworkFactoryServiceRef, taskManager, cyApplicationManager, eventHelper);
        Properties propNodeViewContextMenuFactoryProps = new Properties();
        propNodeViewContextMenuFactoryProps.put("preferredMenu", "WikiData");
        propNodeViewContextMenuFactoryProps.setProperty("title", "wikidata title");
        registerAllServices(bc, propNodeViewContextMenuFactory, propNodeViewContextMenuFactoryProps);

        // set node visual styles
        SetVisualStyleTask setVisualStyleTask = new SetVisualStyleTask();
        taskManager.execute(setVisualStyleTask.createTaskIterator());

        System.out.println("Started Up");
    }

    public static CyAppAdapter getCyAppAdapter() {
        return appAdapter;
    }

    public static void setNodeProps(CyNode node, Set<Property> props) {
        nodeProps.put(node, props);
    }

    public static Set<Property> getNodeProps(CyNode node) {
        return nodeProps.getOrDefault(node, new HashSet<Property>());
    }

}

/*
Q511968 cdk2
Q21296321 tryptophanase
Q1147372 cdk1
Q908221 indoleamine

doesnt work: Q416356

*/
