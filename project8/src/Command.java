public class Command {

    private CommandType commandType;
    private String arg1;
    private int arg2 = -999;

    public Command(CommandType commandType, String arg1, int arg2) {
        this(commandType, arg1);
        this.arg2 = arg2;
    }

    public Command(CommandType commandType, String arg1) {
        this.commandType = commandType;
        this.arg1 = arg1;
    }

    public Command(CommandType commandType) {
        this.commandType = commandType;
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public String getArg1() {
        return arg1;
    }

    public int getArg2() {
        return arg2;
    }
}
