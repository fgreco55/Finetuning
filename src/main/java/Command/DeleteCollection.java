package Command;

import App.PchatService;
import Database.VectorDBException;

import java.util.List;

public class DeleteCollection implements Command {
    private static String description = "deletecollection - drop a collection in the database.";
    private PchatService psvc;
    private int retcode = 600;      // 0 for no error

    public DeleteCollection(PchatService ps) {
        this.psvc = ps;
    }

    public int execute(List<String> cmd) {
        if (cmd.size() > 1) {
            psvc.dropcollection(cmd.get(1));
            retcode = 0;
        }
        return retcode;
    }
}
