import commands.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class CommandParserTest {

    private CommandParser commandParser;
    private RBACSystem mockSystem;
    private TestCommand testCommand;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        commandParser = new CommandParser();
        mockSystem = new RBACSystem();
        testCommand = new TestCommand();

        originalOut = System.out;
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
    }

    @Nested
    @DisplayName("registerCommand and execute tests")
    class RegisterAndExecuteTests {

        @Test
        void registerCommandAddsCommandToParser() {
            commandParser.registerCommand("test", "Test command", testCommand);

            Scanner scanner = new Scanner(System.in);
            commandParser.executeCommand("test", scanner, mockSystem);

            assertTrue(testCommand.wasExecuted());
        }

        @Test
        void executeCommandWithUnknownCommandPrintsError() {
            commandParser.executeCommand("unknown", new Scanner(System.in), mockSystem);

            String output = outputStream.toString().trim();
            assertTrue(output.contains("Command \"unknown\" not found!"));
        }

        @Test
        void executeCommandWithExceptionHandlesError() {
            Command errorCommand = (scanner, system) -> {
                throw new RuntimeException("Test error");
            };

            commandParser.registerCommand("error", "Error command", errorCommand);
            commandParser.executeCommand("error", new Scanner(System.in), mockSystem);

            String output = outputStream.toString().trim();
            assertTrue(output.contains("Error while execute command: Test error"));
        }

        @Test
        void executeCommandWithNullCommandNameDoesNothing() {
            commandParser.executeCommand(null, new Scanner(System.in), mockSystem);

            String output = outputStream.toString().trim();
            assertTrue(output.contains("Command \"null\" not found!"));
        }
    }

    @Nested
    @DisplayName("printHelp method tests")
    class PrintHelpTests {

        @Test
        void printHelpWithNoCommandsPrintsEmptyTable() {
            commandParser.printHelp();

            String output = outputStream.toString();
            assertTrue(output.contains("Command"));
            assertTrue(output.contains("Description"));
        }

        @Test
        void printHelpWithRegisteredCommandsPrintsCommands() {
            commandParser.registerCommand("help", "Show help", null);
            commandParser.registerCommand("exit", "Exit program", null);

            commandParser.printHelp();

            String output = outputStream.toString();
            assertTrue(output.contains("help"));
            assertTrue(output.contains("Show help"));
            assertTrue(output.contains("exit"));
            assertTrue(output.contains("Exit program"));
        }
    }

    @Nested
    @DisplayName("parseAndExecute method tests")
    class ParseAndExecuteTests {

        @Test
        void parseAndExecuteWithValidCommandExecutesIt() {
            commandParser.registerCommand("test", "Test command", testCommand);

            Scanner scanner = new Scanner(new ByteArrayInputStream("".getBytes()));
            commandParser.parseAndExecute("test", scanner, mockSystem);

            assertTrue(testCommand.wasExecuted());
        }

        @Test
        void parseAndExecuteWithArgumentsExecutesCommand() {
            commandParser.registerCommand("test", "Test command", testCommand);

            Scanner scanner = new Scanner(new ByteArrayInputStream("".getBytes()));
            commandParser.parseAndExecute("test arg1 arg2", scanner, mockSystem);

            assertTrue(testCommand.wasExecuted());
        }

        @Test
        void parseAndExecuteWithUppercaseCommandExecutesIt() {
            commandParser.registerCommand("test", "Test command", testCommand);

            Scanner scanner = new Scanner(new ByteArrayInputStream("".getBytes()));
            commandParser.parseAndExecute("TEST", scanner, mockSystem);

            assertTrue(testCommand.wasExecuted());
        }

        @Test
        void parseAndExecuteWithUnknownCommandPrintsError() {
            Scanner scanner = new Scanner(new ByteArrayInputStream("".getBytes()));
            commandParser.parseAndExecute("unknown", scanner, mockSystem);

            String output = outputStream.toString().trim();
            assertTrue(output.contains("Command \"unknown\" not found!"));
        }

        @Test
        void parseAndExecuteWithEmptyInputThrowsException() {
            Scanner scanner = new Scanner(new ByteArrayInputStream("".getBytes()));

            assertThrows(IllegalArgumentException.class,
                    () -> commandParser.parseAndExecute("", scanner, mockSystem));
        }

        @Test
        void parseAndExecuteWithNullInputThrowsException() {
            Scanner scanner = new Scanner(new ByteArrayInputStream("".getBytes()));

            assertThrows(IllegalArgumentException.class,
                    () -> commandParser.parseAndExecute(null, scanner, mockSystem));
        }
    }

    private static class TestCommand implements Command {
        private boolean executed = false;

        @Override
        public void execute(Scanner scanner, RBACSystem system) {
            executed = true;
        }

        public boolean wasExecuted() {
            return executed;
        }
    }
}