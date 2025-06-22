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

    private boolean isFunction;
    private String currentFunctionName;

    // Generate Unique label names when there are multiple EQs, GTs or LTs in same vm file
    private int labelCounter = 0;

    private int returnAddressCounter = 0;

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
        } else if (CommandType.LABEL.equals(command.getCommandType())) {
            writeLabel(command);
        } else if (CommandType.IF_GOTO.equals(command.getCommandType())) {
            writeIfGoto(command);
        } else if (CommandType.GOTO.equals(command.getCommandType())) {
            writeGoto(command);
        } else if (CommandType.FUNCTION.equals(command.getCommandType())) {
            writeFunction(command);
        } else if (CommandType.CALL.equals(command.getCommandType())) {
            writeCall(command);
        } else if (CommandType.RETURN.equals(command.getCommandType())) {
            writeReturn();
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
        } else if (segment.equals("pointer")) {
            pushCommands.addAll(pushPointer(val));
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
        } else if (segment.equals("pointer")) {
            popCommands.addAll(popPointer(val));
        }
        this.assemblerCode.addAll(popCommands);
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

    public void writeLabel(Command command) {
        String label = null;
        if (isFunction) {
            String vmFileName = extractFileNameNoExt();
            label = String.format("(%s.%s$%s)", vmFileName, currentFunctionName, command.getArg1());
        } else {
            label = String.format("(%s)", command.getArg1());
        }
        this.assemblerCode.add(label);
    }

    public void writeIfGoto(Command command) {
        String labelName = null;
        if (isFunction) {
            String vmFileName = extractFileNameNoExt();
            labelName = String.format("%s.%s$%s", vmFileName, currentFunctionName, command.getArg1());
        } else {
            labelName = command.getArg1();
        }
        List<String> commands = new ArrayList<>();
        commands.add("// if-goto " + labelName);
        // Pop condition from stack
        commands.add("@SP");
        commands.add("M=M-1");
        commands.add("A=M");
        commands.add("D=M");
        // Jump to label if condition
        commands.add("@" + labelName);
        commands.add("D;JGT");
        this.assemblerCode.addAll(commands);
    }

    private void writeGoto(Command command) {
        String labelName = null;
        if (isFunction) {
            String vmFileName = extractFileNameNoExt();
            labelName = String.format("%s.%s$%s", vmFileName, currentFunctionName, command.getArg1());
        } else {
            labelName = command.getArg1();
        }
        List<String> commands = new ArrayList<>();
        commands.add("// goto " + labelName);
        commands.add("@" + labelName);
        commands.add("0;JMP");
        this.assemblerCode.addAll(commands);
    }

    private void writeFunction(Command command) {
        String functionName = command.getArg1();
        int nArgs = command.getArg2();
        List<String> commands = new ArrayList<>();
        commands.add("// function " + functionName);

        String vmFileName = extractFileNameNoExt();
        if (functionName.startsWith(vmFileName + ".")) {
            String[] nameParts = functionName.split("\\.");
            functionName = nameParts[1];
        }
        // Function label = (vmFileName.functionName)
        String functionLabel = String.format("(%s.%s)", vmFileName, functionName);
        commands.add(functionLabel);
        // Initialize nArgs arguments to 0
        // push nArgs arguments to stack as 0 and increment SP
        commands.add("// initialize and push arguments to 0");
        for (int i=0; i<nArgs; i++) {
            commands.add("@SP");
            commands.add("A=M");
            commands.add("M=0");
            commands.add("@SP");
            commands.add("M=M+1");
        }
        this.assemblerCode.addAll(commands);
        this.isFunction = true;
        this.currentFunctionName = functionName;
    }

    private void writeCall(Command command) {
        String functionName = command.getArg1();
        int nArgs = command.getArg2();
        String currentVmFileName = extractFileNameNoExt();

        List<String> commands = new ArrayList<>();
        commands.add(String.format("// ---- call %s %d begin -----", functionName, nArgs));

        // push returnAddress of caller function indicated by generateReturnAddressLabel
        String generateReturnAddressLabel = String.format("%s.%s$ret.%d", currentVmFileName, currentFunctionName, returnAddressCounter++);
        commands.add("// push return address");
        commands.add("@" + generateReturnAddressLabel);
        commands.add("D=A");
        commands.add("@SP");
        commands.add("A=M");
        commands.add("M=D");
        commands.add("@SP");
        commands.add("M=M+1");

        // push LCL, ARG, THIS, THAT of caller
        commands.addAll(pushNamedMemorySegment("LCL"));
        commands.addAll(pushNamedMemorySegment("ARG"));
        commands.addAll(pushNamedMemorySegment("THIS"));
        commands.addAll(pushNamedMemorySegment("THAT"));

        // ARG = SP - nArgs - 5
        commands.add("// ARG = SP - nArgs - 5");
        commands.add("@SP");
        commands.add("D=M");
        commands.add("@" + nArgs);
        commands.add("D=D-A");
        commands.add("@5");
        commands.add("D=D-A");
        commands.add("@ARG");
        commands.add("M=D");

        // LCL = SP
        commands.add("// LCL = SP");
        commands.add("@SP");
        commands.add("D=M");
        commands.add("@LCL");
        commands.add("M=D");

        // goto functionName
        commands.add("// goto " + functionName);
        commands.add("@" + functionName);
        commands.add("0;JMP");

        // push returnAddress label
        commands.add(String.format("(%s)", generateReturnAddressLabel));

        this.assemblerCode.addAll(commands);
    }

    private List<String> pushNamedMemorySegment(String namedSegment) {
        List<String> commands = new ArrayList<>();
        commands.add("// push " + namedSegment);
        commands.add("@" + namedSegment);
        commands.add("D=M");
        commands.add("@SP");
        commands.add("A=M");
        commands.add("M=D");
        commands.add("@SP");
        commands.add("M=M+1");
        return commands;
    }

    // SP = 256
    public static List<String> writeBootstrapCode() {
        List<String> commands = new ArrayList<>();
        commands.add("// bootstrap");
        commands.add("@256");
        commands.add("D=A");
        commands.add("@SP");
        commands.add("M=D");
        return commands;
    }

    private void writeReturn() {
        List<String> commands = new ArrayList<>();
        commands.add("// -- return begin --");

        // endFrame = LCL
        commands.add("// endFrame = LCL");
        commands.add("@LCL");
        commands.add("D=M");
        commands.add("@endFrame");
        commands.add("M=D");

        // retAddress = *(endFrame - 5)
        commands.addAll(endFrameMinusX("retAddress",5));

        // *ARG = pop()
        commands.add("// *ARG = pop()");
        commands.add("@SP");
        commands.add("M=M-1");
        commands.add("A=M");
        commands.add("D=M");
        commands.add("@ARG");
        commands.add("A=M");
        commands.add("M=D");

        // SP = ARG + 1
        commands.add("// SP = ARG + 1");
        commands.add("@ARG");
        commands.add("D=M+1");
        commands.add("@SP");
        commands.add("M=D");

        // THAT = *(endFrame - 1)
        commands.addAll(endFrameMinusX("THAT", 1));

        // THIS = *(endFrame - 2)
        commands.addAll(endFrameMinusX("THIS", 2));

        // ARG = *(endFrame - 3)
        commands.addAll(endFrameMinusX("ARG", 3));

        // LCL = *(endFrame - 4)
        commands.addAll(endFrameMinusX("LCL", 4));

        // goto retAddress; note retAddress is just a variable not a label
        // you have to jump to address that is pointed to by retAddress
        commands.add("// goto retAddress");
        commands.add("@retAddress");
        commands.add("A=M");
        commands.add("0;JMP");

        commands.add("// -- return end --");
        this.assemblerCode.addAll(commands);
    }

    private List<String> endFrameMinusX(String storeVariable, int x) {
        List<String> commands = new ArrayList<>();
        commands.add(String.format("// %s = *(endFrame - %d)", storeVariable, x));
        commands.add("@endFrame");
        commands.add("D=M");
        commands.add("@" + x);
        commands.add("D=D-A");
        commands.add("A=D");
        commands.add("D=M");
        commands.add("@" + storeVariable);
        commands.add("M=D");
        return commands;
    }

    private List<String> pushSegment(String segmentCode, int val) {
        List<String> commands = new ArrayList<>();
        String segment = segmentAddrMapping(segmentCode);
        String comment = String.format("push %s %d", segmentCode, val);
        commands.add(String.format(COMMENT, comment));
        commands.add(String.format(ADDRESS_A_NUMBER, val));
        commands.add("D=A");
        commands.add("@"+ segment);
        // If temp segment, do D=D+A  instead of D=D+M
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
        // If temp segment, do D=D+A  instead of D=D+M
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

    private List<String> pushPointer(int val) {
        List<String> commands = new ArrayList<>();
        String comment = String.format("push pointer %d", val);
        commands.add(String.format(COMMENT, comment));
        // If val=0, Get THIS value and store in D, else THAT value
        String segment = val == 0 ? ADDRESS_THIS: ADDRESS_THAT;
        commands.add("@" + segment);
        commands.add("D=M");
        // Push D to stack and increment
        commands.addAll(pushDRegisterToStackAndIncrementSP());
        return commands;
    }

    private List<String> popPointer(int val) {
        List<String> commands = new ArrayList<>();
        String comment = String.format("pop pointer %d", val);
        commands.add(String.format(COMMENT, comment));
        String segment = val == 0 ? ADDRESS_THIS: ADDRESS_THAT;
        commands.addAll(popToARegisterAndDecrementSP());
        // Store result in D
        commands.add(D_EQUALS_M);
        // Assign D to THIS/THAT
        commands.add("@" + segment);
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
        // Jump to end if EQ, else push false (0) to stack. Increment.
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
        // Assume GT 0 and push true(-1) to stack
        commands.addAll(pushTrueInStackAndIncrement());
        // Jump to end if GT, else push false (0) to stack
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
        // Assume LT and push true(-1) to stack
        commands.addAll(pushTrueInStackAndIncrement());
        // Jump to end if LT, else push false (0) to stack
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

    public List<String> getTranslatedAsm() {
        return this.assemblerCode;
    }

    private String extractFileNameNoExt() {
        String fileNameNoExt = null;
        if (fileName.contains("/")) {
            String[] parts = fileName.split("/");
            String lastPart = parts[parts.length-1];
            fileNameNoExt = lastPart.split("\\.")[0];
        } else {
            fileNameNoExt = fileName.split("\\.")[0];
        }
        return fileNameNoExt;
    }

    public void print() {
        for(String line: assemblerCode) {
            System.out.println(line);
        }
    }
}
