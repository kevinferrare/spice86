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
public class JavaStubToStringConverter extends FunctionInformationToStringConverter {
  private static final SegmentRegisters SEGMENT_REGISTERS = new SegmentRegisters();

  @Override
  public String getFileHeader(Collection<SegmentRegisterBasedAddress> allPotentialGlobals,
      Set<SegmentedAddress> whiteListOfSegmentForOffset) {
    // Take only addresses which have been accessed (and not only computed)
    List<SegmentRegisterBasedAddress> globals = allPotentialGlobals.stream()
        .filter(a -> MapUtils.isNotEmpty(a.getAddressOperations()))
        // Filter out the addresses that have this offset but not the segment defined in whiteListOfSegmentForOffset
        .filter(a1 -> whiteListOfSegmentForOffset.stream().noneMatch(a2 -> isOffsetEqualsAndSegmentDifferent(a1, a2)))
        .toList();
    int numberOfGlobals = globals.size();
    // Various classes with values per segment
    String globalsContent = joinNewLine(mapBySegment(globals).entrySet()
        .stream()
        .map(e -> generateClassForGlobalsOnSegment(e.getKey(), e.getValue())));
    String segmentValues = joinNewLine(getValuesTakenBySegments(globals).entrySet()
        .stream()
        .map(e -> getStringSegmentValuesForDisplay(e.getKey(), e.getValue())));
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

  private boolean isOffsetEqualsAndSegmentDifferent(SegmentedAddress address1, SegmentedAddress address2) {
    return address1.getSegment() != address2.getSegment() && address1.getOffset() == address2.getOffset();
  }

  private String getStringSegmentValuesForDisplay(int segmentIndex, Set<Integer> values) {
    String segmentName = SEGMENT_REGISTERS.getRegName(segmentIndex);
    String segmentValues = values.stream().map(ConvertUtils::toHex16).collect(Collectors.joining(","));
    return segmentName + ":" + segmentValues;
  }

  private Map<Integer, Set<Integer>> getValuesTakenBySegments(Collection<SegmentRegisterBasedAddress> globals) {
    return mapBySegment(globals).entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey,
            e -> getSegmentValues(e.getValue())));
  }

  private Map<Integer, List<SegmentRegisterBasedAddress>> getAddressesBySegmentValues(
      Collection<SegmentRegisterBasedAddress> globals) {
    return globals
        .stream()
        .collect(Collectors.groupingBy(SegmentedAddress::getSegment));
  }

  private Set<Integer> getSegmentValues(Collection<SegmentRegisterBasedAddress> globals) {
    return globals.stream().map(SegmentedAddress::getSegment).collect(Collectors.toSet());
  }

  private Map<Integer, Set<SegmentRegisterBasedAddress>> mapBySegment(Collection<SegmentRegisterBasedAddress> globals) {
    Map<Integer, Set<SegmentRegisterBasedAddress>> res = new HashMap<>();
    for (SegmentRegisterBasedAddress address : globals) {
      address.getAddressOperations()
          .values()
          .stream()
          .flatMap(Collection::stream)
          .forEach(segmentIndex -> res.computeIfAbsent(segmentIndex, i -> new HashSet<>()).add(address));
    }
    return res;
  }

  private String generateClassForGlobalsOnCS(Collection<SegmentRegisterBasedAddress> globals) {
    // CS is special, program cannot explicitly change it in the emulator, and it doesn't usually change in the
    // overrides when it should.
    return joinNewLine(getAddressesBySegmentValues(globals).entrySet()
        .stream()
        .map(e -> generateClassForGlobalsOnCSWithValue(e.getKey(), e.getValue())));
  }

  private String generateClassForGlobalsOnCSWithValue(int segmentValue,
      Collection<SegmentRegisterBasedAddress> globals) {
    String segmentValueHex = ConvertUtils.toHex16(segmentValue);
    String globalsContent = generateGettersSettersForAddresses(globals);
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

  private String generateClassForGlobalsOnSegment(int segmentIndex, Collection<SegmentRegisterBasedAddress> globals) {
    if (SegmentRegisters.CS_INDEX == segmentIndex) {
      return generateClassForGlobalsOnCS(globals);
    }
    String segmentName = SEGMENT_REGISTERS.getRegName(segmentIndex);
    String segmentNameCamel = WordUtils.capitalizeFully(segmentName);
    // Generate accessors
    String globalsContent = generateGettersSettersForAddresses(globals);
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

  private String generateGettersSettersForAddresses(Collection<SegmentRegisterBasedAddress> addresses) {
    return joinNewLine(addresses.stream().sorted().map(this::generateGetterSetterForAddress));
  }

  private String generateGetterSetterForAddress(SegmentRegisterBasedAddress address) {
    Map<AddressOperation, Set<Integer>> addressOperations = address.getAddressOperations();
    if (addressOperations.isEmpty()) {
      // Nothing was ever read or written there
      return "";
    }
    // TreeMap so already sorted
    String gettersAndSetters = joinNewLine(new TreeMap<>(completeWithOppositeOperations(addressOperations)).entrySet()
        .stream()
        .map(entry -> generateAddressOperationAsGetterOrSetter(entry.getKey(), entry.getValue(), address)));
    return MessageFormat.format("""
          // Getters and Setters for address {0}.
        {1}""", address, gettersAndSetters);
  }

  private Map<AddressOperation, Set<Integer>>
      completeWithOppositeOperations(Map<AddressOperation, Set<Integer>> addressOperations) {
    // Ensures that for each read there is a write, even with empty registers so that we can generate valid java
    // properties
    Map<AddressOperation, Set<Integer>> res = new HashMap<>(addressOperations);
    for (Map.Entry<AddressOperation, Set<Integer>> entry : addressOperations.entrySet()) {
      AddressOperation operation = entry.getKey();
      ValueOperation opposite = operation.getValueOperation().oppositeOperation();
      AddressOperation toSearch = new AddressOperation(opposite, operation.getOperandSize());
      res.computeIfAbsent(toSearch, k -> new HashSet<>());
    }
    return res;
  }

  private String generateAddressOperationAsGetterOrSetter(AddressOperation addressOperation,
      Set<Integer> registerIndexes, SegmentRegisterBasedAddress address) {
    String comment = "// Operation not registered by running code";
    if (!registerIndexes.isEmpty()) {
      String registers = registerIndexes
          .stream()
          .map(SEGMENT_REGISTERS::getRegName)
          .sorted()
          .collect(Collectors.joining(", "));
      comment = "// Was accessed via the following registers: " + registers;
    }
    OperandSize operandSize = addressOperation.getOperandSize();

    String javaName = ConvertUtils.toJavaString(address) + "_" + operandSize.getName();
    String name = address.getName();
    if (StringUtils.isNotEmpty(name)) {
      javaName += "_" + name;
    }
    String offset = ConvertUtils.toHex16(address.getOffset());
    if (ValueOperation.READ.equals(addressOperation.getValueOperation())) {
      return generateGetter(comment, operandSize, javaName, offset);
    }

    // WRITE
    return generateSetter(comment, operandSize, javaName, offset);
  }

  private String generateGetter(String comment, OperandSize operandSize, String javaName, String offset) {
    int bits = operandSize.getBits();
    String res = MessageFormat.format("""
          {0}
          public int get{1}() '{'
            return getUint{2}({3});
          '}'
        """, comment, javaName, bits, offset);
    if (operandSize == OperandSize.DWORD32) {
      // segmented address
      res += MessageFormat.format("""
            {0}
            public SegmentedAddress getPtr{1}() '{'
              return new SegmentedAddress(getUint16({2} + 2), getUint16({2}));
            '}'
          """, comment, javaName, offset);
    }
    return res;
  }

  private String generateSetter(String comment, OperandSize operandSize, String javaName, String offset) {
    int bits = operandSize.getBits();
    String res = MessageFormat.format("""
          {0}
          public void set{1}(int value) '{'
            setUint{2}({3}, value);
          '}'
        """, comment, javaName, bits, offset);
    if (operandSize == OperandSize.DWORD32) {
      // segmented address
      res += MessageFormat.format("""
            {0}
            public void setPtr{1}(SegmentedAddress value) '{'
              setUint16({2} + 2, value.getSegment());
              setUint16({2}, value.getOffset());
            '}'
          """, comment, javaName, offset);
    }
    return res;
  }

  @Override
  public String getFileFooter() {
    return "}";
  }

  @Override
  public String convert(FunctionInformation functionInformation, Set<FunctionInformation> allFunctions) {
    if (functionInformation.hasOverride()) {
      return getNoStubReasonCommentForMethod(functionInformation, "Function already has an override");
    }
    List<CallType> returnTypes =
        Stream
            .concat(functionInformation.getReturns().keySet().stream(),
                functionInformation.getUnalignedReturns().keySet().stream())
            .map(FunctionReturn::getReturnCallType)
            .distinct()
            .toList();
    if (returnTypes.size() != 1) {
      // Cannot generate code with either no return or mixed returns
      String reason = "Function has no return";
      if (!returnTypes.isEmpty()) {
        reason = "Function has different return types: " + returnTypes;
      }
      return getNoStubReasonCommentForMethod(functionInformation, reason);
    }
    List<FunctionInformation> calls = this.getCalls(functionInformation, allFunctions);
    String callsAsComments = this.getCallsAsComments(calls);
    CallType returnType = returnTypes.get(0);
    String functionName = removeDotsFromFunctionName(functionInformation.getName());
    SegmentedAddress functionAddress = functionInformation.getAddress();
    String functionNameInJava = toJavaName(functionInformation, false);
    String segment = ConvertUtils.toHex16(functionAddress.getSegment());
    String offset = ConvertUtils.toHex16(functionAddress.getOffset());
    String retType = returnType.name().toLowerCase();
    return MessageFormat.format("""
        // defineFunction({0}, {1}, "{2}", this::{3});
        public Runnable {3}() '{'
        {4}
          return {5}Ret();
        '}'
        """,
        segment, offset, functionName, functionNameInJava,
        //
        callsAsComments,
        //
        retType);
  }

  private String getCallsAsComments(List<FunctionInformation> calls) {
    return joinNewLine(calls.stream().map(f -> "  // " + toJavaName(f, true) + "();"));
  }

  private String getNoStubReasonCommentForMethod(FunctionInformation functionInformation, String reason) {
    return "// Not providing stub for " + functionInformation.getName() + ". Reason: " + reason + '\n';
  }
}
