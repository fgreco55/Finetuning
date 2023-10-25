package App;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.FlushResponse;
import io.milvus.param.ConnectParam;
import io.milvus.param.LogLevel;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.DropCollectionParam;
import io.milvus.param.collection.FlushParam;
import io.milvus.param.highlevel.collection.ListCollectionsParam;
import io.milvus.param.highlevel.collection.response.ListCollectionsResponse;

public class DeleteCollection {
    static final String DB_NAME = "frankdb";
    static final String COLLECTION_NAME = "frankcollection";

    public static void main(String[] args) {
        final MilvusServiceClient mc = new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost("localhost")
                        .withPort(19530)
                        .withDatabaseName(DB_NAME)
                        .build()
        );
        mc.setLogLevel(LogLevel.Debug);
        R<ListCollectionsResponse> lc1 = mc.listCollections(ListCollectionsParam.newBuilder().build());
        System.out.println("Collections in the DB: " + lc1.getData());

        System.out.println("DROPPING COLLECTION " + COLLECTION_NAME + " IN [" + DB_NAME + "]..............");
        DropCollectionParam dropParam = DropCollectionParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .build();
        R<RpcStatus> response = mc.dropCollection(dropParam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println("***FAILURE: " + response.getMessage());
        } else
            System.out.println("COLLECTION [" + COLLECTION_NAME + "] Dropped... SUCCESS ***");

        FlushParam param = FlushParam.newBuilder()
                .addCollectionName(COLLECTION_NAME)
                .build();
        R<FlushResponse> res = mc.flush(param);
        if (res.getStatus() != R.Status.Success.getCode()) {
            System.out.println(response.getMessage());
        } else {
            System.out.println("FLUSH successful!");
        }
    }
}
