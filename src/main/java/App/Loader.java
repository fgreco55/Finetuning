package App;

import Database.VectorDB;
import Database.VectorDBException;
import Model.LLM;
import Model.LLMCompletionException;
import Utilities.FinetuningUtils;
import Utilities.Utility;

import java.io.IOException;

public class Loader {

    public static void main(String[] args) throws VectorDBException, LLMCompletionException, IOException {
        PchatService ps = new PchatService();
        ps.usecollection("deepnetts");

        ps.loadtextfile("./src/main/resources/faq-deepnetts2.txt");
        ps.loadwebsite("https://www.deepnetts.com");
        ps.loadurl("https://foojay.io/today/getting-started-with-deep-learning-in-java-using-deep-netts/");
        ps.loadurl("https://www.jcp.org/en/jsr/detail?id=381");
        ps.loadnote("Frank and Zoran meet every Saturday via Zoom.");
        ps.loadpdf("./src/main/resources/getting-started.pdf");
        ps.loadrecording("/Users/fgreco/src/Finetuning/src/main/resources/zoran-embedded.mp3");
    }
}
