package Command;

/*
 * need a better implementation of this command... how would it know about the internal description of each command? -fdg
 */

import java.util.*;

public class Help implements Command {

    private static String description = "Displays all the possible commands.";
    private int retcode = 0;
    private HashMap<String, Command> cmdlist;

    public Help(HashMap<String, Command> list) {
      this.cmdlist = list;
    }
    public static String getDescription() {
        return description;
    }

    @Override
    public int execute(java.util.List<String> argv) {
        return 0;
    }
}
