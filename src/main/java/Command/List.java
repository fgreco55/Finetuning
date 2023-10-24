package Command;

import App.PchatService;
import Database.VectorDBException;

public class List implements Command {
    private PchatService psvc;
    private static String description = "list [collections|databases] - lists available databases and collections.";
    private int retcode = 650;          // 0 for no error

    public List(PchatService ps) {
        this.psvc = ps;
    }

    public int execute(java.util.List<String> cmd) {
        int argc = cmd.size();
        String subcommand;
        try {
            if (argc == 1) {
                System.out.println(psvc.showdatabases());
                System.out.println(psvc.showcollections());
                retcode = 0;
            } else if (argc > 1) {
                subcommand = cmd.get(1);
                if (subcommand.equals("collections")) {
                    System.out.println(psvc.showcollections());
                    retcode = 0;
                } else if (subcommand.equals("databases")) {
                    System.out.println(psvc.showdatabases());
                    retcode = 0;
                } else {
                    System.err.println("***Error.  Unknown subcommand for list");
                }
            }
        } catch (VectorDBException vex) {
            System.err.println("ERROR: LIST CANNOT HANDLE");
        }
        return retcode;
    }
}
