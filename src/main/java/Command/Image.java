package Command;

import App.PchatService;

import java.util.List;

public class Image implements Command {
    private PchatService psvc;
    private static String description = "image \"prompt\" - generates an image based on a prompt.";
    private int retcode = 1000;          // 0 for no error

    Image(PchatService ps) {
        this.psvc = ps;
    }

    public int execute(List<String> argv) {
        if (argv.size() > 1) {
            System.out.println(psvc.getImageURL(argv.get(1)));
        }
        return 0;
    }
}
