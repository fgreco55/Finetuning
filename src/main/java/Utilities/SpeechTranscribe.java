package Utilities;

/*
 Heavily inspired by Ken Kousen - Null Pointer
 */
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

// See docs at https://platform.openai.com/docs/api-reference/audio/createTranscription

// response_format: json (default), text, srt, verbose_json, vtt
//      "text" is used here, as it returns the transcript directly
// language: ISO-639-1 code (optional)
//
// Rather than use multipart form data, add the file as a binary body directly
// Optional "prompt" used to give standard word spellings whisper might miss
//      If there are multiple chunks, the prompt for subsequent chunks should be the
//      transcription of the previous one (244 tokens max)

// file must be mp3, mp4, mpeg, mpga, m4a, wav, or webm
// NOTE: only wav files are supported here (mp3 apparently is proprietary)

// max size is 25MB; otherwise need to break the file into chunks
// See the WavFileSplitter class for that

public class SpeechTranscribe {
    private final static String URL = "https://api.openai.com/v1/audio/transcriptions";
    public final static int MAX_ALLOWED_SIZE = 25 * 1024 * 1024;
    public final static int MAX_CHUNK_SIZE_BYTES = 20 * 1024 * 1024;

    private String apikey;
    private String model;

    // Only model available as of Fall 2023 is whisper-1
    private final static String MODEL = "whisper-1";

    // Need to understand why we need these prompts and why to rotate them?  -fdg
    public static final String WORD_LIST = String.join(", ",
            List.of("Greco", "NYJavaSIG", "JCP", "GSJUG",
                    "NY Java SIG", "SouJava", "JUnit", "Java", "Kotlin", "Groovy",
                    "IOException", "RuntimeException", "UncheckedIOException", "UnsupportedAudioFileException",
                    "assertThrows", "assertTrue", "assertEquals", "assertNull", "assertNotNull", "assertThat",
                    "Spring Boot", "Spring Framework", "Spring Data", "Spring Security"));

    public SpeechTranscribe(String apikey, String model) {
        this.apikey = apikey;
        this.model = model;
    }
    private String transcribeChunk(String prompt, File chunkFile) {
        System.out.printf("Transcribing %s%n", chunkFile.getName());

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(URL);
            httpPost.setHeader("Authorization", "Bearer %s".formatted(apikey));

            HttpEntity entity = MultipartEntityBuilder.create()
                    .setContentType(ContentType.MULTIPART_FORM_DATA)
                    .addPart("file", new FileBody(chunkFile, ContentType.DEFAULT_BINARY))
                    .addPart("model", new StringBody(MODEL, ContentType.DEFAULT_TEXT))
                    .addPart("response_format", new StringBody("text", ContentType.DEFAULT_TEXT))
                    .addPart("prompt", new StringBody(prompt, ContentType.DEFAULT_TEXT))
                    .build();
            httpPost.setEntity(entity);

            return client.execute(httpPost, response -> {
                return EntityUtils.toString(response.getEntity());
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    /*
       transcribe() - main work horse
     */
    public String transcribe(String fileName) {
        File file = new File(fileName);

        List<String> transcriptions = new ArrayList<>();    // Place to stare all the transcriptions
        String prompt = "";     // Apparently OpenAI needs this to connect prior chunk to the current chunk

        if (file.length() <= MAX_ALLOWED_SIZE) {
            String transcription = transcribeChunk(prompt, file);
            transcriptions = List.of(transcription);
        } else {
            var splitter = new SpeechFileSplitter();
            List<File> chunks = splitter.splitWavFileIntoChunks(file);
            for (File chunk : chunks) {
                String transcription = transcribeChunk(prompt, chunk);
                System.err.println("[" + transcription + "]");
                transcriptions.add(transcription);
                prompt = transcription;         // subsequent prompts are the prev transcriptions to stitch them together (as per OpenAI)

                if (!chunk.delete()) {          // Don't need the chunk after transcribing
                    System.out.println("Failed to delete " + chunk.getName());
                }
            }
        }

        String transcription = String.join(" ", transcriptions);     // glom them all together
        return transcription;

    }
    /*********************************************************
          main method - test
     ********************************************************/
    public static void main(String[] args) {
        Utility util = new Utility();
        String key = util.getApikey("/Users/fgreco/src/Finetuning/src/main/resources/llm.properties");

        SpeechTranscribe wt = new SpeechTranscribe(key, "whisper-1");
        String s = wt.transcribe("/Users/fgreco/src/Finetuning/src/main/resources/zoran-groundbreakers.mp3");
        //String s = wt.transcribe("/Users/fgreco/src/Finetuning/src/main/resources/20230913-nyjavasig-abstract.mp3");


        System.out.println(s);
        System.out.println("====================================================");
        util.StringtoSentences(s);
    }
}

