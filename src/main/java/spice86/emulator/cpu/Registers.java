package spice86.emulator.cpu;

import java.util.HashMap;
import java.util.Map;

/**
 * Holder for the regular x86 registers.
 */
public class Registers extends RegistersHolder {
  public static final int AX_INDEX = 0;
  public static final int CX_INDEX = 1;
  public static final int DX_INDEX = 2;
  public static final int BX_INDEX = 3;
  public static final int SP_INDEX = 4;
  public static final int BP_INDEX = 5;
  public static final int SI_INDEX = 6;
  public static final int DI_INDEX = 7;

  public Registers() {
    super(getRegistersNames());
  }

  private static final Map<Integer, String> getRegistersNames() {
    Map<Integer, String> res = new HashMap<>();
    res.put(AX_INDEX, "AX");
    res.put(CX_INDEX, "CX");
    res.put(DX_INDEX, "DX");
    res.put(BX_INDEX, "BX");
    res.put(SP_INDEX, "SP");
    res.put(BP_INDEX, "BP");
    res.put(SI_INDEX, "SI");
    res.put(DI_INDEX, "DI");
    return res;
  }
}
