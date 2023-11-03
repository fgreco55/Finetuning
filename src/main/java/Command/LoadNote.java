package Command;

import App.PchatService;

import java.util.List;

public class LoadNote implements Command {
    private PchatService psvc;
    private static String description = "loadnote string - loads a string into the database";
    private int retcode = 200;      // 0 for no error

    LoadNote(PchatService ps) {
        this.psvc = ps;
    }

    public int execute(List<String> cmd) {
        if (cmd.size() > 1) {
            String note = cmd.get(1);
            psvc.loadnote(note);
            return 0;
        } else {
            String err = "***ERROR: No string specified.";
            System.err.println(err);
            return retcode;
        }
    }
}
