import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompilationEngine {

    private JackTokenizer tokenizer;
    private String analyzerXml;
    private List<String> analyzerFragments = new ArrayList<>();

    private List<String> expressionOp = Arrays.asList(
             "+", "-", "*", "/", "&amp;", "|", "&lt;", "&gt;", "="
    );

    private List<String> unaryOp = Arrays.asList(
            "-", "~"
    );

    public CompilationEngine(JackTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    private String generateTag(Token token) {
        return String.format(token.getType().getXmlTag(), token.getValue());
    }

    public String compileClass() throws Exception {
        analyzerFragments.add(AnalyzerUtils.CLASS_TAG);
        eat(Keyword.CLASS);
        eat(TokenType.IDENTIFIER);
        eatSymbol("{");

        // Now compile class vars
        compileClassVarDec();

        // Now compile sub-routines
        compileSubroutineDec();

        // class closing brace
        eatSymbol("}");

        // class closing tag
        analyzerFragments.add(AnalyzerUtils.CLASS_CLOSING_TAG);

        // return the xml
        analyzerXml = String.join(System.lineSeparator(), analyzerFragments);
        return analyzerXml;
    }

    // classVarDec: (static | field) type varName (, varName)* ;
    private void compileClassVarDec() {
        // check if class vars are present
        while (isNextToken(Arrays.asList(Keyword.STATIC, Keyword.FIELD))) {
            analyzerFragments.add(AnalyzerUtils.CLASS_VAR_DEC_OPENING_TAG);

            // compile class var field type
            eat(Arrays.asList(Keyword.STATIC, Keyword.FIELD));

            // compile class var data type
            eatType();

            // compile class var name
            eat(TokenType.IDENTIFIER);

            // Optional: While next token is ",", then you have one or more varNames, else end with ";"
            // eg: field int x,y,z;
            while(isNextTokenThisSymbol(",")) {
                eatSymbol(",");
                eat(TokenType.IDENTIFIER);
            }
            eatSymbol(";");
            analyzerFragments.add(AnalyzerUtils.CLASS_VAR_DEC_CLOSING_TAG);
        }
    }

    private void compileSubroutineDec() {
        while(isNextToken(Arrays.asList(Keyword.METHOD, Keyword.CONSTRUCTOR, Keyword.FUNCTION))) {
            analyzerFragments.add(AnalyzerUtils.SUBROUTINE_DEC_OPENING_TAG);

            // compile sub-routine type
            eat(Arrays.asList(Keyword.METHOD, Keyword.CONSTRUCTOR, Keyword.FUNCTION));

            // compile return type in sub-routine declaration
            eatType();

            // compile sub-routine identifier
            eat(TokenType.IDENTIFIER);

            eatSymbol("(");

            // compile parameters
            compileParameterList();

            eatSymbol(")");

            // compile body
            compileSubroutineBody();

            analyzerFragments.add(AnalyzerUtils.SUBROUTINE_DEC_CLOSING_TAG);
        }
    }

    private void compileParameterList() {
        analyzerFragments.add(AnalyzerUtils.SUBROUTINE_PARAMS_OPENING_TAG);

        if (!isNextTokenThisSymbol(")")) {
            // we have at least one parameter
            eatType();
            eat(TokenType.IDENTIFIER);

            // do we have more parameters
            // eg: constructor Square new(int Ax, int Ay, int Asize)
            while(isNextTokenThisSymbol(",")) {
                eatSymbol(",");
                eatType();
                eat(TokenType.IDENTIFIER);
            }
        }

        analyzerFragments.add(AnalyzerUtils.SUBROUTINE_PARAMS_CLOSING_TAG);
    }

    private void compileSubroutineBody() {
        analyzerFragments.add(AnalyzerUtils.SUBROUTINE_BODY_OPENING_TAG);
        eatSymbol("{");

        // compile var declarations in subroutine
        compileMethodVarDec();

        // compile statements in subroutine
        compileStatements();

        eatSymbol("}");
        analyzerFragments.add(AnalyzerUtils.SUBROUTINE_BODY_CLOSING_TAG);

    }

    // methodVarDec: var type varName (, varName)* ;
    private void compileMethodVarDec() {
        // first check if there are any var declarations, since var dec is optional in method
        while (isNextToken(Arrays.asList(Keyword.VAR))) {
            analyzerFragments.add(AnalyzerUtils.METHOD_VAR_DEC_OPENING_TAG);

            // first var declaration
            eat(Arrays.asList(Keyword.VAR));

            // compile var data type
            eatType();

            // compile method var name
            eat(TokenType.IDENTIFIER);

            // Optional: While next token is ",", then you have one or more varNames, else end with ";"
            // eg: var int x,y,z;
            while(isNextTokenThisSymbol(",")) {
                eatSymbol(",");
                eat(TokenType.IDENTIFIER);
            }
            eatSymbol(";");
            analyzerFragments.add(AnalyzerUtils.METHOD_VAR_DEC_CLOSING_TAG);
        }
    }

    private void compileStatements() {
        analyzerFragments.add(AnalyzerUtils.STATEMENTS_OPENING_TAG);

        while (isNextToken(Arrays.asList(Keyword.LET, Keyword.IF, Keyword.WHILE, Keyword.DO, Keyword.RETURN))) {
            Keyword statementType = nextStatementTypeKeyword();
            switch(statementType) {
                case LET:
                    compileLetStatement();
                    break;
                case IF:
                    compileIfStatement();
                    break;
                case WHILE:
                    compileWhileStatement();
                    break;
                case DO:
                    compileDoStatement();
                    break;
                case RETURN:
                    compileReturnStatement();
                    break;
            }
        }

        analyzerFragments.add(AnalyzerUtils.STATEMENTS_CLOSING_TAG);

    }

    /**
     * Examples:
     * - let x = 2;
     *   let x[i] = 2;
     */
    private void compileLetStatement() {
        analyzerFragments.add(getStatementTag(Keyword.LET, true));

        eat(Keyword.LET);
        eat(TokenType.IDENTIFIER);

        // Check if let statement has indexing
        // eg: let x[i] = 2;
        if (isNextTokenThisSymbol("[")) {
            eatSymbol("[");
            compileExpression();
            eatSymbol("]");
        }

        eatSymbol("=");

        compileExpression();
        eatSymbol(";");

        analyzerFragments.add(getStatementTag(Keyword.LET, false));

    }

    private void compileIfStatement() {
        analyzerFragments.add(getStatementTag(Keyword.IF, true));

        // compile if (
        eat(Keyword.IF);
        eatSymbol("(");

        compileExpression();

        eatSymbol(")");

        // if body
        eatSymbol("{");
        compileStatements();
        eatSymbol("}");

        // if next token is 'else', handle else block
        if (isNextToken(Arrays.asList(Keyword.ELSE))) {
            eat(Keyword.ELSE);
            eatSymbol("{");
            compileStatements();
            eatSymbol("}");
        }

        analyzerFragments.add(getStatementTag(Keyword.IF, false));
    }

    private void compileWhileStatement() {
        analyzerFragments.add(getStatementTag(Keyword.WHILE, true));

        // while (
        eat(Keyword.WHILE);
        eatSymbol("(");

        compileExpression();

        eatSymbol(")");

        // while body
        eatSymbol("{");
        compileStatements();
        eatSymbol("}");

        analyzerFragments.add(getStatementTag(Keyword.WHILE, false));
    }

    private void compileDoStatement() {
        analyzerFragments.add(getStatementTag(Keyword.DO, true));

        eat(Keyword.DO);
        compileSubRoutineCall();
        eatSymbol(";");

        analyzerFragments.add(getStatementTag(Keyword.DO, false));
    }

    private void compileReturnStatement() {
        analyzerFragments.add(getStatementTag(Keyword.RETURN, true));

        eat(Keyword.RETURN);
        if (!isNextTokenThisSymbol(";")) {
            compileExpression();
        }
        eatSymbol(";");

        analyzerFragments.add(getStatementTag(Keyword.RETURN, false));
    }

    // Either subroutineName(params) or identifier.subroutineName(params)
    // Look ahead 2 tokens, if second token is ".", then it's identifier.subroutineName(params)
    private void compileSubRoutineCall() {
        List<Token> tokens = peekNTokensAhead(2);
        if (tokens.get(1).getValue().equals(".")) {
            eat(TokenType.IDENTIFIER);
            eatSymbol(".");
            compileCall();
        } else {
            compileCall();
        }
    }

    private void compileCall() {
        eat(TokenType.IDENTIFIER);
        eatSymbol("(");

        // compile expression lists
        compileExpressionList();

        eatSymbol(")");

    }

    private void compileExpression() {
        analyzerFragments.add("<expression>");

        // we have atleast one term
        compileTerm();

        // As long as next token is an expressionOp, we have more terms
        // expression: term (op term)*
        while(haveMoreExpressionTerms()) {
            // eat op symbol
            eat(TokenType.SYMBOL);
            compileTerm();
        }
        analyzerFragments.add("</expression>");
    }

    private void compileTerm() {
        analyzerFragments.add("<term>");

        // Identify term type
        Token nextToken = peekNextToken();
        if (nextToken.getType().equals(TokenType.INT_CONST)) {
            eat(TokenType.INT_CONST);
        } else if (nextToken.getType().equals(TokenType.STRING_CONST)) {
            eat(TokenType.STRING_CONST);
        } else if (isTermKeywordConstant(nextToken)) {
            eat(TokenType.KEYWORD);
        } else if (isTermUnaryOp(nextToken)) {
            // term: unaryOp term
            eat(TokenType.SYMBOL);
            compileTerm();
        } else if (nextToken.getValue().equals("(")) {
            // term: (expression)
            eatSymbol("(");
            compileExpression();
            eatSymbol(")");
        } else if (isTermVarName()) {
            // term: varName
            eat(TokenType.IDENTIFIER);
        } else if (isTermVarNameExpression()) {
            // term: varName[expression]
            eat(TokenType.IDENTIFIER);
            eatSymbol("[");
            compileExpression();
            eatSymbol("]");
        } else if (isTermSubRoutineCall()) {
            compileSubRoutineCall();
        }

        analyzerFragments.add("</term>");

    }

    private void compileExpressionList() {
        analyzerFragments.add("<expressionList>");

        if (!isNextTokenThisSymbol(")")) {
            // we have at least one expression
            compileExpression();

            while(isNextTokenThisSymbol(",")) {
                eatSymbol(",");
                compileExpression();
            }
        }
        analyzerFragments.add("</expressionList>");

    }

    private void eat(TokenType type) {
        if (tokenizer.hasMoreTokens()) {
            Token currentToken = tokenizer.advance();
            if (currentToken.getType().equals(type)) {
                analyzerFragments.add(generateTag(currentToken));
            }
        }
    }

    private void eat(Keyword keyword) {
        if (tokenizer.hasMoreTokens()) {
            Token currentToken = tokenizer.advance();
            if (currentToken.getType().equals(TokenType.KEYWORD) && keyword.getKeyword().equals(currentToken.getValue())) {
                analyzerFragments.add(generateTag(currentToken));
            }
        }
    }

    private void eat(List<Keyword> keywords) {
        if (tokenizer.hasMoreTokens()) {
            Token currentToken = tokenizer.advance();
            keywords.forEach(k -> {
                if (currentToken.getType().equals(TokenType.KEYWORD) && k.getKeyword().equals(currentToken.getValue())) {
                    analyzerFragments.add(generateTag(currentToken));
                }
            });
        }
    }

    private void eatSymbol(String symbol) {
        if (tokenizer.hasMoreTokens()) {
            Token currentToken = tokenizer.advance();
            if (currentToken.getType().equals(TokenType.SYMBOL) && currentToken.getValue().equals(symbol)) {
                analyzerFragments.add(generateTag(currentToken));
            }
        }
    }

    // Match on allowed types and an identifier
    // type: int | char | boolean | className(identifier)
    private void eatType() {
        boolean matchKeyword = false;
        List<Keyword> keywords = Arrays.asList(Keyword.INT, Keyword.BOOLEAN, Keyword.CHAR, Keyword.VOID);
        if (tokenizer.hasMoreTokens()) {
            Token currentToken = tokenizer.advance();
            for (Keyword keyword: keywords) {
                if (currentToken.getType().equals(TokenType.KEYWORD) && keyword.getKeyword().equals(currentToken.getValue())) {
                    analyzerFragments.add(generateTag(currentToken));
                    matchKeyword = true;
                    break;
                }
            }
            // Check if identifier matches if none of types matched
            if (!matchKeyword && currentToken.getType().equals(TokenType.IDENTIFIER)) {
                analyzerFragments.add(generateTag(currentToken));
            }
        }
    }

    // Peek and check if the next token is one of the given keywords
    private boolean isNextToken(List<Keyword> keywords) {
        if (tokenizer.hasMoreTokens()) {
            Token nextToken = tokenizer.peek();
            for (Keyword keyword: keywords) {
                if (nextToken.getType().equals(TokenType.KEYWORD) && keyword.getKeyword().equals(nextToken.getValue())) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private boolean isNextTokenThisSymbol(String symbol) {
        if (tokenizer.hasMoreTokens()) {
            Token nextToken = tokenizer.peek();
            if (nextToken.getType().equals(TokenType.SYMBOL) && nextToken.getValue().equals(symbol)) {
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean haveMoreExpressionTerms() {
        if (tokenizer.hasMoreTokens()) {
            Token nextToken = tokenizer.peek();
            if (nextToken.getType().equals(TokenType.SYMBOL) && expressionOp.contains(nextToken.getValue())) {
                return true;
            }
        }
        return false;
    }

    private boolean isTermKeywordConstant(Token token) {
        List<String> keywordConstants = Arrays.asList("false", "true", "null", "this");
        if (token.getType().equals(TokenType.KEYWORD) && keywordConstants.contains(token.getValue())) {
            return true;
        }
        return false;
    }

    /**
     * term: varName[expression]
     *
     * Do we have an identifier token that is followed by a [
     * If yes, then it is a varName[expression] term
     */
    private boolean isTermVarNameExpression() {
        List<Token> tokens = peekNTokensAhead(2);
        if (tokens.get(0).getType().equals(TokenType.IDENTIFIER) && tokens.get(1).getValue().equals("[")) {
            return true;
        }
        return false;
    }

    /**
     * term: subroutineCall
     *
     * Do we have an identifier token that is followed by:
     *      - (
     *      - .
     * If either of above is true, then it is a subroutineCall
     *
     * Examples: let x = Screen.new()
     *           let x = draw()
     *
     */
    private boolean isTermSubRoutineCall() {
        List<Token> tokens = peekNTokensAhead(2);
        if (tokens.get(0).getType().equals(TokenType.IDENTIFIER)) {
            String secondTokenValue = tokens.get(1).getValue();
            if (secondTokenValue.equals(".") || secondTokenValue.equals("(")) {
                return true;
            }
        }
        return false;
    }

    /**
     * term: varName
     * Check if term is just a varName and not varName[expression]
     * Do we have an identifier token that is NOT followed by one of the below:
     *      - [
     *      - .
     *      - (
     * If yes, then it is a varName term
     */
    private boolean isTermVarName() {
        List<Token> tokens = peekNTokensAhead(2);
        if (tokens.get(0).getType().equals(TokenType.IDENTIFIER)) {
            String secondTokenValue = tokens.get(1).getValue();
            if (!secondTokenValue.equals("[") && !secondTokenValue.equals(".") && !secondTokenValue.equals("(")) {
                return true;
            }
        }
        return false;
    }

    private boolean isTermUnaryOp(Token token) {
        if (token.getType().equals(TokenType.SYMBOL) && unaryOp.contains(token.getValue())) {
            return true;
        }
        return false;
    }

    private Token peekNextToken() {
        if (tokenizer.hasMoreTokens()) {
            Token nextToken = tokenizer.peek();
            return nextToken;
        }
        return null;
    }

    private List<Token> peekNTokensAhead(int N) {
        return tokenizer.peekN(N);
    }

    // NOTE: Only call this if you know next token is a keyword
    private Keyword nextStatementTypeKeyword() {
        Token token = peekNextToken();
        switch (token.getValue()) {
            case "let":
                return Keyword.LET;
            case "if":
                return Keyword.IF;
            case "do":
                return Keyword.DO;
            case "while":
                return Keyword.WHILE;
            case "return":
                return Keyword.RETURN;
        }
        return null;
    }

    private String getStatementTag(Keyword statementKeyword, boolean openingTag) {
        String formattedOpenTag = String.format("<%sStatement>", statementKeyword.getKeyword());
        String formattedCloseTag = String.format("</%sStatement>", statementKeyword.getKeyword());
        return openingTag ? formattedOpenTag: formattedCloseTag;
    }
}
