import com.theokanning.openai.embedding.Embedding;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.service.OpenAiService;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Arrays;


public class CompareEmbeddings {
    private final static String DEFAULT_CONFIG = "./src/main/resources/chatgpt.properties";

    public static void main(String[] args) throws IOException {

        //System.out.println("Working Directory = " + System.getProperty("user.dir"));
        Properties prop = getConfigProperties(DEFAULT_CONFIG);
        if (prop == null) {
            System.err.println("Cannot find OpenAI API key.  Your path to the properties is probably incorrect.");
            System.exit(1);
        }
        String token = prop.getProperty("chatgpt.apikey");
        Scanner userinput;

        com.theokanning.openai.service.OpenAiService service = new OpenAiService(token);

        List<Embedding> one = getEmbeddingVec(service, "I often play guitar for a very long time");
        List<Double> emb1 = one.get(0).getEmbedding();
        Double[] emb1d = emb1.toArray(new Double[0]);

        List<Embedding> two = getEmbeddingVec(service, "it is raining today");
        List<Double> emb2 = two.get(0).getEmbedding();
        Double[] emb2d = emb2.toArray(new Double[0]);
        
        double similarity = cosineSimilarity(Double2double(emb1d), Double2double(emb2d));
        System.out.println("Cosine Similarity: " + similarity);
    }

    public static double[] Double2double(Double[] indouble) {
        double[] result = new double[indouble.length];
        for(int i = 0; i < indouble.length; i++) {
              result[i] = indouble[i].doubleValue();
        }
        return result;
    }

    public static List<Embedding> getEmbeddingVec(OpenAiService service, String input) {
        EmbeddingRequest embeddingRequest = EmbeddingRequest.builder()
                .model("text-embedding-3-small")
                .input(Collections.singletonList(input))
                .build();

        List<Embedding> embeddings = service.createEmbeddings(embeddingRequest).getData();

        //System.out.println("There are "+embeddings.size()+" entries.");
        //System.out.println("One embedding has " + embeddings.get(0).getEmbedding().size() + " elements.");
        //embeddings.forEach(System.out::println);    // seems to be 1536 elements of the embedding vector
        return embeddings;
    }

    public static Properties getConfigProperties(String fname) throws IOException {
        Properties prop = new Properties();
        InputStream in;
        try {
            in = new FileInputStream(fname);
        } catch (IOException ix) {
            System.err.println("Properties file error: " + ix.getMessage());
            return null;
        }

        prop.load(in);
        return prop;
    }

    public static double dotProduct(double[] vec1, double[] vec2) {
        double dotProduct = 0;
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
        }
        return dotProduct;
    }

    // Function to calculate magnitude of a vector
    public static double magnitude(double[] vec) {
        double sum = 0;
        for (double v : vec) {
            sum += v * v;
        }
        return Math.sqrt(sum);
    }

    // Function to calculate cosine similarity
    public static double cosineSimilarity(double[] vec1, double[] vec2) {
        double dotProduct = dotProduct(vec1, vec2);
        double magnitudeVec1 = magnitude(vec1);
        double magnitudeVec2 = magnitude(vec2);

        if (magnitudeVec1 == 0 || magnitudeVec2 == 0) {
            return 0; // To avoid division by zero
        } else {
            return dotProduct / (magnitudeVec1 * magnitudeVec2);
        }
    }
}
