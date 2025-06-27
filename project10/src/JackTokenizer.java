import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

/**
 * Tokenize the Jack source code
 * Assumptions:
 *   - A Jack program cannot start with a symbol
 */
public class JackTokenizer {

    public List<String> symbols = Arrays.asList(
            "{", "}", "(", ")", "[", "]", ".", ",", ";", "+", "-", "*", "/", "&", "|", "<", ">", "=", "~"
    );

    private List<Token> tokens;
    private ListIterator<Token> tokenIterator;


    public JackTokenizer(String fileName) {
        this.tokens = new ArrayList<>();
        tokenizeProgram(fileName);
        this.tokenIterator = this.tokens.listIterator();
    }

    private void tokenizeProgram(String fileName) {
        try {
            List<String> lines = Files.readAllLines(Path.of(fileName));
            lines = removeCommentsAndEmptyLines(lines);
            lines = removeInlineComments(lines);
            for (String line: lines) {
                tokenizeLine(line);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void tokenizeLine(String line) {
        StringBuilder sb = new StringBuilder();
        for (Character ch: line.toCharArray()) {
            String currentChar = String.valueOf(ch);
            tokenizeSymbolsAndKeywords(sb);

            // Identify if it's time to flush an identifier or integer constant
            // Current characters that are part of a string constant with spaces will not enter here
            if (isFlushRequired(currentChar, sb)) {
                String currentToken = sb.toString().trim();
                // Check if we have an integer constant
                if (isNumber(currentToken)) {
                    Token token = new Token(TokenType.INT_CONST, currentToken);
                    tokens.add(token);
                    sb.setLength(0);
                } else {
                    // If there is an identifier, sb will be non-empty
                    Token token = new Token(TokenType.IDENTIFIER, currentToken);
                    tokens.add(token);
                    sb.setLength(0);
                }
            }

            // Append the next character to the sb
            sb.append(currentChar);
        }

        // Tokenize last token in sb
        tokenizeSymbolsAndKeywords(sb);
    }

    private void tokenizeSymbolsAndKeywords(StringBuilder sb) {
        String currentToken = sb.toString().trim();
        // Check if we have a symbol
        if (symbols.contains(currentToken)) {
            Token token = new Token(TokenType.SYMBOL, symbolEncoding(currentToken));
            tokens.add(token);
            sb.setLength(0);
        }
        // Check if we have a keyword
        if (isKeyword(currentToken)) {
            Token token = new Token(TokenType.KEYWORD, currentToken);
            tokens.add(token);
            sb.setLength(0);
        }

        if (currentToken.length()> 1 && currentToken.startsWith("\"") && currentToken.endsWith("\"")) {
            String stringConstant = currentToken.substring(1, currentToken.length()-1);
            Token token = new Token(TokenType.STRING_CONST, stringConstant);
            tokens.add(token);
            sb.setLength(0);
        }
    }

    private String symbolEncoding(String symbol) {
        switch(symbol) {
            case ">":
                return "&gt;";
            case "<":
                return "&lt;";
            case "&":
                return "&amp;";
            case "\"":
                return "&quot;";
            default:
                return symbol;
        }
    }

    private boolean isNumber(String number) {
        try {
            Integer.parseInt(number);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    private boolean isKeyword(String currentToken) {
        for(Keyword keyword: Keyword.values()) {
            if (keyword.getKeyword().equalsIgnoreCase(currentToken)) {
                return true;
            }
        }
        return false;
    }

    public boolean isFlushRequired(String currentChar, StringBuilder sb) {
        String currentToken = sb.toString().trim();
        // If the value in string builder starts with ", then we are still tokenizing a string constant
        if (currentToken.startsWith("\"")) {
            return false;
        } else if ((symbols.contains(currentChar) || currentChar.equals(" ")) && currentToken.length() > 0 ) {
            return true;
        }
        return false;
    }

    public List<String> removeCommentsAndEmptyLines(List<String> lines) {
        List<String> cleanedLines = new ArrayList<>();
        for(String line: lines) {
            line = line.strip();
            if (line.startsWith("//") || line.startsWith("/**") || line.startsWith("*") ||  line.isEmpty()) {
                continue;
            }
            cleanedLines.add(line);
        }
        return cleanedLines;
    }

    private List<String> removeInlineComments(List<String> lines) {
        List<String> cleanedLines = new ArrayList<>();
        for(String line: lines) {
            if (line.contains("//")) {
                line = line.substring(0, line.indexOf("//"));
                line = line.strip();
            }
            cleanedLines.add(line);
        }
        return cleanedLines;
    }

    public boolean hasMoreTokens() {
        return tokenIterator.hasNext();
    }

    public Token advance() {
        return tokenIterator.next();
    }

    public Token peek() {
        Token nextToken = tokenIterator.next();
        tokenIterator.previous();
        return nextToken;
    }

    public List<Token> peekN(int N) {
        List<Token> lookaheadTokens = new ArrayList<>();
        for (int i=0;i<N;i++) {
            lookaheadTokens.add(tokenIterator.next());
        }
        for (int i=0;i<N;i++) {
            tokenIterator.previous();
        }
        return lookaheadTokens;
    }

    public static void main(String... args) {
        String fileName = args[0];
        JackTokenizer tokenizer = new JackTokenizer(fileName);
        while(tokenizer.hasMoreTokens()) {
            System.out.println(tokenizer.advance());
        }
    }

}
