import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VMTranslator {

    private List<String> translatedAsm;

    public VMTranslator() throws Exception {
        this.translatedAsm = new ArrayList<>();
    }

    public List<String> getTranslatedAsm() {
        return this.translatedAsm;
    }

    public void translate(String fileName) throws Exception {
        boolean sysInit = false;
        Path path = new File(fileName).toPath();
        if (Files.isRegularFile(path)) {
            TranslatorMain translator = new TranslatorMain(fileName);
            translatedAsm.addAll(translator.translate());
        } else if (Files.isDirectory(path)) {
            sysInit = true;
            List<Path> allPaths = Files.walk(path).collect(Collectors.toUnmodifiableList());
            for (Path filePath: allPaths) {
                String vmFileName = filePath.toFile().getCanonicalPath();
                if (vmFileName.endsWith(".vm")) {
                    System.out.println("VM Filename: " + vmFileName);
                    TranslatorMain translator = new TranslatorMain(vmFileName);
                    translatedAsm.addAll(translator.translate());
                }
            }
        }

        // Bootstrap code
        if (sysInit) {
            List<String> bootstrapAsm = CodeWriter.writeBootstrapCode();
            TranslatorMain bootstrapTranslator = new TranslatorMain("Bootstrap", true);
            bootstrapAsm.addAll(bootstrapTranslator.translate());
            bootstrapAsm.addAll(translatedAsm);
            translatedAsm = bootstrapAsm;
        }
        flush(fileName);
    }

    public void flush(String fileName) {
        Path path = new File(fileName).toPath();
        String inputFileName = fileName.split("\\.")[0];
        String outputFile = null;
        if (Files.isRegularFile(path)) {
            outputFile = inputFileName + ".asm";
        } else {
            outputFile  = String.format("%s/%s.asm", inputFileName, inputFileName);
        }
        System.out.println("Output File: " + outputFile);
        try(FileWriter fw = new FileWriter(outputFile, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {
            for (String line: translatedAsm) {
                out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        String fileName = args[0];
        System.out.println("File name is " + fileName);

        VMTranslator translator = new VMTranslator();
        translator.translate(fileName);
    }
}
