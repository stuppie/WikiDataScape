package org.cytoscape.myapp.internal;

import java.awt.event.ActionEvent;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import java.io.IOException;
import java.math.BigDecimal;

import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentProcessor;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.QuantityValue;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import org.cytoscape.model.CyNetworkManager;

import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

/**
 * Creates a new menu item under Apps menu section.
 *
 */
public class MenuAction extends AbstractCyAction {

    private final CyApplicationManager applicationManager;
    private final CyNetworkManager cyNetworkManager;

    public MenuAction(final CyApplicationManager applicationManager, CyNetworkManager cyNetworkManager, final String menuTitle) {
        super(menuTitle, applicationManager, null, null);
        this.applicationManager = applicationManager;
        this.cyNetworkManager = cyNetworkManager;
        setPreferredMenu("Apps");
    }
    public void actionPerformed(ActionEvent e) {

        final CyNetworkView currentNetworkView = applicationManager.getCurrentNetworkView();
        if (currentNetworkView == null) {
            return;
        }

        // View is always associated with its model.
        final CyNetwork network = currentNetworkView.getModel();
        for (CyNode node : network.getNodeList()) {

            if (network.getNeighborList(node, CyEdge.Type.ANY).isEmpty()) {
                currentNetworkView.getNodeView(node).setVisualProperty(BasicVisualLexicon.NODE_VISIBLE, false);
            }
        }
        currentNetworkView.updateView();
    }

}
