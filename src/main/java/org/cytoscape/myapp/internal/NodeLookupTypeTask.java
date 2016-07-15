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
import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;

/**
 *
 * @author gstupp
 */
public class NodeLookupTypeTask extends AbstractTask {

    private String queryString;
    private TaskManager taskManager;
    private final CyNetworkManager netMgr;
    private final CyApplicationManager applicationManager;
    private final CyNetworkView myView;
    private final CyNetwork myNet;
    private final CyNode node;

    public NodeLookupTypeTask(String queryString, CyNode node) {
        this.queryString = queryString;
        CyAppAdapter adapter = CyActivator.getCyAppAdapter();
        this.netMgr = adapter.getCyNetworkManager();
        this.applicationManager = adapter.getCyApplicationManager();
        this.myView = this.applicationManager.getCurrentNetworkView();
        this.myNet = this.myView.getModel();
        this.node = node;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        CyAppAdapter adapter = CyActivator.getCyAppAdapter();
        this.taskManager = adapter.getTaskManager();
        doQuery(queryString, node);
    }

    private void doQuery(String queryString, CyNode node) {
        System.out.println(queryString);
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService("https://query.wikidata.org/sparql", queryString);
        ResultSet results = qexec.execSelect();
        //ResultSetFormatter.out(System.out, results, query);

        Set<String> types = new HashSet<>();
        String itemLabel = "";
        while (results.hasNext()) {
            QuerySolution statement = results.next();
            itemLabel = statement.getLiteral("itemLabel").getString();
            String valueWdid = statement.getResource("value").toString().replaceFirst("http://www.wikidata.org/entity/", "");
            types.add(valueWdid);
        }
        System.out.println("types: " + types);
        myNet.getDefaultNodeTable().getRow(node.getSUID()).set("name", itemLabel);
        if (myNet.getDefaultNodeTable().getColumn("type") == null) {
            myNet.getDefaultNodeTable().createColumn("type", String.class, true);
        }
        // set nodes type based on subclass and instance
        if (types.contains("Q8054")) // protein
        {
            myNet.getDefaultNodeTable().getRow(node.getSUID()).set("type", "protein");
        } else if (types.contains("Q2996394")) // BIO PROCESS
        {
            myNet.getDefaultNodeTable().getRow(node.getSUID()).set("type", "GO");
        } else if (types.contains("Q14860489")) // mol func
        {
            myNet.getDefaultNodeTable().getRow(node.getSUID()).set("type", "GO");
        } else if (types.contains("Q5058355")) // cell comp
        {
            myNet.getDefaultNodeTable().getRow(node.getSUID()).set("type", "GO");

        } else if (types.contains("Q898273")) // domain
        {
            myNet.getDefaultNodeTable().getRow(node.getSUID()).set("type", "domain");
        } else if (types.contains("Q7187")) // gene
        {
            myNet.getDefaultNodeTable().getRow(node.getSUID()).set("type", "gene");
        } else if (types.contains("Q11173")) // chemical compound
        {
            myNet.getDefaultNodeTable().getRow(node.getSUID()).set("type", "compound");
        }
        // else type will be blank

    }

    public TaskIterator createTaskIterator() {
        System.out.println("createTaskIterator");
        return new TaskIterator(new NodeLookupTypeTask(queryString, node));
    }
}
