package org.cytoscape.WikiDataScape.internal;

import org.cytoscape.WikiDataScape.internal.tasks.IdLookupTask;
import java.awt.event.ActionEvent;
import java.util.List;
import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.TaskManager;


/**
 * Creates a new menu item under Apps menu section.
 * When clicking this, you get a box allowing you to search for a node
 */
public class ItemLookupMenuAction extends AbstractCyAction {

    private final CyApplicationManager applicationManager;
    private final CyNetworkManager cyNetworkManager;
    private final TaskManager taskManager;

    public ItemLookupMenuAction(final CyApplicationManager applicationManager, CyNetworkManager cyNetworkManager, final String menuTitle) {
        super(menuTitle, applicationManager, null, null);
        this.applicationManager = applicationManager;
        this.cyNetworkManager = cyNetworkManager;
        setPreferredMenu("Apps");
        
        CyAppAdapter adapter = CyActivator.getCyAppAdapter();
        this.taskManager = adapter.getTaskManager();
    }
    public void actionPerformed(ActionEvent e) {

        CyNetworkView currentNetworkView = applicationManager.getCurrentNetworkView();
        
        if (currentNetworkView == null) {
            System.out.println("currentNetworkView: " + currentNetworkView);
            CyAppAdapter adapter = CyActivator.getCyAppAdapter();
            CyNetworkFactory cyNetworkFactory = adapter.getCyNetworkFactory();
            CyNetworkViewFactory cyNetworkViewFactory = adapter.getCyNetworkViewFactory();
            CyNetworkViewManager cyNetworkViewManager = adapter.getCyNetworkViewManager();
            
            CyNetwork newNetwork = cyNetworkFactory.createNetwork();
            cyNetworkManager.addNetwork(newNetwork);
            applicationManager.setCurrentNetwork(newNetwork);
            
            CyNetworkView newNetworkView = cyNetworkViewFactory.createNetworkView(newNetwork);
            cyNetworkViewManager.addNetworkView(newNetworkView, true);
                        
            System.out.println("newNetworkView: " + newNetworkView);
        }
        
        
        ItemLookupDialog itemLookupDialog = new ItemLookupDialog();
        //currentNetworkView.updateView();
    }

}
