package Command;

import App.PchatService;

import java.util.List;

public class Instruction implements Command {
    private PchatService psvc;
    private static String description = "Instruction [string] - Sets or Gets current System message.";
    private int retcode = 170;      // 0 for no error

    public String getDescription() {
        return description;
    }

    Instruction(PchatService ps) {
        this.psvc = ps;
    }

    public int execute(List<String> cmd) {
        if (cmd.size() == 1) {
            System.out.println(psvc.getInstruction());
        } else
            psvc.setInstruction(cmd.get(1));
        return 0;
    }
}
