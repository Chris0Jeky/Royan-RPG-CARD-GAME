package com.chris.cardgame.cli;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

public class ScannerInput implements Input {
    private final Scanner scanner;
    private final PrintStream out;

    public ScannerInput(InputStream in, PrintStream out) {
        this.scanner = new Scanner(in);
        this.out = out;
    }

    @Override
    public String readLine(String prompt) {
        out.print(prompt + " ");
        out.flush();
        if (!scanner.hasNextLine()) {
            return "quit";
        }
        return scanner.nextLine().trim();
    }
}
