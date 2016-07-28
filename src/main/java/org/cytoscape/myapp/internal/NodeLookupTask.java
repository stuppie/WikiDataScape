package org.cytoscape.myapp.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.stream.Collectors;
import org.apache.jena.rdf.model.Resource;

/**
 *
 * @author gstupp
 */
public class NodeLookupTask extends AbstractTask {

    private String queryString;
    private TaskManager taskManager;
    private final CyNetworkManager netMgr;
    private final CyApplicationManager applicationManager;
    private final CyNetworkView myView;
    private final CyNetwork myNet;
    private final List<CyNode> nodes;
    private BiMap<CyNode, String> nodeWdid;
    
    String prefix = "PREFIX wd: <http://www.wikidata.org/entity/>\n"
            + "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n"
            + "PREFIX wikibase: <http://wikiba.se/ontology#>\n"
            + "PREFIX p: <http://www.wikidata.org/prop/>\n"
            + "PREFIX ps: <http://www.wikidata.org/prop/statement/>\n"
            + "PREFIX pq: <http://www.wikidata.org/prop/qualifier/>\n"
            + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
            + "PREFIX bd: <http://www.bigdata.com/rdf#>\n"
            + "PREFIX hint: <http://www.bigdata.com/queryHints#>\n";

    public NodeLookupTask(List<CyNode> nodes) {
        this.queryString = queryString;
        CyAppAdapter adapter = CyActivator.getCyAppAdapter();
        this.netMgr = adapter.getCyNetworkManager();
        this.applicationManager = adapter.getCyApplicationManager();
        this.myView = this.applicationManager.getCurrentNetworkView();
        this.myNet = this.myView.getModel();
        this.nodes = nodes;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        CyAppAdapter adapter = CyActivator.getCyAppAdapter();
        this.taskManager = adapter.getTaskManager();

        nodeWdid = HashBiMap.create();
        CyTable nodeTable = myNet.getDefaultNodeTable();
        
        String IDs = "";
        for (CyNode node : nodes) {
            String wdid = nodeTable.getRow(node.getSUID()).get("wdid", String.class);
            IDs = IDs.concat("wd:" + wdid + " ");
            nodeWdid.put(node, wdid); // map ID to node
        }
        System.out.println("nodeWdid" + nodeWdid);
        
        doQuery(IDs);

    }
    
    private void doQuery(String IDs){
        // Get all properties for all selected nodes
        
        // String.format("  values ?item {%s}\n", IDs) +
        String queryString = prefix + 
                "SELECT distinct ?item ?prop ?propLabel ?vals (datatype (?vals) AS ?type) WHERE {\n" +
                "  hint:Query hint:optimizer \"None\" .\n" +
                String.format("  values ?item {%s}\n", IDs) +
                "  ?item ?p ?vals .\n" +
                "  ?prop wikibase:directClaim ?p .\n" +
                "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" }\n" +
                "}";
                
        System.out.println(queryString);
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService("https://query.wikidata.org/sparql", queryString);
        ResultSet results = qexec.execSelect();
        //ResultSetFormatter.out(System.out, results, query);
        
        Map<CyNode, Set<Property>> nodeProps = new HashMap<>();
        Map<CyNode, Set<Property>> nodeIdProps = new HashMap<>();
        while (results.hasNext()) {
            QuerySolution statement = results.next();
            String propLabel = statement.getLiteral("propLabel").getString();
            String prop = statement.getResource("prop").getLocalName();
            String valueWdid = statement.getResource("item").getLocalName();
            Resource typeResource = statement.getResource("type");
            
            CyNode thisNode = this.nodeWdid.inverse().get(valueWdid);
            if (typeResource != null){
                if ("string".equals(typeResource.getLocalName())){
                    // This is an ID property. Add it to the table, don't add it to the right-click menu
                    // TODO: Add to node table
                    // System.out.println("string statement: " + statement);
                    String value = statement.getLiteral("vals").getString();
                    updateNodeTable(thisNode, propLabel, value);
                    
                    if (!nodeIdProps.containsKey(thisNode))
                        nodeIdProps.put(thisNode, new HashSet<>());
                    nodeIdProps.get(thisNode).add(new Property(propLabel, prop));
                } else {
                    // ignore it (example: http://www.wikidata.org/entity/P1121)
                }
            } else {
                // The property may still not be wikidata item
                // example http://www.wikidata.org/entity/P117
                // System.out.println("prop statement: " + statement);
                String valNameSpace = statement.getResource("vals").getNameSpace();
                if ("http://www.wikidata.org/entity/".equals(valNameSpace)){
                    // This property links to a wikidata item. Add it to the right-click menu
                    // Todo: keep a count of the number of items it links to
                    if (!nodeProps.containsKey(thisNode))
                        nodeProps.put(thisNode, new HashSet<>());
                    nodeProps.get(thisNode).add(new Property(propLabel, prop));
                }
            }
        }
        System.out.println("nodeProps: " + nodeProps);
        
        // Set the node properties for each node and lookup the node's type based on them
        for (CyNode thisNode: nodeProps.keySet()){
            CyActivator.setNodeProps(thisNode, nodeProps.get(thisNode));
            String type = propertiesToType(nodeIdProps.get(thisNode));
            myNet.getDefaultNodeTable().getRow(thisNode.getSUID()).set("type", type);
        }
    }
    
    private String propertiesToType(Set<Property> properties){
        Set<String> props = properties.stream().map(Property::getID).collect(Collectors.toSet());
        // Accepts a list of properties. Outputs the type
        if (props.contains("P715"))  //Drugbank ID
            return "compound";
        if (props.contains("P592"))  //ChEMBL ID
            return "compound";
        if (props.contains("P352"))  //UniProt ID
            return "protein";
        if (props.contains("P637"))  //RefSeq Protein ID
            return "protein";
        if (props.contains("P354"))  //HGNC ID
            return "gene";
        if (props.contains("P353"))  //HGNC gene symbol
            return "gene";
        if (props.contains("P351"))  //Entrez Gene ID
            return "gene";
        if (props.contains("P685"))  //NCBI Taxonomy ID
            return "organism";
        if (props.contains("P686"))  //Gene Ontology ID
            return "GO";
        if (props.contains("P2926")) //InterPro ID
            return "interpro";
        return "unknown";
    }
    
    private void updateNodeTable(CyNode node, String propLabel, String value){
        // TODO: If there are multiple values, they will get overwritten
        if (myNet.getDefaultNodeTable().getColumn(propLabel) == null)
            myNet.getDefaultNodeTable().createColumn(propLabel, String.class, false);
        
        myNet.getDefaultNodeTable().getRow(node.getSUID()).set(propLabel, value);
    }
    

    public TaskIterator createTaskIterator() {
        System.out.println("createTaskIterator");
        return new TaskIterator(new NodeLookupTask(nodes));
    }
}