/**
 * Autogenerated by Thrift Compiler (0.12.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package io.cube.tracing.thriftjava;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked", "unused"})
@javax.annotation.Generated(value = "Autogenerated by Thrift Compiler (0.12.0)", date = "2019-11-28")
public class Batch implements org.apache.thrift.TBase<Batch, Batch._Fields>, java.io.Serializable, Cloneable, Comparable<Batch> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("Batch");

  private static final org.apache.thrift.protocol.TField PROCESS_FIELD_DESC = new org.apache.thrift.protocol.TField("process", org.apache.thrift.protocol.TType.STRUCT, (short)1);
  private static final org.apache.thrift.protocol.TField SPANS_FIELD_DESC = new org.apache.thrift.protocol.TField("spans", org.apache.thrift.protocol.TType.LIST, (short)2);

  private static final org.apache.thrift.scheme.SchemeFactory STANDARD_SCHEME_FACTORY = new BatchStandardSchemeFactory();
  private static final org.apache.thrift.scheme.SchemeFactory TUPLE_SCHEME_FACTORY = new BatchTupleSchemeFactory();

  public @org.apache.thrift.annotation.Nullable
  Process process; // required
  public @org.apache.thrift.annotation.Nullable java.util.List<Span> spans; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    PROCESS((short)1, "process"),
    SPANS((short)2, "spans");

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
        case 1: // PROCESS
          return PROCESS;
        case 2: // SPANS
          return SPANS;
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
  public static final java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new java.util.EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.PROCESS, new org.apache.thrift.meta_data.FieldMetaData("process", org.apache.thrift.TFieldRequirementType.REQUIRED,
        new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, Process.class)));
    tmpMap.put(_Fields.SPANS, new org.apache.thrift.meta_data.FieldMetaData("spans", org.apache.thrift.TFieldRequirementType.REQUIRED,
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST,
            new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, Span.class))));
    metaDataMap = java.util.Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(Batch.class, metaDataMap);
  }

  public Batch() {
  }

  public Batch(
    Process process,
    java.util.List<Span> spans)
  {
    this();
    this.process = process;
    this.spans = spans;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public Batch(Batch other) {
    if (other.isSetProcess()) {
      this.process = new Process(other.process);
    }
    if (other.isSetSpans()) {
      java.util.List<Span> __this__spans = new java.util.ArrayList<Span>(other.spans.size());
      for (Span other_element : other.spans) {
        __this__spans.add(new Span(other_element));
      }
      this.spans = __this__spans;
    }
  }

  public Batch deepCopy() {
    return new Batch(this);
  }

  @Override
  public void clear() {
    this.process = null;
    this.spans = null;
  }

  @org.apache.thrift.annotation.Nullable
  public Process getProcess() {
    return this.process;
  }

  public Batch setProcess(@org.apache.thrift.annotation.Nullable Process process) {
    this.process = process;
    return this;
  }

  public void unsetProcess() {
    this.process = null;
  }

  /** Returns true if field process is set (has been assigned a value) and false otherwise */
  public boolean isSetProcess() {
    return this.process != null;
  }

  public void setProcessIsSet(boolean value) {
    if (!value) {
      this.process = null;
    }
  }

  public int getSpansSize() {
    return (this.spans == null) ? 0 : this.spans.size();
  }

  @org.apache.thrift.annotation.Nullable
  public java.util.Iterator<Span> getSpansIterator() {
    return (this.spans == null) ? null : this.spans.iterator();
  }

  public void addToSpans(Span elem) {
    if (this.spans == null) {
      this.spans = new java.util.ArrayList<Span>();
    }
    this.spans.add(elem);
  }

  @org.apache.thrift.annotation.Nullable
  public java.util.List<Span> getSpans() {
    return this.spans;
  }

  public Batch setSpans(@org.apache.thrift.annotation.Nullable java.util.List<Span> spans) {
    this.spans = spans;
    return this;
  }

  public void unsetSpans() {
    this.spans = null;
  }

  /** Returns true if field spans is set (has been assigned a value) and false otherwise */
  public boolean isSetSpans() {
    return this.spans != null;
  }

  public void setSpansIsSet(boolean value) {
    if (!value) {
      this.spans = null;
    }
  }

  public void setFieldValue(_Fields field, @org.apache.thrift.annotation.Nullable Object value) {
    switch (field) {
    case PROCESS:
      if (value == null) {
        unsetProcess();
      } else {
        setProcess((Process)value);
      }
      break;

    case SPANS:
      if (value == null) {
        unsetSpans();
      } else {
        setSpans((java.util.List<Span>)value);
      }
      break;

    }
  }

  @org.apache.thrift.annotation.Nullable
  public Object getFieldValue(_Fields field) {
    switch (field) {
    case PROCESS:
      return getProcess();

    case SPANS:
      return getSpans();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case PROCESS:
      return isSetProcess();
    case SPANS:
      return isSetSpans();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof Batch)
      return this.equals((Batch)that);
    return false;
  }

  public boolean equals(Batch that) {
    if (that == null)
      return false;
    if (this == that)
      return true;

    boolean this_present_process = true && this.isSetProcess();
    boolean that_present_process = true && that.isSetProcess();
    if (this_present_process || that_present_process) {
      if (!(this_present_process && that_present_process))
        return false;
      if (!this.process.equals(that.process))
        return false;
    }

    boolean this_present_spans = true && this.isSetSpans();
    boolean that_present_spans = true && that.isSetSpans();
    if (this_present_spans || that_present_spans) {
      if (!(this_present_spans && that_present_spans))
        return false;
      if (!this.spans.equals(that.spans))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = 1;

    hashCode = hashCode * 8191 + ((isSetProcess()) ? 131071 : 524287);
    if (isSetProcess())
      hashCode = hashCode * 8191 + process.hashCode();

    hashCode = hashCode * 8191 + ((isSetSpans()) ? 131071 : 524287);
    if (isSetSpans())
      hashCode = hashCode * 8191 + spans.hashCode();

    return hashCode;
  }

  @Override
  public int compareTo(Batch other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetProcess()).compareTo(other.isSetProcess());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetProcess()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.process, other.process);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetSpans()).compareTo(other.isSetSpans());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetSpans()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.spans, other.spans);
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
    StringBuilder sb = new StringBuilder("Batch(");
    boolean first = true;

    sb.append("process:");
    if (this.process == null) {
      sb.append("null");
    } else {
      sb.append(this.process);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("spans:");
    if (this.spans == null) {
      sb.append("null");
    } else {
      sb.append(this.spans);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    if (process == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'process' was not present! Struct: " + toString());
    }
    if (spans == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'spans' was not present! Struct: " + toString());
    }
    // check for sub-struct validity
    if (process != null) {
      process.validate();
    }
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
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class BatchStandardSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public BatchStandardScheme getScheme() {
      return new BatchStandardScheme();
    }
  }

  private static class BatchStandardScheme extends org.apache.thrift.scheme.StandardScheme<Batch> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, Batch struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // PROCESS
            if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
              struct.process = new Process();
              struct.process.read(iprot);
              struct.setProcessIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // SPANS
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list40 = iprot.readListBegin();
                struct.spans = new java.util.ArrayList<Span>(_list40.size);
                @org.apache.thrift.annotation.Nullable Span _elem41;
                for (int _i42 = 0; _i42 < _list40.size; ++_i42)
                {
                  _elem41 = new Span();
                  _elem41.read(iprot);
                  struct.spans.add(_elem41);
                }
                iprot.readListEnd();
              }
              struct.setSpansIsSet(true);
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
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, Batch struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.process != null) {
        oprot.writeFieldBegin(PROCESS_FIELD_DESC);
        struct.process.write(oprot);
        oprot.writeFieldEnd();
      }
      if (struct.spans != null) {
        oprot.writeFieldBegin(SPANS_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.spans.size()));
          for (Span _iter43 : struct.spans)
          {
            _iter43.write(oprot);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class BatchTupleSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public BatchTupleScheme getScheme() {
      return new BatchTupleScheme();
    }
  }

  private static class BatchTupleScheme extends org.apache.thrift.scheme.TupleScheme<Batch> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, Batch struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol oprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      struct.process.write(oprot);
      {
        oprot.writeI32(struct.spans.size());
        for (Span _iter44 : struct.spans)
        {
          _iter44.write(oprot);
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, Batch struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol iprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      struct.process = new Process();
      struct.process.read(iprot);
      struct.setProcessIsSet(true);
      {
        org.apache.thrift.protocol.TList _list45 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
        struct.spans = new java.util.ArrayList<Span>(_list45.size);
        @org.apache.thrift.annotation.Nullable Span _elem46;
        for (int _i47 = 0; _i47 < _list45.size; ++_i47)
        {
          _elem46 = new Span();
          _elem46.read(iprot);
          struct.spans.add(_elem46);
        }
      }
      struct.setSpansIsSet(true);
    }
  }

  private static <S extends org.apache.thrift.scheme.IScheme> S scheme(org.apache.thrift.protocol.TProtocol proto) {
    return (org.apache.thrift.scheme.StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
  }
}

