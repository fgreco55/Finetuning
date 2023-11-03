package Command;

import App.PchatService;

import java.util.List;

public class Language implements Command {
    private PchatService psvc;
    private int retcode = 800;    // 0 if no error
    private static String description = "lang [language] - Change the output language.";

    Language(PchatService ps) {
        this.psvc = ps;
    }

    public static String getDescription() {
        return description;
    }

    public int execute(List<String> cmd) {
        if (cmd.size() > 1) {
            psvc.setLanguage(cmd.get(1));
        } else
            System.out.println(psvc.getLanguage());

        return 0;
    }
}
