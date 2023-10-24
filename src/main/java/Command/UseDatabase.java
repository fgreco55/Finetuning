package Command;

import App.PchatService;
import Database.VectorDB;

import java.util.List;

public class UseDatabase implements Command {
    private static String description = "usedatabase database-name - Sets the database name for subsequent inserts and semantic searches.";

    private PchatService ps;
    private int retcode = 400;          // 0 for no error

    public UseDatabase(PchatService ps) {
        this.ps = ps;
    }

    public int execute(List<String> cmd) {

        if (cmd.size() > 1) {
            String dbname = cmd.get(1);
            ps.getVdb().setDatabase(dbname);
            return 0;
        } else {
            System.out.println(ps.getVdb().getDatabase());
            return 0;
        }
    }
}
