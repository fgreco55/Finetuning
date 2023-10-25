package Command;

import java.util.List;
import java.util.Scanner;

public class ArgDumper implements Command {
    private static String description = "Displays all the command line arguments.";
    private int retcode = 100;      // 0 for no error

    public String getDescription() {
        return description;
    }

    public int execute(List<String> cmd) {
        cmd.forEach(System.out::println);
        return cmd.size();
    }
}
