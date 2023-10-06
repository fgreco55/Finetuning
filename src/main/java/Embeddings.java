/*
 Test app to get embeddings from OpenAI's GPT service.
 This app uses the Java API from Theo Kanning.      -fdg
 */
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

public class Embeddings {
    private final static String DEFAULT_CONFIG = "./src/main/resources/chatgpt.properties";

    public static void main(String[] args) throws IOException {

        //System.out.println("Working Directory = " + System.getProperty("user.dir"));
        Properties prop = getConfigProperties(DEFAULT_CONFIG);
        if (prop == (Properties) null) {
            System.err.println("Cannot find OpenAI API key.  Your path to the properties is probably incorrect.");
            System.exit(1);
        }
        String token = prop.getProperty("chatgpt.apikey");
        Scanner userinput;

        com.theokanning.openai.service.OpenAiService service = new OpenAiService(token);

        while (true) {
            System.out.print("String to Embed> ");
            userinput = new java.util.Scanner(System.in);

            if (userinput.hasNextLine()) {
                String cmd = userinput.nextLine();
                if (!cmd.isEmpty()) {

                    EmbeddingRequest embeddingRequest = EmbeddingRequest.builder()
                            .model("text-embedding-ada-002")
                            .input(Collections.singletonList(cmd))
                            .build();

                    List<Embedding> embeddings = service.createEmbeddings(embeddingRequest).getData();
                    System.out.println("There are " + embeddings.size() + " entries.");
                    embeddings.forEach(System.out::println);    // seems to be 1536 elements of the embedding vector
                }
            }
        }
    }
    static Properties getConfigProperties(String fname) throws IOException {
        Properties prop = new Properties();
        InputStream in;
        try {
            in = new FileInputStream(fname);
        } catch (IOException ix) {
            System.err.println("Properties file error: " + ix.getMessage());
            return (Properties) null;
        }
        
        prop.load(in);
        /*for (Enumeration e = prop.propertyNames(); e.hasMoreElements(); ) {
            String key = e.nextElement().toString();
            //System.out.println(key + " = " + prop.getProperty(key));
        }*/
        return prop;
    }
}
