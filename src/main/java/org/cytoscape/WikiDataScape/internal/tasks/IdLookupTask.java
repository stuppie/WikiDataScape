package org.cytoscape.WikiDataScape.internal.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.Iterator;
import org.apache.jena.query.ResultSetFormatter;
import org.cytoscape.WikiDataScape.internal.CyActivator;

/**
 *
 * @author gstupp
 */
public class IdLookupTask extends AbstractTask {

    private final TaskManager taskManager;
    private final CyNetworkManager netMgr;
    private final CyApplicationManager applicationManager;
    private final CyNetworkView myView;
    private final CyNetwork myNet;
    private final String[] ids;
    private final String db;
    private BiMap<String, String> idWdid;
    private Map<String, String> wdid2label;
    String idString;

    String prefix = "PREFIX wd: <http://www.wikidata.org/entity/>\n"
            + "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n"
            + "PREFIX wikibase: <http://wikiba.se/ontology#>\n"
            + "PREFIX p: <http://www.wikidata.org/prop/>\n"
            + "PREFIX ps: <http://www.wikidata.org/prop/statement/>\n"
            + "PREFIX pq: <http://www.wikidata.org/prop/qualifier/>\n"
            + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
            + "PREFIX bd: <http://www.bigdata.com/rdf#>\n";

    public IdLookupTask(String[] ids, String db) {
        CyAppAdapter adapter = CyActivator.getCyAppAdapter();
        this.taskManager = adapter.getTaskManager();
        this.netMgr = adapter.getCyNetworkManager();
        this.applicationManager = adapter.getCyApplicationManager();
        this.myView = this.applicationManager.getCurrentNetworkView();
        this.myNet = this.myView.getModel();
        
        this.ids = ids;
        this.db = db;
        idWdid = HashBiMap.create();
        wdid2label = new HashMap<>();
        idString = "\"" + String.join("\" \"", ids) + "\"";
        
        CyTable nodeTable = myNet.getDefaultNodeTable();
        if (nodeTable.getColumn("wdid") == null)
            nodeTable.createColumn("wdid", String.class, true);
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        doQuery();
        System.out.println("idWdid: " + idWdid);
        makeNodes();
    }

    private void doQuery() {
        // Get all properties for all selected nodes

        String queryString = prefix
                + "SELECT ?item ?ids ?itemLabel WHERE\n"
                + String.format("{values ?ids {%s}\n", idString)
                + String.format("?item wdt:%s ?ids\n", db)
                + "SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" }}";
        System.out.println(queryString);
        //Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService("https://query.wikidata.org/sparql", queryString);
        ResultSet results = qexec.execSelect();
        //ResultSetFormatter.out(System.out, results, query);

        while (results.hasNext()) {
            QuerySolution statement = results.next();
            String id = statement.getLiteral("ids").getString();
            String itemLabel = statement.getLiteral("itemLabel").getString();
            String wdid = statement.getResource("item").toString().replaceFirst("http://www.wikidata.org/entity/", "");
            idWdid.put(id, wdid);
            wdid2label.put(wdid, itemLabel);
        }
    }

    private void makeNodes() {
        HashMap<String, CyNode> nodeNameMap = new HashMap<String, CyNode>();
        List<CyNode> nodeList = myNet.getNodeList();
        for (CyNode node : nodeList) {
            String wdid = myNet.getDefaultNodeTable().getRow(node.getSUID()).get("wdid", String.class);
            nodeNameMap.put(wdid, node);
        }
        System.out.println("nodeNameMap: " + nodeNameMap);
        System.out.println("wdid2label: " + wdid2label);
        List<CyNode> newNodes = new ArrayList<>();
        Iterator wdids = this.idWdid.inverse().keySet().iterator();
        while (wdids.hasNext()) {
            String wdid = (String) wdids.next();
            CyNode node;
            // Check if this node already exists
            if (! nodeNameMap.containsKey(wdid)) {
                node = myNet.addNode();
                newNodes.add(node);
                myNet.getDefaultNodeTable().getRow(node.getSUID()).set("wdid", wdid);
                myNet.getDefaultNodeTable().getRow(node.getSUID()).set("name", wdid2label.get(wdid));
            }
        }
        netMgr.addNetwork(myNet);
        myView.updateView();
        
        // keep track of new nodes and run the lookup task on them
        NodeLookupTask nodeLookupTask = new NodeLookupTask(newNodes);
        taskManager.execute(nodeLookupTask.createTaskIterator());
    }

    public TaskIterator createTaskIterator() {
        System.out.println("createTaskIterator");
        return new TaskIterator(new IdLookupTask(ids, db));
    }
}
