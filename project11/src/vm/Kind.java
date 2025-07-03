package vm;

public enum Kind {

    STATIC("static"),
    FIELD("field"),
    ARGUMENT("argument"),
    LOCAL("local"),
    NONE("none");

    private String label;

    Kind(String label) {
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }
}
