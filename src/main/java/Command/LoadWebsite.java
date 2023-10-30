package Command;

import App.PchatService;

import java.util.List;

public class LoadWebsite implements Command {
    private static String description = "loadwebsite URL - loads the links from a website.";
    private int retcode = 460;

    private PchatService psvc;

    LoadWebsite(PchatService ps) {
        this.psvc = ps;
    }

    public int execute(List<String> cmd) {

        if (cmd.size() > 1) {
            String urlname = cmd.get(1);
            psvc.loadwebsite(urlname);
            return 0;
        } else {
            System.err.println("***ERROR: No Website URL specified.");
            return retcode;
        }
    }
}
