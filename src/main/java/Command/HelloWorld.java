package Command;

import java.util.List;
import java.util.Scanner;

public class HelloWorld implements Command {
    private static String description = "helloworld - Just a hello world along with displaying all the command line arguments";
    private int retcode = 150;          // 0 for no error

    public int execute(List<String> cmd) {
        cmd.forEach(System.out::println);
        return cmd.size();
    }
}
