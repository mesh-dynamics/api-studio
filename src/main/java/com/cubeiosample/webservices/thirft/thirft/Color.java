/**
 * Autogenerated by Thrift Compiler (0.12.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package com.cubeiosample.webservices.thirft.thirft;


@javax.annotation.Generated(value = "Autogenerated by Thrift Compiler (0.12.0)", date = "2019-10-09")
public enum Color implements org.apache.thrift.TEnum {
  BLUE(0),
  RED(1),
  GREEN(2),
  BLACK(3);

  private final int value;

  private Color(int value) {
    this.value = value;
  }

  /**
   * Get the integer value of this enum value, as defined in the Thrift IDL.
   */
  public int getValue() {
    return value;
  }

  /**
   * Find a the enum type by its integer value, as defined in the Thrift IDL.
   * @return null if the value is not found.
   */
  @org.apache.thrift.annotation.Nullable
  public static Color findByValue(int value) {
    switch (value) {
      case 0:
        return BLUE;
      case 1:
        return RED;
      case 2:
        return GREEN;
      case 3:
        return BLACK;
      default:
        return null;
    }
  }
}
