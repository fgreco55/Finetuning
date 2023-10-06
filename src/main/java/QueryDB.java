import com.theokanning.openai.embedding.Embedding;
import com.theokanning.openai.embedding.EmbeddingRequest;
import io.milvus.client.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.ListDatabasesResponse;
import io.milvus.grpc.QueryResults;
import io.milvus.grpc.ShowCollectionsResponse;
import io.milvus.param.ConnectParam;
import io.milvus.param.LogLevel;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.collection.ShowCollectionsParam;
import io.milvus.param.dml.QueryParam;
import io.milvus.response.QueryResultsWrapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class QueryDB {
    static final String COLLECTION_NAME = "frankcollection";
    static final String DB_NAME = "frankdb";
    static final String FIELD1 = "sentence_id";
    static final String FIELD2 = "sentence";            // actual sentence stored as meta-data for later retrieval
    static final String FIELD3 = "sentence_vector";
    static final int MILVUS_PORT = 19530;

    public static void main(String[] args) {
        R<RpcStatus> response = null;
        Scanner userinput;

        final MilvusServiceClient milvusClient = new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost("localhost")
                        .withDatabaseName(DB_NAME)
                        .withPort(MILVUS_PORT)
                        .build()
        );
        milvusClient.setLogLevel(LogLevel.Debug);

        R<ListDatabasesResponse> ddd = milvusClient.listDatabases();
        System.out.print("MILVUS DEBUG: " + ddd.getData());

        show_databases(milvusClient);
        showAllCollections(milvusClient);
        loadCollection(milvusClient, COLLECTION_NAME);
        showAllCollections(milvusClient);
        if ( !doesCollectionExist(milvusClient, DB_NAME, COLLECTION_NAME) ) {
            System.err.println("***ERROR: collection [" + COLLECTION_NAME + "] does not exist!");
        }

        while (true) {
            System.out.print("Query> ");
            userinput = new java.util.Scanner(System.in);

            if (userinput.hasNextLine()) {
                String cmd = userinput.nextLine();
                if (!cmd.isEmpty()) {
                    queryDB(milvusClient, COLLECTION_NAME, cmd);      //"sentence_id in [2,4,6,8]"
                }
            }
        }
    }

    static void loadCollection(MilvusClient mc, String coll) {
        System.out.println("LOADING COLLECTION [" + coll + "]");
        R<RpcStatus> loadc = mc.loadCollection(
                    LoadCollectionParam.newBuilder()
                            .withCollectionName(coll)
                            .build()
            );
            if (loadc.getStatus() != R.Status.Success.getCode()) {
                System.out.println("***ERROR Cannot load collection:  " + loadc.getMessage());
            } else {
                System.out.println("Success loading the collection");
            }
    }
    static void queryDB(MilvusClient mc, String coll, String query) {

        List<String> query_output_fields = Arrays.asList(FIELD1, FIELD2);

        QueryParam queryParam = QueryParam.newBuilder()
                .withCollectionName(coll)
                .withConsistencyLevel(ConsistencyLevelEnum.STRONG)
                .withExpr(query)
                .withOutFields(query_output_fields)
                .withOffset(0L)
                .withLimit(10L)
                .build();
        R<QueryResults> respQuery = mc.query(queryParam);

        QueryResultsWrapper wrapperQuery = new QueryResultsWrapper(respQuery.getData());
        System.out.println(wrapperQuery.getFieldWrapper(FIELD1).getFieldData());
        System.out.println(wrapperQuery.getFieldWrapper(FIELD2).getFieldData());
    }


    static boolean doesCollectionExist(MilvusClient mc, String db, String coll) {

        R<Boolean> respHasCollection = mc.hasCollection(
                HasCollectionParam.newBuilder()
                        .withCollectionName(coll)
                        .build()
        );
        if (respHasCollection.getData() == Boolean.TRUE) {
            System.out.println("Collection exists.");
            return true;
        } else {
            System.out.println("***ERROR:  " + respHasCollection.getMessage());
            return false;
        }
    }

    static void showAllCollections(MilvusClient mc) {
        R<ShowCollectionsResponse> respShowCollections = mc.showCollections(
            ShowCollectionsParam.newBuilder().build()
          );
        System.out.println(respShowCollections);
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

        for (int i = 0; i < dbresponse.getData().getDbNamesCount(); i++) {
            System.out.println(dbresponse.getData().getDbNames(i));
        }

    }

}
