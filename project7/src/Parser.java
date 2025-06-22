import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

public class Parser {

    private ListIterator<String> commandIterator;

    private List<String> arithmetic = Arrays.asList("add", "sub", "neg", "and", "or", "not", "eq", "lt", "gt");

    public Parser(String fileName) throws Exception {
        List<String> lines = Files.readAllLines(Path.of(fileName));
        lines = removeCommentsAndEmptyLines(lines);
        commandIterator = lines.listIterator();
    }

    public boolean hasMoreCommands() {
        return commandIterator.hasNext();
    }

    public Command nextCommand() {
        Command command = null;
        if (hasMoreCommands()) {
            String nextCommand = commandIterator.next();
            String[] parts = nextCommand.trim().split("\\s+");
            if (parts[0].equals("push")) {
                command = new Command(CommandType.C_PUSH, parts[1], Integer.valueOf(parts[2]));
            } else if (parts[0].equals("pop")) {
                command = new Command(CommandType.C_POP, parts[1], Integer.valueOf(parts[2]));
            } else if (arithmetic.contains(parts[0])) {
                command = new Command(CommandType.C_ARITHMETIC, parts[0]);
            }
        }
        return command;
    }

    public List<String> removeCommentsAndEmptyLines(List<String> lines) {
        List<String> cleanedLines = new ArrayList<>();
        for(String line: lines) {
            line = line.strip();
            if (line.startsWith("//") || line.isEmpty()) {
                continue;
            }
            cleanedLines.add(line);
        }
        return cleanedLines;
    }
}
