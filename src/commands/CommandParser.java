package commands;

import utils.FormatUtils;
import utils.ValidationUtils;

import java.util.*;

public class CommandParser {
    private final Map<String, Command> commands = new HashMap<>();
    private final Map<String, String> commandDescriptions = new LinkedHashMap<>();

    public void registerCommand(String name, String description, Command command) {
        commands.put(name, command);
        commandDescriptions.put(name, description);
    }

    public void executeCommand(String commandName, Scanner scanner, RBACSystem system) {
        Command command = commands.get(commandName);

        if (command == null){
            System.out.println((String.format("Command \"%s\" not found!", commandName)));
            return;
        }

        try {
            command.execute(scanner, system);
        } catch (Exception e) {
            System.out.println("Error while execute command: " + e.getMessage());
        }
    }

    public void printHelp() {
        String[] headers = {"Command", "Description"};
        ArrayList<String[]> rows = new ArrayList<>();

        for (Map.Entry<String, String> entry : commandDescriptions.entrySet()) {
            String command = entry.getKey();
            String description = entry.getValue();

            rows.add(new String[]{command, description});
        }

        System.out.println(FormatUtils.formatTable(headers, rows));
    }

    public void parseAndExecute(String input, Scanner scanner, RBACSystem system) {
        ValidationUtils.requireNonEmpty(input, "input");

        String[] commandAndArguments = input.trim().split("\\s+", 2);
        String command = commandAndArguments[0].toLowerCase();

        executeCommand(command, scanner, system);
    }
}
