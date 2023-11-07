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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FinetuningUtils {
    private Utility util = new Utility();
    private LoadedResources lresources = new LoadedResources();

 // Delete createIdList() soon...
/*    public List<Long> createIdList(int num) {
        // int START = ... largest id found in the DB + 1...
        int START = 2000;
        List<Long> idlist = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            idlist.add((long) (START + i));
        }
        return idlist;
    }*/

    /***********************************************************************************************
     * populatefromNote() - load a string into the VDB
     **********************************************************************************************/
    public void populateFromNote(VectorDB v, String collection, LLM m, String note) {
        List<String> sents = new ArrayList<>();
        sents.add(note);
        System.out.println("Populating [" + collection + "-" + sents.size() + "] with " + note);
        insert_sentences(v, collection, m, sents);
    }

    /***********************************************************************************************
     * populatefromURL() - load all the text found on this URL
     **********************************************************************************************/
    public void populateFromURL(VectorDB v, String collection, LLM m, String url) {
        List<String> sents = util.URLtoSentences(url);
        System.out.println("Populating [" + collection + "-" + sents.size() + "] with " + url);
        insert_sentences(v, collection, m, sents);
    }

    /***********************************************************************************************
     * populatefromWebsite() - recursive call to load all the links from a website.
     *                    maxlevels should be low (1 or 2) or else unrelated info will be added
     **********************************************************************************************/
    public void populatefromWebsite(VectorDB v, String collection, LLM m, String websiteURL, int currLevel, int maxLevels) {

        if (currLevel == maxLevels) {
            return;
        }

        if (lresources.alreadyLoaded(websiteURL)) {              // This should be generalized for all resources -fdg
            System.err.println("[" + websiteURL + "] already loaded... skipping");
            return;
        }
        else
            lresources.addResource(websiteURL);

        this.populateFromURL(v, collection, m, websiteURL);     // This is STUFF

        List<String> links = util.URLtoLinks(websiteURL);

        if (links == (List<String>)null) {
            System.err.println("***DEBUG----------- List of links is NULL,  Either HTTP 403 or HTTP 999 Request Denied ");
            return;
        }

        for (String link : links) {
            populatefromWebsite(v, collection, m, link, currLevel+1, maxLevels);
        }
    }
    /***********************************************************************************************
    * populatefromPDF() - Load text from a PDF into the VDB
    **********************************************************************************************/
    public void populateFromPDF(VectorDB v, String collection, LLM m, String pdf) throws IOException {
        List<String> sents = util.PDFfiletoSentences(pdf);
        System.out.println("Populating [" + collection + "-" + sents.size() + "] with " + pdf);
        insert_sentences(v, collection, m, sents);
    }

    /***********************************************************************************************
    * populatefromTextfile() - Load text from a textfile into the VDB
    **********************************************************************************************/
    public void populateFromTextfile(VectorDB v, String collection, LLM m, String textfile) {
        List<String> sents = util.TextfiletoSentences(textfile);
        System.out.println("Populating [" + collection + "-" + sents.size() + "] with " + textfile);
        insert_sentences(v, collection, m, sents);
    }

    /***********************************************************************************************
     * populatefromRecording() - Load text from a transcript of a recording into the VDB
     **********************************************************************************************/
    public void populateFromRecording(VectorDB v, String collection, LLM m, String recordingFile) {
        SpeechTranscribe wt = new SpeechTranscribe(m.getApikey(), m.getSpeech_model());
        String s = wt.transcribe(recordingFile);

        List<String> sents = util.StringtoSentences(s);
        if (sents.size() == 0)
            return;

        System.out.println("Populating [" + collection + "-" + sents.size() + "] with " + recordingFile);
        insert_sentences(v, collection, m, sents);
    }

    /***********************************************************************************************
     * insert_sentences() - Get embeddings for every sentence (chunk) and insert [id, sentence and embedding] arrays
     **********************************************************************************************/
    private void insert_sentences(VectorDB v, String collection, LLM model, List<String> sentences) {
        if (sentences.size() == 0)
            return;

        int max = 0;
        try {
            if ( v.collectionExists(collection) )
                max = getHighestID(v, collection);
        } catch (VectorDBException e) {
            System.err.println("Collection does not exist... Using 0 as highestID");
        }

        List<Long> ids = new ArrayList<>();
        List<List<Float>> veclist = new ArrayList<>();
        for (int i = 0; i < sentences.size(); i++) {
            String s = sentences.get(i);
            List<Float> f = model.sendEmbeddingRequest(s);
            veclist.add(f);
            ids.add(Long.parseLong(max + i + ""));
        }
        try {
            v.insert_collection(collection, ids, sentences, veclist);
            System.out.println("[" + collection + "] has " + v.getCollectionRowCount(collection) + " rows.");
        } catch (VectorDBException vex) {
            System.err.println("***ERROR: main() - Cannot insert collection");
        }
    }

    /***********************************************************************************************
     * getCompletion() - Given a userquery, find the nearest neighbors (size max)
     **********************************************************************************************/
    public String getCompletion(@NotNull LLM m, @NotNull VectorDB v, String coll, String userquery) throws LLMCompletionException, VectorDBException {
        Utility util = new Utility();
        String matches = "";

        /* Make sure the query is moderated for hate, harassment, self-harm, sexual, or violent content  */
        if (isPromptFlagged(m, userquery)) {
            System.err.println("*****NOT ALLOWED*****... [" + userquery + "]");
            return "That type of question or comment is inappropriate and not allowed here.  Please be respectful. ";
        }

        List<List<Float>> smallvec = new ArrayList<>();         // get the embedding vector for the user's prompt
        smallvec.add(m.sendEmbeddingRequest(userquery));

        // Find the matching strings in the VDB
        List<String> match = v.searchDB_using_targetvectors(coll, smallvec, v.getMaxmatches());
        for (String s: match)
            m.addUserHistory(s);                               // Add VDB matches to user history

        m.setSystemHistoryList(new History());               //      SYSTEM  - doesn't really need a History
        m.getSystemHistoryList().add(m.getInstruction());

        String llmresponse = m.sendCompletionRequest(m.getUserHistoryList(), m.getAsstHistoryList(), m.getSystemHistoryList(), userquery);  // USER

        m.addAsstHistory(llmresponse);      // Add to the asst list                   ASSISTANT

        return llmresponse;
    }

    /***********************************************************************************************
     * Test to see if the user submitted a nasty prompt
     **********************************************************************************************/
    public boolean isPromptFlagged(LLM model, String prompt) {
       return model.sendModerationRequest(prompt);
    }

    /***********************************************************************************************
     * Get the URL of an image created by the user prompt
     **********************************************************************************************/
    public String getImageURLFromCompletion(LLM model, String prompt) {
        String url = model.sendImageRequest(prompt);
        return url;
    }

    public String getInstruction(LLM model) {
        return model.getInstruction();
    }
    public void setInstruction(LLM model, String myinstr) {
        model.setInstruction(myinstr);
    }
    public String getInstructionFile(LLM model) {
        return model.getInstructionFile();
    }
    public void setInstructionFile(LLM model, String myinstr) {
        model.setInstructionFile(myinstr);
    }

    public void setInstructionsFromFile(LLM model, String file) {
        model.setInstructionFile(file);
        model.setInstruction(util.TextfiletoString(file));
    }

    /***********************************************************************************************
     * Specific query to get largest ID... Milvus should have these types of primitives -fdg
     **********************************************************************************************/
    public List<String> getIDFromSentence(VectorDB vdb, String coll, String sentence) {
        List<String> res = new ArrayList<>();

        try {
            //String filter =  "sentence in [\"" + sentence + "\"]";
            String filter = "sentence == \"" + sentence + "\"";
            res = vdb.queryDB(coll, filter, "sentence_id", 16384L);   // max results
        } catch (VectorDBException vx) {
            System.err.println("Cannot retrieve IDs from the database.");
        }
        return res;
    }

    /***********************************************************************************************
     * Specific query to get largest ID... Milvus should have these types of primitives -fdg
     **********************************************************************************************/
    public int getHighestID(VectorDB vdb, String coll) {
        int highest = 0;
        List<String> ids;
        List<Integer> idlist = new ArrayList<>();       // initialize

        try {
            ids = vdb.queryDB(coll, "sentence_id > 0", "sentence_id", 16384L);   // max results
            if (ids.size() == 0)
                return 0;

            idlist = ids.stream().map(Integer::parseInt).collect(Collectors.toList());
            Collections.sort(idlist, Collections.reverseOrder());

        } catch (VectorDBException vdx) {
            System.err.println("Cannot get sentence_id's");
        }
        return idlist.get(0);
    }

}
