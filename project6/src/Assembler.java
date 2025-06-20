package com.nand2tetris;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Assembler {

    Map<String, String> compLookup = new HashMap<>();
    Map<String, String> destLookup = new HashMap<>();
    Map<String, String> jmpLookup = new HashMap<>();
    Map<String, String> symbolsLookup = new HashMap<>();

    // Initial value for assigning variables
    private int variableReferenceCounter = 16;

    public Assembler() {
        compLookup.put("0", "101010");
        compLookup.put("1", "111111");
        compLookup.put("-1", "111010");
        compLookup.put("D", "001100");
        compLookup.put("A", "110000");
        compLookup.put("!D", "001101");
        compLookup.put("!A", "110001");
        compLookup.put("-D", "001111");
        compLookup.put("-A", "110011");
        compLookup.put("D+1", "011111");
        compLookup.put("A+1", "110111");
        compLookup.put("D-1", "001110");
        compLookup.put("A-1", "110010");
        compLookup.put("D+A", "000010");
        compLookup.put("D-A", "010011");
        compLookup.put("A-D", "000111");
        compLookup.put("D&A", "000000");
        compLookup.put("D|A", "010101");

        destLookup.put("null", "000");
        destLookup.put("M", "001");
        destLookup.put("D", "010");
        destLookup.put("MD", "011");
        destLookup.put("A", "100");
        destLookup.put("AM", "101");
        destLookup.put("AD", "110");
        destLookup.put("AMD", "111");

        jmpLookup.put("null", "000");
        jmpLookup.put("JGT", "001");
        jmpLookup.put("JEQ", "010");
        jmpLookup.put("JGE", "011");
        jmpLookup.put("JLT", "100");
        jmpLookup.put("JNE", "101");
        jmpLookup.put("JLE", "110");
        jmpLookup.put("JMP", "111");

        symbolsLookup.put("R0", "0");
        symbolsLookup.put("R1", "1");
        symbolsLookup.put("R2", "2");
        symbolsLookup.put("R3", "3");
        symbolsLookup.put("R4", "4");
        symbolsLookup.put("R5", "5");
        symbolsLookup.put("R6", "6");
        symbolsLookup.put("R7", "7");
        symbolsLookup.put("R8", "8");
        symbolsLookup.put("R9", "9");
        symbolsLookup.put("R10", "10");
        symbolsLookup.put("R11", "11");
        symbolsLookup.put("R12", "12");
        symbolsLookup.put("R13", "13");
        symbolsLookup.put("R14", "14");
        symbolsLookup.put("R15", "15");
        symbolsLookup.put("KBD", "24576");
        symbolsLookup.put("SCREEN", "16384");
        symbolsLookup.put("SP", "0");
        symbolsLookup.put("LCL", "1");
        symbolsLookup.put("ARG", "2");
        symbolsLookup.put("THIS", "3");
        symbolsLookup.put("THAT", "4");

    }

    public String translateCompBits(String compAsm) {
        if (compAsm.contains("M")) {
            String convertedComp = compAsm.replace('M', 'A');
            return compLookup.get(convertedComp);
        }
        return compLookup.get(compAsm);
    }

    public String translateJumpBits(String jumpAsm) {
        if (jumpAsm == null || jumpAsm.strip().isEmpty()) {
            return jmpLookup.get("null");
        }
        return jmpLookup.get(jumpAsm);
    }

    public String translateDestBits(String destAsm) {
        if (destAsm == null || destAsm.strip().isEmpty()) {
            return destLookup.get("null");
        }
        return destLookup.get(destAsm);
    }

    public String getABit(String computation) {
        if (computation.contains("M")) {
            return "1";
        } else {
            return "0";
        }
    }

    public String translateAInstruction(String aCommand) {
        String aVal = aCommand.substring(1);
        if (isNumber(aVal)) {
            return convertIntegerTo16BitBinary(aVal);
        }
        // Check if @symbol is found in symbols (OR) Check if @symbol is a label reference
        // Both are handled by looking up symbol map
        if (symbolsLookup.containsKey(aVal)) {
            String replacement = symbolsLookup.get(aVal);
            return convertIntegerTo16BitBinary(replacement);
        } else {
            // Else @symbol is a variable. If first encounter, assign variable counter starting from 16, else get from map
            symbolsLookup.put(aVal, String.valueOf(variableReferenceCounter));
            variableReferenceCounter++;
            String replacement = symbolsLookup.get(aVal);
            return convertIntegerTo16BitBinary(replacement);
        }
    }

    private String convertIntegerTo16BitBinary(String aVal) {
        String binVal = Integer.toBinaryString(Integer.valueOf(aVal));
        int zeroPadding = 16 - binVal.length();
        String format = "%0" + zeroPadding + "d";
        String zeros = String.format(format, 0);
        return zeros + binVal;
    }

    private boolean isNumber(String symbol) {
        try {
            Integer.parseInt(symbol);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    public String translateCInstruction(String cCommand) {
        String comp = null;
        String jump = null;
        String dest = null;
        if (cCommand.contains("=")) {
            String[] cInstParts = cCommand.split("=");
            dest = cInstParts[0];
            if (cInstParts[1].contains(";")) {
                String[] compJumpParts = cInstParts[1].split(";");
                comp = compJumpParts[0];
                jump = compJumpParts[1];
            } else {
                comp = cInstParts[1];
            }
        } else {
            if (cCommand.contains(";")) {
                String[] compJumpParts = cCommand.split(";");
                comp = compJumpParts[0];
                jump = compJumpParts[1];
            }
        }

        String aBit = getABit(comp);
        String compBits = translateCompBits(comp);
        String destBits = translateDestBits(dest);
        String jumpBits = translateJumpBits(jump);
        String translatedInst = String.format("111%s%s%s%s", aBit, compBits, destBits, jumpBits);
        return translatedInst;
    }

    /**
     * Identify labels in the format (***)
     * Assign next line number for lab
     * @param fileName
     * @throws Exception
     */
    public void translateFirstPass(String fileName) throws Exception {
        List<String> lines = Files.readAllLines(Path.of(fileName));
        List<String> cleanedLines  = removeCommentsAndEmptyLines(lines, false);
        List<Statement> statements = getStatements(cleanedLines);
        for (int i=0; i<statements.size(); i++) {
            Statement statement = statements.get(i);
            if (isLabel(statement.line)) {
                String label = extractLabel(cleanedLines.get(i));
                Statement nextStatement = statements.get(i+1);
                // Sometimes, two labels can be one after the other
                if (nextStatement.lineNumber > 0) {
                    symbolsLookup.put(label, String.valueOf(nextStatement.lineNumber));
                } else {
                    Statement secondStatement = statements.get(i+2);
                    if (secondStatement.lineNumber > 0) {
                        symbolsLookup.put(label, String.valueOf(secondStatement.lineNumber));
                    } else {
                        Statement thirdStatement = statements.get(i+3);
                        if (thirdStatement.lineNumber > 0) {
                            symbolsLookup.put(label, String.valueOf(thirdStatement.lineNumber));
                        } else {
                            Statement fourthStatement = statements.get(i+4);
                            symbolsLookup.put(label, String.valueOf(fourthStatement.lineNumber));
                            if (fourthStatement.lineNumber == 0) {
                                System.out.println("Fourth statement is also a label");
                            }
                        }
                    }

                }
            }
        }
    }

    private List<Statement> getStatements(List<String> lines) {
        List<Statement> statements = new ArrayList<>();
        int codeLineNumber = 0;
        for(int i=0; i<lines.size(); i++) {
            String codeLine = lines.get(i);
            Statement codeStatement = null;
            boolean isLabel = isLabel(codeLine);
            if (isLabel) {
                codeStatement = new Statement(0, codeLine);
            } else {
                codeStatement = new Statement(codeLineNumber++, codeLine);
            }
            statements.add(codeStatement);
        }
        return statements;
    }

    public boolean isLabel(String line) {
        if (line.startsWith("(") && line.endsWith(")")) {
            return true;
        }
        return false;
    }

    public String extractLabel(String line) {
        return line.substring(1, line.length()-1);
    }

    public List<String> removeCommentsAndEmptyLines(List<String> lines, boolean ignoreLabel) {
        List<String> cleanedLines = new ArrayList<>();
        for(String line: lines) {
            line = line.strip();
            if (line.startsWith("//") || line.isEmpty()) {
                continue;
            }
            if (ignoreLabel && isLabel(line)) {
                continue;
            }
            cleanedLines.add(line);
        }
        return cleanedLines;
    }
    public void writeOutput(List<String> outputLines, String fileName) {
        try(FileWriter fw = new FileWriter(fileName+".hack", true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {
            for (String line:outputLines) {
                out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void translateFinalPass(String fileName) throws Exception {
        List<String> lines = Files.readAllLines(Path.of(fileName));
        List<String> cleanedLines  = removeCommentsAndEmptyLines(lines, true);
        List<String> output = new ArrayList<>();
        for (String line:cleanedLines) {
            if (line.startsWith("@")) {
                output.add(translateAInstruction(line));
            } else {
                output.add(translateCInstruction(line));
            }
        }
        writeOutput(output, fileName);
    }

    class Statement {
        int lineNumber;
        String line;

        public Statement(int lineNumber, String line) {
            this.lineNumber = lineNumber;
            this.line = line;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public void setLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
        }

        public String getLine() {
            return line;
        }

        public void setLine(String line) {
            this.line = line;
        }

        @Override
        public String toString() {
            return
                    "lineNumber=" + lineNumber +
                    ", line='" + line + '\'' +
                    '}';
        }
    }

    public static void main(String[] args) throws Exception {
        Assembler assembler = new Assembler();

        assembler.translateFirstPass("asm/Pong.asm");
        assembler.translateFinalPass("asm/Pong.asm");
    }
}
