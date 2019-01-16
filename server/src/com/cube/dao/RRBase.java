/**
 * Copyright Cube I O
 */
package com.cube.dao;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.annotation.Generated;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class RRBase {

	public static enum RR {
		Record,
		Replay
	}
	/**
	 * @param path
	 * @param id
	 * @param qparams
	 * @param meta
	 * @param hdrs
	 * @param body
	 */
	public RRBase(Optional<String> reqid, 
			MultivaluedMap<String, String> meta, 
			MultivaluedMap<String, String> hdrs, 
			String body,
			Optional<String> collection,
			Optional<Instant> timestamp, 
			Optional<String> rrtype, 
			Optional<String> customerid,
			Optional<String> app) {
		super();
		this.reqid = reqid;
		this.meta = meta != null ? meta : new MultivaluedHashMap<String, String>();
		this.hdrs = hdrs != null ? hdrs : new MultivaluedHashMap<String, String>();
		this.body = body;
		this.collection = collection;
		this.timestamp = timestamp;
		this.rrtype = rrtype;
		this.customerid = customerid;
		this.app = app;
	}

	
	/**
	 * For jackson json ser/deserialization
	 */
	@SuppressWarnings("unused") RRBase() {
		super();
		this.reqid = Optional.empty();
		this.meta = new MultivaluedHashMap<String, String>();
		this.hdrs = new MultivaluedHashMap<String, String>();
		this.body = "";
		this.collection = Optional.empty();
		this.timestamp = Optional.empty();
		this.rrtype = Optional.empty();
		this.customerid = Optional.empty();
		this.app = Optional.empty();
	}

	/**
	 * @return
	 */
	@JsonIgnore
	public Optional<String> getService() {
		return Optional.ofNullable(meta.get("service")).flatMap(ss -> ss.stream().findFirst());
	}

	
	public final Optional<String> reqid;
    @JsonDeserialize(as=MultivaluedHashMap.class)
	public final MultivaluedMap<String, String> meta; 
    @JsonDeserialize(as=MultivaluedHashMap.class)
	public final MultivaluedMap<String, String> hdrs;
	public final String body;		
	public final Optional<String> collection;
	public final Optional<Instant> timestamp;
	public final Optional<String> rrtype; // this can be "record" or "replay"
	public final Optional<String> customerid;
	public final Optional<String> app;
	
	
	public static class RRMatchSpec {
		
		public enum MatchType {
			NONE,
			FILTER,
			SCORE
		};
		
		
		
		/**
		 * @param mreqid
		 * @param mmeta
		 * @param metafields
		 * @param mhdrs
		 * @param hdrfields
		 * @param mbody
		 * @param mcollection
		 * @param mtimestamp
		 * @param mrrtype
		 * @param mcustomerid
		 * @param mapp
		 */
		protected RRMatchSpec(MatchType mreqid, MatchType mmeta, List<String> metafields, MatchType mhdrs,
				List<String> hdrfields, MatchType mbody, MatchType mcollection, MatchType mtimestamp, MatchType mrrtype,
				MatchType mcustomerid, MatchType mapp) {
			super();
			this.mreqid = mreqid;
			this.mmeta = mmeta;
			this.metafields = metafields;
			this.mhdrs = mhdrs;
			this.hdrfields = hdrfields;
			this.mbody = mbody;
			this.mcollection = mcollection;
			this.mtimestamp = mtimestamp;
			this.mrrtype = mrrtype;
			this.mcustomerid = mcustomerid;
			this.mapp = mapp;
		}
				
		
		// indicates whether these fields should be matched
		final MatchType mreqid;
		final MatchType mmeta;
		final List<String> metafields;
		final MatchType mhdrs;
		final List<String> hdrfields;
		final MatchType mbody;
		final MatchType mcollection;
		final MatchType mtimestamp;
		final MatchType mrrtype;
		final MatchType mcustomerid;
		final MatchType mapp;
		@Generated("SparkTools")
		protected RRMatchSpec(Builder builder) {
			this.mreqid = builder.mreqid;
			this.mmeta = builder.mmeta;
			this.metafields = builder.metafields;
			this.mhdrs = builder.mhdrs;
			this.hdrfields = builder.hdrfields;
			this.mbody = builder.mbody;
			this.mcollection = builder.mcollection;
			this.mtimestamp = builder.mtimestamp;
			this.mrrtype = builder.mrrtype;
			this.mcustomerid = builder.mcustomerid;
			this.mapp = builder.mapp;
		}
		/**
		 * Creates builder to build {@link RRMatchSpec}.
		 * @return created builder
		 */
		@Generated("SparkTools")
		public static Builder builder() {
			return new Builder();
		}
		/**
		 * Builder to build {@link RRMatchSpec}.
		 */
		@Generated("SparkTools")
		public static class Builder {
			private MatchType mreqid = MatchType.NONE;
			private MatchType mmeta = MatchType.NONE;
			private List<String> metafields = Collections.emptyList();
			private MatchType mhdrs = MatchType.NONE;
			private List<String> hdrfields = Collections.emptyList();
			private MatchType mbody = MatchType.NONE;
			private MatchType mcollection = MatchType.NONE;
			private MatchType mtimestamp = MatchType.NONE;
			private MatchType mrrtype = MatchType.NONE;
			private MatchType mcustomerid = MatchType.NONE;
			private MatchType mapp = MatchType.NONE;

			protected Builder() {
			}

			public Builder withMreqid(MatchType mreqid) {
				this.mreqid = mreqid;
				return this;
			}

			public Builder withMmeta(MatchType mmeta) {
				this.mmeta = mmeta;
				return this;
			}

			public Builder withMetafields(List<String> metafields) {
				this.metafields = metafields;
				return this;
			}

			public Builder withMhdrs(MatchType mhdrs) {
				this.mhdrs = mhdrs;
				return this;
			}

			public Builder withHdrfields(List<String> hdrfields) {
				this.hdrfields = hdrfields;
				return this;
			}

			public Builder withMbody(MatchType mbody) {
				this.mbody = mbody;
				return this;
			}

			public Builder withMcollection(MatchType mcollection) {
				this.mcollection = mcollection;
				return this;
			}

			public Builder withMtimestamp(MatchType mtimestamp) {
				this.mtimestamp = mtimestamp;
				return this;
			}

			public Builder withMrrtype(MatchType mrrtype) {
				this.mrrtype = mrrtype;
				return this;
			}

			public Builder withMcustomerid(MatchType mcustomerid) {
				this.mcustomerid = mcustomerid;
				return this;
			}

			public Builder withMapp(MatchType mapp) {
				this.mapp = mapp;
				return this;
			}

			public RRMatchSpec build() {
				return new RRMatchSpec(this);
			}
		}
		
		
		
	}
}