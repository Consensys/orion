package net.consensys.orion.impl.cmd;

import static org.junit.Assert.*;

import net.consensys.orion.api.cmd.Orion;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OrionArgumentsTest {

  private final String usageOut =
      String.format(
          "Usage: "
              + Orion.name
              + " [options] [config file]%n"
              + "where options include:%n"
              + "\t-g%n"
              + "\t--generatekeys <names>%n\t\tgenerate key pairs for each of the names supplied.%n\t\twhere <names> are a comma-seperated list%n"
              + "\t-h%n"
              + "\t--help\tprint this help message%n"
              + "\t-v%n"
              + "\t--version\tprint version information%n");

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private PrintStream originalSystemOut;

  @Before
  public void setupOutputStream() {
    originalSystemOut = System.out;
    System.setOut(new PrintStream(outContent));
  }

  @After
  public void cleanUpOutputStream() {
    System.setOut(originalSystemOut);
  }

  @Test
  public void generateKeysArgumentWithNoKeyNamesProvided() {
    String errorMsg = String.format("Error: Missing key names to generate.%n");
    String[] args = {"-g"};

    OrionArguments arguments = new OrionArguments(args);

    assertEquals(errorMsg + usageOut, outContent.toString());
    assertTrue(arguments.argumentExit());
  }

  @Test
  public void helpOutput() {
    String[] args = {"--help"};

    OrionArguments arguments = new OrionArguments(args);

    assertEquals(usageOut, outContent.toString());
    assertTrue(arguments.argumentExit());
  }

  @Test
  public void versionArgument() {
    String[] args = {"-v"};

    OrionArguments arguments = new OrionArguments(args);

    assertTrue(arguments.displayVersion());
  }

  @Test
  public void invalidOption() {
    String errorMsg = "Invalid option: -x\n";
    String[] args = {"-x"};

    OrionArguments arguments = new OrionArguments(args);

    assertEquals(errorMsg + usageOut, outContent.toString());
    assertTrue(arguments.argumentExit());
  }

  @Test
  public void validAndInvalidOptions() {
    String errorMsg = "Invalid option: -x\n";
    String[] args = {"-x", "-g", "keys"};

    OrionArguments arguments = new OrionArguments(args);

    assertEquals(errorMsg + usageOut, outContent.toString());
    assertTrue(arguments.argumentExit());
  }

  @Test
  public void configFileParam() {
    String[] args = {"config.conf"};

    OrionArguments arguments = new OrionArguments(args);
    assertTrue(arguments.configFileName().isPresent());
    assertEquals("config.conf", arguments.configFileName().get());
  }
}
