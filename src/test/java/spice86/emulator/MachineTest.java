package spice86.emulator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import spice86.emulator.cpu.Cpu;
import spice86.emulator.cpu.Flags;
import spice86.emulator.cpu.State;
import spice86.emulator.errors.InvalidOperationException;
import spice86.emulator.machine.Machine;
import spice86.emulator.memory.Memory;
import spice86.emulator.memory.MemoryUtils;
import spice86.utils.ConvertUtils;

/**
 * Runs Machine tests to check for CPU accuracy.<br/>
 * The tests themselves are binary files to be loaded at F000:0000. Execution starts at F000:FFF0.<br/>
 * The test is successful when the content of the emulated memory matches the res bin file corresponding to the
 * test.<br/>
 * In some cases we patch the expected values to match dosbox behaviour.<br/>
 * Test files themselves are originally from the Zet86 project:
 * https://github.com/marmolejo/zet/tree/master/cores/zet/tests<br/>
 * They were reworked, compiled and packaged by Artem Litvinovich:
 * https://twitter.com/theartlav/status/1299816147165081606
 */
public class MachineTest {

  @Test
  public void testAdd() throws Exception {
    testOneBin("add");
  }

  @Test
  public void testBcdcnv() throws Exception {
    testOneBin("bcdcnv");
  }

  @Test
  public void testBitwise() throws Exception {
    byte[] expected =
        this.getClass().getClassLoader().getResourceAsStream("cpuTests/res/" + "bitwise" + ".bin").readAllBytes();
    // dosbox values
    expected[0x9F] = 0x12;
    expected[0x9D] = 0x12;
    expected[0x9B] = 0x12;
    expected[0x99] = 0x12;

    testOneBin("bitwise", expected);

  }

  @Test
  public void testCmpneg() throws Exception {
    testOneBin("cmpneg");
  }

  @Test
  public void testControl() throws Exception {
    byte[] expected =
        this.getClass().getClassLoader().getResourceAsStream("cpuTests/res/" + "control" + ".bin").readAllBytes();
    // dosbox values
    expected[0x1] = 0x78;
    testOneBin("control", expected);
  }

  @Test
  public void testDatatrnf() throws Exception {
    testOneBin("datatrnf");
  }

  @Test
  public void testDiv() throws Exception {
    testOneBin("div");
  }

  @Test
  public void testInterrupt() throws Exception {
    testOneBin("interrupt");
  }

  @Test
  public void testJump1() throws Exception {
    testOneBin("jump1");
  }

  @Test
  public void testJump2() throws Exception {
    testOneBin("jump2");
  }

  @Test
  public void testJmpmov() throws Exception {
    // 0x4001 in little endian
    byte[] expected = new byte[] { 0x01, 0x40 };
    Machine emulator = testOneBin("jmpmov", expected);
    State state = emulator.getCpu().getState();
    int endAddress = MemoryUtils.toPhysicalAddress(state.getCS(), state.getIP());
    // Last instruction HLT is one byte long and is at 0xF400C
    assertEquals(0xF400D, endAddress);
  }

  @Test
  public void testMul() throws Exception {
    byte[] expected =
        this.getClass().getClassLoader().getResourceAsStream("cpuTests/res/" + "mul" + ".bin").readAllBytes();
    // dosbox values
    expected[0xA2] = 0x2;
    expected[0x9E] = 0x2;
    expected[0x9C] = 0x3;
    expected[0x9A] = 0x3;
    expected[0x98] = 0x2;
    expected[0x96] = 0x2;
    expected[0x92] = 0x2;
    expected[0x73] = 0x2;
    testOneBin("mul", expected);
  }

  @Test
  public void testRep() throws Exception {
    testOneBin("rep");
  }

  @Test
  public void testRotate() throws Exception {
    testOneBin("rotate");
  }

  @Test
  public void testSegpr() throws Exception {
    testOneBin("segpr");
  }

  @Test
  public void testShifts() throws Exception {
    testOneBin("shifts");
  }

  @Test
  public void testStrings() throws Exception {
    testOneBin("strings");
  }

  @Test
  public void testSub() throws Exception {
    testOneBin("sub");
  }

  private Machine testOneBin(String binName) throws Exception {
    byte[] expected =
        this.getClass().getClassLoader().getResourceAsStream("cpuTests/res/" + binName + ".bin").readAllBytes();
    return this.testOneBin(binName, expected);
  }

  private Machine testOneBin(String binName, byte[] expected) throws Exception {
    Machine machine = execute(binName);
    Memory memory = machine.getMemory();
    compareMemoryWithExpected(memory, expected, 0, expected.length - 1);
    return machine;
  }

  private Machine execute(String binName) throws InvalidOperationException, IOException, URISyntaxException {
    Configuration configuration = new Configuration();
    // making sure int8 is not going to be triggered during the tests
    configuration.setInstructionsPerSecond(10000000);
    configuration.setExe(getBinPath(binName));
    try (ProgramExecutor programExecutor = new ProgramExecutor(null, configuration)) {
      Machine machine = programExecutor.getMachine();
      Cpu cpu = machine.getCpu();
      // Disabling custom IO handling
      cpu.setIoPortDispatcher(null);
      cpu.setErrorOnUninitializedInterruptHandler(false);
      State state = cpu.getState();
      state.getFlags().setDosboxCompatibility(false);
      programExecutor.run();
      return machine;
    }
  }

  private String getBinPath(String binName) throws URISyntaxException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    URL binUrl = classLoader.getResource("cpuTests/" + binName + ".bin");
    return new File(binUrl.toURI()).getAbsolutePath();
  }

  private void compareMemoryWithExpected(Memory memory, byte[] expected, int start, int end) {
    byte[] actual = memory.getRam();
    for (int i = 0; i < end; i++) {
      byte actualByte = actual[i];
      byte expectedByte = expected[i];
      if (actualByte != expectedByte) {
        int wordIndex = i;
        if (wordIndex % 2 == 1) {
          wordIndex--;
        }
        int actualWord = MemoryUtils.getUint16(actual, wordIndex);
        int expectedWord = MemoryUtils.getUint16(expected, wordIndex);
        Assertions.fail("Byte value differs at " + createMessageB(i, expectedByte, actualByte) + ". If words, "
            + createMessageW(wordIndex, expectedWord, actualWord));
      }
    }
  }

  private String createMessageB(int address, int expected, int actual) {
    return "address " + ConvertUtils.toHex(address) + " Expected " + hexValueWithFlagsB(expected) + " but got "
        + hexValueWithFlagsB(actual);
  }

  private String hexValueWithFlagsB(int value) {
    return ConvertUtils.toHex8(value) + " (if flags=" + Flags.dumpFlags(value) + ")";
  }

  private String createMessageW(int address, int expected, int actual) {
    return "address " + ConvertUtils.toHex(address) + " Expected " + hexValueWithFlagsW(expected) + " but got "
        + hexValueWithFlagsW(actual);
  }

  private String hexValueWithFlagsW(int value) {
    return ConvertUtils.toHex16(value) + " (if flags=" + Flags.dumpFlags(value) + ")";
  }
}
