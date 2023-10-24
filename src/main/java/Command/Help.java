package Command;

/*
 * need a better implementation of this command... how would it know about the internal description of each command? -fdg
 */

import java.util.*;

public class Help implements Command {
    private static String description = "Displays all the possible commands.";
    private int retcode = 0;

    private static List<String> commands = List.of(
        "helloworld - Simple test to make sure everything is working",
        " argdumper - Dumps args",
        "      help - Show all the legal commands."
    );

    public int execute(List<String> cmd) {
        cmd.forEach(System.out::println);
        return cmd.size();
    }
}
