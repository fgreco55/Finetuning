/*
 Quick pgm to test access to Milvus server using their Java SDK. - fdg

 Stream of consciousness programming just to test new concepts in Milvus - fdg
 */

import io.milvus.client.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.GetCollectionStatisticsResponse;
import io.milvus.grpc.ListDatabasesResponse;
import io.milvus.param.ConnectParam;
import io.milvus.param.LogLevel;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.*;
import io.milvus.param.dml.InsertParam;
import io.milvus.response.GetCollStatResponseWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MilvusTest {
    static final String COLLECTION_NAME = "franktest";
    static final String DB_NAME = "frankdb";
    static final String FIELD1 = "sentence_id";
    static final String FIELD2 = "word_count";
    static final String FIELD3 = "sentence_vector";
    static final int MILVUS_PORT = 19530;

    public static void main(String[] args) throws IOException {

        R<RpcStatus> response = null;

        final MilvusServiceClient milvusClient = new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost("localhost")
                        .withPort(MILVUS_PORT)
                        .build()
        );
        milvusClient.setLogLevel(LogLevel.Debug);

        show_databases(milvusClient);
        //show_collection_stats(milvusClient, COLLECTION_NAME);
        drop_collection(milvusClient, COLLECTION_NAME);
        drop_database(milvusClient, DB_NAME);
        
        list_database_names(milvusClient);
        create_database(milvusClient, DB_NAME);
        list_database_names(milvusClient);


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
                .withDataType(DataType.Int64)
                .build();
        FieldType fieldType3 = FieldType.newBuilder()       // schema for the actual vector
                .withName(FIELD3)
                .withDataType(DataType.FloatVector)
                .withDimension(2)
                .build();

        System.out.println("CREATING COLLECTION [" + COLLECTION_NAME + "]............");

        CreateCollectionParam createCollectionReq = CreateCollectionParam.newBuilder()      // Create collection
                .withCollectionName(COLLECTION_NAME)
                .withDescription("Test frank search")
                .withShardsNum(2)
                .addFieldType(fieldType1)
                .addFieldType(fieldType2)
                .addFieldType(fieldType3)
                .withEnableDynamicField(true)
                .build();
        response = milvusClient.createCollection(createCollectionReq);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println("***FAILURE: " + response.getMessage());
        } else
            System.out.println("Collection created... SUCCESS ***");
        
        /*
            Now that you have the Collection, create the data
         */

        System.out.println("CREATING DATA FOR [" + COLLECTION_NAME + "]............");

        Random ran = new Random();
        List<Long> sentence_id_array = new ArrayList<>();               // to store sentence_ids
        List<Long> word_count_array = new ArrayList<>();                // to store num of words in that sentence
        List<List<Float>> sentence_vector_array = new ArrayList<>();    // to store vector for that sentence

        for (long i = 0L; i < 100; i++) {                               // simulate 100 sentences
            sentence_id_array.add(i);
            word_count_array.add(ran.nextLong(25));               // just pick random word length
            List<Float> vector = new ArrayList<>();
            for (int k = 0; k < 2; k++) {                               // simulated vector (size 2)
                vector.add(ran.nextFloat());
            }
            sentence_vector_array.add(vector);
        }

        /*
            Now inserting the data for the Collection
         */
        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field(FIELD1, sentence_id_array));
        fields.add(new InsertParam.Field(FIELD2, word_count_array));
        fields.add(new InsertParam.Field(FIELD3, sentence_vector_array));

        System.out.println("INSERTING DATA INTO [" + COLLECTION_NAME + "]............");

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                // .withPartitionName("partition1")
                .withFields(fields)
                .build();
        milvusClient.insert(insertParam);

        System.out.println("TESTING IF Milvus HAS COLLECTION [" + COLLECTION_NAME + "]............");

        R<Boolean> respHasCollection = milvusClient.hasCollection(
                HasCollectionParam.newBuilder()
                        .withCollectionName(COLLECTION_NAME)
                        .build()
        );
        if (respHasCollection.getData() == Boolean.TRUE) {
            System.out.println("Collection exists.");
        } else
            System.out.println("Collection DOES NOT exist.");
        drop_database(milvusClient, DB_NAME);
        /*
         A better test would be to test if the collection exists and then retrieve that collections' values.
         Another test needs to test the Milvus vector search.
         */

        milvusClient.close();
    }

    /************************************************************
         show databases
     */
    static void show_databases(MilvusClient mc) {
        R<ListDatabasesResponse> dbresponse = mc.listDatabases();
        System.out.println("Databases in the server==========");
        if (dbresponse.getStatus() != R.Status.Success.getCode()) {
           System.out.println(dbresponse.getMessage());
       } else
            System.out.println("***Success in listing databases...");

        for (int i=0; i< dbresponse.getData().getDbNamesCount(); i++) {
            System.out.println(dbresponse.getData().getDbNames(i));
        }

    }
    /************************************************************
       drop_collection
     */
    static void drop_collection(MilvusClient mc, String coll) {
        R<RpcStatus> response;
        System.out.println("DROPPING COLLECTION " + coll + " IN [" + DB_NAME + "]..............");
        DropCollectionParam dropParam = DropCollectionParam.newBuilder()
                .withCollectionName(coll)
                .build();
        response = mc.dropCollection(dropParam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println("***FAILURE: " + response.getMessage());
        } else
            System.out.println("COLLECTION Dropped... SUCCESS ***");
    }
    /************************************************************
      show collection statistics
     */
    static void show_collection_stats(MilvusClient mc, String coll) {
        System.out.println("GETTING COLLECTION STATS FOR " + coll + " IN [" + DB_NAME + "]..............");
        GetCollectionStatisticsParam param = GetCollectionStatisticsParam.newBuilder()
                .withCollectionName(coll)
                .build();
        R<GetCollectionStatisticsResponse> cresponse = mc.getCollectionStatistics(param);
        if (cresponse.getStatus() != R.Status.Success.getCode()) {
            System.out.println("**ERROR in getting collection stats. " + cresponse.getMessage());
        }  else
            System.out.println("Success in getting collection stats!!");

        GetCollStatResponseWrapper wrapper = new GetCollStatResponseWrapper(cresponse.getData());
        System.out.println("Row count: " + wrapper.getRowCount());
    }

    /************************************************************
        drop specific database
     */
    static void drop_database(MilvusClient mc, String dbname) {
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

    static void list_database_names(MilvusClient mc) {
        System.out.println("LISTING DATABASE NAMES............");
        R<ListDatabasesResponse> ldbresponse = mc.listDatabases();
        if (ldbresponse.getStatus() != R.Status.Success.getCode()) {
            System.out.println("***FAILURE: " + ldbresponse.getMessage());
        } else {
            System.out.println("===========================");
            for (int i = 0; i < ldbresponse.getData().getDbNamesCount(); i++) {
                System.out.println(ldbresponse.getData().getDbNames(i));
            }
        }
    }
}

