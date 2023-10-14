package Utilities;

import Database.VectorDB;
import Database.VectorDBException;
import Model.LLM;
import Model.LLMCompletionException;

import java.util.ArrayList;
import java.util.List;

public class FinetuningUtils {
    public void insertSentences(VectorDB vdb, LLM llm, String coll, String sent) throws VectorDBException {
        Utility util = new Utility();

        List<String> sentlist = util.StringtoSentences(sent);       // sentence list

        int numsent = sentlist.size();
        List<Long> sidlist = createIdList(numsent);                 // sentence_id list

        List<List<Float>> emblist = new ArrayList<>();              // embeddings list
        for (int i = 0; i < numsent; i++) {
            List<Float> emb = llm.getEmbedding(sent);
            emblist.add(emb);
        }

        vdb.insert_collection(coll, sidlist, sentlist, emblist);
    }

    public List<Long> createIdList(int num) {
        int START = 2000;
        List<Long> idlist = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            idlist.add((long) (START + i));
        }
        return idlist;
    }

    public void populate(VectorDB v, String collection, LLM m, Utility vut, String url) {
        System.out.println("Populating collection [" + collection + "] ====================================");
        List<String> sents = vut.URLtoSentences(url);
        System.out.println("DEBUG: rows: " + sents.size());
        sents.forEach(System.out::println);

        List<Long> ids = new ArrayList<>();
        List<List<Float>> veclist = new ArrayList<>();
        for (int i = 0; i < sents.size(); i++) {
            String s = sents.get(i);
            List<Float> f = m.sendEmbeddingRequest(s);
            veclist.add(f);
            ids.add(Long.parseLong(1000 + i + ""));
        }
        try {
            v.insert_collection(collection, ids, sents, veclist);
            System.out.println("[" + collection + "] has " + v.getCollectionRowCount(collection) + " rows.");
        } catch (VectorDBException vex) {
            System.err.println("***ERROR: main() - Cannot insert collection");
        }
    }


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
