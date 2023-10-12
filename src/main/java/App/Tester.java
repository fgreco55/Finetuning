/*
 Simple test program... still need to test LLM methods -fdg
 */
package App;

import Model.LLM;
import Model.LLMCompletionException;
import Utilities.Utility;
import Database.VectorDB;
import Database.VectorDBException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Tester {
    static final String MILVUS_DATABASE = "frankdb";    // default DB
    static final String COLLECTION_NAME = "frankcollection";
    private final static String DEFAULT_CONFIG = "./src/main/resources/llm.properties";
    private final static String PREAMBLE = "./src/main/resources/preamble.txt";
    static final int OPENAI_VECSIZE = 1536;


    public static void main(String[] args) throws VectorDBException, IOException {
        Utility util = new Utility();
        VectorDB vdb = new VectorDB(MILVUS_DATABASE);
        LLM model = new LLM(DEFAULT_CONFIG);

        Tester mytest = new Tester();

        vdb.show_databases();
        if (!vdb.databaseExists(MILVUS_DATABASE)) {
            vdb.create_database(MILVUS_DATABASE);
        }
        System.out.println("COLLECTIONS");
        vdb.show_collections();
        if (vdb.collectionExists(COLLECTION_NAME) == true) {
            System.out.println("collection [" + COLLECTION_NAME + "] does exist");
        } else {
            System.out.println("collection [" + COLLECTION_NAME + "] does NOT exist.  Creating...");
            vdb.create_collection(COLLECTION_NAME, OPENAI_VECSIZE);
        }

        System.out.println("Populating collection [" + COLLECTION_NAME + "] ====================================");

        // now add sentences from testfile
        //vdb.show_collection_stats(COLLECTION_NAME);
        //System.out.println("Collection [" + COLLECTION_NAME + "] has [" + vdb.getCollectionRowCount(COLLECTION_NAME) + "] rows");

        model.setModel("text-embedding-ada-002");       // try embedding
        List<String> sents = util.TextfiletoList("./src/main/resources/faq.txt");
        System.out.println("DEBUG: rows: " + sents.size());
        sents.forEach(System.out::println);

        List<Long> ids = new ArrayList<>();
        List<List<Float>> veclist = new ArrayList<>();
        for (int i = 0; i < sents.size(); i++) {
            String s = sents.get(i);
            List<Float> f = model.sendEmbeddingRequest(s);
            veclist.add(f);
            ids.add(Long.parseLong(1000 + i + ""));
        }
        try {
            vdb.insert_collection(COLLECTION_NAME, ids, sents, veclist);
            System.out.println("[" + COLLECTION_NAME + "] has " + vdb.getCollectionRowCount(COLLECTION_NAME) + " rows.");
        } catch (VectorDBException vex) {
            System.err.println("***ERROR: main() - Cannot insert collection");
        }

        /* Test simple query match  */
        //vdb.queryDB(COLLECTION_NAME, "sentence_id > 10 and sentence_id < 30", 10L);

        System.out.println("=============================================");
        System.out.println("=============================================");
        //String target = "Why does the NYJavaSIG exist?";
        String target = "Has Matt Raible ever spoken at a NYJavaSIG meeting";
        System.out.println("QUERY: " + target);
        try {
            System.out.println(mytest.getCompletion(model, vdb, target));
        } catch (LLMCompletionException lx) {
            System.err.println("***ERROR... Cannot complete user's query.");
        }
        System.out.println("=============================================");
        System.out.println("=============================================");
    }

    /************************************************************
     getCompletion() - just a convenience method
     ***********************************************************/
    public String getCompletion(LLM m, VectorDB v, String userquery) throws LLMCompletionException {
        Utility util = new Utility();

        // Create list of float arrays (list of vectors)... but only need one here
        List<List<Float>> smallvec = new ArrayList<>();
        smallvec.add(m.sendEmbeddingRequest(userquery));

        List<String> match;
        match = v.searchDB_using_targetvectors(COLLECTION_NAME, smallvec, 5);
        //System.out.println("Finding nearest neighbors for [" + userquery + "]... \nSTART-----------------------");
        //match.forEach(System.out::println);     // These are the top "max" nearest neighbors
        //System.out.println("Finding nearest neighbors... \nEND---------------------------");

        String bigprompt = "";
        bigprompt = util.TextfiletoString(PREAMBLE);
        //System.out.println("DEBUG: [" + bigprompt + " ]");
        bigprompt += util.createBigString(match);
        bigprompt += userquery;
        // System.out.println("BIGPROMPT [" + bigprompt + "]");

        m.setModel("gpt-3.5-turbo");       // completion model
        String llmresponse = "";
        try {
            llmresponse = m.sendCompletionRequest("user", bigprompt);
        } catch (LLMCompletionException lex) {
            System.err.println("***ERROR: Cannot send bigprompt to LLM");
        }
        return llmresponse;
    }

    /************************************************************
     population_ollection_dummy - just a convenience method for testing...
     ***********************************************************/
    private void populate_collection_dummy(VectorDB vdb, String coll, int numentries, int vecsize) {
        System.out.println("dummy data... insert_collection() with " + numentries + " rows -------------------------");
        Utility util = new Utility();
        try {
            vdb.insert_collection(coll,
                    util.createDummySentenceIds(numentries),
                    util.createDummySentences(numentries),
                    util.createDummyEmbeddings(numentries, vecsize));
        } catch (VectorDBException vex) {
            System.err.println("***ERROR: Cannot populate coll [" + coll + "] in database.");
        }

    }
}
