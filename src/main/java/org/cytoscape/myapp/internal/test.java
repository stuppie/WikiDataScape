package org.cytoscape.myapp.internal;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;

//http://stackoverflow.com/questions/37398688/how-to-access-the-wikidata-sparql-interface-from-java
public class test {

    public static void main(String[] args) throws MediaWikiApiErrorException {

        test1();
    }

    public static void test1() throws MediaWikiApiErrorException {
        System.out.println("Running: 1");
        String gene = "Q1949517";
        WikibaseDataFetcher wbdf = WikibaseDataFetcher.getWikidataDataFetcher();
        EntityDocument q = wbdf.getEntityDocument(gene);
        List<StatementGroup> statements = ((ItemDocument) q).getStatementGroups();

        System.out.println("The English name for entity " + gene + " is "
                + ((ItemDocument) q).getLabels().get("en").getText());

        //get properties for this item
        for (StatementGroup sg : statements){
            PropertyIdValue p = sg.getProperty();
            PropertyDocument pdoc = (PropertyDocument) wbdf.getEntityDocument(p.getId());
            System.out.println(pdoc.getLabels().get("en").getText());
        }
        System.out.println("bio process");
        StatementGroup findStatementGroup = ((ItemDocument) q).findStatementGroup("P682");
        System.out.println(findStatementGroup);
        System.out.println(findStatementGroup.getStatements());

    }

    public static void test2() throws MediaWikiApiErrorException {
        System.out.println("Running: 2");
        String prefix = "PREFIX wd: <http://www.wikidata.org/entity/>\n"
                + "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n"
                + "PREFIX wikibase: <http://wikiba.se/ontology#>\n"
                + "PREFIX p: <http://www.wikidata.org/prop/>\n"
                + "PREFIX ps: <http://www.wikidata.org/prop/statement/>\n"
                + "PREFIX pq: <http://www.wikidata.org/prop/qualifier/>\n"
                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "PREFIX bd: <http://www.bigdata.com/rdf#>\n";

        String queryString = prefix
                + "SELECT * WHERE {\n"
                + "       ?gene wdt:P353 ?hgnc .\n"
                + "	VALUES ?hgnc {'FOXP3' 'CDKN2A'}\n"
                + "	SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" }\n"
                + "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService("https://query.wikidata.org/sparql", queryString);

        ResultSet results = qexec.execSelect();
        ResultSetFormatter.out(System.out, results, query);

    }
}
