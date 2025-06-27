import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class TokenizationXMLWriter {

    public static void main(String... args) {
        String fileName = args[0];
        String outputFile = fileName + ".T.xml";
        JackTokenizer tokenizer = new JackTokenizer(fileName);

        try(FileWriter fw = new FileWriter(outputFile, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {
            out.println("<tokens>");
            while(tokenizer.hasMoreTokens()) {
                Token token = tokenizer.advance();
                String xmlTag = String.format(token.getType().getXmlTag(), token.getValue());
                out.println(xmlTag);
            }
            out.println("</tokens>");
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
