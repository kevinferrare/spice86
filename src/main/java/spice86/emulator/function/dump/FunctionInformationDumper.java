package spice86.emulator.function.dump;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import spice86.emulator.function.FunctionHandler;
import spice86.emulator.function.FunctionInformation;
import spice86.emulator.function.SegmentRegisterBasedAddress;
import spice86.emulator.function.StaticAddressesRecorder;
import spice86.emulator.memory.SegmentedAddress;

/**
 * Dumps collected function informations to a file
 */
public class FunctionInformationDumper {
  public void dumpFunctionHandlers(String destinationFilePath, FunctionInformationToStringConverter converter,
      StaticAddressesRecorder staticAddressesRecorder, FunctionHandler... functionHandlers) throws IOException {
    List<FunctionInformation> functionInformations =
        mergeFunctionHandlers(functionHandlers).toList();
    // Set for search purposes
    Set<FunctionInformation> functionInformationsSet = new HashSet<>(functionInformations);
    Collection<SegmentRegisterBasedAddress> allGlobals = staticAddressesRecorder.getSegmentRegisterBasedAddress();
    Set<SegmentedAddress> whiteListOfSegmentForOffset = staticAddressesRecorder.getWhiteListOfSegmentForOffset();
    try (PrintWriter printWriter = new PrintWriter(new FileWriter(destinationFilePath))) {
      String header = converter.getFileHeader(allGlobals, whiteListOfSegmentForOffset);
      if (StringUtils.isNotEmpty(header)) {
        printWriter.println(header);
      }
      for (FunctionInformation functionInformation : functionInformations) {
        if (functionInformation.getCalledCount() == 0) {
          continue;
        }
        String res = converter.convert(functionInformation, functionInformationsSet);
        if (StringUtils.isNotEmpty(res)) {
          printWriter.println(res);
        }
      }
      String footer = converter.getFileFooter();
      if (StringUtils.isNotEmpty(footer)) {
        printWriter.println(footer);
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

}
