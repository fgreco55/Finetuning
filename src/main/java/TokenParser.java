import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.BreakIterator;
import java.util.Locale;
import java.util.Scanner;
/*
 Parsing text stream using sentences for now.  -fdg
 We might determine other parsing types in the future.
    e.g., html tags, pdf strings, etc. -fdg
 */

public class TokenParser {
    public static void main(String[] args) {
        // Input text
        String filename = "./src/main/resources/testfile.txt";
        // Create a BreakIterator for sentence tokenization
        BreakIterator sentenceIterator;
        sentenceIterator = BreakIterator.getSentenceInstance(Locale.US);

        // Iterate through sentences and output them one at a time
        int start = sentenceIterator.first();

        String bigString = (String)null;

        try {
            Path path = Path.of(filename);
            byte[] bytes = Files.readAllBytes(path);
            bigString = new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("An error occurred: " + e.getMessage());
        }

        System.out.println("ORIGINAL FILE:\n" + bigString);
        sentenceIterator.setText(bigString);
        
        // Iterate through sentences and output them one at a time
        start = sentenceIterator.first();

        System.out.println("PARSED TEXT BY SENTENCE:");
        for (int end = sentenceIterator.next(); end != BreakIterator.DONE; start = end, end = sentenceIterator.next()) {
            String sentence = bigString.substring(start, end).trim();
            System.out.println("* " + sentence);
        }
    }
}
