package spice86.emulator.function;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import spice86.emulator.memory.SegmentedAddress;
import spice86.utils.ConvertUtils;

/**
 * Dumps collected function informations to disk
 */
public class FunctionHandlerDumper {

  public void dumpJavaStubs(String path, FunctionHandler... functionHandlers) throws IOException {
    forEachFunction(path, this::generateFunctionJavaStub, functionHandlers);
  }

  private String getNoStubReasonCommentForMethod(FunctionInformation functionInformation, String reason) {
    return "// Not providing stub for " + functionInformation.getName() + ". Reason: " + reason + '\n';
  }

  private String generateFunctionJavaStub(FunctionInformation functionInformation,
      Set<FunctionInformation> allFunctions) {
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
          return {4}Ret();
        '}'
        """,
        segment, offset, functionName, functionNameInJava,
        //
        retType);
  }

  private String removeDotsFromFunctionName(String name) {
    String[] functionNameSplit = name.split(".");
    if (functionNameSplit.length > 1) {
      return functionNameSplit[functionNameSplit.length - 1];
    }
    return name;
  }

  public void dump(String path, FunctionHandler... functionHandlers) throws IOException {
    forEachFunction(path, this::dumpFunctionInformation, functionHandlers);
  }

  private void forEachFunction(String path, BiFunction<FunctionInformation, Set<FunctionInformation>, String> action,
      FunctionHandler... functionHandlers) throws IOException {
    List<FunctionInformation> functionInformations =
        mergeFunctionHandlers(functionHandlers).toList();
    // Set for search purposes
    Set<FunctionInformation> functionInformationsSet = new HashSet<>(functionInformations);
    try (PrintWriter printWriter = new PrintWriter(new FileWriter(path))) {
      for (FunctionInformation functionInformation : functionInformations) {
        String res = action.apply(functionInformation, functionInformationsSet);
        if (StringUtils.isNotEmpty(res)) {
          printWriter.println(res);
        }
      }
    }
  }

  private Stream<FunctionInformation> mergeFunctionHandlers(FunctionHandler... functionHandlers) {
    return Arrays.stream(functionHandlers)
        .map(FunctionHandler::getFunctionInformations)
        .map(Map::values)
        .flatMap(Collection::stream)
        .distinct()
        .sorted();
  }

  private String dumpFunctionInformation(FunctionInformation functionInformation,
      Set<FunctionInformation> allFunctions) {
    if (functionInformation.getCalledCount() == 0) {
      return "";
    }

    StringBuilder res = new StringBuilder();
    Map<FunctionReturn, Set<SegmentedAddress>> returns = sort(functionInformation.getReturns());
    Map<FunctionReturn, Set<SegmentedAddress>> unalignedReturns = sort(functionInformation.getUnalignedReturns());
    List<FunctionInformation> callers = getCallers(functionInformation);
    List<FunctionInformation> calls = getCalls(functionInformation, allFunctions);
    int approximateSize = approximateSize(functionInformation);
    String header = "function " + functionInformation.toString();
    header += " returns:" + returns.size();
    header += " callers:" + callers.size();
    header += " called: " + functionInformation.getCalledCount();
    header += " calls:" + calls.size();
    header += " approximateSize:" + approximateSize;
    if (calls.isEmpty() || !containsNonOverride(calls)) {
      header += " overridable";
    }
    res.append(header + '\n');
    res.append(dumpReturns(returns, "returns"));
    res.append(dumpReturns(unalignedReturns, "unaligned returns"));
    for (FunctionInformation caller : callers) {
      res.append(" - caller: " + caller.toString() + '\n');
    }
    for (FunctionInformation call : calls) {
      res.append(" - call: " + call.toString() + '\n');
    }
    return res.toString();
  }

  private String dumpReturns(Map<FunctionReturn, Set<SegmentedAddress>> returns, String prefix) {
    StringBuilder res = new StringBuilder();
    for (Map.Entry<FunctionReturn, Set<SegmentedAddress>> entry : returns.entrySet()) {
      FunctionReturn oneReturn = entry.getKey();
      res.append(" - ");
      res.append(prefix);
      res.append(": ");
      res.append(oneReturn.toString());
      res.append('\n');
      Set<SegmentedAddress> targets = entry.getValue();
      for (SegmentedAddress target : targets) {
        res.append("   - target: ");
        res.append(target);
        res.append('\n');
      }
    }
    return res.toString();
  }

  private int approximateSize(FunctionInformation functionInformation) {
    List<SegmentedAddress> boundaries = getBoundaries(functionInformation);
    SegmentedAddress first = boundaries.get(0);
    SegmentedAddress last = boundaries.get(boundaries.size() - 1);
    return Math.abs(first.toPhysical() - last.toPhysical());
  }

  private List<SegmentedAddress> getBoundaries(FunctionInformation functionInformation) {
    List<SegmentedAddress> boundaries = new ArrayList<>();
    boundaries.addAll(functionInformation.getReturns().keySet().stream().map(FunctionReturn::getAddress).toList());
    boundaries.add(functionInformation.getAddress());
    return boundaries.stream().sorted().toList();
  }

  private boolean containsNonOverride(List<FunctionInformation> calls) {
    return calls.stream().anyMatch(function -> !function.hasOverride());
  }

  private List<FunctionInformation> getCallers(FunctionInformation functionInformation) {
    return sort(functionInformation.getCallers());
  }

  private List<FunctionInformation> getCalls(FunctionInformation functionInformation,
      Set<FunctionInformation> allFunctions) {
    // calls made by this function is the list of functions that get called by it
    return sort(allFunctions.stream().filter(callee -> callee.getCallers().contains(functionInformation)));
  }

  private <T> List<T> sort(Collection<T> collection) {
    return sort(collection.stream());
  }

  private <T> List<T> sort(Stream<T> stream) {
    return stream.sorted().toList();
  }

  private <K, V> Map<K, V> sort(Map<K, V> map) {
    return new TreeMap<>(map);
  }
}
