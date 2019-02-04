/**
 * Copyright Cube I O
 */
package com.cube.dao;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;

import com.cube.core.Utils;
import com.cube.dao.RRBase.RR;
import com.cube.dao.RRBase.RRMatchSpec.MatchType;
import com.cube.dao.Recording.RecordingStatus;
import com.cube.dao.Request.ReqMatchSpec;
import com.cube.drivers.Analysis;
import com.cube.drivers.Analysis.ReqMatchType;
import com.cube.drivers.Analysis.ReqRespMatchResult;
import com.cube.drivers.Analysis.RespMatchType;
import com.cube.drivers.Replay;
import com.cube.drivers.Replay.ReplayStatus;
import com.cube.ws.Config;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author prasad
 *
 */
public class ReqRespStoreSolr extends ReqRespStoreImplBase implements ReqRespStore {
	
    private static final Logger LOGGER = LogManager.getLogger(ReqRespStoreSolr.class);

	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#save(com.cube.dao.ReqRespStore.Request)
	 */
	@Override
	public boolean save(Request req) {
		
		SolrInputDocument doc = reqToSolrDoc(req);
		return saveDoc(doc);
	}

	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#save(com.cube.dao.ReqRespStore.Response)
	 */
	@Override
	public boolean save(Response resp) {

		SolrInputDocument doc = respToSolrDoc(resp);
		return saveDoc(doc);
	}

	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#getRequest()
	 * qr - query request
	 */
	@Override
	public Stream<Request> getRequests(Request qr, ReqMatchSpec mspec, Optional<Integer> nummatches) {
		
		final SolrQuery query = reqMatchSpecToSolrQuery(qr, mspec);

		return SolrIterator.getStream(solr, query, nummatches).flatMap(doc -> {
			return docToRequest(doc).stream();
		});			

	}

	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#getRequests(java.lang.String, java.lang.String, java.lang.String, java.lang.Iterable, com.cube.dao.ReqRespStore.RR, com.cube.dao.ReqRespStore.Types)
	 */
	@Override
	public Result<Request> getRequests(String customerid, String app, String collection, 
			List<String> reqids, List<String> paths,
			RRBase.RR rrtype) {

		final SolrQuery query = new SolrQuery("*:*");
		query.addField("*");
		addFilter(query, TYPEF, Types.Request.toString());
		addFilter(query, CUSTOMERIDF, customerid);
		addFilter(query, APPF, app);
		addFilter(query, COLLECTIONF, collection);
		
		
		String reqfilter = reqids.stream().collect(Collectors.joining(" OR ", "(", ")"));
		if (reqids.size() > 0)
			addFilter(query, REQIDF, reqfilter, false);

		String pathfilter = paths.stream().collect(Collectors.joining(" OR ", "(", ")"));
		if (paths.size() > 0)
			addFilter(query, PATHF, pathfilter, false);

		
		query.addFilterQuery(String.format("%s:%s", RRTYPEF, rrtype.toString()));			
				
		
		return SolrIterator.getResults(solr, query, Optional.empty(), ReqRespStoreSolr::docToRequest);
		
	}

	
	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#getResponse(java.lang.String)
	 */
	@Override
	public Optional<Response> getResponse(String reqid) {
		
		final SolrQuery query = new SolrQuery("*:*");
		query.addField("*");
		//query.setRows(1);

		addFilter(query, TYPEF, Types.Response.toString());
		addFilter(query, REQIDF, reqid);

		Optional<Integer> maxresults = Optional.of(1);
		return SolrIterator.getStream(solr, query, maxresults).findFirst().flatMap(doc -> {
			return docToResponse(doc);
		});			

	}

	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#getRespForReq(com.cube.dao.ReqRespStore.Request)
	 */
	@Override
	public Optional<Response> getRespForReq(Request qr, ReqMatchSpec mspec) {
		// Find request, without considering request id
		Optional<Request> req = getRequests(qr, mspec, Optional.of(1)).findFirst();
		return req.flatMap(reqv -> reqv.reqid).flatMap(idv -> {
			return getResponse(idv);
		});
	}
	
	/**
	 * @param solr
	 * @param config 
	 */
	public ReqRespStoreSolr(SolrClient solr, Config config) {
		super();
		this.solr = solr;
		this.config = config;
	}

	private final SolrClient solr;
	private final Config config;
	
	private static final String CPREFIX = "";
	
	private static final String TYPEF = CPREFIX + "type_s";

	// field names in Solr
	private static final String PATHF = CPREFIX + "path_s";
	private static final String REQIDF = CPREFIX + "reqid_s";
	private static final String METHODF = CPREFIX + "method_s";
	private static final String BODYF = CPREFIX + "body_t";
	private static final String COLLECTIONF = CPREFIX + "collection_s";
	private static final String TIMESTAMPF = CPREFIX + "timestamp_dt";
	private static final String RRTYPEF = CPREFIX + "rrtype_s";
	private static final String CUSTOMERIDF = CPREFIX + "customerid_s";
	private static final String APPF = CPREFIX + "app_s";
	private static final String INSTANCEIDF = CPREFIX + "instanceid_s";
	private static final String STATUSF = CPREFIX + "status_i";

	private static String getPrefix(String ftype) {
		return String.format(CPREFIX + "%s_", ftype);
	}
	private static String getSolrFieldName(String ftype, String fname) {
		String prefix = getPrefix(ftype);
		return String.format("%s%s%s", prefix, fname, FSUFFIX);
	}

	private static final String FSUFFIX = "_ss"; // set of strings in Solr
	// ensure that this pattern is consistent with the prefix and suffixes used above
	private static final String patternStr = "^" + CPREFIX + "([^_]+)_(.*)_ss$";
	private static final Pattern pattern = Pattern.compile(patternStr);
	private static final String QPARAMS = "qp"; 
	private static final String FPARAMS = "fp"; 
	private static final String META = "meta"; 
	private static final String HDR = "hdr"; 

	private static void addFilter(SolrQuery query, String fieldname, String fval, boolean quote) {
		String newfval = quote ? String.format("\"%s\"", StringEscapeUtils.escapeJava(fval)) : fval ;
		query.addFilterQuery(String.format("%s:%s", fieldname, newfval));
	}

	
	private static void addFilter(SolrQuery query, String fieldname, String fval) {
		// add quotes by default in case the strings have spaces in them 
		addFilter(query, fieldname, fval, true);
	}
	
	private static void addFilter(SolrQuery query, String fieldname, Optional<String> fval) {
		fval.ifPresent(val -> addFilter(query, fieldname, val));
	}

	private static void addFilter(SolrQuery query, String fieldname, MultivaluedMap<String, String> fvalmap, List<String> fields) {
		// Empty list of selected fields is treated as if all fields are to be added
		Collection<String> ftoadd = (fields.isEmpty()) ? fvalmap.keySet() : fields;
		ftoadd.forEach(k -> {
			String f = getSolrFieldName(fieldname, k);
			Optional.ofNullable(fvalmap.get(k)).ifPresent(vals -> vals.forEach(v -> {
				addFilter(query, f, v);				
			}));
		});
	}

	
	// for predicates in the solr q param. Assumes *:* is already there in the buffer
	private static void addToQryStr(StringBuffer qstr, String fieldname, String fval, boolean quote) {
		String newfval = quote ? String.format("\"%s\"", StringEscapeUtils.escapeJava(fval)) : fval;
		qstr.append(String.format(" OR %s:%s", fieldname, newfval));
	}
	
	
	private static void addToQryStr(StringBuffer qstr, String fieldname, String fval) {
		// add quotes to field vals by default
		addToQryStr(qstr, fieldname, fval, true);
	}
	
	private static void addToQryStr(StringBuffer qstr, String fieldname, Optional<String> fval) {
		fval.ifPresent(val -> addToQryStr(qstr, fieldname, val));
	}

	private static void addToQryStr(StringBuffer qstr, String fieldname, MultivaluedMap<String, String> fvalmap, List<String> fields) {
		// Empty list of selected fields is treated as if all fields are to be added
		Collection<String> ftoadd = (fields.isEmpty()) ? fvalmap.keySet() : fields;
		ftoadd.forEach(k -> {
			String f = getSolrFieldName(fieldname, k);
			Optional.ofNullable(fvalmap.get(k)).ifPresent(vals -> vals.forEach(v -> {
				addToQryStr(qstr, f, v);				
			}));
		});
	}

	private static void addMatch(MatchType mt, SolrQuery query, StringBuffer qparam, String fieldname, String fval) {
		switch (mt) {
			case FILTER: addFilter(query, fieldname, fval); break;
			case SCORE: addToQryStr(qparam, fieldname, fval); break;
			default:
		}
	}
	
	private static void addMatch(MatchType mt, SolrQuery query, StringBuffer qstr, String fieldname, Optional<String> fval) {
		switch (mt) {
			case FILTER: addFilter(query, fieldname, fval); break;
			case SCORE: addToQryStr(qstr, fieldname, fval); break;
			default:
		}
	}

	private static void addMatch(MatchType mt, SolrQuery query, StringBuffer qstr, String fieldname, MultivaluedMap<String, String> fvalmap, List<String> fields) {
		switch (mt) {
			case FILTER: addFilter(query, fieldname, fvalmap, fields); break;
			case SCORE: addToQryStr(qstr, fieldname, fvalmap, fields); break;
			default:
		}
	}
	
	
	private static void setRRFields(RRBase rr, SolrInputDocument doc) {
		
		rr.reqid.ifPresent(id -> doc.setField(REQIDF, id));
		doc.setField(BODYF, rr.body);
		addFieldsToDoc(doc, META, rr.meta);
		addFieldsToDoc(doc, HDR, rr.hdrs);
		rr.collection.ifPresent(c -> doc.setField(COLLECTIONF, c));
		rr.timestamp.ifPresent(t -> doc.setField(TIMESTAMPF, t.toString()));
		rr.rrtype.ifPresent(c -> doc.setField(RRTYPEF, c.toString()));
		rr.customerid.ifPresent(c -> doc.setField(CUSTOMERIDF, c));
		rr.app.ifPresent(c -> doc.setField(APPF, c));
		
	}


	
	private static SolrInputDocument reqToSolrDoc(Request req) {
		final SolrInputDocument doc = new SolrInputDocument();

		setRRFields(req, doc);
		doc.setField(TYPEF, Types.Request.toString());
		doc.setField(PATHF, req.path);
		doc.setField(METHODF, req.method);
		addFieldsToDoc(doc, QPARAMS, req.qparams);
		addFieldsToDoc(doc, FPARAMS, req.fparams);
		
		
		return doc;
	}

	private static void checkAndAddValues(MultivaluedMap<String, String> cont, String key, Object val) {
		if (val instanceof List) {
			@SuppressWarnings("unchecked")
			List<Object> vals = (List<Object>)val;
			vals.forEach(v -> {
				if (v instanceof String)
					cont.add(key, (String)v);
			});
		}
	}

	private static Optional<String> getStrField(SolrDocument doc, String fname) {
		return Optional.ofNullable(doc.get(fname)).flatMap(v -> {
			if (v instanceof String)
				return Optional.of((String) v);
			return Optional.empty();
		});
	}

	private static List<String> getStrFieldMV(SolrDocument doc, String fname) {
		return Optional.ofNullable(doc.get(fname)).flatMap(v -> {
			@SuppressWarnings("unchecked")
			Optional<List<String>> vals = (v instanceof List<?>) ? Optional.of((List<String>)v) : Optional.empty();
			return vals;
		}).orElse(new ArrayList<String>());
	}

	private static Optional<Integer> getIntField(SolrDocument doc, String fname) {
		return Optional.ofNullable(doc.get(fname)).flatMap(v -> {
			if (v instanceof Integer)
				return Optional.of((Integer) v);
			return Optional.empty();
		});
	}

	private static Optional<Instant> getTSField(SolrDocument doc, String fname) {
		return Optional.ofNullable(doc.get(fname)).flatMap(v -> {
			if (v instanceof Date)
				return Optional.of(((Date) v).toInstant());
			return Optional.empty();
		});
	}

	private static Optional<Boolean> getBoolField(SolrDocument doc, String fname) {
		return Optional.ofNullable(doc.get(fname)).flatMap(v -> {
			if (v instanceof Boolean)
				return Optional.of((Boolean) v);
			return Optional.empty();
		});
	}

	
	private static Optional<Request> docToRequest(SolrDocument doc) {
		
		Optional<String> type = getStrField(doc, TYPEF);
		Optional<String> path = getStrField(doc, PATHF);
		Optional<String> reqid = getStrField(doc, REQIDF);
		MultivaluedMap<String, String> qparams = new MultivaluedHashMap<String, String>(); // query params
		MultivaluedMap<String, String> fparams = new MultivaluedHashMap<String, String>(); // form params
		MultivaluedMap<String, String> meta = new MultivaluedHashMap<String, String>(); 
		MultivaluedMap<String, String> hdrs = new MultivaluedHashMap<String, String>();
		Optional<String> method = getStrField(doc, METHODF);		
		Optional<String> body = getStrField(doc, BODYF);
		Optional<String> collection = getStrField(doc, COLLECTIONF);
		Optional<Instant> timestamp = getTSField(doc, TIMESTAMPF);
		Optional<RR> rrtype = getStrField(doc, RRTYPEF).flatMap(rrt -> Utils.valueOf(RR.class, rrt));
		Optional<String> customerid = getStrField(doc, CUSTOMERIDF);
		Optional<String> app = getStrField(doc, APPF);
		
		for (Entry<String, Object> kv : doc) {
			String k = kv.getKey();
			Object v = kv.getValue();
			Matcher m = pattern.matcher(k);
			if (m.find()) {					
				switch (m.group(1)) {
				case QPARAMS:
					checkAndAddValues(qparams, m.group(2), v);
					break;
				case FPARAMS:
					checkAndAddValues(fparams, m.group(2), v);
					break;
				case META:
					checkAndAddValues(meta, m.group(2), v);
					break;
				case HDR:
					checkAndAddValues(hdrs, m.group(2), v);
					break;
				default:
				}
			}
		};

		final String p = path.orElse("");
		final String m = method.orElse("");		
		final String b = body.orElse("");
		return type.map(t -> {
			if (t.equals(Types.Request.toString()))
				return new Request(p, reqid, qparams, fparams, meta, hdrs, m, b, collection, timestamp, rrtype, customerid, app);
			else
				return null;
		});
	}

	
	private static SolrInputDocument respToSolrDoc(Response resp) {
		final SolrInputDocument doc = new SolrInputDocument();

		setRRFields(resp, doc);
		doc.setField(TYPEF, Types.Response.toString());
		doc.setField(STATUSF, resp.status);
		
		return doc;
	}

	
	private static Optional<Response> docToResponse(SolrDocument doc) {
		
		Optional<String> type = getStrField(doc, TYPEF);
		Optional<Integer> status = getIntField(doc, STATUSF);
		Optional<String> reqid = getStrField(doc, REQIDF);
		MultivaluedMap<String, String> meta = new MultivaluedHashMap<String, String>(); 
		MultivaluedMap<String, String> hdrs = new MultivaluedHashMap<String, String>();
		Optional<String> body = getStrField(doc, BODYF);
		Optional<String> collection = getStrField(doc, COLLECTIONF);
		Optional<Instant> timestamp = getTSField(doc, TIMESTAMPF);
		Optional<RR> rrtype = getStrField(doc, RRTYPEF).flatMap(rrt -> Utils.valueOf(RR.class, rrt));
		Optional<String> customerid = getStrField(doc, CUSTOMERIDF);
		Optional<String> app = getStrField(doc, APPF);
		
		
		for (Entry<String, Object> kv : doc) {
			String k = kv.getKey();
			Object v = kv.getValue();
			Matcher m = pattern.matcher(k);
			if (m.find()) {					
				switch (m.group(1)) {
				case META:
					checkAndAddValues(meta, m.group(2), v);
					break;
				case HDR:
					checkAndAddValues(hdrs, m.group(2), v);
					break;
				default:
				}
			}
		};

		final String b = body.orElse("");
		return type.flatMap(t -> {
			if (t.equals(Types.Response.toString())) {
				return status.map(sv ->{
					return new Response(reqid, sv, meta, hdrs, b, collection, timestamp,rrtype, customerid, app);
				});				
			} else
				return Optional.empty();
		});
	}

	
	private static void addFieldsToDoc(SolrInputDocument doc, 
			String ftype, MultivaluedMap<String, String> fields) {
		
		fields.forEach((f, vl) -> {
			final String fname = getSolrFieldName(ftype, f);
			vl.forEach((v) -> {
				doc.addField(fname, v);
			});
		});
	}

	private boolean saveDoc(SolrInputDocument doc) {

		try {
			solr.add(doc);
		} catch (SolrServerException | IOException e) {
			LOGGER.error("Error in saving response", e);
			return false;
		}		
		return true;
	}
	
	// field names in Solr for Replay object
	private static final String IDF = "id";
	private static final String ENDPOINTF = CPREFIX + "endpoint_s";
	private static final String REQIDSF = CPREFIX + "reqid_ss";
	private static final String REPLAYIDF = CPREFIX + "replayid_s";
	private static final String ASYNCF = CPREFIX + "async_b";
	private static final String REPLAYSTATUSF = CPREFIX + "status_s";
	private static final String PATHSF = CPREFIX + "path_ss";
	private static final String REQCNTF = CPREFIX + "reqcnt_i";
	private static final String REQSENTF = CPREFIX + "reqsent_i";
	private static final String REQFAILEDF = CPREFIX + "reqfailed_i";

	
	private static SolrInputDocument replayToSolrDoc(Replay replay) {
		final SolrInputDocument doc = new SolrInputDocument();

		String type = Types.ReplayMeta.toString();
		String id = type + '-' + replay.replayid; 
		// the id field is set to replay id so that the document can be updated based on id
		doc.setField(IDF, id);
		doc.setField(APPF, replay.app);
		doc.setField(ASYNCF, replay.async);
		doc.setField(COLLECTIONF, replay.collection);
		doc.setField(CUSTOMERIDF, replay.customerid);
		doc.setField(INSTANCEIDF, replay.instanceid);
		doc.setField(ENDPOINTF, replay.endpoint);
		doc.setField(REPLAYIDF, replay.replayid);
		replay.reqids.forEach(reqid -> doc.addField(REQIDSF, reqid));
		doc.setField(REPLAYSTATUSF, replay.status.toString());
		doc.setField(TYPEF, type);
		replay.paths.forEach(path -> doc.addField(PATHSF, path));
		doc.setField(REQCNTF, replay.reqcnt);
		doc.setField(REQSENTF, replay.reqsent);
		doc.setField(REQFAILEDF, replay.reqfailed);
		
		return doc;
	}

	private static Optional<Replay> docToReplay(SolrDocument doc, ReqRespStore rrstore) {
		
		Optional<String> app = getStrField(doc, APPF);
		Optional<String> instanceid = getStrField(doc, INSTANCEIDF);
		Optional<Boolean> async = getBoolField(doc, ASYNCF);
		Optional<String> collection = getStrField(doc, COLLECTIONF);
		Optional<String> customerid = getStrField(doc, CUSTOMERIDF);
		Optional<String> endpoint = getStrField(doc, ENDPOINTF);
		Optional<String> replayid = getStrField(doc, REPLAYIDF);
		List<String> reqids = getStrFieldMV(doc, REQIDSF);
		Optional<ReplayStatus> status = getStrField(doc, REPLAYSTATUSF).flatMap(s -> Utils.valueOf(ReplayStatus.class, s));
		List<String> paths = getStrFieldMV(doc, PATHSF);
		int reqcnt = getIntField(doc, REQCNTF).orElse(0);
		int reqsent = getIntField(doc, REQSENTF).orElse(0);
		int reqfailed = getIntField(doc, REQFAILEDF).orElse(0);
		
		Optional<Replay> replay = Optional.empty();
		if (endpoint.isPresent() && customerid.isPresent() && app.isPresent() && 
				instanceid.isPresent() && collection.isPresent() 
				&& replayid.isPresent() && async.isPresent() && status.isPresent()) {
			replay = Optional.of(new Replay(endpoint.get(), customerid.get(), app.get(), instanceid.get(), collection.get(), 
					reqids, rrstore, replayid.get(), async.get(), status.get(), paths, reqcnt, reqsent, reqfailed));
		} else {
			LOGGER.error(String.format("Not able to convert Solr result to Replay object for replay id %s", replayid.orElse("")));
		}
		
		return replay;
	}


	private boolean softcommit() {
		try {
			solr.commit(false, true, true);
		} catch (SolrServerException | IOException e) {
			LOGGER.error("Error in commiting to Solr", e);
			return false;
		}
		
		return true;
	}

	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#saveReplay(com.cube.drivers.Replay)
	 */
	@Override
	public boolean saveReplay(Replay replay) {
		super.saveReplay(replay);
		SolrInputDocument doc = replayToSolrDoc(replay);
		return saveDoc(doc) && softcommit();
	}

	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#getReplay(java.lang.String)
	 */
	@Override
	public Optional<Replay> getReplay(String replayid) {
		final SolrQuery query = new SolrQuery("*:*");
		query.addField("*");
		//query.setRows(1);
		addFilter(query, TYPEF, Types.ReplayMeta.toString());
		addFilter(query, REPLAYIDF, replayid);
		
		Optional<Integer> maxresults = Optional.of(1);
		return SolrIterator.getStream(solr, query, maxresults).findFirst().flatMap(doc -> {
			return docToReplay(doc, this);
		});			
		
	}
	
	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#getReplay(java.util.Optional, java.util.Optional, java.util.Optional, com.cube.drivers.Replay.ReplayStatus)
	 */
	@Override
	public Stream<Replay> getReplay(Optional<String> customerid, Optional<String> app, Optional<String> instanceid,
			ReplayStatus status) {

		final SolrQuery query = new SolrQuery("*:*");
		query.addField("*");
		addFilter(query, TYPEF, Types.ReplayMeta.toString());
		addFilter(query, CUSTOMERIDF, customerid);
		addFilter(query, APPF, app);
		addFilter(query, INSTANCEIDF, instanceid);
		addFilter(query, REPLAYSTATUSF, status.toString());
		
		Optional<Integer> maxresults = Optional.of(1);
		return SolrIterator.getStream(solr, query, maxresults).flatMap(doc -> {
			return docToReplay(doc, this).stream();
		});			

	}
	
	// Some useful functions
	public static SolrQuery reqMatchSpecToSolrQuery(Request qr, ReqMatchSpec spec) {
		final SolrQuery query = new SolrQuery("*:*");
		final StringBuffer qstr = new StringBuffer("*:*");
		query.addField("*");
		
		addMatch(spec.mreqid, query, qstr, REQIDF, qr.reqid);
		addMatch(spec.mmeta, query, qstr, META, qr.meta, spec.metafields);
		addMatch(spec.mhdrs, query, qstr, HDR, qr.hdrs, spec.hdrfields);
		addMatch(spec.mbody, query, qstr, BODYF, qr.body);
		addMatch(spec.mcollection, query, qstr, COLLECTIONF, qr.collection);
		addMatch(spec.mtimestamp, query, qstr, TIMESTAMPF, qr.timestamp.toString());
		addMatch(spec.mrrtype, query, qstr, RRTYPEF, qr.rrtype.map(rrt -> rrt.toString()));
		addMatch(spec.mcustomerid, query, qstr, CUSTOMERIDF, qr.customerid);
		addMatch(spec.mapp, query, qstr, APPF, qr.app);

		addMatch(spec.mpath, query, qstr, PATHF, qr.path);		
		addMatch(spec.mqparams, query, qstr, QPARAMS, qr.qparams, spec.qparamfields);
		addMatch(spec.mfparams, query, qstr, FPARAMS, qr.fparams, spec.fparamfields);
		addMatch(spec.mmethod, query, qstr, METHODF, qr.method);		
		
		addFilter(query, TYPEF, Types.Request.toString());
		
		query.setQuery(qstr.toString());
		return query;
	}

	private static final String OBJJSONF = CPREFIX + "json_ni";
	
	
	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#saveAnalysis(com.cube.drivers.Analysis)
	 */
	@Override
	public boolean saveAnalysis(Analysis analysis) {
		SolrInputDocument doc = analysisToSolrDoc(analysis);
		return saveDoc(doc) && softcommit();
	}

	/**
	 * @param analysis
	 * @return
	 */
	private SolrInputDocument analysisToSolrDoc(Analysis analysis) {
		final SolrInputDocument doc = new SolrInputDocument();

		String json="";
		try {
			json = config.jsonmapper.writeValueAsString(analysis);
		} catch (JsonProcessingException e) {
			LOGGER.error(String.format("Error in converting Analysis object into string for replay id %d", analysis.replayid), e);
		}
		
		String type = Types.Analysis.toString();
		String id = type + '-' + analysis.replayid; 
		// the id field is set to replay id so that the document can be updated based on id
		doc.setField(IDF, id);
		doc.setField(REPLAYIDF, analysis.replayid);
		doc.setField(OBJJSONF, json);
		doc.setField(TYPEF, type);
				
		return doc;
	}

	private static final String RECORDREQIDF = CPREFIX + "recordreqid_s";
	private static final String REPLAYREQIDF = CPREFIX + "replayreqid_s";
	private static final String REQMTF = CPREFIX + "reqmt_s";
	private static final String NUMMATCHF = CPREFIX + "nummatch_i";
	private static final String RESPMTF = CPREFIX + "respmt_s"; // match type
	private static final String RESPMATCHMETADATAF = CPREFIX + "respmatchmetadata_ni";
	private static final String SERVICEF = CPREFIX + "service_s";
	
	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#saveResult(com.cube.drivers.Analysis.Result)
	 */
	@Override
	public boolean saveResult(ReqRespMatchResult res) {
		SolrInputDocument doc = resultToSolrDoc(res);
		return saveDoc(doc);		
	}

	/**
	 * @param res
	 * @return
	 */
	private SolrInputDocument resultToSolrDoc(ReqRespMatchResult res) {
		final SolrInputDocument doc = new SolrInputDocument();

		// usually result will never be updated. But we set id field uniquely anyway
		
		String type = Types.ReqRespMatchResult.toString();
		// the id field is to (recordreqid, replayreqid) which is unique
		String id = type + '-' + res.recordreqid + '-' + res.replayreqid; 

		doc.setField(TYPEF, type);
		doc.setField(IDF, id);
		doc.setField(RECORDREQIDF, res.recordreqid);
		doc.setField(REPLAYREQIDF, res.replayreqid);
		doc.setField(REQMTF, res.reqmt.toString());
		doc.setField(NUMMATCHF, res.nummatch);
		doc.setField(RESPMTF, res.respmt.toString());
		doc.setField(RESPMATCHMETADATAF, res.respmatchmetadata);
		doc.setField(CUSTOMERIDF, res.customerid);
		doc.setField(APPF, res.app);
		doc.setField(SERVICEF, res.service);
		doc.setField(PATHF, res.path);
		doc.setField(REPLAYIDF, res.replayid);
				
		return doc;
	}

	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#getAnalysis(java.lang.String)
	 */
	@Override
	public Optional<Analysis> getAnalysis(String replayid) {
		final SolrQuery query = new SolrQuery("*:*");
		query.addField("*");
		//query.setRows(1);
		addFilter(query, TYPEF, Types.Analysis.toString());
		addFilter(query, REPLAYIDF, replayid);
		
		Optional<Integer> maxresults = Optional.of(1);
		return SolrIterator.getStream(solr, query, maxresults).findFirst().flatMap(doc -> {
			return docToAnalysis(doc, this);
		});			
		
	}

	/**
	 * @param doc
	 * @param reqRespStoreSolr
	 * @return
	 */
	private Optional<Analysis> docToAnalysis(SolrDocument doc, ReqRespStoreSolr rrstore) {
		
		Optional<String> json = getStrFieldMV(doc, OBJJSONF).stream().findFirst();
		Optional<Analysis> analysis = json.flatMap(j -> {
			try {
				return Optional.ofNullable(config.jsonmapper.readValue(j, Analysis.class));
			} catch (IOException e) {
				LOGGER.error(String.format("Not able to parse json into Analysis object: %s", j), e);
				return Optional.empty();
			}
		});
		return analysis;
	}

	private static final String RECORDINGSTATUSF = CPREFIX + "status_s";


	private static Optional<Recording> docToRecording(SolrDocument doc) {
		
		Optional<String> app = getStrField(doc, APPF);
		Optional<String> instanceid = getStrField(doc, INSTANCEIDF);
		Optional<String> collection = getStrField(doc, COLLECTIONF);
		Optional<String> customerid = getStrField(doc, CUSTOMERIDF);
		Optional<RecordingStatus> status = getStrField(doc, RECORDINGSTATUSF).flatMap(s -> Utils.valueOf(RecordingStatus.class, s));
		
		Optional<Recording> recording = Optional.empty();
		if (customerid.isPresent() && app.isPresent() 
				&& instanceid.isPresent() && collection.isPresent() && status.isPresent()) {
			recording = Optional.of(new Recording(customerid.get(), app.get(), instanceid.get(), collection.get(), 
					status.get()));
		} else {
			LOGGER.error(String.format("Not able to convert Solr result to Recording object for customerid %s, app id %s, instance id %s", 
					customerid.orElse(""), app.orElse(""), instanceid.orElse("")));
		}
		
		return recording;
	}

	private static SolrInputDocument recordingToSolrDoc(Recording recording) {
		final SolrInputDocument doc = new SolrInputDocument();

		String type = Types.Recording.toString();
		// the id field is to (cust app, collection) which is unique
		String id = type + '-' + recording.customerid + '-' + recording.app + '-' + recording.collection; 

		doc.setField(TYPEF, type);
		doc.setField(IDF, id);
		doc.setField(CUSTOMERIDF, recording.customerid);
		doc.setField(APPF, recording.app);
		doc.setField(INSTANCEIDF, recording.instanceid);
		doc.setField(COLLECTIONF, recording.collection);
		doc.setField(RECORDINGSTATUSF, recording.status.toString());
		
		return doc;
	}


	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#saveReplay(com.cube.drivers.Replay)
	 */
	@Override
	public boolean saveRecording(Recording recording) {
		super.saveRecording(recording);
		SolrInputDocument doc = recordingToSolrDoc(recording);
		return saveDoc(doc) && softcommit();
	}

	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#getRecording(java.util.Optional, java.util.Optional, java.util.Optional, com.cube.dao.Recording.RecordingStatus)
	 */
	@Override
	public Stream<Recording> getRecording(Optional<String> customerid, Optional<String> app,
			Optional<String> instanceid, Optional<RecordingStatus> status) {

		final SolrQuery query = new SolrQuery("*:*");
		query.addField("*");
		addFilter(query, TYPEF, Types.Recording.toString());
		addFilter(query, CUSTOMERIDF, customerid);
		addFilter(query, APPF, app);
		addFilter(query, INSTANCEIDF, instanceid);
		addFilter(query, RECORDINGSTATUSF, status.map(s -> s.toString()));
		
		//Optional<Integer> maxresults = Optional.of(1);
		return SolrIterator.getStream(solr, query, Optional.empty()).flatMap(doc -> {
			return docToRecording(doc).stream();
		});			
	}
		

	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#getRecordingByCollection(java.lang.String, java.lang.String, java.lang.String)
	 * (cust, app, collection) is a unique key, so only record will satisfy at most
	 */
	@Override
	public Optional<Recording> getRecordingByCollection(String customerid, String app, String collection) {
		final SolrQuery query = new SolrQuery("*:*");
		query.addField("*");
		addFilter(query, TYPEF, Types.Recording.toString());
		addFilter(query, CUSTOMERIDF, customerid);
		addFilter(query, APPF, app);
		addFilter(query, COLLECTIONF, collection);
		
		Optional<Integer> maxresults = Optional.of(1);
		return SolrIterator.getStream(solr, query, maxresults).findFirst().flatMap(doc -> {
			return docToRecording(doc);
		});			
	}

	final static int FACETLIMIT = 100;
	private static final String REQMTFACET = "reqmt_facets";
	private static final String RESPMTFACET = "respmt_facets";
	private static final String PATHFACET = "path_facets";
	private static final String SERVICEFACET = "service_facets";
	private static final String SOLRJSONFACETPARAM = "json.facet"; // solr facet query param
	private static final String BUCKETFIELD = "buckets"; // term in solr results indicating facet buckets
	private static final String VALFIELD = "val"; // term in solr facet results indicating a distinct value of the field   
	private static final String COUNTFIELD = "count"; // term in solr facet results indicating aggregate value computed
	private static final String FACETSFIELD = "facets"; // term in solr facet results indicating the facet results block
	
	
	/**
	 * Results needed in one instance of MatchResultAggregate come as part of different facets. These
	 * are accumulated into one object by using a map whose key is of type FacetResKey 
	 *
	 */
	static class FacetResKey {		
		
		/**
		 * @param service
		 * @param path
		 */
		private FacetResKey(Optional<String> service, Optional<String> path) {
			super();
			this.service = service;
			this.path = path;
		}



		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((path == null) ? 0 : path.hashCode());
			result = prime * result + ((service == null) ? 0 : service.hashCode());
			return result;
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			FacetResKey other = (FacetResKey) obj;
			if (path == null) {
				if (other.path != null)
					return false;
			} else if (!path.equals(other.path))
				return false;
			if (service == null) {
				if (other.service != null)
					return false;
			} else if (!service.equals(other.service))
				return false;
			return true;
		}



		public Optional<String> service = Optional.empty();
		public Optional<String> path = Optional.empty();
	}
	
	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#getResultAggregate(java.lang.String, java.util.Optional)
	 */
	@Override
	public Collection<MatchResultAggregate> getResultAggregate(String replayid, 
			Optional<String> service, boolean facetpath) {
		
		Optional<Replay> replay = getReplay(replayid);
		Map<FacetResKey, MatchResultAggregate> resMap = new HashMap<FacetResKey, MatchResultAggregate>();
		
		replay.ifPresent(replayv -> {
			final SolrQuery query = new SolrQuery("*:*");
			query.addField("*");
			addFilter(query, TYPEF, Types.ReqRespMatchResult.toString());
			addFilter(query, REPLAYIDF, replayv.replayid);
			service.ifPresent(servicev -> {
				addFilter(query, SERVICEF, servicev);
			});
			
			// First generate the solr facet query with appropriate nested facets
			FacetQ facetq = new FacetQ();
			List<List<String>> facetFields = new ArrayList<List<String>>();
			Facet reqmatchf = Facet.createTermFacet(REQMTF, Optional.of(FACETLIMIT));
			Facet respmatchf = Facet.createTermFacet(RESPMTF, Optional.of(FACETLIMIT));
			facetq.addFacet(REQMTFACET, reqmatchf);
			facetq.addFacet(RESPMTFACET, respmatchf);
			facetFields.add(List.of(REQMTFACET, RESPMTFACET));
			
			List<FacetQ> otherfacets = new ArrayList<FacetQ>();
			
			if (facetpath) {
				Facet pathf = Facet.createTermFacet(PATHF, Optional.of(FACETLIMIT));
				FacetQ pathfq = new FacetQ();
				pathfq.addFacet(PATHFACET, pathf);
				otherfacets.add(pathfq);		
				facetFields.add(List.of(PATHFACET));
			}
			if (service.isEmpty()) {
				// facet on service as well
				Facet servicef = Facet.createTermFacet(SERVICEF, Optional.of(FACETLIMIT));
				FacetQ servicefq = new FacetQ();
				servicefq.addFacet(SERVICEFACET, servicef);
				otherfacets.add(servicefq);
				facetFields.add(List.of(SERVICEFACET));
			}
			
			facetq.nest(otherfacets);
			
			String json="";
			try {
				json = config.jsonmapper.writeValueAsString(facetq);
				query.add(SOLRJSONFACETPARAM, json);
			} catch (JsonProcessingException e) {
				LOGGER.error(String.format("Error in converting facets to json"), e);
			}

			// we don't need any results, set to 1
			query.setRows(1);
			SolrIterator.runQuery(solr, query).ifPresent(response -> {
				getNLFromNL(response.getResponse(), FACETSFIELD).ifPresent(facets -> {
					
					// facet results will be nested since we used nested facets
					// first flatten the results into a tabular structure
					flatten(facets, facetFields).forEach(fr -> {
						
						// combine facet results into MatchResultAggregate objects by using FacetResKey
						FacetResKey frkey = fr.toFacetResKey();
						Optional.ofNullable(resMap.get(frkey)).ifPresentOrElse(mra -> {
							updateMatchResult(mra, fr);
						}, () -> {
							MatchResultAggregate mra = new MatchResultAggregate(replayv.app, replayv.replayid, frkey.service, frkey.path);
							updateMatchResult(mra, fr);
							resMap.put(frkey, mra);
						});
					});
				});							
			});
		});
		
		// if service filter was present initially, add it to all the results, since it was not
		// included in the facet query
		service.ifPresent(servicev -> {
			resMap.forEach((k, mra) -> mra.service = Optional.of(servicev));
		});
		
		return resMap.values();
	}

	/**
	 * @param mra
	 * @param fr
	 */
	private void updateMatchResult(MatchResultAggregate mra, FacetR fr) {

		fr.keys.forEach(frkey -> {
			if (frkey.facetName.equals(REQMTFACET)) {
				Utils.valueOf(ReqMatchType.class, frkey.key).ifPresent(rmt -> {
					switch (rmt) {
						case ExactMatch: mra.reqmatched = fr.val; break;
						case PartialMatch: mra.reqpartiallymatched = fr.val; break;
						case NoMatch: mra.reqnotmatched = fr.val; break;
					}
					
				});
			}
			if (frkey.facetName.equals(RESPMTFACET)) {
				Utils.valueOf(RespMatchType.class, frkey.key).ifPresent(rmt -> {
					switch (rmt) {
						case ExactMatch: mra.reqmatched = fr.val; break;
						case TemplateMatch: mra.reqpartiallymatched = fr.val; break;
						case NoMatch: mra.reqnotmatched = fr.val; break;
					}
					
				});
			}
		});
		
	}

	static class Facet {
		
		private static final String TYPEK = "type";
		private static final String FIELDK = "field";
		private static final String LIMITK = "limit";
		private static final String FACETK = "facet";

		/**
		 * @param params
		 */
		private Facet(Map<String, Object> params) {
			super();
			this.params = params;
		}

		// These annotations are used for Jackson to flatten params while serializing/deserializing
		@JsonAnySetter
		public void addSubFacet(FacetQ subfacet) {
			params.put(FACETK, subfacet);
		}

		// These annotations are used for Jackson to flatten params while serializing/deserializing
		@JsonAnyGetter
		public Map<String, Object> getParams() {
			return params;
		}

		final private Map<String, Object> params;
		
		static public Facet createTermFacet(String fieldname, Optional<Integer> limit) {
			Map<String, Object> params = new HashMap<String, Object>();
			params.put(TYPEK, "terms");
			params.put(FIELDK, fieldname);
			limit.ifPresent(l -> params.put(LIMITK, l));
			
			return new Facet(params);
		}
	}
	
	static class FacetQ {
		
		
		
		/**
		 * 
		 */
		private FacetQ() {
			super();
			facetqs = new HashMap<String, Facet>();
		}

		// These annotations are used for Jackson to flatten params while serializing/deserializing
		@JsonAnySetter
		public void addFacet(String name, Facet facet) {
			facetqs.put(name, facet);
		}
		
		// These annotations are used for Jackson to flatten params while serializing/deserializing
		@JsonAnyGetter
		public Map<String, Facet> getFacetQs() {
			return facetqs;
		}
		
		private void nestFacetQ(FacetQ nestedq) {
			facetqs.forEach((fn, fq) -> fq.addSubFacet(nestedq));
		}
		
		final Map<String, Facet> facetqs;
		
		// create a nested facet query
		public void nest(List<FacetQ> rest) {
			if (rest.size() > 0) {
				FacetQ head = rest.get(0);
				head.nest(rest.subList(1, rest.size()));
				nestFacetQ(head);
			}
		}
	}
	
	static class FacetRKey {
		
		
		
		/**
		 * @param key
		 * @param facetName
		 */
		private FacetRKey(String key, String facetName) {
			super();
			this.key = key;
			this.facetName = facetName;
		}
		
		final String key;
		final String facetName;
	}
	
	// multi-dimensional facet results
	// this represents one entry of the flattened form of solr nested facet results 
	static class FacetR {
		
		
		/**
		 * @param keys
		 * @param val
		 */
		private FacetR(List<FacetRKey> keys, int val) {
			super();
			this.keys = keys;
			this.val = val;
		}
		
		/**
		 * @return
		 */
		public FacetResKey toFacetResKey() {
			Optional<String> service = keys.stream().filter(frkey -> {
				return frkey.facetName.equals(SERVICEFACET);
			}).findFirst().map(frkey -> frkey.key);
			Optional<String> path = keys.stream().filter(frkey -> {
				return frkey.facetName.equals(PATHFACET);
			}).findFirst().map(frkey -> frkey.key);
			
			return new FacetResKey(service, path);
		}

		List<FacetRKey> keys;
		final int val;
		/**
		 * @param rkey
		 */
		public void addKey(FacetRKey rkey) {
			keys.add(rkey);
		}
	}
	
	/* Helper functions to parse solr nested facet results */
	static Optional<NamedList<Object>> objToNL(Object namedlist) {
		if (namedlist instanceof NamedList<?>) {
			@SuppressWarnings("unchecked")
			NamedList<Object> nl = (NamedList<Object>) namedlist;
			return Optional.of(nl);
		}
		return Optional.empty();
	}
	

	static Optional<Object> getObjFromNL(NamedList<Object> namedlist, String name) {
		return objToNL(namedlist).flatMap(nl -> Optional.ofNullable(nl.get(name)));
	}

	@SuppressWarnings("unchecked")
	static Optional<NamedList<Object>> getNLFromNL(NamedList<Object> namedlist, String name) {
		return getObjFromNL(namedlist, name).flatMap(v -> {
			if (v instanceof NamedList<?>) {
				return Optional.of((NamedList<Object>) v);
			}
			return Optional.empty();
		});
	}

	@SuppressWarnings("unchecked")
	static Optional<List<Object>> getListFromNL(NamedList<Object> namedlist, String name) {
		return getObjFromNL(namedlist, name).flatMap(v -> {
			if (v instanceof List<?>) {
				return Optional.of((List<Object>) v);
			}
			return Optional.empty();
		});
	}

	static Optional<String> getStringFromNL(NamedList<Object> namedlist, String name) {
		return getObjFromNL(namedlist, name).flatMap(v -> {
			if (v instanceof String) {
				return Optional.of((String) v);
			}
			return Optional.empty();
		});
	}

	static Optional<Integer> getIntFromNL(NamedList<Object> namedlist, String name) {
		return getObjFromNL(namedlist, name).flatMap(v -> {
			if (v instanceof Integer) {
				return Optional.of((Integer) v);
			}
			return Optional.empty();
		});
	}

	/* Parse and flatten solr nested facet results
	 * Recursive function
	 */
	static List<FacetR> flatten(NamedList<Object> facetresult, List<List<String>> facetFields) {
		List<FacetR> results = new ArrayList<FacetR>();
		if (facetFields.size() > 0) {
			List<String> head = facetFields.get(0);
			List<List<String>> rest = facetFields.subList(1, facetFields.size());
			head.forEach(facetname -> {
				getNLFromNL(facetresult, facetname)
					.flatMap(bucketobj -> getListFromNL(bucketobj, BUCKETFIELD))
					.ifPresent(bucketarrobj -> {
						bucketarrobj.forEach(bucket -> {
							objToNL(bucket).ifPresent(b -> {
								Optional<String> val = getStringFromNL(b, VALFIELD);
								Optional<Integer> count = getIntFromNL(b, COUNTFIELD);
								val.ifPresent(v -> {
									List<FacetR> subfacetr = flatten(b, rest);
									FacetRKey rkey = new FacetRKey(v, facetname);
									subfacetr.forEach(subr -> {
										subr.addKey(rkey);
										results.add(subr);
									});						
									count.ifPresent(c -> {
										List<FacetRKey> frkeys = new ArrayList<FacetRKey>();
										frkeys.add(rkey);
										FacetR newr = new FacetR(frkeys, c);
										results.add(newr);
									});
								});
							});
						});
					});
			});
		}
		return results;
	}

}
