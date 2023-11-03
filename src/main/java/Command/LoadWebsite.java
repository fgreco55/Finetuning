package Command;

import App.PchatService;

import java.util.List;

public class LoadWebsite implements Command {
    private static String description = "loadwebsite URL [levels] - loads the links from a website.  Optionally specify depth level.";
    private int retcode = 460;

    private PchatService psvc;

    LoadWebsite(PchatService ps) {
        this.psvc = ps;
    }

    public int execute(List<String> cmd) throws NumberFormatException {

        if (cmd.size() == 2) {              // loadwebsite url
            String urlname = cmd.get(1);
            psvc.loadwebsite(urlname);
            return 0;
        } else if (cmd.size() == 3) {       // loadwebsite url levels
            String urlname = cmd.get(1);
            int levels = Integer.parseInt(cmd.get(2));      // possible NumberFormatException
            psvc.loadwebsite(urlname, levels);
            return 0;
        } else {
            System.err.println("***ERROR: No Website URL specified.");
            return retcode;
        }
    }
}
