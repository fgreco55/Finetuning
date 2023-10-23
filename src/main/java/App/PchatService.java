package App;

import Database.VectorDB;
import Database.VectorDBException;
import Model.LLM;
import Model.LLMCompletionException;
import Utilities.FinetuningUtils;
import Utilities.Utility;

import java.io.IOException;
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
        vdb.setCollection(collection);
        try {
            if (vdb.collectionExists(collection) != true) {
                vdb.create_collection(collection, model.getVector_size());
            }
        } catch (VectorDBException vex) {
            System.err.println("***ERROR: Cannot create/use collection " + collection);
        }
        return this;
    }

    public void dropcollection(String collection) {
        try {
            vdb.drop_collection(collection);
        } catch (VectorDBException vex) {
            System.err.println("***ERROR: Cannot drop collection [" + collection + "]");
        }
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
            System.err.println("***Error: Cannot load PDF file [" + pdfname + "]");
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
            answer = futil.getCompletion(model, vdb, vdb.getCollection(), request);
        } catch (LLMCompletionException lex) {
            System.err.println("***ERROR: Cannot get completion from the model");
        }

        return answer;
    }

    public PchatService setLanguage(String lang) {
        this.model.setLanguage(lang);
        return this;
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
