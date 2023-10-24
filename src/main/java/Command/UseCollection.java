package Command;

import App.PchatService;
import Database.VectorDB;

import java.util.List;

public class UseCollection implements Command {
    private static String description = "usecollection collection-name - Sets the collection name for inserts and semantic searches.";
    private int retcode = 300;      // 0 for no error

    private PchatService psvc;

    public UseCollection(PchatService ps) {
        this.psvc = ps;
    }

    public int execute(List<String> cmd) {

        if (cmd.size() > 1) {
            String coll = cmd.get(1);
            psvc.getVdb().setCollection(coll);
            return 0;
        } else {
            System.err.println("No collection specified.");
            return retcode;
        }
    }
}
