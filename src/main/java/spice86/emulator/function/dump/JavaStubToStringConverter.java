package spice86.emulator.function.dump;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;

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
  @Override
  public String getFileHeader(Collection<SegmentRegisterBasedAddress> allPotentialGlobals) {
    // Take only addresses which have been accessed (and not only computed)
    List<SegmentRegisterBasedAddress> globals = allPotentialGlobals.stream()
        .filter(a -> CollectionUtils.isNotEmpty(a.getAddressOperations()))
        .sorted()
        .toList();
    int numberOfGlobals = globals.size();
    // Generate accessors
    String globalsContent = globals.stream().map(this::getGetterSetterForAddress).collect(Collectors.joining("\n"));
    // Write header
    return MessageFormat.format("""
        // Getters and setters for what could be global variables. {0} values
        class Globals '{'
        {1}
        '}'

        // Stubs for overrides
        class Stubs '{'
        """, numberOfGlobals, globalsContent);
  }

  private String getGetterSetterForAddress(SegmentRegisterBasedAddress address) {
    String registerName = address.getRegisterName();
    String offset = address.getOffsetHexString();
    Set<AddressOperation> addressOperations = address.getAddressOperations();
    if (addressOperations.isEmpty()) {
      return "";
    }
    String segmentValues = address.getSegmentValues().stream().sorted().map(ConvertUtils::toHex16WithoutX).collect(Collectors.joining(", "));
    
    String gettersAndSetters = addressOperations.stream()
        .sorted()
        .map(addressOperation -> addressOperationToGetterOrSetter(addressOperation, offset))
        .collect(Collectors.joining("\n"));
    return MessageFormat.format("""
          // Getters and Setters for {0}:{1}. {0} was registered with values {2}
        {3}
        """, registerName, offset, segmentValues, gettersAndSetters);
  }

  private String addressOperationToGetterOrSetter(AddressOperation addressOperation, String offset) {
    OperandSize operandSize = addressOperation.getOperandSize();
    int bits = operandSize.getBits();
    String sizeName = operandSize.getName();
    if (ValueOperation.READ.equals(addressOperation.getValueOperation())) {
      return MessageFormat.format("""
            public int get{0}{2}() '{'
              return getUint{1}(0x{0});
            '}'
          """, offset, bits, sizeName);
    }
    // WRITE
    return MessageFormat.format("""
          public int set{0}{2}(int value) '{'
            return setUint{1}(0x{0}, value);
          '}'
        """, offset, bits, sizeName);
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
    String functionNameInJava = removeDotsFromFunctionName(functionInformation.generateName());
    SegmentedAddress functionAddress = functionInformation.getAddress();
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
    return calls.stream().map(f -> "  // " + f.generateName() + "();").collect(Collectors.joining("\n"));
  }

  private String getNoStubReasonCommentForMethod(FunctionInformation functionInformation, String reason) {
    return "// Not providing stub for " + functionInformation.getName() + ". Reason: " + reason + '\n';
  }

  private String removeDotsFromFunctionName(String name) {
    String[] functionNameSplit = name.split(".");
    if (functionNameSplit.length > 1) {
      return functionNameSplit[functionNameSplit.length - 1];
    }
    return name;
  }
}
