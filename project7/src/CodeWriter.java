import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CodeWriter {

    private List<String> assemblerCode;
    private String fileName;

    // Generate Unique label names when there are multiple EQs, GTs or LTs in same vm file
    private int labelCounter = 0;

    private static final String ADDRESS = "@";
    private static final String ADDRESS_A_NUMBER = "@%d";
    private static final String ADDRESS_THE_SP = "@SP";
    private static final String D_EQUALS_A = "D=A";
    private static final String D_EQUALS_M = "D=M";
    private static final String A_EQUALS_M = "A=M";
    private static final String M_EQUALS_D = "M=D";
    private static final String INCREMENT_M = "M=M+1";
    private static final String DECREMENT_M = "M=M-1";
    private static final String D_PLUS_M = "D=D+M";
    private static final String D_EQUALS_NEG_M = "D=-M";
    private static final String M_MINUS_D = "D=M-D";
    private static final String D_EQ_D_AND_M = "D=D&M";
    private static final String D_EQ_D_OR_M = "D=D|M";
    private static final String D_EQ_NOT_D = "D=!D";

    private static final String PUSH_TRUE = "M=-1";
    private static final String PUSH_FALSE = "M=0";

    private static final String ADDRESS_LCL = "LCL";
    private static final String ADDRESS_ARG = "ARG";
    private static final String ADDRESS_THIS = "THIS";
    private static final String ADDRESS_THAT = "THAT";

    private static final String COMMENT = "//%s";

    private static final String ADD = "add";
    private static final String NEG = "neg";
    private static final String SUB = "sub";
    private static final String AND = "and";
    private static final String OR = "or";
    private static final String NOT = "not";
    private static final String EQ = "eq";
    private static final String GT = "gt";
    private static final String LT = "lt";

    private List<String> segments = Arrays.asList("local", "argument", "this", "that", "temp");

    public CodeWriter(String fileName) {
        this.assemblerCode = new ArrayList<>();
        this.fileName = fileName;
    }

    public void write(Command command) {
        if (CommandType.C_PUSH.equals(command.getCommandType())) {
            writePush(command);
        } else if (CommandType.C_POP.equals(command.getCommandType())) {
            writePop(command);
        } else if (CommandType.C_ARITHMETIC.equals(command.getCommandType())) {
            writeArithmetic(command);
        }
    }

    public void writePush(Command command) {
        List<String> pushCommands = new ArrayList<>();
        String segment = command.getArg1();
        int val = command.getArg2();
        if (segment.equals("constant")) {
            pushCommands.addAll(pushConstant(segment, val));
        } else if (segments.contains(segment)) {
            pushCommands.addAll(pushSegment(segment, val));
        } else if (segment.equals("static")) {
            pushCommands.addAll(pushStatic(val));
        }
        this.assemblerCode.addAll(pushCommands);
    }

    public void writePop(Command command) {
        List<String> popCommands = new ArrayList<>();
        String segment = command.getArg1();
        int val = command.getArg2();
        if (segments.contains(segment)) {
            popCommands.addAll(popSegment(segment, val));
        } else if (segment.equals("static")) {
            popCommands.addAll(popStatic(val));
        }
        this.assemblerCode.addAll(popCommands);
    }

    private List<String> pushSegment(String segmentCode, int val) {
        List<String> commands = new ArrayList<>();
        String segment = segmentAddrMapping(segmentCode);
        String comment = String.format("push %s %d", segmentCode, val);
        commands.add(String.format(COMMENT, comment));
        commands.add(String.format(ADDRESS_A_NUMBER, val));
        commands.add("D=A");
        commands.add("@"+ segment);
        // If temp segment, do D=D+A  instead of D=D+M, A=5 in the segmentAddrMapping
        String segAddressSum = isTemp(segment) ? "D=D+A" : "D=D+M";
        commands.add(segAddressSum);
        commands.add("A=D");
        commands.add("D=M");
        commands.addAll(pushDRegisterToStackAndIncrementSP());
        return commands;
    }

    private List<String> pushConstant(String segment, int val) {
        List<String> commands = new ArrayList<>();
        String comment = String.format("push %s %d", segment, val);
        commands.add(String.format(COMMENT, comment));
        commands.add(String.format(ADDRESS_A_NUMBER, val));
        commands.add(D_EQUALS_A);
        commands.addAll(pushDRegisterToStackAndIncrementSP());
        return commands;
    }
    private List<String> popSegment(String segmentCode, int val) {
        List<String> commands = new ArrayList<>();
        String segment = segmentAddrMapping(segmentCode);
        String comment = String.format("pop %s %d", segmentCode, val);
        commands.add(String.format(COMMENT, comment));
        commands.add(ADDRESS_THE_SP);
        commands.add("M=M-1");
        commands.add(String.format(ADDRESS_A_NUMBER, val));
        commands.add("D=A");
        commands.add("@"+ segment);
        // If temp segment, do D=D+A  instead of D=D+M, A=5 in the segmentAddrMapping
        String segAddressSum = isTemp(segment) ? "D=D+A" : "D=D+M";
        commands.add(segAddressSum);
        commands.add("@R13");
        commands.add("M=D");

        commands.add(ADDRESS_THE_SP);
        commands.add("A=M");
        commands.add("D=M");
        commands.add("@R13");
        commands.add("A=M");
        commands.add("M=D");
        return commands;
    }

    private List<String> pushStatic(int val) {
        String staticVar = null;
        if (fileName.contains("/")) {
            String[] parts = fileName.split("/");
            String lastPart = parts[parts.length-1];
            staticVar = lastPart.split("\\.")[0];
        } else {
            staticVar = fileName.split("\\.")[0];
        }
        List<String> commands = new ArrayList<>();
        String comment = String.format("push static %d", val);
        commands.add(String.format(COMMENT, comment));
        commands.add(String.format("@%s.%d", staticVar, val));
        commands.add("D=M");
        commands.add("@SP");
        commands.add("A=M");
        commands.add("M=D");
        commands.add("@SP");
        commands.add("M=M+1");
        return commands;
    }

    private List<String> popStatic(int val) {
        String staticVar = null;
        if (fileName.contains("/")) {
            String[] parts = fileName.split("/");
            String lastPart = parts[parts.length-1];
            staticVar = lastPart.split("\\.")[0];
        } else {
            staticVar = fileName.split("\\.")[0];
        }
        List<String> commands = new ArrayList<>();
        String comment = String.format("pop static %d", val);
        commands.add(String.format(COMMENT, comment));
        commands.add("@SP");
        commands.add("M=M-1");
        commands.add("A=M");
        commands.add("D=M");
        commands.add(String.format("@%s.%d", staticVar, val));
        commands.add("M=D");
        return commands;
    }

    private boolean isTemp(String segment) {
        return segment.equals("5");
    }

    private String segmentAddrMapping(String segmentName) {
        switch (segmentName) {
            case "local":
                return ADDRESS_LCL;
            case "argument":
                return ADDRESS_ARG;
            case "this":
                return ADDRESS_THIS;
            case "that":
                return ADDRESS_THAT;
            case "temp":
                return "5";
            default:
                return segmentName;
        }
    }

    public void writeArithmetic(Command command) {
        String op = command.getArg1();
        switch(op) {
            case ADD:
                this.assemblerCode.addAll(add());
                break;
            case NEG:
                this.assemblerCode.addAll(neg());
                break;
            case SUB:
                this.assemblerCode.addAll(sub());
                break;
            case AND:
                this.assemblerCode.addAll(and());
                break;
            case OR:
                this.assemblerCode.addAll(or());
                break;
            case NOT:
                this.assemblerCode.addAll(not());
                break;
            case EQ:
                this.assemblerCode.addAll(eq());
                break;
            case GT:
                this.assemblerCode.addAll(gt());
                break;
            case LT:
                this.assemblerCode.addAll(lt());
                break;
            default:
                System.out.println("Operation not supported yet");
                break;
        }

    }

    private List<String> pushDRegisterToStackAndIncrementSP() {
        List<String> addIncrementStack = new ArrayList<>();
        addIncrementStack.add(ADDRESS_THE_SP);
        addIncrementStack.add(A_EQUALS_M);
        addIncrementStack.add(M_EQUALS_D);
        addIncrementStack.add(ADDRESS_THE_SP);
        addIncrementStack.add(INCREMENT_M);
        return addIncrementStack;
    }

    private List<String> popToARegisterAndDecrementSP() {
        List<String> commands = new ArrayList<>();
        commands.add(ADDRESS_THE_SP);
        commands.add(DECREMENT_M);
        commands.add(A_EQUALS_M);
        return commands;
    }

    private List<String> add() {
        List<String> commands = new ArrayList<>();
        commands.add(String.format(COMMENT, " add"));
        commands.addAll(popToARegisterAndDecrementSP());
        // Store result in D
        commands.add(D_EQUALS_M);
        commands.addAll(popToARegisterAndDecrementSP());
        // Add and store in D
        commands.add(D_PLUS_M);
        commands.addAll(pushDRegisterToStackAndIncrementSP());
        return commands;
    }

    private List<String> neg() {
        List<String> commands = new ArrayList<>();
        commands.add(String.format(COMMENT, " neg"));
        commands.addAll(popToARegisterAndDecrementSP());
        commands.add(D_EQUALS_NEG_M);
        commands.addAll(pushDRegisterToStackAndIncrementSP());
        return commands;
    }

    private List<String> sub() {
        List<String> commands = new ArrayList<>();
        commands.add(String.format(COMMENT, " sub"));
        commands.addAll(popToARegisterAndDecrementSP());
        // Store val
        commands.add(D_EQUALS_M);
        commands.addAll(popToARegisterAndDecrementSP());
        // Sub and store in D
        commands.add(M_MINUS_D);
        commands.addAll(pushDRegisterToStackAndIncrementSP());
        return commands;
    }

    private List<String> and() {
        List<String> commands = new ArrayList<>();
        commands.add(String.format(COMMENT, " and"));
        commands.addAll(popToARegisterAndDecrementSP());
        // Store result in D
        commands.add(D_EQUALS_M);
        commands.addAll(popToARegisterAndDecrementSP());
        // AND and store in D
        commands.add(D_EQ_D_AND_M);
        commands.addAll(pushDRegisterToStackAndIncrementSP());
        return commands;
    }

    private List<String> or() {
        List<String> commands = new ArrayList<>();
        commands.add(String.format(COMMENT, " or"));
        commands.addAll(popToARegisterAndDecrementSP());
        // Store result in D
        commands.add(D_EQUALS_M);
        commands.addAll(popToARegisterAndDecrementSP());
        // OR and store in D
        commands.add(D_EQ_D_OR_M);
        commands.addAll(pushDRegisterToStackAndIncrementSP());
        return commands;
    }

    private List<String> not() {
        List<String> commands = new ArrayList<>();
        commands.add(String.format(COMMENT, " not"));
        commands.addAll(popToARegisterAndDecrementSP());
        // Store result in D
        commands.add(D_EQUALS_M);
        // NOT D
        commands.add(D_EQ_NOT_D);
        commands.addAll(pushDRegisterToStackAndIncrementSP());
        return commands;
    }

    private List<String> eq() {
        List<String> commands = new ArrayList<>();
        commands.add(String.format(COMMENT, " eq"));
        commands.addAll(popToARegisterAndDecrementSP());
        commands.add(D_EQUALS_M);
        commands.addAll(popToARegisterAndDecrementSP());
        commands.add(M_MINUS_D);
        // Assume EQ and push true(-1) to stack. Increment.
        commands.addAll(pushTrueInStackAndIncrement());
        // Jump to end if EQ, else push false(0) to stack. Increment.
        commands.add(String.format("@EQ_END_%d", labelCounter));
        commands.add("D;JEQ");
        commands.addAll(pushFalseInStackAndIncrement());
        commands.add(String.format("(EQ_END_%d)", labelCounter));
        labelCounter++;
        return commands;
    }

    private List<String> gt() {
        List<String> commands = new ArrayList<>();
        commands.add(String.format(COMMENT, " gt"));
        commands.addAll(popToARegisterAndDecrementSP());
        commands.add(D_EQUALS_M);
        commands.addAll(popToARegisterAndDecrementSP());
        commands.add(M_MINUS_D);
        // Assume EQ and push true(-1) to stack
        commands.addAll(pushTrueInStackAndIncrement());
        // Jump to end if EQ, else push false (0) to stack
        commands.add(String.format("@GT_END_%d", labelCounter));
        commands.add("D;JGT");
        commands.addAll(pushFalseInStackAndIncrement());
        commands.add(String.format("(GT_END_%d)", labelCounter));
        labelCounter++;
        return commands;
    }

    private List<String> lt() {
        List<String> commands = new ArrayList<>();
        commands.add(String.format(COMMENT, " lt"));
        commands.addAll(popToARegisterAndDecrementSP());
        commands.add(D_EQUALS_M);
        commands.addAll(popToARegisterAndDecrementSP());
        commands.add(M_MINUS_D);
        // Assume EQ and push true(-1) to stack
        commands.addAll(pushTrueInStackAndIncrement());
        // Jump to end if EQ, else push false (0) to stack
        commands.add(String.format("@LT_END_%d", labelCounter));
        commands.add("D;JLT");
        commands.addAll(pushFalseInStackAndIncrement());
        commands.add(String.format("(LT_END_%d)", labelCounter));
        labelCounter++;
        return commands;
    }

    private List<String> pushTrueInStackAndIncrement() {
        List<String> commands = new ArrayList<>();
        commands.add(ADDRESS_THE_SP);
        commands.add(A_EQUALS_M);
        commands.add(PUSH_TRUE);
        commands.add(ADDRESS_THE_SP);
        commands.add(INCREMENT_M);
        return commands;
    }

    private List<String> pushFalseInStackAndIncrement() {
        List<String> commands = new ArrayList<>();
        commands.add(ADDRESS_THE_SP);
        commands.add(DECREMENT_M);
        commands.add(A_EQUALS_M);
        commands.add(PUSH_FALSE);
        commands.add(ADDRESS_THE_SP);
        commands.add(INCREMENT_M);
        return commands;
    }

    public void flush() {
        String inputFileName = fileName.split("\\.")[0];
        String outputFile = inputFileName + ".asm";
        try(FileWriter fw = new FileWriter(outputFile, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {
            for (String line:assemblerCode) {
                out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void print() {
        for(String line: assemblerCode) {
            System.out.println(line);
        }
    }
}
