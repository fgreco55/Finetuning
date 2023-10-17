/*
 Simple test program... still need to test LLM methods -fdg
 */
package App;

import Model.LLM;
import Model.LLMCompletionException;
import Utilities.FinetuningUtils;
import Utilities.SpeechTranscribe;
import Utilities.Utility;
import Database.VectorDB;
import Database.VectorDBException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Tester {
    static final String MILVUS_DATABASE = "frankdb";    // default DB
    static final String COLLECTION_NAME = "frankcollection";
    private final static String DEFAULT_CONFIG = "./src/main/resources/llm.properties";
    private final static String PREAMBLE = "./src/main/resources/preamble.txt";
    static final int OPENAI_VECSIZE = 1536;
    private final static String EMBEDDING_MODEL = "text-embedding-ada-002";
    private final static String COMPLETION_MODEL = "gpt-3.5-turbo";

    public static void main(String[] args) throws VectorDBException, LLMCompletionException, IOException {
        Utility util = new Utility();
        FinetuningUtils futil = new FinetuningUtils();
        VectorDB vdb = new VectorDB(DEFAULT_CONFIG);
        LLM model = new LLM(DEFAULT_CONFIG);
        String collection = vdb.getCollection();
        String database = vdb.getDatabase();

        System.out.println("coll: " + collection);
        System.out.println("database: " + database);

        Tester mytest = new Tester();

        vdb.show_databases();
        if (!vdb.databaseExists(database)) {
            vdb.create_database(database);
        }
        System.out.println("COLLECTIONS");
        vdb.show_collections();
        if (vdb.collectionExists(collection) == true) {
            System.out.println("collection [" + collection + "] does exist");
        } else {
            System.out.println("collection [" + collection + "] does NOT exist.  Creating...");
            vdb.create_collection(collection, OPENAI_VECSIZE);
        }

        /*****************************************************
         *  Create some data to insert into the VDB collection
         ****************************************************/
        System.out.println("Populating collection [" + collection + "] ====================================");
        String fname = "./src/main/resources/faq.txt";
        List<String> sents = util.TextfiletoList(fname);
        System.out.println("DEBUG: Textfile " + fname + " has rows: " + sents.size());
        //sents.forEach(System.out::println);

        List<Long> ids = new ArrayList<>();
        List<List<Float>> veclist = new ArrayList<>();
        for (int i = 0; i < sents.size(); i++) {
            String s = sents.get(i);
            List<Float> f = model.sendEmbeddingRequest(s);
            veclist.add(f);
            ids.add(Long.parseLong(1000 + i + ""));         // hard-coded for 1000... not a good idea -fdg
        }
        try {
            vdb.insert_collection(collection, ids, sents, veclist);
            System.out.println("[" + collection + "] has " + vdb.getCollectionRowCount(collection) + " rows.");
        } catch (VectorDBException vex) {
            System.err.println("***ERROR: main() - Cannot insert collection");
        }

        /*
          Insert recording... drum roll...
         */
        SpeechTranscribe wt = new SpeechTranscribe(model.getApikey(), "whisper-1");
        String s = wt.transcribe("/Users/fgreco/src/Finetuning/src/main/resources/20230913-nyjavasig-abstract.mp3");
        futil.insertSentences(vdb, model, collection, s);

        /* Test simple query match  */
        vdb.queryDB(COLLECTION_NAME, "sentence_id > 0 and sentence_id < 30000", 10L);

        Scanner userinput;

        while (true) {
            System.out.print("Query> ");
            userinput = new java.util.Scanner(System.in);

            if (userinput.hasNextLine()) {
                String cmd = userinput.nextLine();
                if (!cmd.isEmpty()) {
                    try {
                        System.out.println(mytest.getCompletion(model, vdb, collection, cmd));
                    } catch (LLMCompletionException lx) {
                        System.err.println("***ERROR... Cannot complete user's query.");
                    }
                }
            }
        }
    }

    /************************************************************
     getCompletion() - just a convenience method
     ***********************************************************/
    public String getCompletion(LLM m, VectorDB v, String coll, String userquery) throws LLMCompletionException {
        Utility util = new Utility();

        // Create list of float arrays (list of vectors)... but only need one here (only want one result)
        List<List<Float>> smallvec = new ArrayList<>();
        smallvec.add(m.sendEmbeddingRequest(userquery));

        List<String> match;
        match = v.searchDB_using_targetvectors(coll, smallvec, 5);
        //System.out.println("Finding nearest neighbors for [" + userquery + "]... \nSTART-----------------------");
        //match.forEach(System.out::println);     // These are the top "max" nearest neighbors
        //System.out.println("Finding nearest neighbors... \nEND---------------------------");

        /*
         * following prompt should be in a Prompt class which encapsulates Prompt strategies. -fdg
         */
        String bigprompt = "";
        bigprompt = util.TextfiletoString(m.getPreamble_file());
        bigprompt += util.createBigString(match);
        bigprompt += userquery;

        String llmresponse = "";
        try {
            llmresponse = m.sendCompletionRequest("user", bigprompt);
        } catch (LLMCompletionException lex) {
            System.err.println("***ERROR: Cannot send bigprompt to LLM");
        }
        return llmresponse;
    }
}
