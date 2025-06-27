public enum Keyword {

    CLASS("class"),
    METHOD("method"),
    FUNCTION("function"),
    CONSTRUCTOR("constructor"),
    INT("int"),
    BOOLEAN("boolean"),
    CHAR("char"),
    VOID("void"),
    VAR("var"),
    STATIC("static"),
    FIELD("field"),
    LET("let"),
    DO("do"), WHILE("while"),
    IF("if"), ELSE("else"),
    RETURN("return"),
    TRUE("true"), FALSE("false"),
    NULL("null"),
    THIS("this");

    private String name;

    Keyword(String name) {
        this.name = name;
    }

    public String getKeyword() {
        return name;
    }
}
