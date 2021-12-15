package spice86.emulator.function.dump;

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

public abstract class JvmFunctionToStringConverter extends FunctionInformationToStringConverter {
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
    return generateFileHeaderWithAccessors(numberOfGlobals, globalsContent, segmentValues);
  }

  protected abstract String generateFileHeaderWithAccessors(int numberOfGlobals, String globalsContent,
      String segmentValues);

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
    return generateClassForGlobalsOnCSWithValue(segmentValueHex, globalsContent);
  }

  protected abstract String generateClassForGlobalsOnCSWithValue(String segmentValueHex, String globalsContent);

  private String generateClassForGlobalsOnSegment(int segmentIndex, Collection<SegmentRegisterBasedAddress> globals) {
    if (SegmentRegisters.CS_INDEX == segmentIndex) {
      return generateClassForGlobalsOnCS(globals);
    }
    String segmentName = SEGMENT_REGISTERS.getRegName(segmentIndex);
    String segmentNameCamel = WordUtils.capitalizeFully(segmentName);
    // Generate accessors
    String globalsContent = generateGettersSettersForAddresses(globals);
    return generateClassForGlobalsOnSegment(segmentName, segmentNameCamel, globalsContent);
  }

  protected abstract String generateClassForGlobalsOnSegment(String segmentName, String segmentNameCamel,
      String globalsContent);

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
    String gettersAndSetters = joinNewLine(new TreeMap<>(completeWithOppositeOperationsAndPointers(addressOperations)).entrySet()
        .stream()
        .map(entry -> generateAddressOperationAsGetterOrSetter(entry.getKey(), entry.getValue(), address)));
    return MessageFormat.format("""
          // Getters and Setters for address {0}.
        {1}""", address, gettersAndSetters);
  }

  private Map<AddressOperation, Set<Integer>>
  completeWithOppositeOperationsAndPointers(Map<AddressOperation, Set<Integer>> addressOperations) {
    // Ensures that for each read there is a write, even with empty registers so that we can generate valid java
    // properties
    Map<AddressOperation, Set<Integer>> res = new HashMap<>(addressOperations);
    for (AddressOperation operation : addressOperations.keySet()) {
      OperandSize operandSize = operation.getOperandSize();
      ValueOperation valueOperation = operation.getValueOperation();
      ValueOperation oppositeValueOperation = valueOperation.oppositeOperation();
      res.computeIfAbsent(new AddressOperation(oppositeValueOperation, operandSize),
          k -> new HashSet<>());
      if (operandSize == OperandSize.DWORD32) {
        // Ensures getter and setters are generated for segmented address accessors
        res.computeIfAbsent(new AddressOperation(valueOperation, OperandSize.DWORD32PTR), k -> new HashSet<>());
        res.computeIfAbsent(new AddressOperation(oppositeValueOperation, OperandSize.DWORD32PTR), k -> new HashSet<>());
      }
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
    if (operandSize == OperandSize.DWORD32PTR) {
      // segmented address
     return generatePointerGetter(comment, javaName, offset);
    }
    int bits = operandSize.getBits();
    return generateNonPointerGetter(comment, javaName, offset, bits);
  }

  protected abstract String generatePointerGetter(String comment, String javaName, String offset);

  protected abstract String generateNonPointerGetter(String comment, String javaName, String offset, int bits);

  private String generateSetter(String comment, OperandSize operandSize, String javaName, String offset) {
    if (operandSize == OperandSize.DWORD32PTR) {
      // segmented address
      return generatePointerSetter(comment, javaName, offset);
    }
    int bits = operandSize.getBits();
    return generateNonPointerSetter(comment, javaName, offset, bits);
  }

  protected abstract String generatePointerSetter(String comment, String javaName, String offset);

  protected abstract String generateNonPointerSetter(String comment, String javaName, String offset, int bits);

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
    return generateFunctionStub(callsAsComments, functionName, functionNameInJava, segment, offset, retType);
  }

  protected abstract String generateFunctionStub(String callsAsComments, String functionName, String functionNameInJava,
      String segment, String offset, String retType);

  private String getCallsAsComments(List<FunctionInformation> calls) {
    return joinNewLine(calls.stream().map(f -> "// " + toJavaName(f, true) + "();"));
  }

  private String getNoStubReasonCommentForMethod(FunctionInformation functionInformation, String reason) {
    return "  // Not providing stub for " + functionInformation.getName() + ". Reason: " + reason + '\n';
  }
}
