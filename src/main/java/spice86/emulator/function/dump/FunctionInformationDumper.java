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

/**
 * Dumps collected function informations to a file
 */
public class FunctionInformationDumper {
  public void dumpFunctionHandlers(String destinationFilePath, FunctionInformationToStringConverter converter,
      FunctionHandler... functionHandlers) throws IOException {
    List<FunctionInformation> functionInformations =
        mergeFunctionHandlers(functionHandlers).toList();
    // Set for search purposes
    Set<FunctionInformation> functionInformationsSet = new HashSet<>(functionInformations);
    try (PrintWriter printWriter = new PrintWriter(new FileWriter(destinationFilePath))) {
      String header = converter.getFileHeader();
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
