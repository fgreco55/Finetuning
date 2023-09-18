/*
 Quick pgm to test access to Milvus server using their Java SDK. - fdg
 */

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.ListDatabasesResponse;
import io.milvus.param.ConnectParam;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.*;

import java.io.IOException;

public class MilvusTest {

    public static void main(String[] args) throws IOException {
        final String COLLECTION_NAME = "franktest";
        final String DB_NAME = "frankdb";

        final MilvusServiceClient milvusClient = new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost("localhost")
                        .withPort(19530)
                        .build()
        );

        System.out.println("DROPPING DATABASE [" + DB_NAME + "]..............");
        DropDatabaseParam param = DropDatabaseParam.newBuilder()
                .withDatabaseName(DB_NAME)
                .build();
        R<RpcStatus> response = milvusClient.dropDatabase(param);

        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println(response.getMessage());
        }

        System.out.println("CREATING DATABASE [" + DB_NAME + "]..............");

        CreateDatabaseParam cparam = CreateDatabaseParam.newBuilder()
                .withDatabaseName(DB_NAME)
                .build();
        R<RpcStatus> cresponse = milvusClient.createDatabase(cparam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println(response.getMessage());
        }

        System.out.println("LISTING DATABASE NAMES............");

        R<ListDatabasesResponse> ldbresponse = milvusClient.listDatabases();
        if (ldbresponse.getStatus() != R.Status.Success.getCode()) {
            System.out.println(ldbresponse.getMessage());
        }
        System.out.println("DROPPING COLLECTION [" + COLLECTION_NAME + "]............");

        milvusClient.dropCollection(
                DropCollectionParam.newBuilder()
                        .withCollectionName(COLLECTION_NAME)
                        .build()
        );
       /*
        Create Milvus collection from input data would go here...
        As a test, I've created some dummy data.
        */
        FieldType fieldType1 = FieldType.newBuilder()
                .withName("book_id")
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(false)
                .build();
        FieldType fieldType2 = FieldType.newBuilder()
                .withName("word_count")
                .withDataType(DataType.Int64)
                .build();
        FieldType fieldType3 = FieldType.newBuilder()
                .withName("book_intro")
                .withDataType(DataType.FloatVector)
                .withDimension(2)
                .build();

        System.out.println("CREATING COLLECTION [" + COLLECTION_NAME + "]............");

        CreateCollectionParam createCollectionReq = CreateCollectionParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withDescription("Test frank search")
                .withShardsNum(2)
                .addFieldType(fieldType1)
                .addFieldType(fieldType2)
                .addFieldType(fieldType3)
                .withEnableDynamicField(true)
                .build();
        R<Boolean> respHasCollection = milvusClient.hasCollection(
                HasCollectionParam.newBuilder()
                        .withCollectionName(COLLECTION_NAME)
                        .build()
        );
        if (respHasCollection.getData() == Boolean.TRUE) {
            System.out.println("Collection exists.");
        } else
            System.out.println("Collection DOES NOT exist.");

        /*
         A better test would be to test if the collection exists and then retrieve that collections' values.
         Another test needs to test the Milvus vector search.
         */

        milvusClient.close();
    }
}
