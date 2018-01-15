package net.consensys.athena.impl.cmd;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AthenaArgumentsTest {

  private final String usageOut =
      "Usage:  java -jar athena.jar [options] [config file]\n"
          + "where options include:\n"
          + "\t-g\n"
          + "\t--generatekeys <names>\n\t\tgenerate key pairs for each of the names supplied.\n\t\twhere <names> are a comma-seperated list\n"
          + "\t-h\n"
          + "\t--help\tprint this help message\n";

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

  @Before
  public void setupOutputStream() {
    System.setOut(new PrintStream(outContent));
  }

  @After
  public void cleanUpOutputStream() {
    System.setOut(null);
  }

  @Test
  public void testGenerateKeysArgumentWithNoKeyNamesProvided() {
    String errorMsg = "Error: Missing key names to generate.\n";
    String[] args = {"-g"};

    AthenaArguments arguments = new AthenaArguments(args);

    assertEquals(errorMsg + usageOut, outContent.toString());
    assertTrue(arguments.argumentExit());
  }

  @Test
  public void testHelpOutput() {
    String[] args = {"--help"};

    AthenaArguments arguments = new AthenaArguments(args);

    assertEquals(usageOut, outContent.toString());
    assertTrue(arguments.argumentExit());
  }

  @Test
  public void testInvalidOption() {
    String errorMsg = "Invalid option: -x\n";
    String[] args = {"-x"};

    AthenaArguments arguments = new AthenaArguments(args);

    assertEquals(errorMsg + usageOut, outContent.toString());
    assertTrue(arguments.argumentExit());
  }

  @Test
  public void testValidAndInvalidOptions() {
    String errorMsg = "Invalid option: -x\n";
    String[] args = {"-x", "-g", "keys"};

    AthenaArguments arguments = new AthenaArguments(args);

    assertEquals(errorMsg + usageOut, outContent.toString());
    assertTrue(arguments.argumentExit());
  }

  @Test
  public void testConfigFileParam() {
    String[] args = {"config.conf"};

    AthenaArguments arguments = new AthenaArguments(args);
    assertTrue(arguments.configFileName().isPresent());
    assertEquals("config.conf", arguments.configFileName().get());
  }
}
