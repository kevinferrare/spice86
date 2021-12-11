package spice86.emulator.function.dump;

import java.text.MessageFormat;

/**
 * Converts FunctionInformation to java stubs for easy override
 */
public class KotlinStubToStringConverter extends JvmFunctionToStringConverter {
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
            public class Stubs : JavaOverrideHelper '{'
              constructor(Map<SegmentedAddress, FunctionInformation> functionInformations, String prefix, Machine machine) '{'
                super(functionInformations, prefix, machine);
              '}'
            """,
        numberOfGlobals, segmentValues, globalsContent);
  }

  protected String generateClassForGlobalsOnCSWithValue(String segmentValueHex, String globalsContent) {
    return MessageFormat.format("""
        @SuppressWarnings("java:S100")
        public class GlobalsOnCsSegment{0} : MemoryBasedDataStructureWithBaseAddress '{'
          constructor(Machine machine) '{'
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
        public class GlobalsOn{1} : MemoryBasedDataStructureWith{1}BaseAddress '{'
          constructor(Machine machine) '{'
            super(machine);
          '}'

        {2}
        '}'
        """, segmentName, segmentNameCamel, globalsContent);
  }

  protected String generatePointerGetter(String comment, String javaName, String offset) {
    return MessageFormat.format("""
            var `{1}`: SegmentedAddress
              {0}
              get() = new SegmentedAddress(getUint16({2} + 2), getUint16({2}))
          """, comment, javaName, offset);
  }

  protected String generateNonPointerGetter(String comment, String javaName, String offset, int bits) {
      return MessageFormat.format("""
            var `{1}`: Int
              {0}
              get() = getUint{2}({3})
          """, comment, javaName, bits, offset);
  }

  protected String generatePointerSetter(String comment, String javaName, String offset) {
    return MessageFormat.format("""
              {0}
              set(value: SegmentedAddress) = '{'
                setUint16({2} + 2, value.getSegment())
                setUint16({2}, value.getOffset())
              '}'
          """, comment, javaName, offset);
  }

  protected String generateNonPointerSetter(String comment, String javaName, String offset, int bits) {
    return MessageFormat.format("""
              {0}
              set(value: Int) = setUint{2}({3}, value)
          """, comment, javaName, bits, offset);
  }

  protected String generateFunctionStub(String callsAsComments, String functionName, String functionNameInJava, String segment,
      String offset, String retType) {
    return MessageFormat.format("""
          // defineFunction({0}, {1}, "{2}") '{' {3}() '}'
          fun {3}(): Runnable '{'
          {4}
            return {5}Ret()
          '}'
        """,
        segment, offset, functionName, functionNameInJava,
        //
        callsAsComments.indent(2),
        //
        retType);
  }
}
