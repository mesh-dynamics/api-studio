/**
 * Autogenerated by Thrift Compiler (0.12.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package com.cubeiosample.webservices.thirft.thirft;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked", "unused"})
@javax.annotation.Generated(value = "Autogenerated by Thrift Compiler (0.12.0)", date = "2019-10-09")
public class ListMovieResult implements org.apache.thrift.TBase<ListMovieResult, ListMovieResult._Fields>, java.io.Serializable, Cloneable, Comparable<ListMovieResult> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("ListMovieResult");

  private static final org.apache.thrift.protocol.TField MOVIE_INFO_LIST_FIELD_DESC = new org.apache.thrift.protocol.TField("movieInfoList", org.apache.thrift.protocol.TType.LIST, (short)1);

  private static final org.apache.thrift.scheme.SchemeFactory STANDARD_SCHEME_FACTORY = new ListMovieResultStandardSchemeFactory();
  private static final org.apache.thrift.scheme.SchemeFactory TUPLE_SCHEME_FACTORY = new ListMovieResultTupleSchemeFactory();

  public @org.apache.thrift.annotation.Nullable java.util.List<MovieInfo> movieInfoList; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    MOVIE_INFO_LIST((short)1, "movieInfoList");

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
        case 1: // MOVIE_INFO_LIST
          return MOVIE_INFO_LIST;
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
    tmpMap.put(_Fields.MOVIE_INFO_LIST, new org.apache.thrift.meta_data.FieldMetaData("movieInfoList", org.apache.thrift.TFieldRequirementType.DEFAULT,
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST,
            new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, MovieInfo.class))));
    metaDataMap = java.util.Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(ListMovieResult.class, metaDataMap);
  }

  public ListMovieResult() {
  }

  public ListMovieResult(
    java.util.List<MovieInfo> movieInfoList)
  {
    this();
    this.movieInfoList = movieInfoList;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public ListMovieResult(ListMovieResult other) {
    if (other.isSetMovieInfoList()) {
      java.util.List<MovieInfo> __this__movieInfoList = new java.util.ArrayList<MovieInfo>(other.movieInfoList.size());
      for (MovieInfo other_element : other.movieInfoList) {
        __this__movieInfoList.add(new MovieInfo(other_element));
      }
      this.movieInfoList = __this__movieInfoList;
    }
  }

  public ListMovieResult deepCopy() {
    return new ListMovieResult(this);
  }

  @Override
  public void clear() {
    this.movieInfoList = null;
  }

  public int getMovieInfoListSize() {
    return (this.movieInfoList == null) ? 0 : this.movieInfoList.size();
  }

  @org.apache.thrift.annotation.Nullable
  public java.util.Iterator<MovieInfo> getMovieInfoListIterator() {
    return (this.movieInfoList == null) ? null : this.movieInfoList.iterator();
  }

  public void addToMovieInfoList(MovieInfo elem) {
    if (this.movieInfoList == null) {
      this.movieInfoList = new java.util.ArrayList<MovieInfo>();
    }
    this.movieInfoList.add(elem);
  }

  @org.apache.thrift.annotation.Nullable
  public java.util.List<MovieInfo> getMovieInfoList() {
    return this.movieInfoList;
  }

  public ListMovieResult setMovieInfoList(@org.apache.thrift.annotation.Nullable java.util.List<MovieInfo> movieInfoList) {
    this.movieInfoList = movieInfoList;
    return this;
  }

  public void unsetMovieInfoList() {
    this.movieInfoList = null;
  }

  /** Returns true if field movieInfoList is set (has been assigned a value) and false otherwise */
  public boolean isSetMovieInfoList() {
    return this.movieInfoList != null;
  }

  public void setMovieInfoListIsSet(boolean value) {
    if (!value) {
      this.movieInfoList = null;
    }
  }

  public void setFieldValue(_Fields field, @org.apache.thrift.annotation.Nullable Object value) {
    switch (field) {
    case MOVIE_INFO_LIST:
      if (value == null) {
        unsetMovieInfoList();
      } else {
        setMovieInfoList((java.util.List<MovieInfo>)value);
      }
      break;

    }
  }

  @org.apache.thrift.annotation.Nullable
  public Object getFieldValue(_Fields field) {
    switch (field) {
    case MOVIE_INFO_LIST:
      return getMovieInfoList();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case MOVIE_INFO_LIST:
      return isSetMovieInfoList();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof ListMovieResult)
      return this.equals((ListMovieResult)that);
    return false;
  }

  public boolean equals(ListMovieResult that) {
    if (that == null)
      return false;
    if (this == that)
      return true;

    boolean this_present_movieInfoList = true && this.isSetMovieInfoList();
    boolean that_present_movieInfoList = true && that.isSetMovieInfoList();
    if (this_present_movieInfoList || that_present_movieInfoList) {
      if (!(this_present_movieInfoList && that_present_movieInfoList))
        return false;
      if (!this.movieInfoList.equals(that.movieInfoList))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = 1;

    hashCode = hashCode * 8191 + ((isSetMovieInfoList()) ? 131071 : 524287);
    if (isSetMovieInfoList())
      hashCode = hashCode * 8191 + movieInfoList.hashCode();

    return hashCode;
  }

  @Override
  public int compareTo(ListMovieResult other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetMovieInfoList()).compareTo(other.isSetMovieInfoList());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetMovieInfoList()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.movieInfoList, other.movieInfoList);
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
    StringBuilder sb = new StringBuilder("ListMovieResult(");
    boolean first = true;

    sb.append("movieInfoList:");
    if (this.movieInfoList == null) {
      sb.append("null");
    } else {
      sb.append(this.movieInfoList);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
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
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class ListMovieResultStandardSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public ListMovieResultStandardScheme getScheme() {
      return new ListMovieResultStandardScheme();
    }
  }

  private static class ListMovieResultStandardScheme extends org.apache.thrift.scheme.StandardScheme<ListMovieResult> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, ListMovieResult struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) {
          break;
        }
        switch (schemeField.id) {
          case 1: // MOVIE_INFO_LIST
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list56 = iprot.readListBegin();
                struct.movieInfoList = new java.util.ArrayList<MovieInfo>(_list56.size);
                @org.apache.thrift.annotation.Nullable MovieInfo _elem57;
                for (int _i58 = 0; _i58 < _list56.size; ++_i58)
                {
                  _elem57 = new MovieInfo();
                  _elem57.read(iprot);
                  struct.movieInfoList.add(_elem57);
                }
                iprot.readListEnd();
              }
              struct.setMovieInfoListIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, ListMovieResult struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.movieInfoList != null) {
        oprot.writeFieldBegin(MOVIE_INFO_LIST_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.movieInfoList.size()));
          for (MovieInfo _iter59 : struct.movieInfoList)
          {
            _iter59.write(oprot);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class ListMovieResultTupleSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public ListMovieResultTupleScheme getScheme() {
      return new ListMovieResultTupleScheme();
    }
  }

  private static class ListMovieResultTupleScheme extends org.apache.thrift.scheme.TupleScheme<ListMovieResult> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, ListMovieResult struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol oprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      java.util.BitSet optionals = new java.util.BitSet();
      if (struct.isSetMovieInfoList()) {
        optionals.set(0);
      }
      oprot.writeBitSet(optionals, 1);
      if (struct.isSetMovieInfoList()) {
        {
          oprot.writeI32(struct.movieInfoList.size());
          for (MovieInfo _iter60 : struct.movieInfoList)
          {
            _iter60.write(oprot);
          }
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, ListMovieResult struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol iprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      java.util.BitSet incoming = iprot.readBitSet(1);
      if (incoming.get(0)) {
        {
          org.apache.thrift.protocol.TList _list61 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
          struct.movieInfoList = new java.util.ArrayList<MovieInfo>(_list61.size);
          @org.apache.thrift.annotation.Nullable MovieInfo _elem62;
          for (int _i63 = 0; _i63 < _list61.size; ++_i63)
          {
            _elem62 = new MovieInfo();
            _elem62.read(iprot);
            struct.movieInfoList.add(_elem62);
          }
        }
        struct.setMovieInfoListIsSet(true);
      }
    }
  }

  private static <S extends org.apache.thrift.scheme.IScheme> S scheme(org.apache.thrift.protocol.TProtocol proto) {
    return (org.apache.thrift.scheme.StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
  }
}

