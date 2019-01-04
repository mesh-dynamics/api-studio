/**
 * Copyright Cube I O
 */
package com.cube.dao;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

import com.cube.core.Utils;
import com.cube.drivers.Replay;
import com.cube.drivers.Replay.ReplayStatus;

/**
 * @author prasad
 *
 */
public class ReqRespStoreSolr implements ReqRespStore {
	
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
	public Optional<Request> getRequest(Request qr, boolean ignoreId) {

		final SolrQuery query = new SolrQuery("*:*");
		query.addField("*");
		query.setRows(1);
		addFilter(query, TYPEF, Types.Request.toString());
		addFilter(query, PATHF, qr.path);

		if (!ignoreId) 
			addFilter(query, REQIDF, qr.reqid);
		
		addFilter(query, QPARAMS, qr.qparams);
		addFilter(query, FPARAMS, qr.fparams);
		
		addFilter(query, RRTYPEF, qr.rrtype);
		addFilter(query, CUSTOMERIDF, qr.customerid);
		addFilter(query, APPF, qr.app);

		
		return query(solr, query).flatMap(documents -> {
			return documents.stream().findFirst().flatMap(doc -> {
				return docToRequest(doc);
			});			
		});

	}

	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#getRequests(java.lang.String, java.lang.String, java.lang.String, java.lang.Iterable, com.cube.dao.ReqRespStore.RR, com.cube.dao.ReqRespStore.Types)
	 */
	@Override
	public List<Request> getRequests(String customerid, String app, String collection, 
			List<String> reqids, List<String> paths,
			RR rrtype) {
		// TODO Auto-generated method stub

		final SolrQuery query = new SolrQuery("*:*");
		query.addField("*");
		addFilter(query, TYPEF, Types.Request.toString());
		addFilter(query, CUSTOMERIDF, customerid);
		addFilter(query, APPF, app);
		addFilter(query, COLLECTIONF, collection);
		
		
		String reqfilter = reqids.stream().collect(Collectors.joining(" OR ", "(", ")"));
		if (reqids.size() > 0)
			addFilter(query, REQIDF, reqfilter);

		String pathfilter = paths.stream().collect(Collectors.joining(" OR ", "(", ")"));
		if (paths.size() > 0)
			addFilter(query, PATHF, pathfilter);

		
		query.addFilterQuery(String.format("%s:%s", RRTYPEF, rrtype.toString()));			
				
		return query(solr, query).map(documents -> {
			return documents.stream().flatMap(doc -> {
				return docToRequest(doc).stream();
			}).collect(Collectors.toList());			
		}).orElse(new ArrayList<Request>());
	}

	
	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#getResponse(java.lang.String)
	 */
	@Override
	public Optional<Response> getResponse(String reqid) {
		
		final SolrQuery query = new SolrQuery("*:*");
		query.addField("*");
		query.setRows(1);

		addFilter(query, TYPEF, Types.Response.toString());
		addFilter(query, REQIDF, reqid);

		return query(solr, query).flatMap(documents -> {
			return documents.stream().findFirst().flatMap(doc -> {
				return docToResponse(doc);
			});			
		});

	}

	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#getRespForReq(com.cube.dao.ReqRespStore.Request)
	 */
	@Override
	public Optional<Response> getRespForReq(Request qr) {
		// Find request, without considering request id
		Optional<Request> req = getRequest(qr, true);
		return req.flatMap(reqv -> reqv.reqid).flatMap(idv -> {
			return getResponse(idv);
		});
	}
	
	/**
	 * @param solr
	 */
	public ReqRespStoreSolr(SolrClient solr) {
		super();
		this.solr = solr;
	}

	private final SolrClient solr;
	
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

	
	private static void addFilter(SolrQuery query, String fieldname, String fval) {
		query.addFilterQuery(String.format("%s:%s", fieldname, fval));			
	}
	
	private static void addFilter(SolrQuery query, String fieldname, Optional<String> fval) {
		fval.ifPresent(val -> {
			query.addFilterQuery(String.format("%s:%s", fieldname, val));			
		});
	}

	private static void addFilter(SolrQuery query, String fieldname, MultivaluedMap<String, String> fvalmap) {
		fvalmap.forEach((k, values) -> {
			String f = getSolrFieldName(fieldname, k);
			values.forEach(v -> {
				addFilter(query, f, v);
			});
		});
	}

	private static Optional<SolrDocumentList> query(SolrClient solr, SolrQuery query) {

		LOGGER.info(String.format("Running Solr query %s", query.toQueryString()));

		QueryResponse response;
		try {
			response = solr.query(query);
		} catch (SolrServerException | IOException e) {
			LOGGER.error("Error in querying Solr", e);
			return Optional.empty();
		}
		return Optional.ofNullable(response.getResults());

	}
	

	
	private static void setRRFields(RRBase rr, SolrInputDocument doc) {
		
		rr.reqid.ifPresent(id -> doc.setField(REQIDF, id));
		doc.setField(BODYF, rr.body);
		addFieldsToDoc(doc, META, rr.meta);
		addFieldsToDoc(doc, HDR, rr.hdrs);
		rr.collection.ifPresent(c -> doc.setField(COLLECTIONF, c));
		rr.timestamp.ifPresent(t -> doc.setField(TIMESTAMPF, t.toString()));
		rr.rrtype.ifPresent(c -> doc.setField(RRTYPEF, c));
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
		Optional<String> rrtype = getStrField(doc, RRTYPEF);
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
		Optional<String> rrtype = getStrField(doc, RRTYPEF);
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

		// the id field is set to replay id so that the document can be updated based on id
		doc.setField(IDF, replay.replayid);
		doc.setField(APPF, replay.app);
		doc.setField(ASYNCF, replay.async);
		doc.setField(COLLECTIONF, replay.collection);
		doc.setField(CUSTOMERIDF, replay.customerid);
		doc.setField(ENDPOINTF, replay.endpoint);
		doc.setField(REPLAYIDF, replay.replayid);
		replay.reqids.forEach(id -> doc.addField(REQIDSF, id));
		doc.setField(REPLAYSTATUSF, replay.status.toString());
		doc.setField(TYPEF, Types.ReplayMeta.toString());
		replay.paths.forEach(path -> doc.addField(PATHSF, path));
		doc.setField(REQCNTF, replay.reqcnt);
		doc.setField(REQSENTF, replay.reqsent);
		doc.setField(REQFAILEDF, replay.reqfailed);
		
		return doc;
	}

	private static Optional<Replay> docToReplay(SolrDocument doc, ReqRespStore rrstore) {
		
		Optional<String> app = getStrField(doc, APPF);
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
		if (endpoint.isPresent() && customerid.isPresent() && app.isPresent() && collection.isPresent() 
				&& replayid.isPresent() && async.isPresent() && status.isPresent()) {
			replay = Optional.of(new Replay(endpoint.get(), customerid.get(), app.get(), collection.get(), 
					reqids, rrstore, replayid.get(), async.get(), status.get(), paths, reqcnt, reqsent, reqfailed));
		} else {
			LOGGER.error(String.format("Not able to convert Solr result to Replay object for replay id %s", replayid.orElse("")));
		}
		
		return replay;
	}



	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#saveReplay(com.cube.drivers.Replay)
	 */
	@Override
	public boolean saveReplay(Replay replay) {
		SolrInputDocument doc = replayToSolrDoc(replay);
		return saveDoc(doc);
	}

	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#getReplay(java.lang.String)
	 */
	@Override
	public Optional<Replay> getReplay(String replayid) {
		final SolrQuery query = new SolrQuery("*:*");
		query.addField("*");
		query.setRows(1);
		addFilter(query, TYPEF, Types.ReplayMeta.toString());
		addFilter(query, REPLAYIDF, replayid);
		
		return query(solr, query).flatMap(documents -> {
			return documents.stream().findFirst().flatMap(doc -> {
				return docToReplay(doc, this);
			});			
		});
	}

}
