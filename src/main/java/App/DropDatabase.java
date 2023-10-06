package App;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.LogLevel;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.DropDatabaseParam;
import io.milvus.param.highlevel.collection.ListCollectionsParam;
import io.milvus.param.highlevel.collection.response.ListCollectionsResponse;

public class DropDatabase {
    static final String DB_NAME = "frankdb";

    public static void main(String[] args) {
        final MilvusServiceClient milvusClient = new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost("localhost")
                        .withPort(19530)
                        .withDatabaseName(DB_NAME)
                        .build()
        );
        milvusClient.setLogLevel(LogLevel.Debug);
        R<ListCollectionsResponse> lc1 = milvusClient.listCollections(ListCollectionsParam.newBuilder().build());
        System.out.println("Collections in the DB: " + lc1.getData());

        R<RpcStatus> response;
        System.out.println("DROPPING DATABASE [" + DB_NAME + "]..............");
        DropDatabaseParam dbparam = DropDatabaseParam.newBuilder()
                .withDatabaseName(DB_NAME)
                .build();
        response = milvusClient.dropDatabase(dbparam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println("***FAILURE: " + response.getMessage());
        } else
            System.out.println("DB Dropped... SUCCESS ***");
    }
}
