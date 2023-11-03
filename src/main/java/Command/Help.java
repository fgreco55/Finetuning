package Command;

/*
 * need a better implementation of this command... how would it know about the internal description of each command? -fdg
 */

import java.util.*;
import java.util.List;

public class Help implements Command {

    private static String description = "help - Displays all the possible commands.";
    private int retcode = 0;
    private HashMap<String, Command> cmdlist;

    public Help(HashMap<String, Command> list) {
      this.cmdlist = list;
    }
    public static String getDescription() {
        return description;
    }


    public int execute(List<String> cmd) {
        showList();
        return 0;
    }

    private void showList() {
        System.out.println("""
       argdumper                - Displays all the command line arguments.
       helloworld               - Just a hello world along with displaying all the command line arguments
       usecollection coll-name  - Sets the collection name for inserts and semantic searches.
       c coll-name              - alias for "usecollection"
       deletecollection coll-name - drop a collection in the database.
       dc collection-name       - alias for "deletecollection".
       usedatabase db-name      - Sets the database name for subsequent inserts and semantic searches.
       list [colls|databases]   - lists available databases and collections. 
       loadnote string          - loads a string into the database
       loadtextfile pathname    - loads a textfile into the database given it's pathname.  Handles .txt and .pdf
       loadfile pathname        - alias for loadtextfile.
       loadurl URL              - loads the text from a webpage into the database given it's URL. 
       loadwebsite URL [levels] - loads the links from a website.  Optionally specify depth level.
       lw URL [levels]          - alias for loadwebsite.
       instruction [string]     - set or show the system message
       inst [string]            - alias for instruction
       instructionfile [file]   - set or show the instruction file
       instfile [file]          - alias for instructionfile
       lang [language]          - Change the output language.
       l [language]             - alias for lang
       image "prompt"           - generates an image based on a prompt.
       help                     - Displays all the possible commands.
     
       send                     - Sends a user prompt to the model and retrieves a response (default command).
                """);
    }
}
