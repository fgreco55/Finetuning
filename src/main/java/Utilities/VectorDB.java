package Utilities;

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
import io.milvus.response.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class VectorDB {
    static final int MILVUS_PORT = 19530;                // default port
    static final String MILVUS_HOST = "localhost";      // default host
    static final String MILVUS_DATABASE = "frankdb";    // default DB
    static final String COLLECTION_NAME = "frankcollection";
    static final String PARTITION_NAME = "sentence_partition";
    static final String COLLECTION_DESCRIPTION = "Simple test of collection insertion and query/search";
    static final String FIELD1 = "sentence_id";
    static final String FIELD2 = "sentence";            // actual sentence stored as meta-data for later retrieval
    static final String FIELD3 = "sentence_vector";
    String host = MILVUS_HOST;
    int port = MILVUS_PORT;
    String database;
    boolean initialized = false;
    static final int OPENAI_VECSIZE = 1536;
    private MilvusServiceClient mc;

    public VectorDB(String h, int p, String dbname) {
        this.host = h;
        this.port = p;
        this.database = dbname;
        connectToMilvus(h, p);
        this.initialized = true;
    }

    public VectorDB(String h, int p) {
        this.host = h;
        this.port = p;
        connectToMilvus(h, p);
        this.initialized = true;
    }

    public VectorDB(String db) {
        this(MILVUS_HOST, MILVUS_PORT, db);
        this.database = db;
    }

    /************************************************************
     Connect to the Milvus server.  Right now just using localhost/Docker
     ***********************************************************/
    private void connectToMilvus(String host, int port) {
        mc = new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost(host)
                        .withPort(port)
                        .build()
        );
        mc.setLogLevel(LogLevel.Debug);
    }

    /************************************************************
     handleResponseStatus - internal method for Milvus
     ***********************************************************/
    private void handleResponseStatus(R<?> r) {
        if (r.getStatus() != R.Status.Success.getCode()) {
            throw new RuntimeException(r.getMessage());
        }
    }
    /************************************************************
     *
     *       DATABASE METHODS
     *
     ************************************************************/


    /************************************************************
     Make sure VDB class is initialized
     ***********************************************************/
    public void check_init() throws VectorDBException {
        if (!initialized) {
            throw new VectorDBException("VectorDB is not initalized.");
        }
    }

    /************************************************************
     drop named database
     ***********************************************************/
    public void drop_database(String dbname) throws VectorDBException {
        check_init();
        R<ListCollectionsResponse> lc2 = mc.listCollections(ListCollectionsParam.newBuilder().build());
        System.out.println("In drop_database() - Collections in the DB: " + lc2.getData());

        R<RpcStatus> response;
        System.out.println("DROPPING DATABASE [" + dbname + "]..............");
        DropDatabaseParam dbparam = DropDatabaseParam.newBuilder()
                .withDatabaseName(dbname)
                .build();
        response = mc.dropDatabase(dbparam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println("***FAILURE: " + response.getMessage());
        } else
            System.out.println("DB Dropped... SUCCESS ***");
    }

    /************************************************************
     create database
     ***********************************************************/
    public void create_database(String dbname) throws VectorDBException {
        check_init();
        R<RpcStatus> response = null;
        System.out.println("CREATING DATABASE [" + dbname + "]..............");
        CreateDatabaseParam cparam = CreateDatabaseParam.newBuilder()
                .withDatabaseName(dbname)
                .build();
        response = mc.createDatabase(cparam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println("***FAILURE: " + response.getMessage());
        } else
            System.out.println("DB Created... SUCCESS ***");
    }

    /************************************************************
     Show all the databases in the server
     ***********************************************************/
    public void show_databases() throws VectorDBException {
        check_init();

        R<ListDatabasesResponse> dbresponse = mc.listDatabases();
        System.out.println("Databases in the server==========");
        if (dbresponse.getStatus() != R.Status.Success.getCode()) {
            System.out.println(dbresponse.getMessage());
        } else {
            System.out.println("***Success in listing databases...");
            for (int i = 0; i < dbresponse.getData().getDbNamesCount(); i++) {
                System.out.println(dbresponse.getData().getDbNames(i));
            }
        }
    }

    /************************************************************
     *
     *       COLLECTION METHODS
     *
     ************************************************************/

    /************************************************************
     Create the field schemas, create the collection and insert some dummy data
     */
    public void create_collection(String coll, int vecsize) {
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
                .withIndexType(IndexType.FLAT)      // Brute force search... slow
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

    /*********************************************************************
     Populate collection with dummy data

     sentences - list of sentences to be inserted into Milvus
     sentence_id - list of sentence ids
     svec - list of vectors, one list for each sentence
     vecsize - size of the vector array for each element of sentences list (only changes if embedding algo changes)
     ********************************************************************/
    private void populate_collection(String coll,
                                      List<Long> sentence_id, List<String> sentences, List<List<Float>> svec)
                                        throws VectorDBException {
        
        // Arrays must be the same length...
        if ( !(sentence_id.size() == sentences.size() && sentence_id.size() == svec.size()) ) {
           throw new VectorDBException("***ERROR: populate_collection() - array sizes must match");
        }

        System.out.println("CREATING DATA FOR [" + coll + "]............");

        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field(FIELD1, sentence_id));
        fields.add(new InsertParam.Field(FIELD2, sentences));
        fields.add(new InsertParam.Field(FIELD3, svec));

        System.out.println("INSERTING DATA INTO [" + coll + "]............");
        System.out.println("sentence id [" + sentence_id.size() + "]");
        System.out.println("sentences [" + sentences.size() + "]");
        System.out.println("vector [" + svec.size() + "]");
        System.out.println("vector embedding [" + svec.get(0).size() + "]");

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(coll)
                .withPartitionName(PARTITION_NAME)
                .withFields(fields)
                .build();
        R<MutationResult> response = mc.insert(insertParam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.err.println("***ERROR: insert() failed");
            throw new VectorDBException("Cannot insert() into collection [" + coll + "]");
        } else {
            System.out.println("Success - insert()");
        }

    }

    /************************************************************
     population_ollection_dummy - just a convenience method for teting...
     ***********************************************************/
    public void populate_collection_dummy(String coll, int numentries, int vecsize) {
        System.out.println("dummy data... populate_collection() with " + numentries + " rows -------------------------");
        Utility util = new Utility();
        try {
            populate_collection(coll,
                      util.createDummySentenceIds(numentries),
                      util.createDummySentences(numentries),
                      util.createDummyEmbeddings(numentries, vecsize));
        } catch (VectorDBException vex) {
            System.err.println("***ERROR: Cannot populate coll [" + coll + "] in database.");
        }

    }

    /************************************************************
     load Collection - collection must be loaded into memory to work
     ***********************************************************/
    public void loadCollection(String coll) {
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
     show all the collections in the database
     ***********************************************************/
    public void show_collections() throws VectorDBException {
        check_init();
        R<ListCollectionsResponse> cr = mc.listCollections(ListCollectionsParam.newBuilder().build());
        System.out.println("Collections in the DB: " + cr.getData());
    }

    /************************************************************
     drop a specific collection
     ***********************************************************/
    public void drop_collection(String coll) throws VectorDBException {
        check_init();
        System.out.println("DROPPING COLLECTION " + coll + " IN [" + this.database + "]..............");
        DropCollectionParam dropParam = DropCollectionParam.newBuilder()
                .withCollectionName(coll)
                .build();
        R<RpcStatus> response = mc.dropCollection(dropParam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println("***FAILURE: " + response.getMessage());
        } else
            System.out.println("COLLECTION [" + coll + "] Dropped... SUCCESS ***");
    }

    /************************************************************
     If collection exists return true
     ***********************************************************/
    public boolean collectionExists(String coll) throws VectorDBException {
        check_init();
        R<Boolean> response = mc.hasCollection(HasCollectionParam.newBuilder()
                .withCollectionName(coll)
                .build());
        handleResponseStatus(response);
        return response.getData().booleanValue();
    }

    /************************************************************
     Just a convenience method
     ***********************************************************/
    private void collExists(String coll) throws VectorDBException {
        if (collectionExists(coll) == true) {
            System.out.println("collection [" + coll + "] does exist");
        } else
            System.out.println("collection [" + coll + "] does NOT exist");
    }

    /************************************************************
     Flush - data is sealed and then flushed to storage
     ***********************************************************/
    public void flush_collection(String coll) {
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
     show collection statistics
     ***********************************************************/
    public void show_collection_stats(String coll) throws VectorDBException {
        check_init();
        System.out.println("GETTING COLLECTION STATS FOR " + coll + " IN [" + MILVUS_DATABASE + "]..............");
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

    long getCollectionRowCount(String coll) throws VectorDBException {
        long rows = 0;

        if (collectionExists(coll) != true) {
            System.err.println("Collection [" + coll + "] does not exist.");
            return 0;
        }
        GetCollectionStatisticsParam param = GetCollectionStatisticsParam.newBuilder()
                .withCollectionName(coll)
                .build();
        R<GetCollectionStatisticsResponse> cresponse = mc.getCollectionStatistics(param);
        if (cresponse.getStatus() != R.Status.Success.getCode()) {
            System.out.println("**ERROR in getting collection row. " + cresponse.getMessage());
        } else {
            System.out.println("Success in getting collection row!!");

            GetCollStatResponseWrapper wrapper = new GetCollStatResponseWrapper(cresponse.getData());
            rows = wrapper.getRowCount();
            System.out.println("collection [" + coll + "] Row count: " + rows);
        }
        return rows;
    }

    /************************************************************
     *
     *       QUERY and SEARCH
     *       Query - find entries that match a filter
     *       Search - find semantically close entries [optional: that match a filter]
     *
     ************************************************************/

    /************************************************************
     Query the DB for specific filters
     - currently restricted to "id, sentence, sentence-embedding-vector"
     as FIELD1, FIELD2, and FIELD3
     coll - collection name
     query - query filter
     max - limit on how many to return
     ***********************************************************/
    public List<String> queryDB(String coll, String query, Long max) throws VectorDBException {
        loadCollection(coll);
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
            throw new VectorDBException("**ERROR: Query FAILED! " + respQuery.getMessage());
        } else {
            System.out.println("QUERY successful!");
        }

        QueryResultsWrapper wrapperQuery = new QueryResultsWrapper(respQuery.getData());
        long numrows = wrapperQuery.getRowCount();
        System.out.println("query returned " + numrows + " rows.");

        List<String> res = new ArrayList<>();
        List<QueryResultsWrapper.RowRecord> records = wrapperQuery.getRowRecords();
        for (QueryResultsWrapper.RowRecord record : records) {
            res.add(record.toString());
        }
        return res;
        //System.out.println(wrapperQuery.getFieldWrapper(FIELD1).getFieldData());
        //System.out.println(wrapperQuery.getFieldWrapper(FIELD2).getFieldData());
    }

    /************************************************************
     Semantic Search the DB - optionally use a filter
     - currently restricted to "id, sentence, sentence-embedding-vector"
     as FIELD1, FIELD2, and FIELD3

     coll - Collection name
     vec - list of embedding vectors.  You want to find nearest neighbors to these vectors
     For our Question/Answer application, we only have one element for this list
     max - maximum number of returned matches
     ***********************************************************/
    public List<String> searchDB_using_targetvectors(String coll, List<List<Float>> vec, int max) {
        loadCollection(coll);

        System.err.println("DEBUG: size of targetVectors: " + vec.size());
        SearchParam param = SearchParam.newBuilder()
                .withCollectionName(coll)
                .withMetricType(MetricType.L2)
                .withTopK(max)
                .withVectors(vec)                   // Search closest neighbors to these
                .withVectorFieldName(FIELD3)        // ... using this field (my embedding array)
                .withConsistencyLevel(ConsistencyLevelEnum.EVENTUALLY)
                .addOutField(FIELD2)                // IMPORTANT FIELD TO ADD!
                //.withParams("{\"nprobe\":10,\"offset\":2, \"limit\":3}")
                //.withParams("{\"nprobe\":10")
                .build();

        R<SearchResults> resp = mc.search(param);
        if (resp.getStatus() != R.Status.Success.getCode()) {
            System.err.println("***ERROR: Cannot Search. " + resp.getMessage());
            return new ArrayList<>();
        } else {
            System.err.println("SEMANTIC SEARCH SUCCESS!!");
            return getSearchData(resp, vec.size());     // get the actual data
        }
    }

    /************************************************************
     Semantic Search the DB - optionally use a filter
     - currently restricted to "id, sentence, sentence-embedding-vector"
     as FIELD1, FIELD2, and FIELD3

     coll - Collection name
     target - original prompt from user
     max - maximum number of returned matches
     ***********************************************************/

    public List<String> searchDB(String coll, String target, int max) {
        loadCollection(coll);
        Random random = new Random();

        List<List<Float>> targetVectors = new ArrayList<>(2);     // I only need 1 target embedding
        List<Float> tv = new ArrayList<>(2);
        for (int i = 0; i < OPENAI_VECSIZE; i++) {                        // **********  HARDCODED
            tv.add(random.nextFloat());
        }
        targetVectors.add(tv);

        return searchDB_using_targetvectors(coll, targetVectors, max);
    }

    private List<String> getSearchData(R<SearchResults> resp, int size) {
        SearchResultsWrapper wrapper = new SearchResultsWrapper(resp.getData().getResults());
        System.out.println("Search results:");
        List<String> results = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            List<SearchResultsWrapper.IDScore> scores = wrapper.getIDScore(i);
            for (SearchResultsWrapper.IDScore score : scores) {
                System.out.println("[" + score.getScore() + "]" + "[" + score.get(FIELD2) + "]");
                results.add((String) score.get(FIELD2));
            }
        }
        return results;
    }

    /*
      Just some VectorDB tests...
     */
    public static void main(String[] args) throws VectorDBException {
        VectorDB vdb = new VectorDB(MILVUS_DATABASE);

        vdb.show_databases();
        vdb.show_collection_stats(COLLECTION_NAME);

        vdb.collExists(COLLECTION_NAME);
        System.out.println("Before drop.  Rows: " + vdb.getCollectionRowCount(COLLECTION_NAME));

        if (vdb.collectionExists(COLLECTION_NAME)) {
            vdb.drop_collection(COLLECTION_NAME);
        }
        System.out.println("After drop.  Rows: " + vdb.getCollectionRowCount(COLLECTION_NAME));

        vdb.collExists(COLLECTION_NAME);

        vdb.show_databases();
        vdb.create_database(MILVUS_DATABASE);
        vdb.show_databases();

        System.out.println("Creating collection [" + COLLECTION_NAME + "]");
        vdb.create_collection(COLLECTION_NAME, OPENAI_VECSIZE);

        System.out.println("Populating collection [" + COLLECTION_NAME + "]");
        vdb.populate_collection_dummy(COLLECTION_NAME, 200, OPENAI_VECSIZE);
        vdb.flush_collection(COLLECTION_NAME);         // You need to flush the collection to storage!

        System.out.println("After populate_collection().  Rows: " + vdb.getCollectionRowCount(COLLECTION_NAME));

        vdb.show_collection_stats(COLLECTION_NAME);

        vdb.collExists(COLLECTION_NAME);

        List<String> qres = vdb.queryDB(COLLECTION_NAME, "sentence_id > 25 and sentence_id < 75", Long.parseLong("5"));
        qres.forEach(System.out::println);

        System.out.println("========================================");

        List<String> res = vdb.searchDB(COLLECTION_NAME, "why does the NYJavaSIG exist?", 5);
        res.forEach(System.out::println);
    }
}
