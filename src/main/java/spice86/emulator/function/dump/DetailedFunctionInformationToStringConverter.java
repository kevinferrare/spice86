package spice86.emulator.function.dump;

import java.util.List;
import java.util.Map;
import java.util.Set;

import spice86.emulator.function.FunctionInformation;
import spice86.emulator.function.FunctionReturn;
import spice86.emulator.memory.SegmentedAddress;

/**
 * Converts FunctionInformation to custom human readable format with details
 */
public class DetailedFunctionInformationToStringConverter extends FunctionInformationToStringConverter {

  @Override
  public String convert(FunctionInformation functionInformation,
      Set<FunctionInformation> allFunctions) {
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
    if (isOverridable(calls)) {
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
}
