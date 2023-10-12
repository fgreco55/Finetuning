/*********************************************************************
 Utilities and Convenience methods
 ********************************************************************/
package Utilities;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class Utility {
    public String URLtoText(String url) {
        try {
            // Connect to the URL and retrieve the HTML content
            Document document = Jsoup.connect(url).get();
            return document.text();
        } catch (IOException e) {
            e.printStackTrace();
            return (String) null;
        }
    }

    public List<String> URLtoSentences(String url) {
        String urlstring = URLtoText(url);
        return TexttoSentences(urlstring);
    }

    public String TextfiletoString(String filename) {
        String bigString = (String) null;

        try {
            Path path = Path.of(filename);
            byte[] bytes = Files.readAllBytes(path);
            bigString = new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("***ERROR: " + e.getMessage());
        }

        //System.out.println("Text of File:\n" + bigString);
        return bigString;
    }

    public List<String> TextfiletoSentences(String filename) {
        return TexttoSentences(TextfiletoString(filename));
    }

    public String PDFfiletoText(String filename) throws IOException {
        PDDocument document = PDDocument.load(new File(filename));
        PDFTextStripper pdfTextStripper = new PDFTextStripper();
        String pdfText = pdfTextStripper.getText(document);
        document.close();
        return pdfText;
    }

    public List<String> PDFfiletoSentences(String filename) throws IOException {
        return TexttoSentences(PDFfiletoText(filename));
    }

    public List<String> TextfiletoList(String filename) throws IOException {
        List<String> lines = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return lines;
    }
    
    public List<String> TexttoSentences(String text) {
        //System.out.println("INPUT TEXT [" + text + "]");

        List<String> sentences = new ArrayList<>();
        // Create a BreakIterator for sentence tokenization
        BreakIterator sentenceIterator = BreakIterator.getSentenceInstance(Locale.US);
        sentenceIterator.setText(text);

        // Iterate through sentences and output them one at a time
        int start = sentenceIterator.first();

        System.out.println("PARSED TEXT BY SENTENCE:");
        for (int end = sentenceIterator.next(); end != BreakIterator.DONE; start = end, end = sentenceIterator.next()) {
            String s = text.substring(start, end).trim();
            System.out.println("* " + s);
            sentences.add(s);
        }
        return sentences;
    }

    /*********************************************************************
     * Some convenience methods for a simulation - eventually not needed
     ********************************************************************/
    public List<Long> createDummySentenceIds(int num) {
        List<Long> idlist = new ArrayList<>();
        for (long i = 0; i < num; i++) {
            idlist.add(i);
        }
        return idlist;
    }

    public List<String> createDummySentences(int num) {
        List<String> sentences = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            sentences.add(this.randomString());
        }
        return sentences;
    }

    public List<List<Float>> createDummyEmbeddings(int num, int vecsize) {
        Random ran = new Random();
        List<List<Float>> embeddings = new ArrayList<>();

        for (int j = 0; j < num; j++) {
            List<Float> vector = new ArrayList<>();
            for (int i = 0; i < vecsize; i++) {
                vector.add(ran.nextFloat());
            }
            embeddings.add(vector);
        }

        return embeddings;
    }

    public String createBigString(List<String> sarr) {
        String bigstr = "";
        for (int i = 0; i < sarr.size(); i++) {
            bigstr = bigstr.concat(sarr.get(i));
            bigstr = bigstr.concat("\n");
        }
        return bigstr;
    }

    /************************************************************
     Create random strings just for simulation.
     This data would normally come from a file.
     Eventually this method isn't needed since we now
     have List<String> TextfiletoSentences(filename) in this class.
     ***********************************************************/
    public String randomString() {
        String[] universe = {
                "Q. What is the NYJavaSIG?  A. The New York Java Special Interest Group (NYJavaSIG) is based in New York City and attracts Java developers from the tri-state region. Through its regular monthly general meetings, bi-monthly specialty workgroup meetings and its website, the NYJavaSIG brings together members of New York's Java community so they can share their tips, techniques, knowledge, and experience.",
                "Q. When was the NYJavaSIG founded? A. The NYJavaSIG was founded in 1995 by Frank Greco",
                "Q. When are the NYJavaSIG meetings held? A. There are at least one meeting per month",
                "Q. Who has presented to the NYJavaSIG?  A. Arthur van Hoff, Jim Waldo, Scott Oaks, Henry Wong, Frank Greco, Doug Lea, Calvin Austin, Guy Steele, David Sherr, Brian Goetz, Pratik Patel, Josh Bloch, Justin Lee, Gil Tene, James Ward, Karl Jacobs, Peter Bell, Ethan Henry, Nat Wyatt, Roman Stanek, Anne Thomas-Manes, Simon Phipps, Jeanne Boyarsky, Reza Rahman, Fabiane Nardon, Peter Haggar, Max Goff, Maurice Balick, Gavin King, Jonathan Nobels, Bob Pasker, Rinaldo DiGiorgio, Steve Ross-Talbot, Talip Ozturk, John Davies, Rod Johnson, Bert Ertman, Simon Ritter, Kirk Pepperdine, Matt Raible, Edson Yanaga, Victor Grazi, Chandra Guntur",
                "Q. Where are the meetings?  A. The meetings are typically at Credit Suisse, Google, Microsoft, BNY Mellon, Cockroach Labs, Betterment, and other locations in Manhattan",
                "Q. What is a SIG? A. A SIG is a Special Interest Group, historically a subset of a larger group",
                "Q. When was the first formal Java tutorial? A. Frank delivered the first Java tutorial (along with Scott Oaks) way back on September 21, 1995, at the Equitable Center in midtown Manhattan.",
                "Q. How many members were at the first NYJavaSIG meeting? A. Five",
                "Q. What is a HOW? A. A HOW is a NYJavaSIG Hands-On-Workshop",
                "Q. How can I present to the NYJavaSIG?  A. If you would like to share your Java Technology, solutions, source code, designs, opinions, or ideas, send a note to info At javasig*dot*com.",
                "Q. Where are the meeting announcements posted?  A. Our monthly meeting host locations are always posted on our website.",
                "Q. When are the HOW meetings held?  A. HOW meetings are held quarterly.",
                "Q. How many people attend a NYJavaSIG meeting? A. We currently average over 50 attendees per monthly meeting (pre-COVID, we averaged 125-150).",
                "Q. What is the goal of the NYJavaSIG? A. The goal of the NYJavaSIG has always been to help software engineers, technologists, and technical managers in the NY area stay on the cutting edge"
        };
        Random random = new Random();
        return universe[random.nextInt(universe.length)];
    }

    public String currentDir() {
        return System.getProperty("user.dir");
    }

    public static void main(String[] args) throws IOException {
        Utility util = new Utility();

        /* Test string->sentences parser */
        System.out.println(util.TexttoSentences("hello world.  This is Frank.  Nice to hear from you."));
        /* Test textfile->sentences parser */
        util.TextfiletoSentences("./src/main/resources/testfile.txt").forEach(System.out::println);
        /* Test URL scrape -> text */
        System.out.println(util.URLtoText("http://www.javasig.com"));
        /* Test URL scrape -> sentences */
        util.URLtoSentences("https://www.espn.com").forEach(System.out::println);
        /* Test pdf file -> sentences */
        util.PDFfiletoSentences("./src/main/resources/testfile.pdf").forEach(System.out::println);
    }
}

