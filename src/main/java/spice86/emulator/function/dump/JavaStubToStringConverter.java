package spice86.emulator.function.dump;

import java.text.MessageFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import spice86.emulator.function.CallType;
import spice86.emulator.function.FunctionInformation;
import spice86.emulator.function.FunctionReturn;
import spice86.emulator.memory.SegmentedAddress;
import spice86.utils.ConvertUtils;

/**
 * Converts FunctionInformation to java stubs for easy override
 */
public class JavaStubToStringConverter extends FunctionInformationToStringConverter {
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
