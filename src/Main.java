import commands.CommandParser;
import commands.CommandRegistry;
import commands.RBACSystem;
import java.util.Scanner;

void main() {
    RBACSystem rbacSystem = new RBACSystem();
    rbacSystem.initialize();
    rbacSystem.setCurrentUser("admin");

    CommandParser commandParser = new CommandParser();
    CommandRegistry.setupCommands(commandParser);

    Scanner scanner = new Scanner(System.in);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        System.out.println("\nShutdown signal received. Cleaning up...");
        rbacSystem.shutdown();
    }));

    while (true) {
        System.out.print("\n[RBAC]> ");
        String input = scanner.nextLine();

        if (input.isBlank()) continue;

        if (input.equalsIgnoreCase("exit")) {
            rbacSystem.shutdown();
            System.out.println("Goodbye!");
            break;
        }

        commandParser.parseAndExecute(input, scanner, rbacSystem);
    }

    scanner.close();
}