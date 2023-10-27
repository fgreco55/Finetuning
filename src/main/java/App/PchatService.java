package App;

import Database.VectorDB;
import Database.VectorDBException;
import Model.LLM;
import Model.LLMCompletionException;
import Utilities.FinetuningUtils;
import Utilities.Utility;
import io.milvus.grpc.ShowCollectionsResponse;
import io.milvus.param.R;
import io.milvus.param.collection.ShowCollectionsParam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PchatService {
    private VectorDB vdb;
    private LLM model;
    private Utility util = new Utility();                       // Need to remove this from this level... -fdg
    private FinetuningUtils futil = new FinetuningUtils();      // name should be PchatFacade, or something like that
    private final static String DEFAULT_CONFIG = "/Users/fgreco/src/Finetuning/src/main/resources/llm.properties";

    public PchatService() {
        Properties prop = futil.getConfigProperties(DEFAULT_CONFIG);
        this.openVectorDB(prop);
        this.openLLM(prop);

        /*try {
            String instructions = model.getInstruction_file();           // If there's a system instructions file, use it
            if (instructions != null) {
                model.sendCompletionRequest("system", util.TextfiletoString(instructions));
            }
        } catch (LLMCompletionException lx) {
            System.err.println("***ERROR: Cannot send system instructions to the LLM.");
        }*/
    }

    public VectorDB getVdb() {
        return vdb;
    }

    public void setVdb(VectorDB vdb) {
        this.vdb = vdb;
    }

    public LLM getModel() {
        return model;
    }

    public void setModel(LLM model) {
        this.model = model;
    }

    public void openLLM(Properties prop) {
        try {
            this.model = new LLM(prop);
        } catch (LLMCompletionException lex) {
            System.err.println("***ERROR: Cannot create an LLM.");
        }
    }

    public void openVectorDB(Properties prop) {
        this.vdb = new VectorDB(prop);
    }

    public PchatService usedatabase(String database) {
        try {
            if (!vdb.databaseExists(database)) {
                vdb.create_database(database);
            }
        } catch (VectorDBException vex) {
            System.err.println("***ERROR: issue with vector database use/creation.");
        }
        return this;
    }

    public PchatService usecollection(String collection) {
        try {
            if (vdb.collectionExists(collection) != true) {
                vdb.create_collection(collection, model.getVector_size());
            }
            vdb.setCollection(collection);
        } catch (VectorDBException vex) {
            System.err.println("***ERROR: Cannot create/use collection " + collection);
        }
        return this;
    }

    public void dropcollection(String collection) {
        try {
            vdb.drop_collection(collection);
        } catch (VectorDBException vex) {
            System.err.println("***ERROR: Cannot drop collection [" + collection + "] in database [" + vdb.getDatabase() + "].");
        }
    }


    public String showcollections() throws VectorDBException{
        return vdb.show_collections();
    }

    public String showdatabases() {
        List<String> dblist = new ArrayList<>();
        try {
            dblist = vdb.get_databases();
        } catch (VectorDBException vex) {
            System.err.println("***ERROR: Cannot show databases");
        }
        return util.listToString(dblist);
    }

    public PchatService loadtextfile(String filename) {
        futil.populateFromTextfile(vdb, vdb.getCollection(), model, filename);
        vdb.flush_collection(vdb.getCollection());
        return this;
    }

    public PchatService loadpdf(String pdfname) {
        try {
            futil.populateFromPDF(vdb, vdb.getCollection(), model, pdfname);
            vdb.flush_collection(vdb.getCollection());
        } catch (IOException iox) {
            System.err.println("***Erråor: Cannot load PDF file [" + pdfname + "]");
        }
        return this;
    }

    public PchatService loadurl(String urlname) {
        futil.populateFromURL(vdb, vdb.getCollection(), model, urlname);
        vdb.flush_collection(vdb.getCollection());
        return this;
    }

    public PchatService loadrecording(String recpath) {
        futil.populateFromRecording(vdb, vdb.getCollection(), model, recpath);
        vdb.flush_collection(vdb.getCollection());
        return this;
    }

    public PchatService loadnote(String note) {
        futil.populateFromNote(vdb, vdb.getCollection(), model, note);
        vdb.flush_collection(vdb.getCollection());
        return this;
    }

    public String getCompletion(String request) {
        String answer = "";

        try {
            //answer = futil.getCompletion(model, vdb, vdb.getCollection(), request);
            answer = futil.getCompletion(model, vdb, vdb.getCollection(), request);      // XXX
        } catch (LLMCompletionException lex) {
            System.err.println("***ERROR: Cannot get completion from the model.");
        } catch (VectorDBException vdbx) {
            System.err.println("***ERROR: Cannot get completion from the model due to database issue");
        }

        return answer;
    }

    public PchatService setLanguage(String lang) {
        this.model.setLanguage(lang);
        return this;
    }

    public String getLanguage() {
        return this.model.getLanguage();
    }

    public PchatService setDatabase(String dbname) {
        vdb.setDatabase(dbname);
        return this;
    }

    public PchatService setCollection(String collname) {
        try {
            if (!vdb.collectionExists(collname)) {
                vdb.create_collection(collname);
            }
        } catch (VectorDBException e) {
            System.err.println("***ERROR: Cannot set collection [" + collname + "]");
            throw new RuntimeException(e);
        }

        vdb.setCollection(collname);
        return this;
    }
    public String getImageURL(String prompt) {
        return futil.getImageURLFromCompletion(model, prompt);
    }

    public boolean isPromptFlaggedByModeration(String prompt) {
        return futil.isPromptFlagged(model, prompt);
    }

    /*
     * Simple tester
     */
    public static void main(String[] args) {
        PchatService ps = new PchatService();
        //ps.usedatabase("frankdb");              // Should be defaults for db and collection.
        //ps.usecollection("trialcollection");    // Maybe the collection name should be based on $USER-$DATE ??

        //ps.usecollection("dncollection");
        ps.usedatabase("frankdb")
                .usecollection("trialcollection")
                .setLanguage("english");

        ps.loadurl("https://www.deepnetts.com/");
       /* ps.loadtextfile("./src/main/resources/faq-deepnetts.txt");
        ps.loadnote("Frank and Zoran meet via Zoom every Saturday");
        ps.loadurl("https://www.deepnetts.com/about-us/");
        ps.loadurl("https://www.jcp.org/en/jsr/detail?id=381");*/

        String in, out;

        in = "What is deep netts?";
        out = ps.getCompletion(in);
        System.out.println(in + " -> " + out);

        in = "Do Zoran and Frank meet on Friday?";
        out = ps.getCompletion(in);
        System.out.println(in + " -> " + out);

        ps.simplecompletion("serbian", "Is the founder of DeepNetts a Java Champion?");
        ps.simplecompletion("italian", "Is the founder of DeepNetts a Java Champion?");
        ps.simplecompletion("french", "Is the founder of DeepNetts a Java Champion?");
        ps.simplecompletion("brazilian", "Is the founder of DeepNetts a Java Champion?");
        ps.simplecompletion("japanese", "Is the founder of DeepNetts a Java Champion?");

        ps.simplecompletion("english", "Which JCP companies supported the jsr381 standard?");

        ps.setLanguage("serbian");
        in = "Which JCP companies supported the jsr381 standard?";
        out = ps.getCompletion(in);
        System.out.println(in + " -> " + out);

        ps.simplecompletion("english", "How many JCP companies supported the jsr381 standard?");
        ps.simplecompletion("english", "How can I make lasagna?");
        ps.simplecompletion("english", "How fast can a human throw a baseball?");

        //ps.dropcollection("dncollection");
    }

    public void simplecompletion(String lang, String prompt) {
        String in, out;
        setLanguage(lang);
        in = prompt;
        out = getCompletion(in);
        System.out.println(in + " -> " + out);
    }
}
