package spice86.emulator.function.dump;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;

import spice86.emulator.cpu.SegmentRegisters;
import spice86.emulator.function.AddressOperation;
import spice86.emulator.function.CallType;
import spice86.emulator.function.FunctionInformation;
import spice86.emulator.function.FunctionReturn;
import spice86.emulator.function.OperandSize;
import spice86.emulator.function.SegmentRegisterBasedAddress;
import spice86.emulator.function.ValueOperation;
import spice86.emulator.memory.SegmentedAddress;
import spice86.utils.ConvertUtils;

/**
 * Converts FunctionInformation to java stubs for easy override
 */
public class JavaStubToStringConverter extends JvmFunctionToStringConverter {
  @Override
  protected String generateFileHeaderWithAccessors(int numberOfGlobals, String globalsContent, String segmentValues) {
    return MessageFormat.format(
        """
            import java.util.Map;

            import spice86.emulator.function.FunctionInformation;
            import spice86.emulator.machine.Machine;
            import spice86.emulator.memory.SegmentedAddress;
            import spice86.emulator.reverseengineer.JavaOverrideHelper;
            import spice86.emulator.reverseengineer.MemoryBasedDataStructureWithCsBaseAddress;
            import spice86.emulator.reverseengineer.MemoryBasedDataStructureWithDsBaseAddress;
            import spice86.emulator.reverseengineer.MemoryBasedDataStructureWithEsBaseAddress;
            import spice86.emulator.reverseengineer.MemoryBasedDataStructureWithSsBaseAddress;

            /*
             * Getters and setters for what could be global variables, split per segment register. {0} addresses in total.
             * Observed values for segments:
            {1}
             */
            {2}
            // Stubs for overrides
            @SuppressWarnings("java:S100")
            public class Stubs extends JavaOverrideHelper '{'
              public Stubs(Map<SegmentedAddress, FunctionInformation> functionInformations, String prefix, Machine machine) '{'
                super(functionInformations, prefix, machine);
              '}'
            """,
        numberOfGlobals, segmentValues, globalsContent);
  }

  protected String generateClassForGlobalsOnCSWithValue(String segmentValueHex, String globalsContent) {
    return MessageFormat.format("""
        @SuppressWarnings("java:S100")
        public class GlobalsOnCsSegment{0} extends MemoryBasedDataStructureWithBaseAddress '{'
          public GlobalsOnCsSegment{0}(Machine machine) '{'
            super(machine.getMemory(), {0} * 0x10);
          '}'

        {1}
        '}'
        """, segmentValueHex, globalsContent);
  }

  protected String generateClassForGlobalsOnSegment(String segmentName, String segmentNameCamel, String globalsContent) {
    return MessageFormat.format("""
        // Accessors for values accessed via register {0}
        @SuppressWarnings("java:S100")
        public class GlobalsOn{1} extends MemoryBasedDataStructureWith{1}BaseAddress '{'
          public GlobalsOn{1}(Machine machine) '{'
            super(machine);
          '}'

        {2}
        '}'

        """, segmentName, segmentNameCamel, globalsContent);
  }

  protected String generatePointerGetter(String comment, String javaName, String offset) {
    return MessageFormat.format("""
          {0}
          public SegmentedAddress getPtr{1}() '{'
            return new SegmentedAddress(getUint16({2} + 2), getUint16({2}));
          '}'
        """, comment, javaName, offset);
  }

  protected String generateNonPointerGetter(String comment, String javaName, String offset, int bits) {
    return MessageFormat.format("""
          {0}
          public int get{1}() '{'
            return getUint{2}({3});
          '}'
        """, comment, javaName, bits, offset);
  }

  protected String generatePointerSetter(String comment, String javaName, String offset) {
    return MessageFormat.format("""
          {0}
          public void setPtr{1}(SegmentedAddress value) '{'
            setUint16({2} + 2, value.getSegment());
            setUint16({2}, value.getOffset());
          '}'
        """, comment, javaName, offset);
  }

  protected String generateNonPointerSetter(String comment, String javaName, String offset, int bits) {
    return MessageFormat.format("""
          {0}
          public void set{1}(int value) '{'
            setUint{2}({3}, value);
          '}'
        """, comment, javaName, bits, offset);
  }

  protected String generateFunctionStub(String callsAsComments, String functionName, String functionNameInJava, String segment,
      String offset, String retType) {
    return MessageFormat.format("""
          // defineFunction({0}, {1}, "{2}", this::{3});
          public Runnable {3}() '{'
          {4}
            return {5}Ret();
          '}'
        """,
        segment, offset, functionName, functionNameInJava,
        //
        callsAsComments.indent(2),
        //
        retType);
  }
}
