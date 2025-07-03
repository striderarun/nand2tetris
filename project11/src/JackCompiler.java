import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class JackCompiler {

    private String filePath;

    public JackCompiler(String filePath) {
        this.filePath = filePath;
    }

    public void compile() throws Exception {
        Path path = new File(filePath).toPath();
        if (Files.isRegularFile(path)) {
            compileFile(filePath);
        } else if (Files.isDirectory(path)) {
            List<Path> allPaths = Files.walk(path).collect(Collectors.toUnmodifiableList());
            for (Path filePath: allPaths) {
                String jackFile = filePath.toFile().getCanonicalPath();
                if (jackFile.endsWith(".jack")) {
                    System.out.println("Jack file: " + jackFile);
                    compileFile(jackFile);
                }
            }
        }
    }

    private void compileFile(String filePath) {
        try {
            JackTokenizer tokenizer = new JackTokenizer(filePath);
            CompilationEngine compilationEngine = new CompilationEngine(tokenizer);
            String vmCode = compilationEngine.compileClass();

            // extract file name
            String outputFileName = filePath.split("\\.")[0];
            outputFileName = outputFileName + ".vm";
            System.out.println("Output file name: " + outputFileName);
            writeCompilerOutput(outputFileName, vmCode);
        } catch (Exception ex) {
            System.out.println("Exception occurred: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }

    }

    private void writeCompilerOutput(String outputFile, String compiledCode) {
        try(FileWriter fw = new FileWriter(outputFile, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {
            out.println(compiledCode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        JackCompiler compiler = new JackCompiler(args[0]);
        compiler.compile();
    }

}
