import commands.CommandParser;
import commands.CommandRegistry;
import commands.RBACSystem;
import entities.*;
import utils.DateUtils;

void main() {
    RBACSystem rbacSystem = new RBACSystem();
    rbacSystem.initialize();
    rbacSystem.setCurrentUser("admin");

    CommandParser commandParser = new CommandParser();
    CommandRegistry.setupCommands(commandParser);

    Scanner scanner = new Scanner(System.in);
    while (true) {
        System.out.println("\n[RBAC]>");
        String input = scanner.nextLine();

        if (input.isBlank())
            continue;

        commandParser.parseAndExecute(input, scanner, rbacSystem);
    }
}
