package Command;

import App.PchatService;

import java.util.List;

public class LoadURL implements Command {
    private static String description = "loadurl URL - loads the text from a webpage into the database given it's URL.";
    private int retcode = 450;

    private PchatService psvc;
    LoadURL(PchatService ps) {
        this.psvc = ps;
    }

    public int execute(List<String> cmd) {

        if (cmd.size() > 1) {
            String urlname = cmd.get(1);
            psvc.loadurl(urlname);
            return 0;
        } else {
            System.err.println("***ERROR: No URL specified.");
            return retcode;
        }
    }
}
