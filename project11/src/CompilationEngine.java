import vm.Kind;
import vm.Segment;
import vm.StackOp;
import vm.Subroutine;

import java.util.List;
import java.util.Objects;

public class CompilationEngine {

    private final JackTokenizer tokenizer;
    private String className;
    private final SymbolTable symbolTable = new SymbolTable();
    private final VMWriter vmWriter = new VMWriter();

    private int ifLabelCounter = 0;
    private int ifEndLabelCounter = 0;

    private int whileBeginLabelCounter = 0;
    private int whileEndLabelCounter = 0;

    private final List<String> expressionOp = List.of(
             "+", "-", "*", "/", "&amp;", "|", "&lt;", "&gt;", "="
    );

    private final List<String> unaryOp = List.of(
            "-", "~"
    );

    public CompilationEngine(JackTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public String compileClass() {
        eat(Keyword.CLASS);

        // record the class name; eat(TokenType.IDENTIFIER);
        this.className = getNextToken().getValue();

        eatSymbol("{");

        // Now compile class vars
        compileClassVarDec();

        // Now compile sub-routines
        compileSubroutineDec();

        // class closing brace
        eatSymbol("}");

        // return the compiled vm code statements
        return String.join(System.lineSeparator(), vmWriter.getCompiledStatements());
    }

    // classVarDec: (static | field) type varName (, varName)* ;
    private void compileClassVarDec() {
        // check if class vars are present
        while (isNextToken(List.of(Keyword.STATIC, Keyword.FIELD))) {
            // compile class var field type
            // eat(List.of(Keyword.STATIC, Keyword.FIELD));
            String varKind = getNextToken().getValue();


            // compile class var data type; eatType();
            String varType = getNextToken().getValue();

            // compile class var name; eat(TokenType.IDENTIFIER);
            String varName = getNextToken().getValue();

            VarMdl classVarMdl = new VarMdl(varName, varType, Kind.valueOf(varKind.toUpperCase()));
            symbolTable.addClassVar(classVarMdl);

            // Optional: While next token is ",", then you have one or more varNames, else end with ";"
            // eg: field int x,y,z;
            while(isNextTokenThisSymbol(",")) {
                // skip over comma
                eatSymbol(",");

                // next variable, eat(TokenType.IDENTIFIER);
                String nextVarName = getNextToken().getValue();
                VarMdl nextClassVarMdl = new VarMdl(nextVarName, varType, Kind.valueOf(varKind.toUpperCase()));
                symbolTable.addClassVar(nextClassVarMdl);

            }
            // skip comma,
            eatSymbol(";");
        }
    }

    private void compileSubroutineDec() {
        while(isNextToken(List.of(Keyword.METHOD, Keyword.CONSTRUCTOR, Keyword.FUNCTION))) {
            // compile sub-routine type; eat(List.of(Keyword.METHOD, Keyword.CONSTRUCTOR, Keyword.FUNCTION));
            // Get the function type
            Subroutine subroutineType = Subroutine.valueOf(getNextToken().getValue().toUpperCase());

            // compile return type in sub-routine declaration
            eatType();

            // compile sub-routine name; eat(TokenType.IDENTIFIER);
            String subroutineName = getNextToken().getValue();

            eatSymbol("(");

            // start the subroutine variable tracker
            symbolTable.startSubroutine();
            // compile parameters
            compileParameterList();

            eatSymbol(")");

            // compile body
            compileSubroutineBody(subroutineName, subroutineType);
        }
    }

    private void compileParameterList() {
        if (!isNextTokenThisSymbol(")")) {
            // we have at least one parameter, get it's type; eatType();
            String varType = getNextToken().getValue();

            // get varName; eat(TokenType.IDENTIFIER);
            String varName = getNextToken().getValue();

            VarMdl methodVarMdl = new VarMdl(varName, varType, Kind.ARGUMENT);
            symbolTable.addMethodVar(methodVarMdl);

            // do we have more parameters
            // eg: constructor Square new(int Ax, int Ay)
            while(isNextTokenThisSymbol(",")) {
                // skip the comma;
                eatSymbol(",");

                // get type;  eatType();
                String nextVarType = getNextToken().getValue();

                //get varName; eat(TokenType.IDENTIFIER);
                String nextVarName = getNextToken().getValue();

                VarMdl nextMethodVarMdl = new VarMdl(nextVarName, nextVarType, Kind.ARGUMENT);
                symbolTable.addMethodVar(nextMethodVarMdl);
            }
        }
    }

    private void compileSubroutineBody(String subroutineName, Subroutine subroutineType) {
        eatSymbol("{");

        // compile local var declarations in subroutine
        // update symbol table with local variables
        compileMethodVarDec();

        // write the function definition now
        // When defining a function, nArgs refers to number of LOCAL variables
        // When calling a function, nArgs refers to number of ARGUMENTs
        int varCount = symbolTable.getVariableCount(Kind.LOCAL);
        vmWriter.writeFunction(String.format("%s.%s", className, subroutineName), varCount);

        // Constructor specific vm code
        if (subroutineType.equals(Subroutine.CONSTRUCTOR)) {
            int arguments = symbolTable.getVariableCount(Kind.FIELD);
            vmWriter.writeConstructor(arguments);
        }

        /**
         * All methods in a class need to have below vm code.
         *
         * eg: method void draw() {}
         *
         * The caller of someObj.draw() would have passed the 'this' object as first implicit argument
         * We want to retrieve this first argument and set it to the 'this' pointer
         *
         * push the implicit first argument (this) of method to stack
         * push argument 0
         *
         * pop it to pointer 0 to set this
         * pop pointer 0
         *
         * Now, we can access object fields using the aligned 'this' pointer
        */
        if (subroutineType.equals(Subroutine.METHOD)) {
            // push argument 0
            vmWriter.writePush(Segment.ARGUMENT, 0);
            // pop pointer 0
            vmWriter.writePop(Segment.POINTER, 0);
        }

        // compile statements in subroutine
        compileStatements();

        eatSymbol("}");
    }

    // methodVarDec: var type varName (, varName)* ;
    private void compileMethodVarDec() {
        // first check if there are any var declarations, since var dec is optional in method
        while (isNextToken(List.of(Keyword.VAR))) {
            // first var declaration; eat(List.of(Keyword.VAR));
            // skip the var token
            advance();

            // compile var data type; eatType();
            String varType = getNextToken().getValue();

            // compile method var name; eat(TokenType.IDENTIFIER);
            String varName = getNextToken().getValue();

            VarMdl methodVarMdl = new VarMdl(varName, varType, Kind.LOCAL);
            symbolTable.addMethodVar(methodVarMdl);

            // Optional: While next token is ",", then you have one or more varNames, else end with ";"
            // eg: var int x,y,z;
            while(isNextTokenThisSymbol(",")) {
                // skip comma,
                eatSymbol(",");

                // compile method var name; eat(TokenType.IDENTIFIER);
                String nextVarName = getNextToken().getValue();

                VarMdl nextMethodVarMdl = new VarMdl(nextVarName, varType, Kind.LOCAL);
                symbolTable.addMethodVar(nextMethodVarMdl);

            }
            // skip semicolon;
            eatSymbol(";");
        }
    }

    private void compileStatements() {
        while (isNextToken(List.of(Keyword.LET, Keyword.IF, Keyword.WHILE, Keyword.DO, Keyword.RETURN))) {
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
    }

    /**
     * Examples:
     * - let x = 2;
     *   let x[i] = 2;
     */
    private void compileLetStatement() {
        boolean isArrayAddressing = false;
        eat(Keyword.LET);

        // Get the variable name; eat(TokenType.IDENTIFIER);
        Token varToken = getNextToken();

        // Check if let statement has indexing
        // eg: let x[i] = y[j];
        if (isNextTokenThisSymbol("[")) {
            isArrayAddressing = true;
            eatSymbol("[");

            // push array variable
            compileClassOrMethodVariable(varToken, StackOp.PUSH);

            // compile the expression
            compileExpression();

            // add
            vmWriter.writeOp("+");

            eatSymbol("]");
        }

        eatSymbol("=");

        // Compile the right hand side of the let statement and place the result on stack
        compileExpression();

        if (isArrayAddressing) {
            vmWriter.addStatement("pop temp 0");
            vmWriter.addStatement("pop pointer 1");
            vmWriter.addStatement("push temp 0");
            vmWriter.addStatement("pop that 0");

        } else {
            // Assignment to a non array variable
            compileClassOrMethodVariable(varToken, StackOp.POP);
        }
        eatSymbol(";");
    }

    private void compileIfStatement() {
        // compile if (
        eat(Keyword.IF);
        eatSymbol("(");

        // compile the if expression
        compileExpression();

        // negate the if expression
        vmWriter.negate();

        // generate a label name for else statement; (since we eval if (!expression))
        int currentLabelCount = this.ifLabelCounter++;
        String ifNotTrueLabel = String.format("%s_IF_NOT_TRUE_%d", this.className, currentLabelCount);
        vmWriter.writeIfGoto(ifNotTrueLabel);

        eatSymbol(")");

        // if body
        eatSymbol("{");

        // statements if condition=true
        compileStatements();

        // generate a label for end of the if condition
        String ifEndLabel = String.format("%s_IF_END_%d", this.className, currentLabelCount);
        vmWriter.writeGoto(ifEndLabel);

        eatSymbol("}");

        // if next token is 'else', handle else block
        if (isNextToken(List.of(Keyword.ELSE))) {
            eat(Keyword.ELSE);
            eatSymbol("{");

            // write the label
            vmWriter.writeLabel(ifNotTrueLabel);

            // else block statements
            compileStatements();
            eatSymbol("}");
        }

        // write the label denoting the end of the if statement
        vmWriter.writeLabel(ifEndLabel);
    }

    private void compileWhileStatement() {
        // while (
        eat(Keyword.WHILE);

        // generate and insert a label denoting <begin> of the while loop
        String whileBeginLabel = String.format("%s_WHILE_LOOP_BEGIN_%d", this.className, whileBeginLabelCounter++);
        vmWriter.writeLabel(whileBeginLabel);

        eatSymbol("(");

        // compile the while expression and negate it
        compileExpression();
        vmWriter.negate();

        eatSymbol(")");

        // generate a label denoting <end> of the while loop
        // goto end of the while loop
        String whileEndLabel = String.format("%s_WHILE_LOOP_END_%d", this.className, whileEndLabelCounter++);
        vmWriter.writeIfGoto(whileEndLabel);

        // while body statements
        eatSymbol("{");
        compileStatements();

        // goto <begin> of the while loop
        vmWriter.writeGoto(whileBeginLabel);

        eatSymbol("}");

        // insert the end label
        vmWriter.writeLabel(whileEndLabel);
    }

    private void compileDoStatement() {
        eat(Keyword.DO);
        compileSubRoutineCall();
        eatSymbol(";");

        // do statements are effectively void function calls
        // the void function still returns constant 0
        // here we discard the return value from the do call into temp 0
        // delete unused return values and free up stack space
        vmWriter.writePop(Segment.TEMP, 0);
    }

    private void compileReturnStatement() {
        eat(Keyword.RETURN);

        // constructor return this
        if (peekNextToken().getValue().equals("this")) {
            eat(Keyword.THIS);
            // push pointer 0
            vmWriter.writePush(Segment.POINTER, 0);
        } else if (!isNextTokenThisSymbol(";")) {
            compileExpression();
        } else {
            // empty return, push constant 0
            vmWriter.writePush(Segment.CONSTANT, 0);
        }
        eatSymbol(";");
        vmWriter.writeReturn();
    }

    // Either subroutineName(params) or identifier.subroutineName(params)
    // Look ahead 2 tokens, if second token is ".", then it's identifier.subroutineName(params)
    private void compileSubRoutineCall() {
        boolean thisFlag = false;
        StringBuilder subRoutineCallName = new StringBuilder();
        List<Token> tokens = peekTwoTokensAhead();
        int subRoutineParamsCount;
        if (tokens.get(1).getValue().equals(".")) {
            // get the first token abc in abc.fun()
            String reference = getNextToken().getValue();
            if (symbolTable.doesVariableExist(reference)) {
                /**
                 * This is a case where a method is being called on an object reference.
                 * eg:
                 *  field Square square;
                 *  method void moveSquare() {
                 *       do square.moveUp();
                 *  }
                 *
                 * We need to pass the 'this' object as implicit first argument into moveUp
                 * when compiling do square.moveUp()
                 * Then lookup the Type of the obj ref 'square'
                 *
                 * Then square.moveUp() is translated as
                 *
                 * push this 0
                 * call Square.moveUp 1
                 *
                 * Notice how Square is the class type of the object ref 'square' in jack code
                 */
                vmWriter.writePush(Segment.THIS, 0);
                String varType = symbolTable.getTypeOf(reference);
                // call varType.method
                subRoutineCallName.append(varType).append(".");
                thisFlag = true;
                eatSymbol(".");
            } else {
                // There is no variable with this name, so this is a class reference
                // A static method invocation on a Class
                // eg: do Screen.draw(), Screen is the current token and it's a class since we did not find this in the symbol table
                // Get the class identifier; eat(TokenType.IDENTIFIER);
                subRoutineCallName.append(reference).append(".");
                eatSymbol(".");
            }

        } else if (tokens.get(1).getValue().equals("(")) {
            /**
             * Case where the do statement is part of a method calling another method in a class
             * eg:
             * method void draw() {
             *     do erase();
             *     return;
             * }
             *
             * We have to push 'this' as implicit first argument into the erase() method
             * push this -> push pointer 0
             *
             * Then the do statement is translated as "call Square.erase 1"
             * Since we have one implicit argument, we have to increment erase() call arguments by 1.
             *
             */
            vmWriter.writePush(Segment.POINTER, 0);
            thisFlag = true;

            // call ClassName.method
            subRoutineCallName.append(className).append(".");
        }
        // Get the callee function name; eat(TokenType.IDENTIFIER);
        subRoutineCallName.append(getNextToken().getValue());

        // Compile the arguments of the function call
        subRoutineParamsCount = compileCallArguments();

        // If we pushed 'this' as implicit first argument, we have an extra param in the call arguments
        if (thisFlag) {
            subRoutineParamsCount++;
        }
        // Call the function now
        vmWriter.writeCall(subRoutineCallName.toString(), subRoutineParamsCount);
    }

    private int compileCallArguments() {
        eatSymbol("(");

        // compile expression lists
        int argumentsCount = compileExpressionList();

        eatSymbol(")");
        return argumentsCount;

    }

    private int compileExpressionList() {
        int expressionsCount = 0;
        if (!isNextTokenThisSymbol(")")) {
            // we have at least one expression
            compileExpression();
            expressionsCount++;

            while(isNextTokenThisSymbol(",")) {
                eatSymbol(",");
                compileExpression();
                expressionsCount++;
            }
        }
        return expressionsCount;
    }

    private void compileExpression() {
        // we have at least one term
        Token term1 = getNextToken();
        compileTerm(term1);

        // As long as next token is an expressionOp, we have more terms
        // expression: term1 (op term2)*
        while(haveMoreExpressionTerms()) {
            // When we have term1 (op term2)*, compile in postfix notation, (term1 term2 op)
            // Meaning we compile the terms first and then the expression op

            // Get the expression op symbol; eat(TokenType.SYMBOL);
            Token expressionOp = getNextToken();

            // Get term2
            Token term2 = getNextToken();

            // Compile term2, then compile the exp op
            compileTerm(term2);
            vmWriter.writeOp(expressionOp.getValue());
        }
    }

    private void compileTerm(Token term) {
        // Identify term type
        if (term.getType().equals(TokenType.INT_CONST)) {
            vmWriter.writePush(Segment.CONSTANT, Integer.parseInt(term.getValue()));
        } else if (term.getType().equals(TokenType.STRING_CONST)) {
            // eat(TokenType.STRING_CONST);
            String text = term.getValue();
            vmWriter.writeText(text);
        } else if (isTermKeywordConstant(term)) {
            // eat(TokenType.KEYWORD);
            vmWriter.writeKeywordConstants(term.getValue());
        } else if (isTermUnaryOp(term)) {
            // term: (unaryOp term)
            Token unaryOpTerm = getNextToken();

            // Compile in postfix, the term first
            compileTerm(unaryOpTerm);

            // the unary op next; eat(TokenType.SYMBOL);
            vmWriter.writeUnaryOp(term.getValue());
        } else if (term.getValue().equals("(")) {
            backwards();
            // term: (expression);
            eatSymbol("(");
            compileExpression();
            eatSymbol(")");
        } else if (isTermVariableName(term)) {
            // The variable can be a class or a method variable
            // term: varName; eat(TokenType.IDENTIFIER);
            compileClassOrMethodVariable(term, StackOp.PUSH);
        } else if (isTermVarNameExpression(term)) {
            // term: varName[expression]; eat(TokenType.IDENTIFIER);
            // push array variable
            compileClassOrMethodVariable(term, StackOp.PUSH);

            eatSymbol("[");
            // compile the expression
            compileExpression();

            // add
            vmWriter.writeOp("+");

            // align the result with 'that' 0
            vmWriter.writePop(Segment.POINTER, 1);
            vmWriter.writePush(Segment.THAT, 0);

            eatSymbol("]");
        } else if (isTermSubRoutineCall(term)) {
            backwards();
            compileSubRoutineCall();
        }
    }

    private void compileClassOrMethodVariable(Token varToken, StackOp op) {
        if (symbolTable.doesVariableExist(varToken.getValue())) {
            Kind kind = symbolTable.getKindOf(varToken.getValue());
            int index = symbolTable.getIndexOf(varToken.getValue());
            vmWriter.writeMemorySegment(kind, index, op);
        }
    }


    // Simply skip over tokens which you don't need to process
    private void advance() {
        tokenizer.advance();
    }

    private void backwards() {
        tokenizer.backwards();
    }

    private void eat(TokenType type) {
        if (tokenizer.hasMoreTokens()) {
            Token currentToken = tokenizer.advance();
            if (!currentToken.getType().equals(type)) {
                throw new RuntimeException("Something wrong");
            }
        }
    }

    private void eat(Keyword keyword) {
        if (tokenizer.hasMoreTokens()) {
            Token currentToken = tokenizer.advance();
            if (!(currentToken.getType().equals(TokenType.KEYWORD) && keyword.getKeyword().equals(currentToken.getValue()))) {
                throw new RuntimeException("Something wrong");
            }
        }
    }

    private void eat(List<Keyword> keywords) {
        if (tokenizer.hasMoreTokens()) {
            Token currentToken = tokenizer.advance();
            if (!anyKeywordMatch(keywords, currentToken)) {
                throw new RuntimeException("Something wrong");
            }

        }
    }

    private void eatSymbol(String symbol) {
        if (tokenizer.hasMoreTokens()) {
            Token currentToken = tokenizer.advance();
            if (!(currentToken.getType().equals(TokenType.SYMBOL) && currentToken.getValue().equals(symbol))) {
                throw new RuntimeException("Something wrong");
            }
        }
    }

    // Match on allowed types and an identifier
    // type: int | char | boolean | className(identifier)
    private void eatType() {
        boolean match;
        List<Keyword> keywords = List.of(Keyword.INT, Keyword.BOOLEAN, Keyword.CHAR, Keyword.VOID);
        if (tokenizer.hasMoreTokens()) {
            Token currentToken = tokenizer.advance();
            match = anyKeywordMatch(keywords, currentToken);
            // Check if identifier matches as none of types matched
            if (!match && currentToken.getType().equals(TokenType.IDENTIFIER)) {
                match = true;
            }
            if (!match) {
                throw new RuntimeException("Something wrong");
            }
        }
    }

    private boolean anyKeywordMatch(List<Keyword> keywords, Token currentToken) {
        for (Keyword keyword: keywords) {
            if (currentToken.getType().equals(TokenType.KEYWORD) && keyword.getKeyword().equals(currentToken.getValue())) {
                return true;
            }
        }
        return false;
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
            return nextToken.getType().equals(TokenType.SYMBOL) && nextToken.getValue().equals(symbol);
        }
        return false;
    }

    private boolean haveMoreExpressionTerms() {
        if (tokenizer.hasMoreTokens()) {
            Token nextToken = tokenizer.peek();
            return nextToken.getType().equals(TokenType.SYMBOL) && expressionOp.contains(nextToken.getValue());
        }
        return false;
    }

    private boolean isTermKeywordConstant(Token token) {
        List<String> keywordConstants = List.of("false", "true", "null", "this");
        return token.getType().equals(TokenType.KEYWORD) && keywordConstants.contains(token.getValue());
    }

    /**
     * term: varName[expression]
     * Do we have an identifier token that is followed by a [
     * If yes, then it is a varName[expression] term
     */
    private boolean isTermVarNameExpression(Token term) {
        Token nextToken = peekNextToken();
        return term.getType().equals(TokenType.IDENTIFIER) && nextToken.getValue().equals("[");
    }

    /**
     * term: subroutineCall
     * Do we have an identifier token that is followed by:
     *      - (
     *      - .
     * If either of above is true, then it is a subroutineCall
     * Examples: let x = Screen.new()
     *           let x = draw()
     *
     */
    private boolean isTermSubRoutineCall(Token term) {
        if (term.getType().equals(TokenType.IDENTIFIER)) {
            Token nextToken = peekNextToken();
            assert nextToken != null;
            String nextTokenValue = nextToken.getValue();
            return nextTokenValue.equals(".") || nextTokenValue.equals("(");
        }
        return false;
    }

    /**
     * term: variableName
     * Check if term is just a variableName and not variableName[expression]
     * Do we have an identifier token that is NOT followed by one of the below:
     *      - [
     *      - .
     *      - (
     * If yes, then it is a variableName term
     * Note: A variable here can refer to a class variable(field, static) or a method variable (argument, local)
     */
    private boolean isTermVariableName(Token term) {
        if (term.getType().equals(TokenType.IDENTIFIER)) {
            Token nextToken = peekNextToken();
            assert nextToken != null;
            String nextTokenValue = nextToken.getValue();
            return !nextTokenValue.equals("[") && !nextTokenValue.equals(".") && !nextTokenValue.equals("(");
        }
        return false;
    }

    private boolean isTermUnaryOp(Token token) {
        return token.getType().equals(TokenType.SYMBOL) && unaryOp.contains(token.getValue());
    }

    private Token peekNextToken() {
        if (tokenizer.hasMoreTokens()) {
            return tokenizer.peek();
        }
        return null;
    }

    private Token getNextToken() {
        return tokenizer.advance();
    }

    private List<Token> peekTwoTokensAhead() {
        return tokenizer.peekN(2);
    }

    // NOTE: Only call this if you know next token is a keyword
    private Keyword nextStatementTypeKeyword() {
        Token token = peekNextToken();
        switch (token.getValue()) {
            case "let": return Keyword.LET;
            case "if": return Keyword.IF;
            case "do": return Keyword.DO;
            case "while": return Keyword.WHILE;
            case "return": return Keyword.RETURN;
            default: throw new RuntimeException("Unexpected token value");
        }
    }
}
