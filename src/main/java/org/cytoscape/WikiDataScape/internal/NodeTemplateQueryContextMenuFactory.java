package org.cytoscape.WikiDataScape.internal;

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JMenuItem;
import javax.swing.JMenu;
import org.cytoscape.application.CyApplicationManager;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.TaskManager;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;

/**
 * This is the right click menu when clicking a node, populated with template queries
 *
 * @author gstupp
 */
public class NodeTemplateQueryContextMenuFactory implements CyNodeViewContextMenuFactory {

    private CyNetworkView netView;
    private final TaskManager taskManager;
    String prefix = "PREFIX wd: <http://www.wikidata.org/entity/>\n"
            + "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n"
            + "PREFIX wikibase: <http://wikiba.se/ontology#>\n"
            + "PREFIX p: <http://www.wikidata.org/prop/>\n"
            + "PREFIX ps: <http://www.wikidata.org/prop/statement/>\n"
            + "PREFIX pq: <http://www.wikidata.org/prop/qualifier/>\n"
            + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
            + "PREFIX bd: <http://www.bigdata.com/rdf#>\n";
    private View<CyNode> nodeView;

    public NodeTemplateQueryContextMenuFactory(CyNetworkManager cyNetworkManager, CyNetworkFactory cyNetworkFactory,
            TaskManager taskManager, CyApplicationManager applicationManager, CyEventHelper eventHelper) {
        this.taskManager = taskManager;
    }

    @Override
    public CyMenuItem createMenuItem(CyNetworkView netView, View<CyNode> nodeView) {
        this.netView = netView;
        this.nodeView = nodeView;

        JMenu root = new JMenu("Query");

        // determine which right click menu to show based on selected items (protein? drug?)
        CyNetwork myNet = this.netView.getModel();
        List<CyNode> nodes = CyTableUtil.getNodesInState(myNet, "selected", true);
        Set<String> types = new HashSet<>();
        CyTable nodeTable = myNet.getDefaultNodeTable();
        for (CyNode node : nodes) {
            String type = nodeTable.getRow(node.getSUID()).get("type", String.class);
            types.add(type);
        }
        System.out.println(types);

        if (types.contains("protein")) {

            JMenu sub = new JMenu("Protein");
            root.add(sub);

            JMenuItem menuItem = new JMenuItem("Get Cell Component");
            menuItem.addActionListener((ActionEvent e) -> {
                clicked(e, "cellComponent");
            });
            sub.add(menuItem);

            menuItem = new JMenuItem("Get Molecular Function");
            menuItem.addActionListener((ActionEvent e) -> {
                clicked(e, "molFunction");
            });
            sub.add(menuItem);

            menuItem = new JMenuItem("Get Biological Process");
            menuItem.addActionListener((ActionEvent e) -> {
                clicked(e, "bioProcess");
            });
            sub.add(menuItem);

            JMenuItem menuItem3 = new JMenuItem("Get Products");
            menuItem3.addActionListener((ActionEvent e) -> {
                clicked(e, "productFromProtein");
            });
            sub.add(menuItem3);

            JMenuItem menuItem4 = new JMenuItem("Get Protein Domains");
            menuItem4.addActionListener((ActionEvent e) -> {
                clicked(e, "domainFromProtein");
            });
            sub.add(menuItem4);

            menuItem = new JMenuItem("Get Genes");
            menuItem.addActionListener((ActionEvent e) -> {
                clicked(e, "geneFromProtein");
            });
            sub.add(menuItem);
            
            menuItem = new JMenuItem("Get Taxon");
            menuItem.addActionListener((ActionEvent e) -> {
                clicked(e, "taxonFromProtein");
            });
            sub.add(menuItem);
        }

        if (types.contains("gene")) {
            JMenu sub = new JMenu("Gene");
            root.add(sub);

            JMenuItem menuItem = new JMenuItem("Get Proteins");
            menuItem.addActionListener((ActionEvent e) -> {
                clicked(e, "proteinFromGene");
            });
            sub.add(menuItem);

            menuItem = new JMenuItem("Get Orthologs");
            menuItem.addActionListener((ActionEvent e) -> {
                clicked(e, "orthologFromGene");
            });
            sub.add(menuItem);
        }

        if (types.contains("compound")) {
            JMenu sub = new JMenu("compound");
            root.add(sub);

            JMenuItem menuItem = new JMenuItem("Get Proteins");
            menuItem.addActionListener((ActionEvent e) -> {
                clicked(e, "proteinFromCompound");
            });
            sub.add(menuItem);

        }
        if (types.contains("GO")) {
            JMenu sub = new JMenu("GO Term");
            root.add(sub);

            JMenuItem menuItem = new JMenuItem("Get Proteins");
            menuItem.addActionListener((ActionEvent e) -> {
                clicked(e, "proteinFromGO");
            });
            sub.add(menuItem);

            JMenuItem menuItem2 = new JMenuItem("Get GO Parents");
            menuItem2.addActionListener((ActionEvent e) -> {
                clicked(e, "goParent");
            });
            sub.add(menuItem2);
        }
        if (types.contains("domain")) {
            JMenu sub = new JMenu("Protein Domain");
            root.add(sub);

            JMenuItem menuItem = new JMenuItem("Get proteins containing domain");
            menuItem.addActionListener((ActionEvent e) -> {
                clicked(e, "proteinFromDomain");
            });
            sub.add(menuItem);
        }

        CyMenuItem cyMenuItem = new CyMenuItem(root, 0);
        return cyMenuItem;
    }

    private void goParent(String IDs) {
        String queryString = prefix
                + "SELECT ?item ?value ?valueLabel ?valueId WHERE {\n"
                + "  ?item wdt:P279 ?value .\n"
                + "  ?value wdt:P686 ?valueId .\n"
                + "  values ?item {%s}\n"
                + "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" }\n"
                + "}";
        queryString = String.format(queryString, IDs);
        doQuery(queryString, "GO", "parent");
    }

    private void proteinFromGO(String IDs) {
        String queryString = prefix
                + "SELECT ?item ?value ?valueLabel ?valueId WHERE {\n"
                + "  values ?item {%s}\n"
                + "  { ?value wdt:P680 ?item . }\n"
                + "  union { ?value wdt:P681 ?item . }\n"
                + "  union { ?value wdt:P682 ?item . }\n"
                + "  ?value wdt:P352 ?valueId .\n"
                + "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" }\n"
                + "}";
        queryString = String.format(queryString, IDs);
        doQuery(queryString, "protein", "has GO");
    }

    private void proteinFromCompound(String IDs) {
        String queryString = prefix
                + "SELECT ?item ?value ?valueLabel ?valueId WHERE {\n"
                + "  ?value wdt:P1056 ?item .\n"
                + "  ?value wdt:P352 ?valueId .\n"
                + "  values ?item {%s}\n"
                + "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" }\n"
                + "}";
        queryString = String.format(queryString, IDs);
        doQuery(queryString, "protein", "product");
    }
    
    private void taxonFromProtein(String IDs) {
        String queryString = prefix
                + "SELECT ?item ?value ?valueLabel ?valueId WHERE {\n"
                + "  ?item wdt:P703 ?value .\n"
                + "  ?value wdt:P685 ?valueId .\n"
                + "  values ?item {%s}\n"
                + "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" }\n"
                + "}";
        queryString = String.format(queryString, IDs);
        doQuery(queryString, "organism", "found in");
    }

    private void productFromProtein(String IDs) {
        String queryString = prefix
                + "SELECT ?item ?value ?valueLabel ?valueId WHERE {\n"
                + "  ?item wdt:P1056 ?value .\n"
                + "  ?value wdt:P662 ?valueId .\n"
                + "  values ?item {%s}\n"
                + "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" }\n"
                + "}";
        queryString = String.format(queryString, IDs);
        doQuery(queryString, "compound", "produced by");

    }

    private void cellComponent(String IDs) {
        /* Get the cell component of any items */
        String queryString = prefix
                + "SELECT ?item ?value ?valueLabel ?valueId WHERE {\n"
                + "  ?item wdt:P681 ?value .\n"
                + "  ?value wdt:P686 ?valueId .\n"
                + "  values ?item {%s}\n"
                + "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" }\n"
                + "}";
        queryString = String.format(queryString, IDs);
        doQuery(queryString, "GO", "has GO");
    }

    private void bioProcess(String IDs) {
        /* Get the biological process go terms of any items */
        String queryString = prefix
                + "SELECT ?item ?value ?valueLabel ?valueId WHERE {\n"
                + "  ?item wdt:P682 ?value .\n"
                + "  ?value wdt:P686 ?valueId .\n"
                + "  values ?item {%s}\n"
                + "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" }\n"
                + "}";
        queryString = String.format(queryString, IDs);
        doQuery(queryString, "GO", "has GO");
    }

    private void molFunction(String IDs) {
        /* Get the molecular function of any items */
        String queryString = prefix
                + "SELECT ?item ?value ?valueLabel ?valueId WHERE {\n"
                + "  ?item wdt:P680 ?value .\n"
                + "  ?value wdt:P686 ?valueId .\n"
                + "  values ?item {%s}\n"
                + "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" }\n"
                + "}";
        queryString = String.format(queryString, IDs);
        doQuery(queryString, "GO", "has GO");
    }

    private void domainFromProtein(String IDs) {
        String queryString = prefix
                + "SELECT ?item ?value ?valueLabel ?itemLabel ?valueId WHERE {\n"
                + "  values ?item {%s}\n"
                + "  ?item wdt:P527 ?value . #has part\n"
                + "  ?value wdt:P279 wd:Q898273 . #subclass of protein domain\n"
                + "  ?value wdt:P2926 ?valueId . \n"
                + "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" }\n"
                + "}";
        queryString = String.format(queryString, IDs);
        doQuery(queryString, "domain", "part of");
    }

    private void proteinFromDomain(String IDs) {
        // get all proteins containing this domain
        String queryString = prefix
                + "SELECT ?item ?value ?valueLabel ?itemLabel ?valueId WHERE {\n"
                + "  values ?item {%s}\n"
                + "  ?value wdt:P527 ?item . #has part\n"
                + "  ?value wdt:P352 ?valueId . \n"
                + "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" }\n"
                + "}";
        queryString = String.format(queryString, IDs);
        doQuery(queryString, "protein", "contains domain");
    }

    private void proteinFromGene(String IDs) {
        String queryString = prefix
                + "SELECT ?item ?value ?valueLabel ?valueId WHERE {\n"
                + "  ?item wdt:P688 ?value .\n"
                + "  ?value wdt:P352 ?valueId .\n"
                + "  values ?item {%s}\n"
                + "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" }\n"
                + "}";
        queryString = String.format(queryString, IDs);
        doQuery(queryString, "protein", "encodes");
    }

    private void orthologFromGene(String IDs) {
        String queryString = prefix
                + "SELECT ?item ?value ?valueLabel ?valueId WHERE {\n"
                + "  ?item wdt:P684 ?value .\n"
                + "  ?value wdt:P351 ?valueId .\n" // entrez
                + "  values ?item {%s}\n"
                + "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" }\n"
                + "}";
        queryString = String.format(queryString, IDs);
        doQuery(queryString, "gene", "ortholog");
    }

    private void geneFromProtein(String IDs) {
        String queryString = prefix
                + "SELECT ?item ?value ?valueLabel ?valueId WHERE {\n"
                + "  ?item wdt:P702 ?value .\n"
                + "  ?value wdt:P351 ?valueId .\n" // entrez
                + "  values ?item {%s}\n"
                + "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" }\n"
                + "}";
        queryString = String.format(queryString, IDs);
        doQuery(queryString, "gene", "encoded by");
    }

    private void doQuery(String queryString, String type, String interaction) {
        // call the template query task
        TemplateQueryTask templateQueryTask = new TemplateQueryTask(queryString, type, interaction);
        taskManager.execute(templateQueryTask.createTaskIterator());
    }

    public void clicked(ActionEvent e, String kind) {
        System.out.println("----------------------");

        // set node visual styles
        SetVisualStyleTask setVisualStyleTask = new SetVisualStyleTask();
        taskManager.execute(setVisualStyleTask.createTaskIterator());

        WikibaseDataFetcher wbdf = WikibaseDataFetcher.getWikidataDataFetcher();

        CyNetwork myNet = this.netView.getModel();
        List<CyNode> nodes = CyTableUtil.getNodesInState(myNet, "selected", true);
        String IDs = "";
        CyTable nodeTable = myNet.getDefaultNodeTable();
        for (CyNode node : nodes) {
            String gene = nodeTable.getRow(node.getSUID()).get("wdid", String.class);
            IDs = IDs.concat("wd:" + gene + " ");
        }
        System.out.println(IDs);

        switch (kind) {
            case "bioProcess":
                bioProcess(IDs);
                break;
            case "cellComponent":
                cellComponent(IDs);
                break;
            case "molFunction":
                molFunction(IDs);
                break;
            case "productFromProtein":
                productFromProtein(IDs);
                break;
            case "proteinFromCompound":
                proteinFromCompound(IDs);
                break;
            case "proteinFromGO":
                proteinFromGO(IDs);
                break;
            case "goParent":
                goParent(IDs);
                break;
            case "domainFromProtein":
                domainFromProtein(IDs);
                break;
            case "geneFromProtein":
                geneFromProtein(IDs);
                break;
            case "orthologFromGene":
                orthologFromGene(IDs);
                break;
            case "proteinFromGene":
                proteinFromGene(IDs);
                break;
            case "proteinFromDomain":
                proteinFromDomain(IDs);
                break;
            case "taxonFromProtein":
                taxonFromProtein(IDs);
                break;
            default:
                System.out.println("bad");
        }
    }
}
