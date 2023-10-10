/*
 Simple test program... still need to test LLM methods -fdg
 */
package App;

import Utilities.Utility;
import Database.VectorDB;
import Database.VectorDBException;

import java.io.IOException;
import java.util.List;

public class Tester {
    static final String DATABASE = "frankdb";    // default DB
    static final String COLLECTION = "frankcollection";


    public static void main(String[] args) throws VectorDBException, IOException {
        VectorDB vdb = new VectorDB(DATABASE);
        Utility util = new Utility();

        vdb.show_databases();
        if (vdb.collectionExists(COLLECTION) == true) {
            System.out.println("collection does exist");
        } else
            System.out.println("collection does NOT exist");
        vdb.show_collection_stats(COLLECTION);
        vdb.show_databases();

        /*
         Next test:
            clear the collection
            get array of strings from a source (text/pdf file, url, notes, etc)
            create vector embeddings for those strings
            insert data in VDB
            test query and search

            TBD - fdg   Oct 4, 2023
         */

        /* Test simple query match  */
        vdb.queryDB(COLLECTION, "sentence_id > 25 and sentence_id < 75", 10L);

        List<String> match;
        /* Test nearest semantic neighbors */
        match = vdb.searchDB(COLLECTION, "why does the NYJavaSIG exist?", 5);
        System.out.println("Finding nearest neighbors... START");
        match.forEach(System.out::println);     // These are the top "max" nearest neighbors
        System.out.println("Finding nearest neighbors... END");

        /* Test string->sentences parser */
        System.out.println(util.TexttoSentences("hello world.  This is Frank.  Nice to hear from you."));
        /* Test textfile->sentences parser */
        util.TextfiletoSentences("./src/main/resources/testfile.txt").forEach(System.out::println);
        /* Test URL scrape -> text */
        System.out.println(util.URLtoText("http://www.javasig.com"));
        /* Test URL scrape -> sentences */
        util.URLtoSentences("https://www.espn.com").forEach(System.out::println);
        /* Test pdf file -> sentences */
        util.PDFfiletoSentences("./src/main/resources/testfile.pdf").forEach(System.out::println);

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
