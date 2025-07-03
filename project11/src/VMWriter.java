import vm.Kind;
import vm.Segment;
import vm.StackOp;

import java.util.ArrayList;
import java.util.List;

public class VMWriter {

    private List<String> vmStatements = new ArrayList<>();

    public void writeMemorySegment(Kind kind, int index, StackOp op) {
        if (op.equals(StackOp.PUSH)) {
            writePush(kind, index);
        }
        if (op.equals(StackOp.POP)) {
            writePop(kind, index);
        }
    }

    public void writePush(Kind kind, int index) {
        Segment segment = kindToSegment(kind);
        vmStatements.add(String.format("push %s %d", segment.getCommand(), index));
    }

    public void writePop(Kind kind, int index) {
        Segment segment = kindToSegment(kind);
        vmStatements.add(String.format("pop %s %d", segment.getCommand(), index));
    }

    public void writePush(Segment segment, int index) {
        vmStatements.add(String.format("push %s %d", segment.getCommand(), index));
    }

    public void writePop(Segment segment, int index) {
        vmStatements.add(String.format("pop %s %d", segment.getCommand(), index));
    }

    private Segment kindToSegment(Kind kind) {
        switch (kind) {
            case FIELD: return Segment.THIS;
            case STATIC: return Segment.STATIC;
            case ARGUMENT: return Segment.ARGUMENT;
            case LOCAL: return Segment.LOCAL;
            default : throw new RuntimeException("Unexpected Kind");
        }
    }

    public void writeOp(String command) {
        switch (command) {
            case "+": vmStatements.add("add"); break;
            case "*": writeCall("Math.multiply", 2); break;
            case "-": vmStatements.add("sub"); break;
            case "/": writeCall("Math.divide", 2); break;
            case "=": vmStatements.add("eq"); break;
            case "&gt;": vmStatements.add("gt"); break;
            case "&lt;": vmStatements.add("lt"); break;
            case "&amp;": vmStatements.add("and"); break;
            case "|": vmStatements.add("or"); break;
        }
    }

    public void writeUnaryOp(String command) {
        switch (command) {
            case "-": vmStatements.add("neg"); break;
            case "~": vmStatements.add("not"); break;
        }
    }

    public void negate() {
        vmStatements.add("not");
    }

    public void writeKeywordConstants(String keyword) {
        if (keyword.equals("true")) {
            vmStatements.add("push constant 0");
            vmStatements.add("not");
        } else if (keyword.equals("false")) {
            vmStatements.add("push constant 0");
        } else if (keyword.equals("this")) {
            /**
             * eg:
             * method void draw() {
             *     do Memory.dealloc(this);
             *     return;
             * }
             */

            // We encountered this when compiling the do statement "do Memory.dealloc(this)"
            // Push 'this' on the stack before calling call Memory.dealloc 1
            vmStatements.add("push pointer 0");
        }
    }

    public void writeLabel(String label) {
        vmStatements.add("label " + label);
    }

    public void writeGoto(String label) {
        vmStatements.add("goto " + label);
    }

    public void writeIfGoto(String label) {
        vmStatements.add("if-goto " + label);
    }

    public void writeCall(String name, int nArgs) {
        String callCommand = String.format("call %s %s", name, nArgs);
        vmStatements.add(callCommand);
    }

    public void writeFunction(String name, int nLocals) {
        String functionDef = String.format("function %s %d", name, nLocals);
        vmStatements.add(functionDef);
    }

    public void writeReturn() {
        vmStatements.add("return");
    }

    public void writeText(String text) {
        writePush(Segment.CONSTANT, text.length());
        writeCall("String.new", 1);
        for (char c: text.toCharArray()) {
            writePush(Segment.CONSTANT, c);
            writeCall("String.appendChar", 2);
        }
    }

    // Call Memory.alloc to find and reserve memory block of reserved size (words)
    // Returns base address which needs to be written to pointer 0 to anchor 'this' pointer at the base address
    public void writeConstructor(int words) {
        writePush(Segment.CONSTANT, words);
        vmStatements.add("call Memory.alloc 1");
        writePop(Segment.POINTER, 0);
    }

    public List<String> getCompiledStatements() {
        return vmStatements;
    }

    public void addStatement(String statement) {
        vmStatements.add(statement);
    }


}
