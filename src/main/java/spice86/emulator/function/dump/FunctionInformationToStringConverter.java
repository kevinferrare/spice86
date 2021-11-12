package spice86.emulator.function.dump;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import spice86.emulator.function.FunctionInformation;
import spice86.emulator.function.FunctionReturn;
import spice86.emulator.function.SegmentRegisterBasedAddress;
import spice86.emulator.memory.SegmentedAddress;
import spice86.utils.ConvertUtils;

/**
 * Base class for FunctionInformation to String conversion. Each subclass could implement a custom format.
 */
public abstract class FunctionInformationToStringConverter {

  /**
   * Generates the header for the file.
   * 
   * @param allGlobals List of addresses encountered during the execution. Hacky to pass it there, but used by Java stub generator.
   * @param whiteListOfSegmentForOffset Set of segmented addresses for which nothing should be displayed if the offset matches but not the segmen 
   * @return
   */
  public String getFileHeader(Collection<SegmentRegisterBasedAddress> allGlobals, Set<SegmentedAddress> whiteListOfSegmentForOffset) {
    return "";
  }

  /**
   * Generates the footer of the file.
   * 
   * @return
   */
  public String getFileFooter() {
    return "";
  }

  /**
   * Called once per functionInformation.
   * 
   * @param functionInformation
   * @param allFunctions
   * @return
   */
  public abstract String convert(FunctionInformation functionInformation, Set<FunctionInformation> allFunctions);

  protected int approximateSize(FunctionInformation functionInformation) {
    List<SegmentedAddress> boundaries = getBoundaries(functionInformation);
    SegmentedAddress first = boundaries.get(0);
    SegmentedAddress last = boundaries.get(boundaries.size() - 1);
    return Math.abs(first.toPhysical() - last.toPhysical());
  }

  protected List<SegmentedAddress> getBoundaries(FunctionInformation functionInformation) {
    List<SegmentedAddress> boundaries = new ArrayList<>();
    boundaries.addAll(functionInformation.getReturns().keySet().stream().map(FunctionReturn::getAddress).toList());
    boundaries.add(functionInformation.getAddress());
    return boundaries.stream().sorted().toList();
  }

  protected boolean containsNonOverride(List<FunctionInformation> calls) {
    return calls.stream().anyMatch(function -> !function.hasOverride());
  }

  protected boolean isOverridable(List<FunctionInformation> calls) {
    return calls.isEmpty() || !containsNonOverride(calls);
  }

  protected List<FunctionInformation> getCallers(FunctionInformation functionInformation) {
    return sort(functionInformation.getCallers());
  }

  protected List<FunctionInformation> getCalls(FunctionInformation functionInformation,
      Set<FunctionInformation> allFunctions) {
    // calls made by this function is the list of functions that get called by it
    return sort(allFunctions.stream().filter(callee -> callee.getCallers().contains(functionInformation)));
  }

  protected <T> List<T> sort(Collection<T> collection) {
    return sort(collection.stream());
  }

  protected <T> List<T> sort(Stream<T> stream) {
    return stream.sorted().toList();
  }

  protected <K, V> Map<K, V> sort(Map<K, V> map) {
    return new TreeMap<>(map);
  }

  protected String joinNewLine(Stream<String> stream) {
    return stream.collect(Collectors.joining("\n"));
  }

  protected String toJavaName(FunctionInformation functionInformation, boolean dots) {
    String nameToUse = functionInformation.getName();
    if(!dots) {
      nameToUse = removeDotsFromFunctionName(nameToUse);
    }
    return nameToUse + "_" + ConvertUtils.toJavaStringWithPhysical(functionInformation.getAddress());
  }

  protected String removeDotsFromFunctionName(String name) {
    String[] functionNameSplit = name.split("\\.");
    if (functionNameSplit.length > 1) {
      return functionNameSplit[functionNameSplit.length - 1];
    }
    return name;
  }
}
