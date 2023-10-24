package Command;

import java.util.List;
import java.util.Scanner;

/*
 * Implement Command pattern for simple interpreter
 */
public interface Command {
    int execute(List<String> argv);
}
