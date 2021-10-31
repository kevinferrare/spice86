package spice86.emulator.function;

import java.util.Comparator;

/**
 * Describes an operation done on a value stored at an address.<br/>
 * Operation can be 8/16/32bit read / write
 */
public class AddressOperation implements Comparable<AddressOperation> {
  private static final Comparator<AddressOperation> NATURAL_ORDER_COMPARATOR =
      Comparator.comparing(AddressOperation::getOperandSize)
          .thenComparing(AddressOperation::getValueOperation);
  private ValueOperation valueOperation;
  private OperandSize operandSize;

  public AddressOperation(ValueOperation valueOperation, OperandSize operandSize) {
    this.valueOperation = valueOperation;
    this.operandSize = operandSize;
  }

  public ValueOperation getValueOperation() {
    return valueOperation;
  }

  public OperandSize getOperandSize() {
    return operandSize;
  }

  @Override
  public int compareTo(AddressOperation other) {
    return NATURAL_ORDER_COMPARATOR.compare(this, other);
  }

  @Override
  public int hashCode() {
    return operandSize.ordinal() << 2 | valueOperation.ordinal();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    return (obj instanceof AddressOperation other) && operandSize == other.operandSize
        && valueOperation == other.valueOperation;
  }

}
