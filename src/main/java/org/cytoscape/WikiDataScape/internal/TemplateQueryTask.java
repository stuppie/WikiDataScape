package org.cytoscape.WikiDataScape.internal;

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
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;

/**
 *
 * @author gstupp
 */
public class TemplateQueryTask extends AbstractTask {

    private String type;
    private String queryString;
    private String interaction;
    private TaskManager taskManager;

    public TemplateQueryTask(String queryString, String type, String interaction) {
        this.queryString = queryString;
        this.type = type; // this is the type of the new nodes
        this.interaction = interaction; // the name of the relationship that is formed
        System.out.println("TemplateQueryTask");
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        System.out.println("running template query task");
        CyAppAdapter adapter = CyActivator.getCyAppAdapter();
        this.taskManager = adapter.getTaskManager();
        doQuery(queryString, type, interaction);
        System.out.println("done template query task");
    }

    private void doQuery(String queryString, String type, String interaction) {
        System.out.println(queryString);
        Map<Item, List<Item>> valueProt = new HashMap<>();
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService("https://query.wikidata.org/sparql", queryString);
        ResultSet results = qexec.execSelect();
        //ResultSetFormatter.out(System.out, results, query);

        while (results.hasNext()) {
            QuerySolution statement = results.next();
            System.out.println("statement: " + statement.toString());
            String wdid = statement.getResource("item").getLocalName();
            // TODO: check this
            Item item = new Item("", "", wdid);
            String valueId = statement.getLiteral("valueId").getString();
            String valueWdid = statement.getResource("value").getLocalName();
            String valueLabel = statement.getLiteral("valueLabel").getString();

            Item gt = new Item(valueLabel, type, valueWdid);
            if (!valueProt.containsKey(gt)) {
                valueProt.put(gt, new ArrayList<Item>());
            }
            valueProt.get(gt).add(item);
        }
        //System.out.println("goProt: " + valueProt);

        TransformTask transformTask = new TransformTask(valueProt, interaction);
        taskManager.execute(transformTask.createTaskIterator());
    }

    public TaskIterator createTaskIterator() {
        System.out.println("createTaskIterator");
        return new TaskIterator(new TemplateQueryTask(queryString, type, interaction));
    }
}
