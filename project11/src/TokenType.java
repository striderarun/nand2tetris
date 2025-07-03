public enum TokenType {

    KEYWORD("<keyword> %s </keyword>"),
    SYMBOL("<symbol> %s </symbol>"),
    IDENTIFIER("<identifier> %s </identifier>"),
    INT_CONST("<integerConstant> %s </integerConstant>"),
    STRING_CONST("<stringConstant> %s </stringConstant>");


    private String name;
    private String xmlTag;

    TokenType(String xmlTag) {
        this.xmlTag = xmlTag;
    }

    public String getXmlTag() {
        return xmlTag;
    }
}
