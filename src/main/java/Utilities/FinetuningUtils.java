package Utilities;
/*
 * FinetuningUtils - Facade over LLM and VectorDB classes
 *         There should be no mention of the specific underlying vector database or the specific model used
 *         here in this class.
 *
 */

import Database.VectorDB;
import Database.VectorDBException;
import Model.LLM;
import Model.LLMCompletionException;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Element;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class FinetuningUtils {
    private Utility util = new Utility();
    private LoadedResources lresources = new LoadedResources();


    public List<Long> createIdList(int num) {
        int START = 2000;
        List<Long> idlist = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            idlist.add((long) (START + i));
        }
        return idlist;
    }

    public void populateFromNote(VectorDB v, String collection, LLM m, String note) {
        List<String> sents = new ArrayList<>();
        sents.add(note);
        System.out.println("Populating [" + collection + "-" + sents.size() + "] with " + note);
        insert_sentences(v, collection, m, sents);
    }

    public void populateFromURL(VectorDB v, String collection, LLM m, String url) {
        List<String> sents = util.URLtoSentences(url);
        System.out.println("Populating [" + collection + "-" + sents.size() + "] with " + url);
        insert_sentences(v, collection, m, sents);
    }

    /*
     * populatefromWebsite() - recursive call to load all the links from a website.  maxlevels should be low (1 or 2) or else unrelated info will be added
     */
    public void populatefromWebsite(VectorDB v, String collection, LLM m, String websiteURL, int currLevel, int maxLevels) {

        if (currLevel == maxLevels) {
            return;
        }

        /*for(int i = 0; i < currLevel; i++) {
            System.err.print("+-----> ");
        }
        System.err.println("LEVEL: " + currLevel + " URL: " + websiteURL );*/
        if (lresources.alreadyLoaded(websiteURL))               // This should be generalized for all resources -fdg
            return;
        else
            lresources.addResource(websiteURL);

        this.populateFromURL(v, collection, m, websiteURL);     // This is STUFF

        List<String> links = util.URLtoLinks(websiteURL);

        if (links == (List<String>)null) {
            System.err.println("DEBUG----------- links is NULL");
            return;
        }

        for (String link : links) {
            //populateFromURL(v, collection, m, link);
            //System.out.println("Loading link: " + link + " at Level " + currLevel);     // simulated action
            populatefromWebsite(v, collection, m, link, currLevel+1, maxLevels);
        }
    }

    public void populateFromPDF(VectorDB v, String collection, LLM m, String pdf) throws IOException {
        List<String> sents = util.PDFfiletoSentences(pdf);
        System.out.println("Populating [" + collection + "-" + sents.size() + "] with " + pdf);
        insert_sentences(v, collection, m, sents);
    }

    public void populateFromTextfile(VectorDB v, String collection, LLM m, String textfile) {
        List<String> sents = util.TextfiletoSentences(textfile);
        System.out.println("Populating [" + collection + "-" + sents.size() + "] with " + textfile);
        insert_sentences(v, collection, m, sents);
    }

    public void populateFromRecording(VectorDB v, String collection, LLM m, String recordingFile) {
        SpeechTranscribe wt = new SpeechTranscribe(m.getApikey(), m.getSpeech_model());
        String s = wt.transcribe(recordingFile);

        List<String> sents = util.StringtoSentences(s);
        if (sents.size() == 0)
            return;

        System.out.println("Populating [" + collection + "-" + sents.size() + "] with " + recordingFile);
        insert_sentences(v, collection, m, sents);
    }

    /*
     * insert_sentences() - Get embeddings for every sentence (chunk) and insert [id, sentence and embedding] arrays
     */
    private void insert_sentences(VectorDB v, String collection, LLM model, List<String> sentences) {
        if (sentences.size() == 0)
            return;

        List<Long> ids = new ArrayList<>();
        List<List<Float>> veclist = new ArrayList<>();
        for (int i = 0; i < sentences.size(); i++) {
            String s = sentences.get(i);
            List<Float> f = model.sendEmbeddingRequest(s);
            veclist.add(f);
            ids.add(Long.parseLong(1000 + i + ""));
        }
        try {
            v.insert_collection(collection, ids, sentences, veclist);
            System.out.println("[" + collection + "] has " + v.getCollectionRowCount(collection) + " rows.");
        } catch (VectorDBException vex) {
            System.err.println("***ERROR: main() - Cannot insert collection");
        }
    }
    /*
     * getCompletion() - Given a userquery, find the nearest neighbors (size max)
     */

    public String getCompletion(@NotNull LLM m, @NotNull VectorDB v, String coll, String userquery) throws LLMCompletionException, VectorDBException {
        Utility util = new Utility();
        String matches = "";

        List<List<Float>> smallvec = new ArrayList<>();         // get the embedding vector for the user's prompt
        smallvec.add(m.sendEmbeddingRequest(userquery));

        /* Make sure the query is moderated for hate, harassment, self-harm, sexual, or violent content  */
        if (isPromptFlagged(m, userquery)) {
            System.err.println("*****FLAGGED*****... [" + userquery + "]");
            return "That type of question or comment is not allowed here. ";
        }

        // Find the matching strings in the VDB
        List<String> match = v.searchDB_using_targetvectors(coll, smallvec, v.getMaxmatches());
        for (String s: match)
            m.addUserHistory(s);            // Add VDB matches to user history
        //m.addUserHistory(userquery);        // Add user query to the user history      USER

        m.setSystemHistoryList(new History());                                  //      SYSTEM
        m.getSystemHistoryList().add(util.TextfiletoString(m.getInstruction_file()));

        String llmresponse = m.sendCompletionRequest(m.getUserHistoryList(), m.getAsstHistoryList(), m.getSystemHistoryList(), userquery);

        m.addAsstHistory(llmresponse);      // Add to the asst list                   ASSISTANT

        return llmresponse;
    }
    /*public String getCompletionXX(@NotNull LLM m, @NotNull VectorDB v, String coll, String userquery) throws LLMCompletionException, VectorDBException {
      Utility util = new Utility();
      String matches = "";

      List<List<Float>> smallvec = new ArrayList<>();
      smallvec.add(m.sendEmbeddingRequest(userquery));

      List<String> match;
      match = v.searchDB_using_targetvectors(coll, smallvec, v.getMaxmatches());

      *//* Make sure the query is moderated for hate, harassment, self-harm, sexual, or violent content  *//*
      if (isPromptFlagged(m, userquery)) {
          System.err.println("*****FLAGGED*****... [" + userquery + "]");
          return "That type of question or comment is not allowed here. ";
      }

      matches = util.createBigString(match);                          // assistant msgs
      String sysmsg = util.TextfiletoString(m.getInstruction_file()); // how to behave

      String llmresponse = m.sendCompletionRequest(userquery, matches, sysmsg, m.getHistoryListAsString());
      m.addHistory(" " + llmresponse + " " + userquery);    // add user query?? -fdg

      return llmresponse;
  }*/
    /*
     * Test to see if the user submitted a nasty prompt
     */
    public boolean isPromptFlagged(LLM model, String prompt) {
       return model.sendModerationRequest(prompt);
    }

    /*
     * Get the URL of an image created by the user prompt
     */
    public String getImageURLFromCompletion(LLM model, String prompt) {
        String url = model.sendImageRequest(prompt);
        return url;
    }

    public Properties getConfigProperties(String fname) {
        Properties prop = new Properties();
        InputStream in;

        try {
            in = new FileInputStream(fname);
            prop.load(in);
        } catch (IOException iox) {
            System.err.println("***ERROR: cannot open [" + fname + "] - " + iox.getMessage());
        }

        return prop;
    }
}
