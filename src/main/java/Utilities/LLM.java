package Utilities;

import com.theokanning.openai.completion.chat.*;
import com.theokanning.openai.embedding.Embedding;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.service.OpenAiService;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/************************************************************
 LLM - Large Language Model abstraction

 QnD implementation... Needs to be refactored -fdg

 Use SPI for multiple implementations?
 Maybe split Embeddings from Completions?
 Maybe create CompletionRequest, CompletionResults,
 EmbeddingsRequest, EmbeddingsResults?
 ***********************************************************/
public class LLM {
    OpenAiService service;              // Needed for Theo Kanning API
    private String apikey;              // API key - should eventually ncrypt after use
    private String user;
    private final static String DEFAULT_CONFIG = "/Users/fgreco/src/Finetuning/src/main/resources/llm.properties";
    private String model;               // LLM model name (should ensure correct names)
                                        // "gpt-3.5-turbo" decent completion model
                                        // "text-embedding-ada-002" embedding model
    private int maxTokensRequested;     // how many tokens in completion
    private boolean stream = false;     // send results with SSE or not
    private float temperature;          // sampling temperature - how "creative" (higher is more) is the completion (0-2, default:1.0)
    private float top;                  // nucleus sampling % [alternative to temperature - (0-1, default:1.0)
    private int numCompletionsRequested;    // should typically be 1

    /************************************************************
     Constructor
     ***********************************************************/
    public LLM(String configfile)  {
        Properties prop = null;

        try {
            prop = getConfigProperties(configfile);
        } catch (IOException iox) {
            System.err.println("Cannot find config file [" + configfile + "]");
        }

        String token = prop.getProperty("llmservice.apikey");
        String model = prop.getProperty("llmservice.model");
        int maxtokens = Integer.parseInt(prop.getProperty("llmservice.maxtokensrequested", "512"));
        float temp = Float.parseFloat(prop.getProperty("llmservice.temperature", "1.0"));
        float top = Float.parseFloat(prop.getProperty("llmservice.percentsampled", "1.0"));
        int numcompletions = Integer.parseInt(prop.getProperty("llmservice.numcompletions", "1"));
        boolean stream = Boolean.parseBoolean(prop.getProperty("llmservice.stream", "false"));

        setParams(token, model, maxtokens, temp, top, numcompletions, stream);
        this.service = new OpenAiService(this.apikey);
    }

    public LLM(String apikey, String model, int maxtokens, float temp, float percentSampled, int numCompletionsRequested) {
        setParams(apikey, model, maxtokens, temp, top, numCompletionsRequested, false);    // handle stream/SSE TBD
        this.service = new OpenAiService(this.apikey);
    }

    public LLM(String key, String model, int maxtokens, float temp) {
        this(key, model, maxtokens, temp, 1, 1);
    }

    private void setParams(String token, String model, int maxtokens, float temp, float percentSampled, int numcompletions, boolean stream) {
        this.apikey = token;
        this.model = model;
        this.maxTokensRequested = maxtokens;
        this.temperature = temp;
        this.top = percentSampled;
        this.numCompletionsRequested = numcompletions;
        this.stream = false;            // currently only allow non-streaming (SSE TBD)
    }
    /************************************************************
     Set/Get Methods
     ***********************************************************/

    public OpenAiService getService() {
        return service;
    }
    public void setService(OpenAiService service) {
          this.service = service;
      }

    public String getApikey() {
        return apikey;
    }
    public void setApikey(String apikey) {
        this.apikey = apikey;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getMaxTokensRequested() {
        return maxTokensRequested;
    }

    public void setMaxTokensRequested(int maxTokensRequested) {
        this.maxTokensRequested = maxTokensRequested;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    /************************************************************
     Talk to LLM Methods
     ***********************************************************/
    public String sendCompletionRequest(String role, String msg) throws LLMCompletionException {
        if (!isLegalRole(role))
            throw new LLMCompletionException("Role [" + role + "] not recognizable");

        List<String> results = new ArrayList<>();

        final List<ChatMessage> messages = new ArrayList<>();
        final ChatMessage systemMessage = new ChatMessage(ChatMessageRole.USER.value(), msg);
        messages.add(systemMessage);
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(this.model)
                .messages(messages)
                .n(this.numCompletionsRequested)          // should be 1 - future we might return more than 1
                .maxTokens(this.maxTokensRequested)
                .logitBias(new HashMap<>())               // ???   Not sure what this is for
                .stream(this.stream)
                .build();
        List<ChatCompletionChunk> chunks = new ArrayList<>();
        System.out.println("this.service: " + this.service.toString());

        List<ChatCompletionChoice> choices = this.service.createChatCompletion(chatCompletionRequest).getChoices();
        for(ChatCompletionChoice s: choices) {
         results.add(s.getMessage().getContent().trim());
        }
        return results.get(0).toString();
    }

    public List<Double> sendEmbeddingRequest(String msg) {
        List<Double> results = new ArrayList<>();
        EmbeddingRequest embeddingRequest = EmbeddingRequest.builder()
                .model(this.model)
                .input(Collections.singletonList(msg))
                .build();
        List<Embedding> embedding = this.service.createEmbeddings(embeddingRequest).getData();
        List<Double> emb = embedding.get(0).getEmbedding();
        int size =  embedding.get(0).getEmbedding().size();
        for (int i = 0; i < size; i++) {
                results.add(emb.get(i));
        }
        return results;
    }
    

    /************************************************************
     main - Test some things...
     ***********************************************************/
    public static void main(String[] args) throws IOException, LLMCompletionException {

        LLM myllm = new LLM(DEFAULT_CONFIG);
        String s = myllm.sendCompletionRequest("user", "roses are red, violets are");
        System.out.println("RESULT: " + s);

        myllm.setModel("text-embedding-ada-002");       // try embedding
        List<Double> vec = myllm.sendEmbeddingRequest("hello world");
        for(Double d: vec){
            System.out.print(d + " ");
        }
        System.out.println();
        System.out.println("num of elements: " + vec.size());
    }

    private Properties getConfigProperties(String fname) throws IOException {
        Properties prop = new Properties();
        InputStream in = new FileInputStream(fname);

        prop.load(in);

        for (Enumeration e = prop.propertyNames(); e.hasMoreElements(); ) {
            String key = e.nextElement().toString();
            System.out.println(key + " = " + prop.getProperty(key));
        }
        return prop;
    }

    void displayResponse(List<ChatCompletionChoice> res) {
        for (ChatCompletionChoice s : res) {
            System.out.println(s.getMessage().getContent());
        }
    }
    private boolean isLegalRole(String r) {
        return switch (r) {
            case "user", "assistant", "system" -> true;
            default -> false;
        };
    }

}
