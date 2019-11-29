/**
 * Autogenerated by Thrift Compiler (0.12.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package io.jaegertracing.thriftjava;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked", "unused"})
@javax.annotation.Generated(value = "Autogenerated by Thrift Compiler (0.12.0)", date = "2019-11-28")
public class BatchSubmitResponse implements org.apache.thrift.TBase<io.jaegertracing.thriftjava.BatchSubmitResponse, io.jaegertracing.thriftjava.BatchSubmitResponse._Fields>, java.io.Serializable, Cloneable, Comparable<io.jaegertracing.thriftjava.BatchSubmitResponse> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("BatchSubmitResponse");

  private static final org.apache.thrift.protocol.TField OK_FIELD_DESC = new org.apache.thrift.protocol.TField("ok", org.apache.thrift.protocol.TType.BOOL, (short)1);

  private static final org.apache.thrift.scheme.SchemeFactory STANDARD_SCHEME_FACTORY = new BatchSubmitResponseStandardSchemeFactory();
  private static final org.apache.thrift.scheme.SchemeFactory TUPLE_SCHEME_FACTORY = new BatchSubmitResponseTupleSchemeFactory();

  public boolean ok; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    OK((short)1, "ok");

    private static final java.util.Map<String, _Fields> byName = new java.util.HashMap<String, _Fields>();

    static {
      for (_Fields field : java.util.EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    @org.apache.thrift.annotation.Nullable
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // OK
          return OK;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    @org.apache.thrift.annotation.Nullable
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  private static final int __OK_ISSET_ID = 0;
  private byte __isset_bitfield = 0;
  public static final java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new java.util.EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.OK, new org.apache.thrift.meta_data.FieldMetaData("ok", org.apache.thrift.TFieldRequirementType.REQUIRED,
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.BOOL)));
    metaDataMap = java.util.Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(
        io.jaegertracing.thriftjava.BatchSubmitResponse.class, metaDataMap);
  }

  public BatchSubmitResponse() {
  }

  public BatchSubmitResponse(
    boolean ok)
  {
    this();
    this.ok = ok;
    setOkIsSet(true);
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public BatchSubmitResponse(io.jaegertracing.thriftjava.BatchSubmitResponse other) {
    __isset_bitfield = other.__isset_bitfield;
    this.ok = other.ok;
  }

  public io.jaegertracing.thriftjava.BatchSubmitResponse deepCopy() {
    return new io.jaegertracing.thriftjava.BatchSubmitResponse(this);
  }

  @Override
  public void clear() {
    setOkIsSet(false);
    this.ok = false;
  }

  public boolean isOk() {
    return this.ok;
  }

  public io.jaegertracing.thriftjava.BatchSubmitResponse setOk(boolean ok) {
    this.ok = ok;
    setOkIsSet(true);
    return this;
  }

  public void unsetOk() {
    __isset_bitfield = org.apache.thrift.EncodingUtils.clearBit(__isset_bitfield, __OK_ISSET_ID);
  }

  /** Returns true if field ok is set (has been assigned a value) and false otherwise */
  public boolean isSetOk() {
    return org.apache.thrift.EncodingUtils.testBit(__isset_bitfield, __OK_ISSET_ID);
  }

  public void setOkIsSet(boolean value) {
    __isset_bitfield = org.apache.thrift.EncodingUtils.setBit(__isset_bitfield, __OK_ISSET_ID, value);
  }

  public void setFieldValue(_Fields field, @org.apache.thrift.annotation.Nullable Object value) {
    switch (field) {
    case OK:
      if (value == null) {
        unsetOk();
      } else {
        setOk((Boolean)value);
      }
      break;

    }
  }

  @org.apache.thrift.annotation.Nullable
  public Object getFieldValue(_Fields field) {
    switch (field) {
    case OK:
      return isOk();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case OK:
      return isSetOk();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof io.jaegertracing.thriftjava.BatchSubmitResponse)
      return this.equals((io.jaegertracing.thriftjava.BatchSubmitResponse)that);
    return false;
  }

  public boolean equals(io.jaegertracing.thriftjava.BatchSubmitResponse that) {
    if (that == null)
      return false;
    if (this == that)
      return true;

    boolean this_present_ok = true;
    boolean that_present_ok = true;
    if (this_present_ok || that_present_ok) {
      if (!(this_present_ok && that_present_ok))
        return false;
      if (this.ok != that.ok)
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = 1;

    hashCode = hashCode * 8191 + ((ok) ? 131071 : 524287);

    return hashCode;
  }

  @Override
  public int compareTo(io.jaegertracing.thriftjava.BatchSubmitResponse other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetOk()).compareTo(other.isSetOk());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetOk()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.ok, other.ok);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  @org.apache.thrift.annotation.Nullable
  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    scheme(iprot).read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    scheme(oprot).write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("BatchSubmitResponse(");
    boolean first = true;

    sb.append("ok:");
    sb.append(this.ok);
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // alas, we cannot check 'ok' because it's a primitive and you chose the non-beans generator.
    // check for sub-struct validity
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
      __isset_bitfield = 0;
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class BatchSubmitResponseStandardSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public BatchSubmitResponseStandardScheme getScheme() {
      return new BatchSubmitResponseStandardScheme();
    }
  }

  private static class BatchSubmitResponseStandardScheme extends org.apache.thrift.scheme.StandardScheme<io.jaegertracing.thriftjava.BatchSubmitResponse> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, io.jaegertracing.thriftjava.BatchSubmitResponse struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // OK
            if (schemeField.type == org.apache.thrift.protocol.TType.BOOL) {
              struct.ok = iprot.readBool();
              struct.setOkIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      if (!struct.isSetOk()) {
        throw new org.apache.thrift.protocol.TProtocolException("Required field 'ok' was not found in serialized data! Struct: " + toString());
      }
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, io.jaegertracing.thriftjava.BatchSubmitResponse struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      oprot.writeFieldBegin(OK_FIELD_DESC);
      oprot.writeBool(struct.ok);
      oprot.writeFieldEnd();
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class BatchSubmitResponseTupleSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public BatchSubmitResponseTupleScheme getScheme() {
      return new BatchSubmitResponseTupleScheme();
    }
  }

  private static class BatchSubmitResponseTupleScheme extends org.apache.thrift.scheme.TupleScheme<io.jaegertracing.thriftjava.BatchSubmitResponse> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, io.jaegertracing.thriftjava.BatchSubmitResponse struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol oprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      oprot.writeBool(struct.ok);
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, io.jaegertracing.thriftjava.BatchSubmitResponse struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol iprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      struct.ok = iprot.readBool();
      struct.setOkIsSet(true);
    }
  }

  private static <S extends org.apache.thrift.scheme.IScheme> S scheme(org.apache.thrift.protocol.TProtocol proto) {
    return (org.apache.thrift.scheme.StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
  }
}

