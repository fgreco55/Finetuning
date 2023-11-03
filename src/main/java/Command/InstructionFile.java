package Command;

import App.PchatService;

import java.util.List;

public class InstructionFile implements Command {
    private PchatService psvc;
    private static String description = "InstructionFile [string] - Sets or Gets current System message file.";
    private int retcode = 175;      // 0 for no error

    public String getDescription() {
        return description;
    }

    InstructionFile(PchatService ps) {
        this.psvc = ps;
    }

    public int execute(List<String> cmd) {
        if (cmd.size() == 1) {
            System.out.println(psvc.getInstructionFile());
        } else if (cmd.get(1).equals("reload")) {
            psvc.setInstructionsFromFile(psvc.getInstructionFile());
        } else
            psvc.setInstructionFile(cmd.get(1));
        return 0;
    }
}
