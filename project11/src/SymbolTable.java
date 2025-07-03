import vm.Kind;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {

    private final Map<String, VarMdl> classSymbolTable;
    private Map<String, VarMdl> subroutineSymbolTable;

    private final Map<Kind, Integer> classVarCounter;
    private final Map<Kind, Integer> subroutineVarCounter;


    public SymbolTable() {
        this.classSymbolTable = new HashMap<>();
        this.subroutineSymbolTable = new HashMap<>();
        this.classVarCounter = new HashMap<>();
        this.subroutineVarCounter = new HashMap<>();

        this.classVarCounter.put(Kind.FIELD, 0);
        this.classVarCounter.put(Kind.STATIC, 0);

        this.subroutineVarCounter.put(Kind.ARGUMENT, 0);
        this.subroutineVarCounter.put(Kind.LOCAL, 0);
    }

    public void addClassVar(VarMdl varMdl) {
        int classKindIndex = classVarCounter.get(varMdl.getKind());
        varMdl.setIndex(classKindIndex);
        classVarCounter.put(varMdl.getKind(), classKindIndex+1);
        classSymbolTable.put(varMdl.getName(), varMdl);
    }

    public void addMethodVar(VarMdl varMdl) {
        int kindIndex = subroutineVarCounter.get(varMdl.getKind());
        varMdl.setIndex(kindIndex);
        subroutineVarCounter.put(varMdl.getKind(), kindIndex+1);
        subroutineSymbolTable.put(varMdl.getName(), varMdl);
    }

    public void startSubroutine() {
        this.subroutineSymbolTable.clear();
        this.subroutineVarCounter.clear();

        this.subroutineVarCounter.put(Kind.ARGUMENT, 0);
        this.subroutineVarCounter.put(Kind.LOCAL, 0);
    }

    public int getVariableCount(Kind kind) {
        int varCount = 0;
        if (kind.equals(Kind.FIELD) || kind.equals(Kind.STATIC)) {
            varCount = classVarCounter.get(kind);
        } else if (kind.equals(Kind.ARGUMENT) || kind.equals(Kind.LOCAL)) {
            varCount = subroutineVarCounter.get(kind);
        }
        return varCount;
    }

    public Kind getKindOf(String varName) {
        if (subroutineSymbolTable.containsKey(varName)) {
            VarMdl mdl = subroutineSymbolTable.get(varName);
            return mdl.getKind();
        }
        if (classSymbolTable.containsKey(varName)) {
            VarMdl mdl = classSymbolTable.get(varName);
            return mdl.getKind();
        }
        return Kind.NONE;
    }

    public String getTypeOf(String varName) {
        if (subroutineSymbolTable.containsKey(varName)) {
            VarMdl mdl = subroutineSymbolTable.get(varName);
            return mdl.getType();
        }
        if (classSymbolTable.containsKey(varName)) {
            VarMdl mdl = classSymbolTable.get(varName);
            return mdl.getType();
        }
        return null;
    }

    public int getIndexOf(String varName) {
        if (subroutineSymbolTable.containsKey(varName)) {
            VarMdl mdl = subroutineSymbolTable.get(varName);
            return mdl.getIndex();
        }
        if (classSymbolTable.containsKey(varName)) {
            VarMdl mdl = classSymbolTable.get(varName);
            return mdl.getIndex();
        }
        return -1;
    }

    public boolean doesVariableExist(String varName) {
        return subroutineSymbolTable.containsKey(varName) || classSymbolTable.containsKey(varName);
    }
}