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

import com.cube.dao.RRBase.RRMatchSpec.MatchType;
import com.cube.drivers.Analysis.ReqMatchType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class Request extends RRBase {
	/**
	 * @param path
	 * @param id
	 * @param qparams
	 * @param meta
	 * @param hdrs
	 * @param body
	 */
	public Request(String path, Optional<String> reqid, 
			MultivaluedMap<String, String> qparams,
			MultivaluedMap<String, String> fparams,
			MultivaluedMap<String, String> meta, 
			MultivaluedMap<String, String> hdrs, 
			String method, 
			String body,
			Optional<String> collection,
			Optional<Instant> timestamp, 
			Optional<RR> rrtype, 
			Optional<String> customerid,
			Optional<String> app) {
		super(reqid, meta, hdrs, body, collection, timestamp, rrtype, customerid, app);
		this.path = path; 
		this.qparams = qparams != null ? qparams : emptyMap();
		this.fparams = fparams != null ? fparams : emptyMap();
		this.method = method;
	}
	
	
	
	/**
	 * @param path
	 * @param qparams
	 * @param fparams
	 */
	public Request(String path, Optional<String> id, 
			MultivaluedMap<String, String> qparams, 
			MultivaluedMap<String, String> fparams, 
			MultivaluedMap<String, String> hdrs, 
			String service, 
			Optional<String> collection, 
			Optional<RR> rrtype, 
			Optional<String> customerid,
			Optional<String> app) {
		this(path, id, qparams, fparams, emptyMap(), 
				hdrs, "", "", collection, Optional.empty(), rrtype, customerid, app);
		meta.add(RRBase.SERVICEFIELD, service);
	}

	public Request(Optional<String> serviceid, 
			String path,
			String method,
			Optional<RR> rrtype,
			Optional<String> customerid,
			Optional<String> app) {
		this(path, Optional.empty(), emptyMap(), emptyMap(), emptyMap(), 
				emptyMap(), method, "", Optional.empty(), Optional.empty(), rrtype, customerid, app);
		serviceid.ifPresent(s -> setService(s));
	}
	
	
	
	/**
	 * For jackson json ser/deserialization
	 */
	@SuppressWarnings("unused")
	private Request() {
		super();
		this.path = ""; 
		this.qparams = new MultivaluedHashMap<String, String>();
		this.fparams = new MultivaluedHashMap<String, String>();
		this.method = "";
	}



	static final TypeReference<MultivaluedHashMap<String, String>> typeRef 
	  = new TypeReference<MultivaluedHashMap<String, String>>() {};
	
	public final String path;
    @JsonDeserialize(as=MultivaluedHashMap.class)
	public final MultivaluedMap<String, String> qparams; // query params
    @JsonDeserialize(as=MultivaluedHashMap.class)
	public final MultivaluedMap<String, String> fparams; // form params
	public final String method;

	private static MultivaluedHashMap<String, String> emptyMap () {
		return new MultivaluedHashMap<String, String>();
	}
	
	public static class ReqMatchSpec extends RRMatchSpec {
		
		
		
		/**
		 * @param mpath
		 * @param mqparams
		 * @param qparamfields
		 * @param mfparams
		 * @param fparamfields
		 * @param mmethod
		 */
		private ReqMatchSpec(MatchType mreqid, MatchType mmeta, List<String> metafields, MatchType mhdrs,
				List<String> hdrfields, MatchType mbody, MatchType mcollection, MatchType mtimestamp, MatchType mrrtype,
				MatchType mcustomerid, MatchType mapp, MatchType mpath, MatchType mqparams, List<String> qparamfields, MatchType mfparams,
				List<String> fparamfields, MatchType mmethod) {
			super(mreqid, mmeta, metafields, mhdrs, hdrfields, mbody, mcollection, mtimestamp, mrrtype, mcustomerid, mapp);
			this.mpath = mpath;
			this.mqparams = mqparams;
			this.qparamfields = qparamfields;
			this.mfparams = mfparams;
			this.fparamfields = fparamfields;
			this.mmethod = mmethod;
		}
		
		final MatchType mpath;
		final MatchType mqparams;
		final List<String> qparamfields;
		final MatchType mfparams;
		final List<String> fparamfields;
		final MatchType mmethod;
		
		@Generated("SparkTools")
		private ReqMatchSpec(Builder builder) {
			super(builder);
			this.mpath = builder.mpath;
			this.mqparams = builder.mqparams;
			this.qparamfields = builder.qparamfields;
			this.mfparams = builder.mfparams;
			this.fparamfields = builder.fparamfields;
			this.mmethod = builder.mmethod;
		}
		/**
		 * Creates builder to build {@link ReqMatchSpec}.
		 * @return created builder
		 */
		@Generated("SparkTools")
		public static Builder builder() {
			return new Builder();
		}
		/**
		 * Builder to build {@link ReqMatchSpec}.
		 */
		@Generated("SparkTools")
		public static final class Builder extends RRBase.RRMatchSpec.Builder {
			private MatchType mpath = MatchType.NONE;
			private MatchType mqparams = MatchType.NONE;
			private List<String> qparamfields = Collections.emptyList();
			private MatchType mfparams = MatchType.NONE;
			private List<String> fparamfields = Collections.emptyList();
			private MatchType mmethod = MatchType.NONE;

			private Builder() {
				super();
			}

			public Builder withMpath(MatchType mpath) {
				this.mpath = mpath;
				return this;
			}

			public Builder withMqparams(MatchType mqparams) {
				this.mqparams = mqparams;
				return this;
			}

			public Builder withQparamfields(List<String> qparamfields) {
				this.qparamfields = qparamfields;
				return this;
			}

			public Builder withMfparams(MatchType mfparams) {
				this.mfparams = mfparams;
				return this;
			}

			public Builder withFparamfields(List<String> fparamfields) {
				this.fparamfields = fparamfields;
				return this;
			}

			public Builder withMmethod(MatchType mmethod) {
				this.mmethod = mmethod;
				return this;
			}

			public ReqMatchSpec build() {
				return new ReqMatchSpec(this);
			}
		}
	}

	/**
	 * This function is used for checking if results returned from the store actually matched completely
	 * Relevant when the matching spec allows for optional matching of some fields
	 * @param replayreq
	 * @param mspec
	 * @return
	 */
	public ReqMatchType compare(Request other, ReqMatchSpec mspec) {

		ReqMatchType ret = ReqMatchType.ExactMatch;
		
		if ((ret = ret.And(checkMatch(mspec.mreqid, reqid, other.reqid))) == ReqMatchType.NoMatch) return ret;
		if ((ret = ret.And(checkMatch(mspec.mmeta, meta, other.meta, mspec.metafields))) == ReqMatchType.NoMatch) return ret;
		if ((ret = ret.And(checkMatch(mspec.mhdrs, hdrs, other.hdrs, mspec.hdrfields))) == ReqMatchType.NoMatch) return ret;
		if ((ret = ret.And(checkMatch(mspec.mbody, body, other.body))) == ReqMatchType.NoMatch) return ret;
		if ((ret = ret.And(checkMatch(mspec.mcollection, collection, other.collection))) == ReqMatchType.NoMatch) return ret;
		if ((ret = ret.And(checkMatch(mspec.mtimestamp, timestamp, other.timestamp))) == ReqMatchType.NoMatch) return ret;
		if ((ret = ret.And(checkMatch(mspec.mrrtype, rrtype, other.rrtype))) == ReqMatchType.NoMatch) return ret;
		if ((ret = ret.And(checkMatch(mspec.mcustomerid, customerid, other.customerid))) == ReqMatchType.NoMatch) return ret;
		if ((ret = ret.And(checkMatch(mspec.mapp, app, other.app))) == ReqMatchType.NoMatch) return ret;

		if ((ret = ret.And(checkMatch(mspec.mpath, path, other.path))) == ReqMatchType.NoMatch) return ret;
		if ((ret = ret.And(checkMatch(mspec.mqparams, qparams, other.qparams, mspec.qparamfields))) == ReqMatchType.NoMatch) return ret;
		if ((ret = ret.And(checkMatch(mspec.mfparams, fparams, other.fparams, mspec.fparamfields))) == ReqMatchType.NoMatch) return ret;
		if ((ret = ret.And(checkMatch(mspec.mmethod, method, other.method))) == ReqMatchType.NoMatch) return ret;
		
		return ret;

	}
	
	/**
	 * @param mt
	 * @param thisfvals
	 * @param fvals
	 * @return
	 */
	private ReqMatchType checkMatch(MatchType mt, MultivaluedMap<String, String> thisfmap,
			MultivaluedMap<String, String> fmap, List<String> fieldstomatch) {

		if (mt == MatchType.FILTER || mt == MatchType.SCORE) {
			ReqMatchType ret = ReqMatchType.ExactMatch;
			for (String f : fieldstomatch) {
				List<String> thisfvals = thisfmap.get(f);
				List<String> fvals = fmap.get(f);
				// check if all values match
				if (!fvals.containsAll(thisfvals)) {
					if (mt == MatchType.SCORE) { // for soft match, its ok to not match on the field val
						ret = ReqMatchType.PartialMatch;
					} else {
						ret = ReqMatchType.NoMatch;
						break;
					}
				}				
			}
			return ret;
		}
		return ReqMatchType.ExactMatch; // default is match 
	}



	// This will work for both String and Optional<String> values
	private static ReqMatchType checkMatch(MatchType mt,  
			Object thisfval, Object fval) {
		
		if (mt == MatchType.FILTER || mt == MatchType.SCORE) {
			if (thisfval.equals(fval)) {
				return ReqMatchType.ExactMatch;
			} 
			if (mt == MatchType.SCORE) {// for soft match, its ok to not match on the field val 
				return ReqMatchType.PartialMatch;
			} 
			return ReqMatchType.NoMatch;
		}
		return ReqMatchType.ExactMatch; // default is match 
	}

}