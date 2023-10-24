package Utilities;

/*
 * CommandLineParser - breaks up string that could have embedded quoted strings.
 *                      Currently, cannot detect mismatched quotes
 */

import java.util.ArrayList;
import java.util.Scanner;

public class CommandLineParser {
    public ArrayList<String> parseCommandLine(String commandLine) {
        ArrayList<String> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        boolean insideQuotes = false;
        char quoteType = ' ';

        for (int i = 0; i < commandLine.length(); i++) {
            char currentChar = commandLine.charAt(i);
            if (currentChar == '"' || currentChar == '\'') {
                if (!insideQuotes) {
                    insideQuotes = true;
                    quoteType = currentChar;
                } else if (currentChar == quoteType) {
                    insideQuotes = false;
                    tokens.add(currentToken.toString());
                    currentToken.setLength(0); // clear the StringBuilder
                }
            } else if (currentChar == ' ' && !insideQuotes) {
                if (currentToken.length() > 0) {
                    tokens.add(currentToken.toString());
                    currentToken.setLength(0); // clear the StringBuilder
                }
            } else {
                currentToken.append(currentChar);
            }
        }
        // Add the last token
        if (currentToken.length() > 0) {
            tokens.add(currentToken.toString());
        }
        return tokens;
    }

    /*
     * Simple tester
     */

    public static void main(String[] args) {
           CommandLineParser clp = new CommandLineParser();

           Scanner scanner = new Scanner(System.in);
           System.out.print("Cmd: ");
           String commandLine = scanner.nextLine();
           ArrayList<String> parsedArgs = clp.parseCommandLine(commandLine);
           for (String arg : parsedArgs) {
               System.out.println(arg);
           }
       }
}
