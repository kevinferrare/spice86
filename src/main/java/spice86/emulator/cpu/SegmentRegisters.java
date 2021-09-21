package spice86.emulator.cpu;

import java.util.HashMap;
import java.util.Map;

/**
 * Holder for the segment registers. Supports from CS to GS.
 */
public class SegmentRegisters extends RegistersHolder {
  public static final int ES_INDEX = 0;
  public static final int CS_INDEX = 1;
  public static final int SS_INDEX = 2;
  public static final int DS_INDEX = 3;
  public static final int FS_INDEX = 4;
  public static final int GS_INDEX = 5;

  public SegmentRegisters() {
    super(getRegistersNames());
  }

  private static final Map<Integer, String> getRegistersNames() {
    Map<Integer, String> res = new HashMap<>();
    res.put(ES_INDEX, "ES");
    res.put(CS_INDEX, "CS");
    res.put(SS_INDEX, "SS");
    res.put(DS_INDEX, "DS");
    res.put(FS_INDEX, "FS");
    res.put(GS_INDEX, "GS");
    return res;
  }
}
