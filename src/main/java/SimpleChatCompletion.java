import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class SimpleChatCompletion {
    private final static String DEFAULT_CONFIG = "./src/main/resources/chatgpt.properties";

    public static void main(String[] args) throws IOException {
        Properties prop = getConfigProperties(DEFAULT_CONFIG);
        if (prop == (Properties) null) {
            System.err.println("Cannot find OpenAI API key.  Your path to the properties is probably incorrect.");
            System.exit(1);
        }
        String token = prop.getProperty("chatgpt.apikey");
        OpenAiService service = new OpenAiService(token, Duration.ofSeconds(30));
        System.out.println("Creating completion...");

        final List<ChatMessage> messages = new ArrayList<>();
        final ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), "You are a extremely funny comedian and will respond as one.");
        final ChatMessage userMessage = new ChatMessage(ChatMessageRole.USER.value(), "How should I learn Java?");

        messages.add(systemMessage);
        messages.add(userMessage);

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
                .builder()
                .model("gpt-3.5-turbo")
                .messages(messages)
                .n(1)
                .maxTokens(30)
                .logitBias(new HashMap<>())
                .build();

        service.createChatCompletion(chatCompletionRequest).getChoices().forEach(System.out::println);
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
        return prop;
    }
}
