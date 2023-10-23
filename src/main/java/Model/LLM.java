package Model;

import Utilities.Utility;
import com.theokanning.openai.completion.chat.*;
import com.theokanning.openai.embedding.Embedding;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.service.OpenAiService;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
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
    private long timeout;               // socket timeout
    private float temperature;          // sampling temperature - how "creative" (higher is more) is the completion (0-2, default:1.0)
    private float top;                  // nucleus sampling % [alternative to temperature - (0-1, default:1.0)   "percent sampled"
    private int numCompletionsRequested;    // should typically be 1
    private final static String EMBEDDING_MODEL = "text-embedding-ada-002";
    private final static String COMPLETION_MODEL = "gpt-3.5-turbo";
    private final static String SPEECH_MODEL = "whisper-1";
    private String completion_model = COMPLETION_MODEL;     // default
    private String embedding_model = EMBEDDING_MODEL;       // default
    private final static int VEC_SIZE = 1536;
    private int vector_size = VEC_SIZE;                         // default... dependent on embedding vector length
    private String speech_model = SPEECH_MODEL;
    private String preamble_file;
    private String instruction_file;
    private final static String DEFAULT_LANGUAGE = "english";
    private String language = DEFAULT_LANGUAGE;        // what language the LLM should use when conversing with the user;

    /************************************************************
     Constructors
     ***********************************************************/
    public LLM(Properties prop) throws LLMCompletionException {
        String token = prop.getProperty("llmservice.apikey");

        if (token == (String) null)            // Cannot continue without an API key from LLM provider
            throw new LLMCompletionException("You need an API key from the LLM provider");

        setparams(prop);
        this.service = new OpenAiService(this.apikey, Duration.ofSeconds(this.getTimeout()));
    }


    public LLM(String configfile) throws LLMCompletionException {
        Properties prop = null;
        Utility util = new Utility();

        try {
            prop = util.getConfigProperties(configfile);
        } catch (IOException iox) {
            System.err.println("Cannot find config file [" + configfile + "]");
        }

       this.apikey = prop.getProperty("llmservice.apikey");

        if (this.apikey == (String) null)            // Cannot continue without an API key from LLM provider
            throw new LLMCompletionException("You need an API key from the LLM provider");

        setparams(prop);

        this.service = new OpenAiService(this.apikey, Duration.ofSeconds(this.getTimeout()));
    }

    // Need to restructure the constructors so this one below is the main one    - TBD
    public LLM(String apikey, String completion_model, String embedding_model,
               int maxtokens, float temp, float percentSampled,
               int numCompletionsRequested, String pfile, String ifile, int vecsize, String lang) {

        setSpecificParams(apikey, completion_model, embedding_model, speech_model, maxtokens, temp, top, numCompletionsRequested, false,
                pfile, ifile, vecsize, lang);    // handle stream/SSE TBD

        this.service = new OpenAiService(this.apikey);
    }

    private void setSpecificParams(String token, String cmodel, String emodel, String smodel,
                           int maxtokens, float temp, float percentSampled, int numcompletions, boolean stream,
                           String pfile, String ifile, int vecsize, String lang) {
        this.apikey = token;
        this.completion_model = cmodel;
        this.embedding_model = emodel;
        this.speech_model = smodel;
        this.maxTokensRequested = maxtokens;
        this.temperature = temp;
        this.top = percentSampled;
        this.numCompletionsRequested = numcompletions;
        this.stream = false;            // currently only allow non-streaming (SSE TBD)
        this.preamble_file = pfile;
        this.vector_size = vecsize;
        this.language = lang;
    }

    private void setparams(Properties prop) {
        this.apikey = prop.getProperty("llmservice.apikey");
        this.completion_model = prop.getProperty("llmservice.completion_model", COMPLETION_MODEL);
        this.embedding_model = prop.getProperty("llmservice.embedding_model", EMBEDDING_MODEL);
        this.speech_model = prop.getProperty("llmservice.speech_model", SPEECH_MODEL);
        this.maxTokensRequested = Integer.parseInt(prop.getProperty("llmservice.maxtokensrequested", "512"));
        this.temperature = Float.parseFloat(prop.getProperty("llmservice.temperature", "1.0"));
        this.top = Float.parseFloat(prop.getProperty("llmservice.percentsampled", "1.0"));
        this.numCompletionsRequested = Integer.parseInt(prop.getProperty("llmservice.numcompletions", "1"));
        this.stream = Boolean.parseBoolean(prop.getProperty("llmservice.stream", "false"));
        this.preamble_file = prop.getProperty("llmservice.preamble");
        this.instruction_file = prop.getProperty("llmservice.instructions");
        this.vector_size = Integer.parseInt(prop.getProperty("llmservice.vector_size", VEC_SIZE + ""));
        this.language = prop.getProperty("llmservice.language", DEFAULT_LANGUAGE);
        this.timeout = Long.parseLong(prop.getProperty("llmservice.timeout", "10"));
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

    /*
     * getApikey - need to decrypt it - TBD
     */
    public String getApikey() {
        return apikey;
    }

    /*
     * setApikey - need to encrypt it - TBD
     */
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

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getInstruction_file() {
        return instruction_file;
    }

    public void setInstruction_file(String instruction_file) {
        this.instruction_file = instruction_file;
    }

    public int getVector_size() {
        return vector_size;
    }

    public void setVector_size(int vector_size) {
        this.vector_size = vector_size;
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
        ChatMessage systemMessage = null;
        if (role.equalsIgnoreCase("user")) {
            systemMessage = new ChatMessage(ChatMessageRole.USER.value(), msg);
        }   else if (role.equalsIgnoreCase("system")) {
            systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), msg);
        }

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

    /*
     * getEmbedding() - just a convenience method
     */
    public List<Float> getEmbedding(String str) {
        return sendEmbeddingRequest(str);
    }

    /*
     * Display all the completion (responses)
     */
    void displayResponse(List<ChatCompletionChoice> res) {
        for (ChatCompletionChoice s : res) {
            System.out.println(s.getMessage().getContent());
        }
    }

    /*
     * Make sure the role is a legal one (maybe OpenAI specific?)
     */
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
