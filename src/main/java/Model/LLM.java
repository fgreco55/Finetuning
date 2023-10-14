package Model;

import Utilities.Utility;
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
    private String user;                // user id of the llm interaction
    private final static String DEFAULT_CONFIG = "/Users/fgreco/src/Finetuning/src/main/resources/llm.properties";
    private int maxTokensRequested;     // how many tokens in completion
    private boolean stream = false;     // send results with SSE or not
    private float temperature;          // sampling temperature - how "creative" (higher is more) is the completion (0-2, default:1.0)
    private float top;                  // nucleus sampling % [alternative to temperature - (0-1, default:1.0)
    private int numCompletionsRequested;    // should typically be 1
    private final static String EMBEDDING_MODEL = "text-embedding-ada-002";
    private final static String COMPLETION_MODEL = "gpt-3.5-turbo";
    private final static String SPEECH_MODEL = "whisper-1";
    private String completion_model = COMPLETION_MODEL;     // default
    private String embedding_model = EMBEDDING_MODEL;       // default
    private String speech_model = SPEECH_MODEL;
    private String preamble_file;

    /************************************************************
     Constructor
     ***********************************************************/
    public LLM(String configfile) throws LLMCompletionException {
        Properties prop = null;
        Utility util = new Utility();

        try {
            prop = util.getConfigProperties(configfile);
        } catch (IOException iox) {
            System.err.println("Cannot find config file [" + configfile + "]");
        }

        String token = prop.getProperty("llmservice.apikey");

        if (token == (String) null)            // Cannot continue without an API key from LLM provider
            throw new LLMCompletionException("You need an API key from the LLM provider");

        setParams(token,
                prop.getProperty("llmservice.completion_model"),
                prop.getProperty("llmservice.embedding_model"),
                Integer.parseInt(prop.getProperty("llmservice.maxtokensrequested", "512")),
                Float.parseFloat(prop.getProperty("llmservice.temperature", "1.0")),
                Float.parseFloat(prop.getProperty("llmservice.percentsampled", "1.0")),
                Integer.parseInt(prop.getProperty("llmservice.numcompletions", "1")),
                Boolean.parseBoolean(prop.getProperty("llmservice.stream", "false")),
                prop.getProperty("llmservice.preamble")
                );

        this.service = new OpenAiService(this.apikey);
    }

    public LLM(String apikey, String completion_model, String embedding_model,
               int maxtokens, float temp, float percentSampled,
               int numCompletionsRequested, String pfile) {
        setParams(apikey, completion_model, embedding_model, maxtokens, temp, top, numCompletionsRequested, false, pfile);    // handle stream/SSE TBD
        this.service = new OpenAiService(this.apikey);
    }

    /*public LLM(String key, String cmodel, String emodel, int maxtokens, float temp) {
        this(key, cmodel, emodel, maxtokens, temp, 1, 1);
    }*/

    private void setParams(String token, String cmodel, String emodel,
                           int maxtokens, float temp, float percentSampled, int numcompletions, boolean stream, String pfile) {
        this.apikey = token;
        this.completion_model = cmodel;
        this.embedding_model = emodel;
        this.maxTokensRequested = maxtokens;
        this.temperature = temp;
        this.top = percentSampled;
        this.numCompletionsRequested = numcompletions;
        this.stream = false;            // currently only allow non-streaming (SSE TBD)
        this.preamble_file = pfile;
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

    public String getCompletion_model() {
        return completion_model;
    }

    public void setCompletion_model(String completion_model) {
        this.completion_model = completion_model;
    }

    public String getEmbedding_model() {
        return embedding_model;
    }

    public void setEmbedding_model(String embedding_model) {
        this.embedding_model = embedding_model;
    }

    public String getSpeech_model() {
        return speech_model;
    }

    public void setSpeech_model(String speech_model) {
        this.speech_model = speech_model;
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

    public String getPreamble_file() {
        return preamble_file;
    }

    public void setPreamble_file(String preamble_file) {
        this.preamble_file = preamble_file;
    }

    /************************************************************
     Talk to LLM Methods
     ***********************************************************/
    public String sendCompletionRequest(String role, String msg) throws LLMCompletionException {
        if (!isLegalRole(role))
            throw new LLMCompletionException("Role [" + role + "] not recognizable");

        String cmodel = getCompletion_model();      // make sure to use correct completion model
        List<String> results = new ArrayList<>();

        final List<ChatMessage> messages = new ArrayList<>();
        final ChatMessage systemMessage = new ChatMessage(ChatMessageRole.USER.value(), msg);
        messages.add(systemMessage);
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(cmodel)
                .messages(messages)
                .n(this.numCompletionsRequested)          // should be 1 - future we might return more than 1
                .maxTokens(this.maxTokensRequested)
                .logitBias(new HashMap<>())               // ???   Not sure what this is for
                .stream(this.stream)
                .build();
        List<ChatCompletionChunk> chunks = new ArrayList<>();
        //System.out.println("this.service: " + this.service.toString());

        List<ChatCompletionChoice> choices = this.service.createChatCompletion(chatCompletionRequest).getChoices();
        for (ChatCompletionChoice s : choices) {
            results.add(s.getMessage().getContent().trim());
        }
        return results.get(0).toString();
    }

    public List<Float> sendEmbeddingRequest(String msg) {
        Utility util = new Utility();

        List<Float> results = new ArrayList<>();
        EmbeddingRequest embeddingRequest = EmbeddingRequest.builder()
                .model(getEmbedding_model())
                .input(Collections.singletonList(msg))
                .build();

        List<Embedding> embedding = this.service.createEmbeddings(embeddingRequest).getData();
        List<Double> emb = embedding.get(0).getEmbedding();     // OpenAI returns Doubles... Milvus wants Floats...
        List<Float> newb = util.Double2Float(emb);
        int size = embedding.get(0).getEmbedding().size();
        for (int i = 0; i < size; i++) {
            results.add(newb.get(i));
        }
        return results;
    }

    public List<Float> getEmbedding(String str) {
        return sendEmbeddingRequest(str);
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

    /************************************************************
     main - Test some things...
     ***********************************************************/
    public static void main(String[] args) throws IOException, LLMCompletionException {

        LLM myllm = new LLM(DEFAULT_CONFIG);
        System.out.println("Completion model: " + myllm.getCompletion_model());

        String s = myllm.sendCompletionRequest("user", "roses are red, violets are");
        System.out.println("RESULT: " + s);

        System.out.println("Embedding model: " + myllm.getEmbedding_model());
        List<Float> vec = myllm.sendEmbeddingRequest("hello world");
        for (Float d : vec) {
            System.out.print(d + " ");
        }
        System.out.println();
        System.out.println("num of elements: " + vec.size());
    }
}
