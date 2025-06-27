import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class JackAnalyzer {

    private String filePath;

    public JackAnalyzer(String filePath) {
        this.filePath = filePath;
    }

    public void analyze() throws Exception {
        Path path = new File(filePath).toPath();
        if (Files.isRegularFile(path)) {
            analyzeFile(filePath);
        } else if (Files.isDirectory(path)) {
            List<Path> allPaths = Files.walk(path).collect(Collectors.toUnmodifiableList());
            for (Path filePath: allPaths) {
                String jackFile = filePath.toFile().getCanonicalPath();
                if (jackFile.endsWith(".jack")) {
                    System.out.println("Jack file: " + jackFile);
                    analyzeFile(jackFile);
                }
            }
        }
    }

    private void analyzeFile(String filePath) {
        try {
            JackTokenizer tokenizer = new JackTokenizer(filePath);
            CompilationEngine compilationEngine = new CompilationEngine(tokenizer);
            String analyzeOutput = compilationEngine.compileClass();

            // extract file name
            String outputFileName = filePath.split("\\.")[0];
            outputFileName = outputFileName + ".xml";
            System.out.println("Output file name: " + outputFileName);
            writeAnalyzerOutput(outputFileName, analyzeOutput);
        } catch (Exception ex) {
            System.out.println("Exception occurred: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }

    }

    private void writeAnalyzerOutput(String outputFile, String analyzerXml) {
        try(FileWriter fw = new FileWriter(outputFile, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {
            out.println(analyzerXml);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        JackAnalyzer analyzer = new JackAnalyzer(args[0]);
        analyzer.analyze();
    }

}
