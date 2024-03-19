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

    /***************************************************************
     *   Constructors
     **************************************************************/
    public PchatService() {
        String configfile = System.getenv ("PCHAT_CONFIG");
        if (configfile == null)
            configfile = DEFAULT_CONFIG;

        Properties prop = util.getConfigProperties(configfile);
        this.openVectorDB(prop);
        this.openLLM(prop);
    }

    /***************************************************************
     *   openLLM() and openVectorDB() should only be called
     *                                      within this class
     **************************************************************/
    private void openLLM(Properties prop) {
        try {
            this.model = new LLM(prop);
        } catch (LLMCompletionException lex) {
            System.err.println("***ERROR: Cannot create an LLM.");
        }
    }

    private void openVectorDB(Properties prop) {
        this.vdb = new VectorDB(prop);
    }

    /***************************************************************
     *   system level methods - deals with the database
     **************************************************************/
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


    public String showcollections() throws VectorDBException {
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
    /***************************************************************
     *   user methods
     **************************************************************/

    /***************************************************************
     *   loadtextfile() - load text file into the VDB
     **************************************************************/
    public PchatService loadtextfile(String filename) {
        futil.populateFromTextfile(vdb, vdb.getCollection(), model, filename);
        vdb.flush_collection(vdb.getCollection());
        return this;
    }

    /***************************************************************
     *   loadpdf() - load pdf file into the VDB
     **************************************************************/
    public PchatService loadpdf(String pdfname) {
        try {
            futil.populateFromPDF(vdb, vdb.getCollection(), model, pdfname);
            vdb.flush_collection(vdb.getCollection());
        } catch (IOException iox) {
            System.err.println("***Erråor: Cannot load PDF file [" + pdfname + "]");
        }
        return this;
    }

    /*********************************************************************
     * Load a webpage given a URL, non-recursive
     ********************************************************************/
    public PchatService loadurl(String urlname) {
        futil.populateFromURL(vdb, vdb.getCollection(), model, urlname);
        vdb.flush_collection(vdb.getCollection());
        return this;
    }

    /*********************************************************************
     * Load a URL recursively (website).  Num of recursive levels is in
     *                                  the getWebloader_levels() property
     ********************************************************************/
    public PchatService loadwebsite(String urlname) {
        futil.populatefromWebsite(vdb, vdb.getCollection(), model, urlname, 0, getModel().getWebloader_levels());
        vdb.flush_collection(vdb.getCollection());
        return this;
    }

    /*********************************************************************
     * Load a website given a URL and the number of recursive levels
     ********************************************************************/
    public PchatService loadwebsite(String urlname, int levels) {
        futil.populatefromWebsite(vdb, vdb.getCollection(), model, urlname, 0, levels);
        vdb.flush_collection(vdb.getCollection());
        return this;
    }

    /*********************************************************************
     * Transcribe a recording, parse into sentences, and load into the DB
     *   Currently limited to 25MB files (filesplitter is broken)
     ********************************************************************/
    public PchatService loadrecording(String recpath) {
        futil.populateFromRecording(vdb, vdb.getCollection(), model, recpath);
        vdb.flush_collection(vdb.getCollection());
        return this;
    }

    /*********************************************************************
     * Load a string into the DB
     ********************************************************************/
    public PchatService loadnote(String note) {
        futil.populateFromNote(vdb, vdb.getCollection(), model, note);
        vdb.flush_collection(vdb.getCollection());
        return this;
    }

    /*********************************************************************
     * Primary method for sending a prompt to the LLM and getting a result
     ********************************************************************/
    public String getCompletion(String request) {
        String answer = "";

        try {
            answer = futil.getCompletion(model, vdb, vdb.getCollection(), request);
        } catch (LLMCompletionException lex) {
            System.err.println("***ERROR: Cannot get completion from the model.");
        } catch (VectorDBException vdbx) {
            System.err.println("***ERROR: Cannot get completion from the model due to a database issue");
        }

        return answer;
    }

    /*********************************************************************
     * Set the language for subsequent conversations with the LLM
     ********************************************************************/
    public PchatService setLanguage(String lang) {
        this.model.setLanguage(lang);
        return this;
    }

    public String getLanguage() {
        return this.model.getLanguage();
    }


    public String getImageURL(String prompt) {
        return futil.getImageURLFromCompletion(model, prompt);
    }

    public boolean isPromptFlaggedByModeration(String prompt) {
        return futil.isPromptFlagged(model, prompt);
    }

    public String getInstruction() {
        return futil.getInstruction(model);
    }

    public void setInstruction(String inst) {
        futil.setInstruction(model, inst);
    }

    public String getInstructionFile() {
        return futil.getInstructionFile(model);
    }

    public void setInstructionFile(String inst) {
        futil.setInstructionFile(model, inst);
    }

    public void setInstructionsFromFile(String filename) {
        futil.setInstructionsFromFile(model, filename);
    }

    /****************************************************
     * Simple tester
     ***************************************************/
    public static void main(String[] args) {
        PchatService ps = new PchatService();
        ps.usedatabase("frankdb")
                .usecollection("testcollection")
                .setLanguage("english");

        ps.loadurl("https://www.deepnetts.com/");

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

    /*
     * convenience method for main() test
     */
    public void simplecompletion(String lang, String prompt) {
        String in, out;
        setLanguage(lang);
        in = prompt;
        out = getCompletion(in);
        System.out.println(in + " -> " + out);
    }
}
