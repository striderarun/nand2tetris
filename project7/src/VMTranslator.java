public class VMTranslator {

    private Parser parser;
    private CodeWriter codeWriter;

    public VMTranslator(String fileName) throws Exception {
        this.parser = new Parser(fileName);
        this.codeWriter = new CodeWriter(fileName);
    }

    public void translate() {
        while(this.parser.hasMoreCommands()) {
            Command command = this.parser.nextCommand();
            codeWriter.write(command);
        }
        codeWriter.flush();
    }
    public static void main(String[] args) throws Exception {
        String fileName = args[0];
        System.out.println("File name is " + fileName);
        VMTranslator translator = new VMTranslator(fileName);
        translator.translate();
    }
}
