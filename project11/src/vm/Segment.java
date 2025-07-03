package vm;

public enum Segment {

    CONSTANT("constant"),
    ARGUMENT("argument"),
    LOCAL("local"),
    STATIC("static"),
    THIS("this"),
    THAT("that"),
    POINTER("pointer"),
    TEMP("temp");

    private String command;

    Segment(String name) {
        this.command = name;
    }

    public String getCommand() {
        return this.command;
    }
}
