package spice86.emulator.memory;

import static spice86.utils.ConvertUtils.uint16;

import spice86.utils.ConvertUtils;

/**
 * An address that is represented with a real mode segment and an offset.
 */
public class SegmentedAddress implements Comparable<SegmentedAddress> {
  private int segment;
  private int offset;

  public SegmentedAddress(int segment, int offset) {
    this.segment = uint16(segment);
    this.offset = uint16(offset);
  }

  public int getSegment() {
    return segment;
  }

  public int getOffset() {
    return offset;
  }

  public String toSegmentOffsetRepresentation() {
    return ConvertUtils.toSegmentedAddressRepresentation(segment, offset);
  }

  public int toPhysical() {
    return MemoryUtils.toPhysicalAddress(segment, offset);
  }

  @Override
  public int hashCode() {
    return toPhysical();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return (obj instanceof SegmentedAddress other)
        && MemoryUtils.toPhysicalAddress(segment, offset) == MemoryUtils.toPhysicalAddress(other.segment, other.offset);
  }

  @Override
  public int compareTo(SegmentedAddress other) {
    return Integer.compare(this.toPhysical(), other.toPhysical());
  }

  @Override
  public String toString() {
    return toSegmentOffsetRepresentation() + '/' + ConvertUtils.toHex(toPhysical());
  }
}
