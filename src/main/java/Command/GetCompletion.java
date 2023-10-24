package Command;

import App.PchatService;

import java.util.List;

public class GetCompletion implements Command {
    private PchatService psvc;

    public static String getDescription() {
        return description;
    }

    private int retcode = 1;    // 0 if no error

    private static String description = "Sends a user prompt to the model and retrieves a response.";


    GetCompletion(PchatService ps) {
        this.psvc = ps;
    }

    public int execute(List<String> cmd) {
        if (cmd.size() > 1) {
            System.out.println(psvc.getCompletion(cmd.get(1)));
            return 0;
        }
        return retcode;
    }
}