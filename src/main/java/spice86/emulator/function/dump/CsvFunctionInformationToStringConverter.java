package spice86.emulator.function.dump;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import spice86.emulator.function.FunctionInformation;
import spice86.emulator.function.SegmentRegisterBasedAddress;
import spice86.emulator.memory.SegmentedAddress;

/**
 * Converts FunctionInformation to CSV
 */
public class CsvFunctionInformationToStringConverter extends FunctionInformationToStringConverter {
  @Override
  public String getFileHeader(Collection<SegmentRegisterBasedAddress> allGlobals, Set<SegmentedAddress> whiteListOfSegmentForOffset) {
    return generateLine("Name", "Returns", "UnalignedReturns", "Callers", "Called", "Calls", "ApproximateSize",
        "Overridable", "Overriden");
  }

  @Override
  public String convert(FunctionInformation functionInformation, Set<FunctionInformation> allFunctions) {
    List<FunctionInformation> calls = getCalls(functionInformation, allFunctions);
    return generateLine(toJavaName(functionInformation, true),
        size(functionInformation.getReturns()),
        size(functionInformation.getUnalignedReturns()),
        size(getCallers(functionInformation)),
        Integer.toString(functionInformation.getCalledCount()),
        size(calls),
        Integer.toString(approximateSize(functionInformation)),
        Boolean.toString(isOverridable(calls)),
        Boolean.toString(functionInformation.hasOverride()));
  }

  private String size(Map<?, ?> map) {
    return size(map.entrySet());
  }

  private String size(Collection<?> collection) {
    return Integer.toString(collection.size());
  }

  @SuppressWarnings({
      // This method has a lot of parameters, but it's there to assure consistency between the header and the values.
      "java:S107"
  })
  private static String generateLine(String name, String returns, String unalignedReturns, String callers,
      String called, String calls, String approximateSize, String overridable, String overridden) {
    StringBuilder res = new StringBuilder();
    res.append(name);
    res.append(',');
    res.append(returns);
    res.append(',');
    res.append(unalignedReturns);
    res.append(',');
    res.append(callers);
    res.append(',');
    res.append(called);
    res.append(',');
    res.append(calls);
    res.append(',');
    res.append(approximateSize);
    res.append(',');
    res.append(overridable);
    res.append(',');
    res.append(overridden);
    return res.toString();
  }
}
