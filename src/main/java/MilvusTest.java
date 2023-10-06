/*
 Quick pgm to test access to Milvus server using their Java SDK. - fdg

 Stream of consciousness programming just to test new concepts in Milvus - fdg
 */

import io.milvus.client.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.*;
import io.milvus.param.*;
import io.milvus.param.collection.*;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.QueryParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.highlevel.collection.ListCollectionsParam;
import io.milvus.param.highlevel.collection.response.ListCollectionsResponse;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.param.partition.CreatePartitionParam;
import io.milvus.response.DescCollResponseWrapper;
import io.milvus.response.GetCollStatResponseWrapper;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


public class MilvusTest {
    static final String COLLECTION_NAME = "frankcollection";
    static final String COLLECTION_DESCRIPTION = "Simple test of collection insertion and query/search";
    static final String DB_NAME = "frankdb";
    static final String PARTITION_NAME = "sentence_partition";
    static final String FIELD1 = "sentence_id";
    static final String FIELD2 = "sentence";            // actual sentence stored as meta-data for later retrieval
    static final String FIELD3 = "sentence_vector";
    static final String FIELD4 = "sentence_metadata";   // metadata for each sentence, eg, timestamp, origin, etc - TBD
    static final int MILVUS_PORT = 19530;               // default milvus port on local machine
    static final int OPENAI_VECSIZE = 1536;             // From empirical tests

    /************************************************************
     main - Test some things...
     ***********************************************************/
    public static void main(String[] args) throws IOException, InterruptedException {

        final MilvusServiceClient milvusClient = new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost("localhost")
                        .withPort(MILVUS_PORT)
                        .build()
        );
        milvusClient.setLogLevel(LogLevel.Debug);
        
        System.out.println("BEGIN -------------------------");
        show_databases(milvusClient);

        if (collectionExists(milvusClient, COLLECTION_NAME)) {
            drop_collection(milvusClient, COLLECTION_NAME);
        }

        drop_database(milvusClient, DB_NAME);
        System.out.println("AFTER DB Drop.....................");

        show_databases(milvusClient);
        create_database(milvusClient, DB_NAME);
        show_databases(milvusClient);

        create_collection(milvusClient, COLLECTION_NAME, OPENAI_VECSIZE);
        populate_collection(milvusClient, COLLECTION_NAME, 100, OPENAI_VECSIZE);

        show_collections(milvusClient);
        flush_collection(milvusClient, COLLECTION_NAME);         // You need to flush the collection to storage!

        //hasCollection(milvusClient, COLLECTION_NAME);
        show_collection_stats(milvusClient, COLLECTION_NAME);

        queryDB(milvusClient, COLLECTION_NAME, "sentence_id > 25 and sentence_id < 75", 10L);

        /* Now need to test search - TBD */
        searchDB(milvusClient, COLLECTION_NAME, "who is frank greco?", 5);
        searchDB(milvusClient, COLLECTION_NAME, "why does the NYJavaSIG exist?", 5);
       // drop_database(milvusClient, DB_NAME);
        show_collection_stats(milvusClient, COLLECTION_NAME);

        milvusClient.close();
    }

    /************************************************************
       drop_collection
       */
    static void show_collections(MilvusClient mc) {
        R<ListCollectionsResponse> cr = mc.listCollections(ListCollectionsParam.newBuilder().build());
        System.out.println("Collections in the DB: " + cr.getData());
    }

    /************************************************************
     drop_collection
     */
    static void drop_collection(MilvusClient mc, String coll) {
        System.out.println("DROPPING COLLECTION " + coll + " IN [" + DB_NAME + "]..............");
        DropCollectionParam dropParam = DropCollectionParam.newBuilder()
                .withCollectionName(coll)
                .build();
        R<RpcStatus> response = mc.dropCollection(dropParam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println("***FAILURE: " + response.getMessage());
        } else
            System.out.println("COLLECTION [" + coll + "] Dropped... SUCCESS ***");
    }

    private static void handleResponseStatus(R<?> r) {
        if (r.getStatus() != R.Status.Success.getCode()) {
            throw new RuntimeException(r.getMessage());
        }
    }

    /************************************************************
     if Collection exists
     */
    static boolean collectionExists(MilvusClient mc, String coll) {
        R<Boolean> response = mc.hasCollection(HasCollectionParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .build());
        handleResponseStatus(response);
        //System.out.println(response);
        return response.getData().booleanValue();
    }


    /************************************************************
     show collection statistics
     */
    static void show_collection_stats(MilvusClient mc, String coll) {
        System.out.println("GETTING COLLECTION STATS FOR " + coll + " IN [" + DB_NAME + "]..............");
        R<DescribeCollectionResponse> respDescribeCollection = mc.describeCollection(
                // Return the name and schema of the collection.
                DescribeCollectionParam.newBuilder()
                        .withCollectionName(coll)
                        .build()
        );
        DescCollResponseWrapper wrapperDescribeCollection = new DescCollResponseWrapper(respDescribeCollection.getData());
        System.out.println("[" + coll + "]: " + wrapperDescribeCollection);

        GetCollectionStatisticsParam param = GetCollectionStatisticsParam.newBuilder()
                .withCollectionName(coll)
                .build();
        R<GetCollectionStatisticsResponse> cresponse = mc.getCollectionStatistics(param);
        if (cresponse.getStatus() != R.Status.Success.getCode()) {
            System.out.println("**ERROR in getting collection stats. " + cresponse.getMessage());
        } else {
            System.out.println("Success in getting collection stats!!");

            GetCollStatResponseWrapper wrapper = new GetCollStatResponseWrapper(cresponse.getData());
            System.out.println("Row count: " + wrapper.getRowCount());
            System.out.println("[" + coll + "]: " + wrapperDescribeCollection);
        }
    }

    /************************************************************
     drop specific database
     */
    static void drop_database(MilvusClient mc, String dbname) {

        R<ListCollectionsResponse> lc2 = mc.listCollections(ListCollectionsParam.newBuilder().build());
                               System.out.println("In drop_database() - Collections in the DB: " + lc2.getData());
        R<RpcStatus> response;
        System.out.println("DROPPING DATABASE [" + DB_NAME + "]..............");
        DropDatabaseParam dbparam = DropDatabaseParam.newBuilder()
                .withDatabaseName(DB_NAME)
                .build();
        response = mc.dropDatabase(dbparam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println("***FAILURE: " + response.getMessage());
        } else
            System.out.println("DB Dropped... SUCCESS ***");
    }

    /************************************************************
     create database
     */
    static void create_database(MilvusClient mc, String dbname) {
        R<RpcStatus> response = null;
        System.out.println("CREATING DATABASE [" + DB_NAME + "]..............");
        CreateDatabaseParam cparam = CreateDatabaseParam.newBuilder()
                .withDatabaseName(dbname)
                .build();
        response = mc.createDatabase(cparam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println("***FAILURE: " + response.getMessage());
        } else
            System.out.println("DB Created... SUCCESS ***");
    }

    static void show_databases(MilvusClient mc) {
            R<ListDatabasesResponse> dbresponse = mc.listDatabases();
            System.out.println("Databases in the server==========");
            if (dbresponse.getStatus() != R.Status.Success.getCode()) {
                System.out.println(dbresponse.getMessage());
            } else
                System.out.println("***Success in listing databases...");

            for (int i = 0; i < dbresponse.getData().getDbNamesCount(); i++) {
                System.out.println(dbresponse.getData().getDbNames(i));
            }
        }

    /*
              Populate collection with dummy data

      sentences - list of sentences to be inserted into Milvus
      sentence_id - list of sentence ids
      svec - list of vectors, one list for each sentence
      vecsize - size of the vector array for each element of sentences list (only changes if embedding algo changes)
     */
    static void _populate_collection(MilvusClient mc, String coll, List<Long> sentence_id, List<String> sentences, List<List<Float>> svec) {

        System.out.println("CREATING DATA FOR [" + coll + "]............");

        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field(FIELD1, sentence_id));
        fields.add(new InsertParam.Field(FIELD2, sentences));
        fields.add(new InsertParam.Field(FIELD3, svec));

        System.out.println("INSERTING DATA INTO [" + coll + "]............");

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(coll)
                .withPartitionName(PARTITION_NAME)
                .withFields(fields)
                .build();
        mc.insert(insertParam);
    }

    static boolean isDataSameLength(List<Long> id, List<String> str, List<List<Float>> svec) {
        return (id.size() == str.size()  &&  id.size() == svec.size()) ;
    }

    static void populate_collection(MilvusClient mc, String coll, int numentries, int vecsize) {
        _populate_collection(mc,
                coll,
                createDummySentenceId(numentries), createDummySentences(numentries), createDummyEmbeddings(numentries, vecsize));
    }

    static List<Long> createDummySentenceId(int num) {
        List<Long> idlist = new ArrayList<>();
        for (long i = 0; i < num; i++) {
            idlist.add(i);
        }
        return idlist;
    }

    static List<String> createDummySentences(int num) {
        List<String> sentences = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            sentences.add(randomString());
        }
        return sentences;
    }

    static List<List<Float>> createDummyEmbeddings(int num, int vecsize) {
        Random ran = new Random();
        List<List<Float>> embeddings = new ArrayList<>();

        for(int j = 0; j < num; j++) {
            List<Float> vector = new ArrayList<>();
            for (int i = 0; i < vecsize; i++) {
                     vector.add(ran.nextFloat());
                 }
            embeddings.add(vector);
        }

        return embeddings;
    }


    /************************************************************
     Create the field schemas, create the collection and insert some dummy data
     */
    static void create_collection(MilvusClient mc, String coll, int vecsize) {
        R<RpcStatus> response = null;
        /*
         Create Milvus collection schema... you need datatype for each field
             A Milvus Collection is like a RDBMS table
         */
        FieldType fieldType1 = FieldType.newBuilder()        // schema for the id of the entry
                .withName(FIELD1)
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(false)
                .build();
        FieldType fieldType2 = FieldType.newBuilder()       // schema for num words in sentence (just an arbitrary field)
                .withName(FIELD2)
                .withDataType(DataType.VarChar)
                .withMaxLength(1024)                       // VarChar requires "withMaxLength()
                .build();
        FieldType fieldType3 = FieldType.newBuilder()       // schema for the actual vector
                .withName(FIELD3)
                .withDataType(DataType.FloatVector)
                .withDimension(vecsize)                        // hard-code to 2 floats for this hello-world
                .build();

        System.out.println("CREATING COLLECTION [" + coll + "]............");

        CreateCollectionParam createCollectionReq = CreateCollectionParam.newBuilder()      // Create collection
                .withCollectionName(coll)
                .withDescription(COLLECTION_DESCRIPTION)
                .withShardsNum(2)
                .addFieldType(fieldType1)
                .addFieldType(fieldType2)
                .addFieldType(fieldType3)
                .withEnableDynamicField(true)
                .build();
        response = mc.createCollection(createCollectionReq);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println("***FAILURE: " + response.getMessage());
        } else
            System.out.println("Collection created... SUCCESS ***");

        mc.createPartition(
                CreatePartitionParam.newBuilder()
                        .withCollectionName(coll)
                        .withPartitionName(PARTITION_NAME)
                        .build()
        );

        CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                .withCollectionName(coll)
                .withFieldName(FIELD3)
                .withIndexType(IndexType.FLAT)
                .withMetricType(MetricType.L2)
                .withSyncMode(Boolean.TRUE)
                .build();
        R<RpcStatus> createIndexR = mc.createIndex(indexParam);

        if (createIndexR.getStatus() != R.Status.Success.getCode()) {
            System.out.print("***ERROR:  " + createIndexR.getMessage());
        } else {
            System.out.println("Success creating the INDEX...");
        }
    }

    /************************************************************
     Flush - data is sealed and then flushed to storage
     */
    static void flush_collection(MilvusClient mc, String coll) {
        FlushParam param = FlushParam.newBuilder()
                .addCollectionName(coll)
                .build();
        R<FlushResponse> response = mc.flush(param);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println(response.getMessage());
        } else {
            System.out.println("FLUSH successful!");
        }
    }

    /************************************************************
     If database has this collection.  btw, a "collection" is like an RDBMS table
     */
    static boolean hasCollection(MilvusClient mc, String coll) {
        System.out.println("TESTING IF Milvus HAS COLLECTION [" + coll + "]............");

        R<Boolean> respHasCollection = mc.hasCollection(
                HasCollectionParam.newBuilder()
                        .withCollectionName(COLLECTION_NAME)
                        .build()
        );
        if (respHasCollection.getData() == Boolean.TRUE) {
            System.out.println("Collection exists.");
            return true;
        } else {
            System.out.println("Collection DOES NOT exist.");
            return false;
        }
    }

    /************************************************************
     Create random strings for simulation
     */
    static String randomString() {
        String[] universe = {
                "Q. What is the NYJavaSIG?  A. The New York Java Special Interest Group (NYJavaSIG) is based in New York City and attracts Java developers from the tri-state region. Through its regular monthly general meetings, bi-monthly specialty workgroup meetings and its website, the NYJavaSIG brings together members of New York's Java community so they can share their tips, techniques, knowledge, and experience.",
                "Q. When was the NYJavaSIG founded? A. The NYJavaSIG was founded in 1995 by Frank Greco",
                "Q. When are the NYJavaSIG meetings held? A. There are at least one meeting per month",
                "Q. Who has presented to the NYJavaSIG?  A. Arthur van Hoff, Jim Waldo, Scott Oaks, Henry Wong, Frank Greco, Doug Lea, Calvin Austin, Guy Steele, David Sherr, Brian Goetz, Pratik Patel, Josh Bloch, Justin Lee, Gil Tene, James Ward, Karl Jacobs, Peter Bell, Ethan Henry, Nat Wyatt, Roman Stanek, Anne Thomas-Manes, Simon Phipps, Jeanne Boyarsky, Reza Rahman, Fabiane Nardon, Peter Haggar, Max Goff, Maurice Balick, Gavin King, Jonathan Nobels, Bob Pasker, Rinaldo DiGiorgio, Steve Ross-Talbot, Talip Ozturk, John Davies, Rod Johnson, Bert Ertman, Simon Ritter, Kirk Pepperdine, Matt Raible, Edson Yanaga, Victor Grazi, Chandra Guntur",
                "Q. Where are the meetings?  A. The meetings are typically at Credit Suisse, Google, Microsoft, BNY Mellon, Cockroach Labs, Betterment, and other locations in Manhattan",
                "Q. What is a SIG? A. A SIG is a Special Interest Group, historically a subset of a larger group",
                "Q. When was the first formal Java tutorial? A. Frank delivered the first Java tutorial (along with Scott Oaks) way back on September 21, 1995, at the Equitable Center in midtown Manhattan.",
                "Q. How many members were at the first NYJavaSIG meeting? A. Five",
                "Q. What is a HOW? A. A HOW is a NYJavaSIG Hands-On-Workshop",
                "Q. What is the goal of the NYJavaSIG? A. The goal of the NYJavaSIG has always been to help software engineers, technologists, and technical managers in the NY area stay on the cutting edge"
        };
        Random random = new Random();
        return universe[random.nextInt(universe.length)];
    }

    /************************************************************
     load Collection - collection must be loaded into memory to work
     */

    static void loadCollection(MilvusClient mc, String coll) {
        R<RpcStatus> loadc = mc.loadCollection(
                LoadCollectionParam.newBuilder()
                        .withCollectionName(coll)
                        .build()
        );
        if (loadc.getStatus() != R.Status.Success.getCode()) {
            System.out.print("***ERROR:  " + loadc.getMessage());
        } else {
            System.out.println("Success loading the collection [" + coll + "]");
        }
    }

    /************************************************************
     Query the DB for specific filters
     */
    static void queryDB(MilvusClient mc, String coll, String query, Long max) {

        loadCollection(mc, coll);
        List<String> query_output_fields = Arrays.asList(FIELD1, FIELD2);

        QueryParam queryParam = QueryParam.newBuilder()
                .withCollectionName(coll)
                .withConsistencyLevel(ConsistencyLevelEnum.STRONG)
                .withExpr(query)
                .withOutFields(query_output_fields)
                .withOffset(0L)
                .withLimit(max)         // max entries returned
                .build();
        R<QueryResults> respQuery = mc.query(queryParam);
        if (respQuery.getStatus() != R.Status.Success.getCode()) {
            System.out.println("**ERROR: Query FAILED!");
            System.out.println(respQuery.getMessage());
        } else {
            System.out.println("QUERY successful!");
        }

        QueryResultsWrapper wrapperQuery = new QueryResultsWrapper(respQuery.getData());
        long numrows = wrapperQuery.getRowCount();
        System.out.println("query returned " + numrows + " rows.");

        List<QueryResultsWrapper.RowRecord> records = wrapperQuery.getRowRecords();
        for (QueryResultsWrapper.RowRecord record : records) {
            System.out.println(record);
        }
        //System.out.println(wrapperQuery.getFieldWrapper(FIELD1).getFieldData());
        //System.out.println(wrapperQuery.getFieldWrapper(FIELD2).getFieldData());
    }

    /************************************************************
     Semantic Search the DB
     */
    static void searchDB(MilvusClient mc, String coll, String target, int max) {
        System.err.println("In searchDB()...");

        loadCollection(mc, coll);

        List<List<Float>> targetVectors = new ArrayList<>(2);     // I only need 1 target embedding
        List<Float> tv = new ArrayList<>(2);
        Random ran = new Random();
        for(int i = 0; i < OPENAI_VECSIZE; i++){
            tv.add(ran.nextFloat());
        }
      /*     tv.add(0.012345F);       // just dummy values
           tv.add(-0.082223F);*/
        targetVectors.add(tv);

        System.err.println("DEBUG: size of targetVectors: " + targetVectors.size());                // vectors.size()
        System.err.println("DEBUG: size of tv: " + tv.size());
        System.err.println("DEBUG: size of targetVectors.get(0): " + targetVectors.get(0).size());  //dim

        System.err.println("About to call newBuilder().build()");
        SearchParam param = SearchParam.newBuilder()
                .withCollectionName(coll)
                .withMetricType(MetricType.L2)
                .withTopK(max)
                .withVectors(targetVectors)     // Search closest neighbors to these
                .withVectorFieldName(FIELD3)    // ... using this field (my embedding array)
                .withConsistencyLevel(ConsistencyLevelEnum.EVENTUALLY)
                .addOutField(FIELD2)            // IMPORTANT FIELD TO ADD!
                //.withParams("{\"nprobe\":10,\"offset\":2, \"limit\":3}")
                //.withParams("{\"nprobe\":10")
                .build();

        System.err.println("About to call search()");
        R<SearchResults> resp = mc.search(param);
        if (resp.getStatus() != R.Status.Success.getCode()) {
            System.err.println("***ERROR: Cannot Search. " + resp.getMessage());
        } else {
            System.err.println("SEMANTIC SEARCH SUCCESS!! for [" + target + "]");
            
            getSearchData(resp, targetVectors);
        }
    }

    private static void getSearchData(R<SearchResults> resp, List<List<Float>> tvectors) {
        SearchResultsWrapper wrapper = new SearchResultsWrapper(resp.getData().getResults());
        System.out.println("Search results:");
        for (int i = 0; i < tvectors.size(); i++) {
            List<SearchResultsWrapper.IDScore> scores = wrapper.getIDScore(i);
            for (SearchResultsWrapper.IDScore score : scores) {
                //System.out.println(score);
                System.out.println(score + " : " + wrapper.getFieldData(FIELD2, i));
            }
        }
    }
}

