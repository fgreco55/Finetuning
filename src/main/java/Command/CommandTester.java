package Command;

import App.PchatService;
import Utilities.CommandLineParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class CommandTester {

    public static void main(String[] args) {
        Scanner userinput;                                  // user inputted line as a Scanner
        String cmdline;                                     // user inputted line as a String
        CommandLineParser clp = new CommandLineParser();    // parse user input and handle quoted strings
        ArrayList<String> argv;                             // list of args from user, parsed for quoted strings

        PchatService ps = new PchatService();

        HashMap<String, Command> commands = new HashMap<>();
        commands.put("helloworld", new HelloWorld());
        commands.put("argdumper", new ArgDumper());
        commands.put("collection", new UseCollection(ps));
        commands.put("deletecollection", new DeleteCollection(ps));
        commands.put("dc", new DeleteCollection(ps));
        commands.put("database", new UseDatabase(ps));          // maybe "list" - databases, collections, loaded files, etc??
        commands.put("loadnote", new LoadNote(ps));
        commands.put("loadtextfile", new LoadTextfile(ps));
        commands.put("loadfile", new LoadTextfile(ps));
        commands.put("loadurl", new LoadURL(ps));
        commands.put("query", new GetCompletion(ps));
        commands.put("help", new Help());                       // doesn't work yet...

        while (true) {

            userinput = new Scanner(System.in);
            System.out.print("Command> ");
            cmdline = userinput.nextLine();

            if (cmdline.isEmpty())
                continue;

            argv = clp.parseCommandLine(cmdline);
            String mycmd = argv.get(0);

            if (commands.containsKey(argv.get(0))) {
                commands.get(mycmd).execute(argv);
            } else {
                System.out.println("[" + mycmd + "] is not a recognized command");
            }
        }
    }
}
