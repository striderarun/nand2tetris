import java.util.ArrayList;
import java.util.List;

public class TranslatorMain {

    private Parser parser;
    private CodeWriter codeWriter;

    public TranslatorMain(String fileName) throws Exception {
        this.parser = new Parser(fileName);
        this.codeWriter = new CodeWriter(fileName);
    }

    public TranslatorMain(String fileName, boolean bootstrap) throws Exception {
        this.parser = new Parser(fileName, bootstrap);
        this.codeWriter = new CodeWriter(fileName);
    }

    public List<String> translate() {
        while(this.parser.hasMoreCommands()) {
            Command command = this.parser.nextCommand();
            codeWriter.write(command);
        }
        return codeWriter.getTranslatedAsm();
    }

}
