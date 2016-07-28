package org.cytoscape.myapp.internal;

import java.awt.event.ActionEvent;
import java.util.List;
import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.work.TaskManager;


/**
 * Creates a new menu item under Apps menu section.
 * When clicking this, you get a box allowing you to create nodes from a list of identifiers
 */
public class MenuAction extends AbstractCyAction {

    private final CyApplicationManager applicationManager;
    private final CyNetworkManager cyNetworkManager;
    private final TaskManager taskManager;

    public MenuAction(final CyApplicationManager applicationManager, CyNetworkManager cyNetworkManager, final String menuTitle) {
        super(menuTitle, applicationManager, null, null);
        this.applicationManager = applicationManager;
        this.cyNetworkManager = cyNetworkManager;
        setPreferredMenu("Apps");
        
        CyAppAdapter adapter = CyActivator.getCyAppAdapter();
        this.taskManager = adapter.getTaskManager();
    }
    public void actionPerformed(ActionEvent e) {

        final CyNetworkView currentNetworkView = applicationManager.getCurrentNetworkView();
        if (currentNetworkView == null) {
            return;
        }
        IdLookupDialog x = new IdLookupDialog();
        if (x.getResult()==0){
            System.out.println(x.getIds());
            System.out.println(x.getDb());
            String[] ids = x.getIds().split("\n");
            String db = x.getDb();
            
            IdLookupTask idLookupTask = new IdLookupTask(ids, db);
            taskManager.execute(idLookupTask.createTaskIterator());
        
        }


        //currentNetworkView.updateView();
    }

}
