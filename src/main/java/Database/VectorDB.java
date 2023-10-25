package Database;

import Utilities.Utility;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.*;
import io.milvus.param.*;
import io.milvus.param.collection.*;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.QueryParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.highlevel.collection.ListCollectionsParam;
import io.milvus.param.highlevel.collection.response.ListCollectionsResponse;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.param.partition.CreatePartitionParam;
import io.milvus.response.*;
import io.milvus.response.SearchResultsWrapper;


import java.io.IOException;
import java.util.*;

public class VectorDB {
    static final int MILVUS_PORT = 19530;                // default port
    static final String MILVUS_HOST = "localhost";      // default host
    //static final String MILVUS_DATABASE = "frankdb";    // DB to start off... Milvus doesn't have "use db" yet
    //static final String COLLECTION_NAME; = "frankcollection";
    static final String PARTITION_NAME = "sentence_partition";      // chunk-partition??
    static final String COLLECTION_DESCRIPTION = "Simple test of collection insertion and query/search";
    static final String FIELD1 = "sentence_id";         // in the future... change these to "chunk" not sentences
    static final String FIELD2 = "sentence";            // actual sentence stored as meta-data for later retrieval
    static final String FIELD3 = "sentence_vector";
    static final int MAX_SENTENCE_LENGTH = 5120;
    private int maxSentenceLength = MAX_SENTENCE_LENGTH;
    private String host = MILVUS_HOST;
    private int port = MILVUS_PORT;
    private String database;
    private String collection;
    private boolean initialized = false;
    static final int OPENAI_VECSIZE = 1536;
    private int vecsize;
    private final static String DEFAULT_CONFIG = "/Users/fgreco/src/Finetuning/src/main/resources/llm.properties";
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

    public VectorDB(String configfile) {
        Utility util = new Utility();
        Properties prop = null;
        try {
            prop = util.getConfigProperties(configfile);
        } catch (IOException iox) {
            System.err.println("Cannot find config file [" + configfile + "]");
        }

        setparams(prop);
    }

    public VectorDB(Properties prop) {
        setparams(prop);
    }

    private void setparams(Properties prop) {
        this.database = prop.getProperty("vdbservice.database");
        this.collection = prop.getProperty("vdbservice.collection");
        this.host = prop.getProperty("vdbservice.host");
        this.port = Integer.parseInt(prop.getProperty("vdbservice.port", "19530"));
        this.maxSentenceLength = Integer.parseInt(prop.getProperty("vdbservice.sentence_size", "5120"));
        this.vecsize = Integer.parseInt(prop.getProperty("llmservice.vector_size", "" + OPENAI_VECSIZE));

        connectToMilvus(this.host, this.port);
        this.initialized = true;
    }

    public void setMaxSentenceLength(int maxSentenceLength) {
        this.maxSentenceLength = maxSentenceLength;
    }

    /************************************************************
     * Can't go over this sentence size
     ***********************************************************/
    public int getMaxSentenceLength() {
        return maxSentenceLength;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    /************************************************************
     Currently you can not change the database after you create one dynamically
     ... seems odd to me since RDBMS have been doing this for decades.
     ***********************************************************/
    public String getDatabase() {
        return database;
    }

    public int getVecsize() {
        return vecsize;
    }

    public void setVecsize(int vecsize) {
        this.vecsize = vecsize;
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
        mc.setLogLevel(LogLevel.Warning);       // Debug has more info
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
        //System.out.println("In drop_database() - Collections in the DB: " + lc2.getData());

        R<RpcStatus> response;
        //System.out.println("DROPPING DATABASE [" + dbname + "]..............");
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
        //System.out.println("CREATING DATABASE [" + dbname + "]..............");
        CreateDatabaseParam cparam = CreateDatabaseParam.newBuilder()
                .withDatabaseName(dbname)
                .build();
        response = mc.createDatabase(cparam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println("***FAILURE: " + response.getMessage());
        }
    }

    /************************************************************
     Show all the databases in the server
     ***********************************************************/
    public List<String> get_databases() throws VectorDBException {
        check_init();
        List<String> dbnames = new ArrayList<>();
        String dbstr = "";

        R<ListDatabasesResponse> dbresponse = mc.listDatabases();
        //System.out.println("Databases in the server==========");
        if (dbresponse.getStatus() != R.Status.Success.getCode()) {
            System.out.println(dbresponse.getMessage());
        } else {
            //System.out.println("***Success in listing databases...");
            for (int i = 0; i < dbresponse.getData().getDbNamesCount(); i++) {
                dbstr = dbresponse.getData().getDbNames(i);
                dbnames.add(dbstr);
                //System.out.println(dbstr);
            }
        }
        return dbnames;
    }

    public String show_databases() throws VectorDBException {
        return this.get_databases().toString();
    }

    /************************************************************
     If database name exists in the server, return true.
     ***********************************************************/
    public boolean databaseExists(String dbname) throws VectorDBException {
        List<String> databases = new ArrayList<>();
        boolean exists = false;
        databases = get_databases();
        for (int i = 0; i < databases.size(); i++) {
            if (databases.get(i).equalsIgnoreCase(dbname))
                return true;
        }
        return exists;
    }

    /************************************************************
     *
     *       COLLECTION METHODS
     *
     ************************************************************/
    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    /************************************************************
     Create the field schemas, create the collection schemas in the DB
     Create Milvus collection schema... you need datatype for each field
     A Milvus Collection is like a RDBMS table.

     TBD - change "sentence" to "chunk"... we may not use sentences in the future.
     ***********************************************************/
    public void create_collection(String coll, int vecsize) {
        R<RpcStatus> response = null;

        FieldType fieldType1 = FieldType.newBuilder()        // schema for the id of the entry
                .withName(FIELD1)
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(false)
                .build();
        FieldType fieldType2 = FieldType.newBuilder()       // schema for num words in sentence (just an arbitrary field)
                .withName(FIELD2)
                .withDataType(DataType.VarChar)
                .withMaxLength(MAX_SENTENCE_LENGTH)         // VarChar requires "withMaxLength()
                .build();
        FieldType fieldType3 = FieldType.newBuilder()       // schema for the actual vector
                .withName(FIELD3)
                .withDataType(DataType.FloatVector)
                .withDimension(vecsize)
                .build();

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
        }

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
        }
    }

    public void create_collection(String coll) {
        this.create_collection(coll, this.getVecsize());
    }

    /*********************************************************************
     Populate collection with data

     sentences - list of sentences to be inserted into Milvus
     sentence_id - list of sentence ids
     svec - list of vectors, one list for each sentence
     vecsize - size of the vector array for each element of sentences list (only changes if embedding algo changes)
     ********************************************************************/
    public void insert_collection(String coll, List<Long> sentence_id, List<String> sentences, List<List<Float>> svec)
            throws VectorDBException {

        // Arrays must be the same length...
        if (!(sentence_id.size() == sentences.size() && sentence_id.size() == svec.size())) {
            throw new VectorDBException("***ERROR: populate_collection() - array sizes must match.");
        } else if (sentences == (List<String>) null) {
            throw new VectorDBException("***ERROR: sentence array is null.  Cannot insert into database.");
        } else if (svec == (List<List<Float>>) null) {
            throw new VectorDBException("***ERROR: embeddings array is null.  Cannot insert into database.");
        }

        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field(FIELD1, sentence_id));
        fields.add(new InsertParam.Field(FIELD2, sentences));
        fields.add(new InsertParam.Field(FIELD3, svec));

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(coll)
                .withPartitionName(PARTITION_NAME)
                .withFields(fields)
                .build();
        R<MutationResult> response = mc.insert(insertParam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.err.println("***ERROR: insert() failed");
            throw new VectorDBException("Cannot insert() into collection [" + coll + "]");
        }
    }

    /************************************************************
     population_collection_dummy - just a convenience method for testing...
     Need to delete this method SOON!
     ***********************************************************/
    private void populate_collection_dummy(String coll, int numentries, int vecsize) {
        System.out.println("****dummy data... populate_collection() with " + numentries + " rows -------------------------");
        Utility util = new Utility();
        try {
            insert_collection(coll,
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
        }
    }

    /************************************************************
     show all the collections in the database
     ***********************************************************/
    public String get_collections() throws VectorDBException {
        check_init();
        R<ListCollectionsResponse> cr = mc.listCollections(ListCollectionsParam.newBuilder().build());
        System.out.println("Collections in the DB: " + cr.getData());
        return cr.getData().toString();
    }

    public String show_collections() throws VectorDBException {
        String buffer = "";

        R<ShowCollectionsResponse> respShowCollections = mc.showCollections(
                ShowCollectionsParam.newBuilder().build()
        );
        for (int i = 0; i < respShowCollections.getData().getCollectionNamesCount(); i++) {
            buffer += respShowCollections.getData().getCollectionNames(i) + " ";
        }
        return buffer;
    }

    /************************************************************
     drop a specific collection
     ***********************************************************/
    public void drop_collection(String coll) throws VectorDBException {
        check_init();
        DropCollectionParam dropParam = DropCollectionParam.newBuilder()
                .withCollectionName(coll)
                .build();
        R<RpcStatus> response = mc.dropCollection(dropParam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println("***ERROR: Cannot drop collection.  " + response.getMessage());
        }
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
     insert_entry() - Insert an entry from a collection - NEED TO TEST
     NEED TO DETECT INSERTING DUPLICATES... need to store hash of sentence and compare to hash of incoming sentence.
     ***********************************************************/
    public void insert_entry(String coll, String sent, List<Float> vec) {
        List<Integer> ilist = new ArrayList<>();
        ilist.add(1000);                            // arbitrary int  - FIX... need to get largest id from VDB, add 1 -fdg
        List<String> slist = new ArrayList<>();
        slist.add(sent);

        List<InsertParam.Field> fieldsInsert = new ArrayList<>();
        fieldsInsert.add(new InsertParam.Field(FIELD1, ilist));
        fieldsInsert.add(new InsertParam.Field(FIELD2, slist));
        fieldsInsert.add(new InsertParam.Field(FIELD3, vec));

        InsertParam param = InsertParam.newBuilder()
                .withCollectionName(coll)
                .withFields(fieldsInsert)
                .build();

        R<MutationResult> resp = mc.insert(param);
        if (resp.getStatus() != R.Status.Success.getCode()) {
            System.err.println("***ERROR:  Cannot insert single row.");
        } else {
            flush_collection(coll);
        }
    }


    /************************************************************
     delete_entry() - Delete an entry from a collection where there is a string match - NEED TO TEST - fdg
     NEED A BETTER WAY TO DELETE AN ENTRY.
     ***********************************************************/
    public void delete_entry(String coll, String str) {
        String deleteStr = "sentence = " + "\"" + str + "\"";   // NOT sure if this is a good idea... -fdg

        R<MutationResult> resp = mc.delete(
                DeleteParam.newBuilder()
                        .withCollectionName(coll)
                        .withExpr(deleteStr)
                        .build()
        );

        if (resp.getStatus() != R.Status.Success.getCode()) {
            System.out.println(resp.getMessage());
        } else {
            System.out.println("Delete entry successful!");
            flush_collection(coll);
        }
    }

    /************************************************************
     Flush - data is sealed and then flushed to storage    - important
     ***********************************************************/
    public void flush_collection(String coll) {
        FlushParam param = FlushParam.newBuilder()
                .addCollectionName(coll)
                .build();
        R<FlushResponse> response = mc.flush(param);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println(response.getMessage());
        }
    }

    /************************************************************
     show collection statistics
     ***********************************************************/
    public void show_collection_stats(String coll) throws VectorDBException {
        check_init();
        //System.out.println("GETTING COLLECTION STATS FOR " + coll + " IN [" + MILVUS_DATABASE + "]..............");

        if (!collectionExists(coll)) {
            System.err.println("***ERROR:  Collection [" + coll + "] does not exist");
            return;
        }

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
            GetCollStatResponseWrapper wrapper = new GetCollStatResponseWrapper(cresponse.getData());
            System.out.println("Row count: " + wrapper.getRowCount());
            System.out.println("[" + coll + "]: " + wrapperDescribeCollection);
        }
    }

    /************************************************************
     Get the number of rows in a collection
     ***********************************************************/
    public long getCollectionRowCount(String coll) throws VectorDBException {
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
            System.out.println("**ERROR in getting collection stats. " + cresponse.getMessage());
        } else {
            GetCollStatResponseWrapper wrapper = new GetCollStatResponseWrapper(cresponse.getData());
            rows = wrapper.getRowCount();
            //System.out.println("collection [" + coll + "] Row count: " + rows);
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
     Query the DB for specific filters   - see search() for finding vector neighbors
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
    public List<String> searchDB_using_targetvectors(String coll, List<List<Float>> vec, int max) throws VectorDBException{
        loadCollection(coll);

        //System.err.println("DEBUG: size of targetVectors: " + vec.size());
        SearchParam param = SearchParam.newBuilder()
                .withCollectionName(coll)
                .withMetricType(MetricType.L2)
                .withTopK(max)
                .withVectors(vec)                   // Search closest neighbors to these
                .withVectorFieldName(FIELD3)        // ... using this field (my embedding array)
                .withConsistencyLevel(ConsistencyLevelEnum.EVENTUALLY)
                .addOutField(FIELD2)                // IMPORTANT FIELD TO ADD!
                //.withParams("{\"nprobe\":10,\"offset\":2, \"limit\":3}")
                .build();

        R<SearchResults> resp = mc.search(param);
        SearchResultsWrapper wrapper;

        if (resp.getStatus() != R.Status.Success.getCode()) {
            System.err.println("***ERROR: Cannot Search. " + resp.getMessage());
            return new ArrayList<>();
        } else {
            wrapper = new SearchResultsWrapper(resp.getData().getResults());
            //System.err.println("NUM ROWS: " + wrapper.getRowRecords().size());
            if (wrapper.getRowRecords().size() == 0) {                  // FIX THIS!!! if its 0, it should skip the search...
                //throw new VectorDBException("No entries match in the database");
                return new ArrayList<>();
            }
            return getSearchData(resp, vec.size());     // get the actual data
        }
    }


    private List<String> getSearchData(R<SearchResults> resp, int size) {
        SearchResultsWrapper wrapper = new SearchResultsWrapper(resp.getData().getResults());
        List<String> results = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            List<SearchResultsWrapper.IDScore> scores = wrapper.getIDScore(i);
            for (SearchResultsWrapper.IDScore score : scores) {
                //System.out.println("[" + score.getScore() + "]" + "[" + score.get(FIELD2) + "]");
                results.add((String) score.get(FIELD2));
            }
        }
        return results;
    }

    /******************************************************
     main() - Just some VectorDB tests...
     *****************************************************/
    public static void main(String[] args) throws VectorDBException {
        VectorDB vdb = new VectorDB(DEFAULT_CONFIG);

        vdb.show_databases();
        vdb.setCollection(vdb.getCollection());

        String collection = vdb.getCollection();
        String database = vdb.getDatabase();

        vdb.show_collection_stats(collection);

        vdb.collExists(collection);
        System.out.println("Before drop.  Rows: " + vdb.getCollectionRowCount(collection));

        if (vdb.collectionExists(collection)) {
            vdb.drop_collection(collection);
        }
        System.out.println("After drop.  Rows: " + vdb.getCollectionRowCount(collection));

        vdb.collExists(collection);

        if (!vdb.databaseExists(database)) {
            vdb.create_database(database);
        }
        vdb.show_databases();

        System.out.println("Creating collection [" + collection + "]");
        vdb.create_collection(collection, OPENAI_VECSIZE);

        System.out.println("Populating collection [" + collection + "]");    // just use dummy data here...
        vdb.populate_collection_dummy(collection, 200, OPENAI_VECSIZE);
        vdb.populate_collection_dummy(collection, 200, OPENAI_VECSIZE);          //Should have 400 entries by this point

        vdb.flush_collection(collection);         // You need to flush the collection to storage!

        System.out.println("After populate_collection().  Rows: " + vdb.getCollectionRowCount(collection));

        vdb.show_collection_stats(collection);

        vdb.collExists(collection);

        List<String> qres = vdb.queryDB(collection, "sentence_id > 25 and sentence_id < 75", Long.parseLong("5"));
        qres.forEach(System.out::println);

        System.out.println("========================================");

        List<String> res = vdb.searchDB(collection, "why does the NYJavaSIG exist?", 5);
        res.forEach(System.out::println);
    }

    /************************************************************
     Semantic Search the DB - optionally use a filter
     - currently restricted to "id, sentence, sentence-embedding-vector"
     as FIELD1, FIELD2, and FIELD3

     coll - Collection name
     target - original prompt from user
     max - maximum number of returned matches
     ***********************************************************/

    public List<String> searchDB(String coll, String target, int max) throws VectorDBException{
        System.err.println("DUMMY CALL TO SearchDB().  Do NOT use this method... only for testing.");
        loadCollection(coll);
        Random random = new Random();
        System.out.println("********** DO NOT USE searchDB()... just for TESTING");
        List<List<Float>> targetVectors = new ArrayList<>(2);     // I only need 1 target embedding
        List<Float> tv = new ArrayList<>(2);
        for (int i = 0; i < OPENAI_VECSIZE; i++) {                        // **********  HARDCODED
            tv.add(random.nextFloat());
        }
        targetVectors.add(tv);

        return searchDB_using_targetvectors(coll, targetVectors, max);
    }
}
