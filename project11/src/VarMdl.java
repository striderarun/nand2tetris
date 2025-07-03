import vm.Kind;

public class VarMdl {

    private String name;
    private String type;
    private Kind kind;
    private int index;

    public VarMdl(String name, String type, Kind kind) {
        this.name = name;
        this.type = type;
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
