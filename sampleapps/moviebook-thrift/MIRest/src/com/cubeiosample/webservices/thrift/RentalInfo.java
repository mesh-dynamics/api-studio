/**
 * Autogenerated by Thrift Compiler (0.12.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package com.cubeiosample.webservices.thrift;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked", "unused"})
@javax.annotation.Generated(value = "Autogenerated by Thrift Compiler (0.12.0)", date = "2019-12-02")
public class RentalInfo implements org.apache.thrift.TBase<RentalInfo, RentalInfo._Fields>, java.io.Serializable, Cloneable, Comparable<RentalInfo> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("RentalInfo");

  private static final org.apache.thrift.protocol.TField FILM_ID_FIELD_DESC = new org.apache.thrift.protocol.TField("filmId", org.apache.thrift.protocol.TType.I32, (short)1);
  private static final org.apache.thrift.protocol.TField STORE_ID_FIELD_DESC = new org.apache.thrift.protocol.TField("storeId", org.apache.thrift.protocol.TType.I32, (short)2);
  private static final org.apache.thrift.protocol.TField CUSTOMER_ID_FIELD_DESC = new org.apache.thrift.protocol.TField("customerId", org.apache.thrift.protocol.TType.I32, (short)3);
  private static final org.apache.thrift.protocol.TField DURATION_FIELD_DESC = new org.apache.thrift.protocol.TField("duration", org.apache.thrift.protocol.TType.I32, (short)4);
  private static final org.apache.thrift.protocol.TField STAFF_ID_FIELD_DESC = new org.apache.thrift.protocol.TField("staffId", org.apache.thrift.protocol.TType.I32, (short)5);

  private static final org.apache.thrift.scheme.SchemeFactory STANDARD_SCHEME_FACTORY = new RentalInfoStandardSchemeFactory();
  private static final org.apache.thrift.scheme.SchemeFactory TUPLE_SCHEME_FACTORY = new RentalInfoTupleSchemeFactory();

  public int filmId; // required
  public int storeId; // required
  public int customerId; // required
  public int duration; // required
  public int staffId; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    FILM_ID((short)1, "filmId"),
    STORE_ID((short)2, "storeId"),
    CUSTOMER_ID((short)3, "customerId"),
    DURATION((short)4, "duration"),
    STAFF_ID((short)5, "staffId");

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
        case 1: // FILM_ID
          return FILM_ID;
        case 2: // STORE_ID
          return STORE_ID;
        case 3: // CUSTOMER_ID
          return CUSTOMER_ID;
        case 4: // DURATION
          return DURATION;
        case 5: // STAFF_ID
          return STAFF_ID;
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
  private static final int __FILMID_ISSET_ID = 0;
  private static final int __STOREID_ISSET_ID = 1;
  private static final int __CUSTOMERID_ISSET_ID = 2;
  private static final int __DURATION_ISSET_ID = 3;
  private static final int __STAFFID_ISSET_ID = 4;
  private byte __isset_bitfield = 0;
  public static final java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new java.util.EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.FILM_ID, new org.apache.thrift.meta_data.FieldMetaData("filmId", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.STORE_ID, new org.apache.thrift.meta_data.FieldMetaData("storeId", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.CUSTOMER_ID, new org.apache.thrift.meta_data.FieldMetaData("customerId", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.DURATION, new org.apache.thrift.meta_data.FieldMetaData("duration", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.STAFF_ID, new org.apache.thrift.meta_data.FieldMetaData("staffId", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    metaDataMap = java.util.Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(RentalInfo.class, metaDataMap);
  }

  public RentalInfo() {
  }

  public RentalInfo(
    int filmId,
    int storeId,
    int customerId,
    int duration,
    int staffId)
  {
    this();
    this.filmId = filmId;
    setFilmIdIsSet(true);
    this.storeId = storeId;
    setStoreIdIsSet(true);
    this.customerId = customerId;
    setCustomerIdIsSet(true);
    this.duration = duration;
    setDurationIsSet(true);
    this.staffId = staffId;
    setStaffIdIsSet(true);
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public RentalInfo(RentalInfo other) {
    __isset_bitfield = other.__isset_bitfield;
    this.filmId = other.filmId;
    this.storeId = other.storeId;
    this.customerId = other.customerId;
    this.duration = other.duration;
    this.staffId = other.staffId;
  }

  public RentalInfo deepCopy() {
    return new RentalInfo(this);
  }

  @Override
  public void clear() {
    setFilmIdIsSet(false);
    this.filmId = 0;
    setStoreIdIsSet(false);
    this.storeId = 0;
    setCustomerIdIsSet(false);
    this.customerId = 0;
    setDurationIsSet(false);
    this.duration = 0;
    setStaffIdIsSet(false);
    this.staffId = 0;
  }

  public int getFilmId() {
    return this.filmId;
  }

  public RentalInfo setFilmId(int filmId) {
    this.filmId = filmId;
    setFilmIdIsSet(true);
    return this;
  }

  public void unsetFilmId() {
    __isset_bitfield = org.apache.thrift.EncodingUtils.clearBit(__isset_bitfield, __FILMID_ISSET_ID);
  }

  /** Returns true if field filmId is set (has been assigned a value) and false otherwise */
  public boolean isSetFilmId() {
    return org.apache.thrift.EncodingUtils.testBit(__isset_bitfield, __FILMID_ISSET_ID);
  }

  public void setFilmIdIsSet(boolean value) {
    __isset_bitfield = org.apache.thrift.EncodingUtils.setBit(__isset_bitfield, __FILMID_ISSET_ID, value);
  }

  public int getStoreId() {
    return this.storeId;
  }

  public RentalInfo setStoreId(int storeId) {
    this.storeId = storeId;
    setStoreIdIsSet(true);
    return this;
  }

  public void unsetStoreId() {
    __isset_bitfield = org.apache.thrift.EncodingUtils.clearBit(__isset_bitfield, __STOREID_ISSET_ID);
  }

  /** Returns true if field storeId is set (has been assigned a value) and false otherwise */
  public boolean isSetStoreId() {
    return org.apache.thrift.EncodingUtils.testBit(__isset_bitfield, __STOREID_ISSET_ID);
  }

  public void setStoreIdIsSet(boolean value) {
    __isset_bitfield = org.apache.thrift.EncodingUtils.setBit(__isset_bitfield, __STOREID_ISSET_ID, value);
  }

  public int getCustomerId() {
    return this.customerId;
  }

  public RentalInfo setCustomerId(int customerId) {
    this.customerId = customerId;
    setCustomerIdIsSet(true);
    return this;
  }

  public void unsetCustomerId() {
    __isset_bitfield = org.apache.thrift.EncodingUtils.clearBit(__isset_bitfield, __CUSTOMERID_ISSET_ID);
  }

  /** Returns true if field customerId is set (has been assigned a value) and false otherwise */
  public boolean isSetCustomerId() {
    return org.apache.thrift.EncodingUtils.testBit(__isset_bitfield, __CUSTOMERID_ISSET_ID);
  }

  public void setCustomerIdIsSet(boolean value) {
    __isset_bitfield = org.apache.thrift.EncodingUtils.setBit(__isset_bitfield, __CUSTOMERID_ISSET_ID, value);
  }

  public int getDuration() {
    return this.duration;
  }

  public RentalInfo setDuration(int duration) {
    this.duration = duration;
    setDurationIsSet(true);
    return this;
  }

  public void unsetDuration() {
    __isset_bitfield = org.apache.thrift.EncodingUtils.clearBit(__isset_bitfield, __DURATION_ISSET_ID);
  }

  /** Returns true if field duration is set (has been assigned a value) and false otherwise */
  public boolean isSetDuration() {
    return org.apache.thrift.EncodingUtils.testBit(__isset_bitfield, __DURATION_ISSET_ID);
  }

  public void setDurationIsSet(boolean value) {
    __isset_bitfield = org.apache.thrift.EncodingUtils.setBit(__isset_bitfield, __DURATION_ISSET_ID, value);
  }

  public int getStaffId() {
    return this.staffId;
  }

  public RentalInfo setStaffId(int staffId) {
    this.staffId = staffId;
    setStaffIdIsSet(true);
    return this;
  }

  public void unsetStaffId() {
    __isset_bitfield = org.apache.thrift.EncodingUtils.clearBit(__isset_bitfield, __STAFFID_ISSET_ID);
  }

  /** Returns true if field staffId is set (has been assigned a value) and false otherwise */
  public boolean isSetStaffId() {
    return org.apache.thrift.EncodingUtils.testBit(__isset_bitfield, __STAFFID_ISSET_ID);
  }

  public void setStaffIdIsSet(boolean value) {
    __isset_bitfield = org.apache.thrift.EncodingUtils.setBit(__isset_bitfield, __STAFFID_ISSET_ID, value);
  }

  public void setFieldValue(_Fields field, @org.apache.thrift.annotation.Nullable Object value) {
    switch (field) {
    case FILM_ID:
      if (value == null) {
        unsetFilmId();
      } else {
        setFilmId((Integer)value);
      }
      break;

    case STORE_ID:
      if (value == null) {
        unsetStoreId();
      } else {
        setStoreId((Integer)value);
      }
      break;

    case CUSTOMER_ID:
      if (value == null) {
        unsetCustomerId();
      } else {
        setCustomerId((Integer)value);
      }
      break;

    case DURATION:
      if (value == null) {
        unsetDuration();
      } else {
        setDuration((Integer)value);
      }
      break;

    case STAFF_ID:
      if (value == null) {
        unsetStaffId();
      } else {
        setStaffId((Integer)value);
      }
      break;

    }
  }

  @org.apache.thrift.annotation.Nullable
  public Object getFieldValue(_Fields field) {
    switch (field) {
    case FILM_ID:
      return getFilmId();

    case STORE_ID:
      return getStoreId();

    case CUSTOMER_ID:
      return getCustomerId();

    case DURATION:
      return getDuration();

    case STAFF_ID:
      return getStaffId();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case FILM_ID:
      return isSetFilmId();
    case STORE_ID:
      return isSetStoreId();
    case CUSTOMER_ID:
      return isSetCustomerId();
    case DURATION:
      return isSetDuration();
    case STAFF_ID:
      return isSetStaffId();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof RentalInfo)
      return this.equals((RentalInfo)that);
    return false;
  }

  public boolean equals(RentalInfo that) {
    if (that == null)
      return false;
    if (this == that)
      return true;

    boolean this_present_filmId = true;
    boolean that_present_filmId = true;
    if (this_present_filmId || that_present_filmId) {
      if (!(this_present_filmId && that_present_filmId))
        return false;
      if (this.filmId != that.filmId)
        return false;
    }

    boolean this_present_storeId = true;
    boolean that_present_storeId = true;
    if (this_present_storeId || that_present_storeId) {
      if (!(this_present_storeId && that_present_storeId))
        return false;
      if (this.storeId != that.storeId)
        return false;
    }

    boolean this_present_customerId = true;
    boolean that_present_customerId = true;
    if (this_present_customerId || that_present_customerId) {
      if (!(this_present_customerId && that_present_customerId))
        return false;
      if (this.customerId != that.customerId)
        return false;
    }

    boolean this_present_duration = true;
    boolean that_present_duration = true;
    if (this_present_duration || that_present_duration) {
      if (!(this_present_duration && that_present_duration))
        return false;
      if (this.duration != that.duration)
        return false;
    }

    boolean this_present_staffId = true;
    boolean that_present_staffId = true;
    if (this_present_staffId || that_present_staffId) {
      if (!(this_present_staffId && that_present_staffId))
        return false;
      if (this.staffId != that.staffId)
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = 1;

    hashCode = hashCode * 8191 + filmId;

    hashCode = hashCode * 8191 + storeId;

    hashCode = hashCode * 8191 + customerId;

    hashCode = hashCode * 8191 + duration;

    hashCode = hashCode * 8191 + staffId;

    return hashCode;
  }

  @Override
  public int compareTo(RentalInfo other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetFilmId()).compareTo(other.isSetFilmId());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetFilmId()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.filmId, other.filmId);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetStoreId()).compareTo(other.isSetStoreId());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetStoreId()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.storeId, other.storeId);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetCustomerId()).compareTo(other.isSetCustomerId());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetCustomerId()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.customerId, other.customerId);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetDuration()).compareTo(other.isSetDuration());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetDuration()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.duration, other.duration);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetStaffId()).compareTo(other.isSetStaffId());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetStaffId()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.staffId, other.staffId);
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
    StringBuilder sb = new StringBuilder("RentalInfo(");
    boolean first = true;

    sb.append("filmId:");
    sb.append(this.filmId);
    first = false;
    if (!first) sb.append(", ");
    sb.append("storeId:");
    sb.append(this.storeId);
    first = false;
    if (!first) sb.append(", ");
    sb.append("customerId:");
    sb.append(this.customerId);
    first = false;
    if (!first) sb.append(", ");
    sb.append("duration:");
    sb.append(this.duration);
    first = false;
    if (!first) sb.append(", ");
    sb.append("staffId:");
    sb.append(this.staffId);
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
      // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
      __isset_bitfield = 0;
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class RentalInfoStandardSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public RentalInfoStandardScheme getScheme() {
      return new RentalInfoStandardScheme();
    }
  }

  private static class RentalInfoStandardScheme extends org.apache.thrift.scheme.StandardScheme<RentalInfo> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, RentalInfo struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // FILM_ID
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.filmId = iprot.readI32();
              struct.setFilmIdIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // STORE_ID
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.storeId = iprot.readI32();
              struct.setStoreIdIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // CUSTOMER_ID
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.customerId = iprot.readI32();
              struct.setCustomerIdIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 4: // DURATION
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.duration = iprot.readI32();
              struct.setDurationIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 5: // STAFF_ID
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.staffId = iprot.readI32();
              struct.setStaffIdIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, RentalInfo struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      oprot.writeFieldBegin(FILM_ID_FIELD_DESC);
      oprot.writeI32(struct.filmId);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(STORE_ID_FIELD_DESC);
      oprot.writeI32(struct.storeId);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(CUSTOMER_ID_FIELD_DESC);
      oprot.writeI32(struct.customerId);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(DURATION_FIELD_DESC);
      oprot.writeI32(struct.duration);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(STAFF_ID_FIELD_DESC);
      oprot.writeI32(struct.staffId);
      oprot.writeFieldEnd();
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class RentalInfoTupleSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public RentalInfoTupleScheme getScheme() {
      return new RentalInfoTupleScheme();
    }
  }

  private static class RentalInfoTupleScheme extends org.apache.thrift.scheme.TupleScheme<RentalInfo> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, RentalInfo struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol oprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      java.util.BitSet optionals = new java.util.BitSet();
      if (struct.isSetFilmId()) {
        optionals.set(0);
      }
      if (struct.isSetStoreId()) {
        optionals.set(1);
      }
      if (struct.isSetCustomerId()) {
        optionals.set(2);
      }
      if (struct.isSetDuration()) {
        optionals.set(3);
      }
      if (struct.isSetStaffId()) {
        optionals.set(4);
      }
      oprot.writeBitSet(optionals, 5);
      if (struct.isSetFilmId()) {
        oprot.writeI32(struct.filmId);
      }
      if (struct.isSetStoreId()) {
        oprot.writeI32(struct.storeId);
      }
      if (struct.isSetCustomerId()) {
        oprot.writeI32(struct.customerId);
      }
      if (struct.isSetDuration()) {
        oprot.writeI32(struct.duration);
      }
      if (struct.isSetStaffId()) {
        oprot.writeI32(struct.staffId);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, RentalInfo struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol iprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      java.util.BitSet incoming = iprot.readBitSet(5);
      if (incoming.get(0)) {
        struct.filmId = iprot.readI32();
        struct.setFilmIdIsSet(true);
      }
      if (incoming.get(1)) {
        struct.storeId = iprot.readI32();
        struct.setStoreIdIsSet(true);
      }
      if (incoming.get(2)) {
        struct.customerId = iprot.readI32();
        struct.setCustomerIdIsSet(true);
      }
      if (incoming.get(3)) {
        struct.duration = iprot.readI32();
        struct.setDurationIsSet(true);
      }
      if (incoming.get(4)) {
        struct.staffId = iprot.readI32();
        struct.setStaffIdIsSet(true);
      }
    }
  }

  private static <S extends org.apache.thrift.scheme.IScheme> S scheme(org.apache.thrift.protocol.TProtocol proto) {
    return (org.apache.thrift.scheme.StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
  }
}

