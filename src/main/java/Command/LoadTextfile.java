package Command;

import App.PchatService;

import java.util.List;

public class LoadTextfile implements Command {
    private PchatService psvc;
    private static String description = "loadtextfile pathname - loads a textfile into the database given it's pathname.";
    private int retcode = 200;          // 0 for no error

    LoadTextfile(PchatService ps) {
        this.psvc = ps;
    }

    public int execute(List<String> cmd) {

        if (cmd.size() > 1) {
            String tname = cmd.get(1);
            psvc.loadtextfile(tname);
            return 0;
        } else {
            String err = "***ERROR: No text file specified.";
            System.err.println(err);
            return retcode;
        }
    }
}
