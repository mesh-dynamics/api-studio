/**
 * Copyright Cube I O
 */
package com.cube.dao;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		try {
			solr.add(doc);
		} catch (SolrServerException | IOException e) {
			LOGGER.error("Error in saving request", e);
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#save(com.cube.dao.ReqRespStore.Response)
	 */
	@Override
	public boolean save(Response resp) {

		SolrInputDocument doc = respToSolrDoc(resp);
		try {
			solr.add(doc);
		} catch (SolrServerException | IOException e) {
			LOGGER.error("Error in saving response", e);
			return false;
		}		
		return true;
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
		query.addFilterQuery(String.format("%s:%s", TYPEF, Types.Request.toString()));
		query.addFilterQuery(String.format("%s:%s", PATHF, qr.path));			
		if (!ignoreId) 
			qr.reqid.ifPresent(reqid -> {
				query.addFilterQuery(String.format("%s:%s", REQIDF, reqid));			
			});
		qr.qparams.forEach((k, values) -> {
			String f = getSolrFieldName(QPARAMS, k);
			values.forEach(v -> {
				query.addFilterQuery(String.format("%s:%s", f, v));							
			});
		});
		qr.fparams.forEach((k, values) -> {
			String f = getSolrFieldName(FPARAMS, k);
			values.forEach(v -> {
				query.addFilterQuery(String.format("%s:%s", f, v));							
			});
		});
		qr.rrtype.ifPresent(reqid -> {
			query.addFilterQuery(String.format("%s:%s", RRTYPEF, reqid));			
		});
		qr.customerid.ifPresent(reqid -> {
			query.addFilterQuery(String.format("%s:%s", CUSTOMERIDF, reqid));			
		});
		qr.app.ifPresent(reqid -> {
			query.addFilterQuery(String.format("%s:%s", APPF, reqid));			
		});

		LOGGER.info(String.format("Running Solr query %s", query.toQueryString()));

		QueryResponse response;
		try {
			response = solr.query(query);
		} catch (SolrServerException | IOException e) {
			LOGGER.error("Error in querying Solr", e);
			return Optional.empty();
		}
		final SolrDocumentList documents = response.getResults();

		return documents.stream().findFirst().flatMap(doc -> {
			return docToRequest(doc);
		});
	}

	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#getResponse(java.lang.String)
	 */
	@Override
	public Optional<Response> getResponse(String reqid) {
		
		final SolrQuery query = new SolrQuery("*:*");
		query.addField("*");
		query.setRows(1);
		query.addFilterQuery(String.format("%s:%s", TYPEF, Types.Response.toString()));
		query.addFilterQuery(String.format("%s:%s", REQIDF, reqid));

		LOGGER.info(String.format("Running Solr query %s", query.toQueryString()));
		
		QueryResponse response;
		try {
			response = solr.query(query);
		} catch (SolrServerException | IOException e) {
			LOGGER.error("Error in querying Solr", e);
			return Optional.empty();
		}
		final SolrDocumentList documents = response.getResults();

		return documents.stream().findFirst().flatMap(doc -> {
			return docToResponse(doc);
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
	
	private static final String TYPEF = "_c_type_s";
	enum Types {
		Request,
		Response
	}

	// field names in Solr
	private static final String PATHF = "_c_path_s";
	private static final String REQIDF = "_c_reqid_s";
	private static final String METHODF = "_c_method_s";
	private static final String BODYF = "_c_body_t";
	private static final String COLLECTIONF = "_c_collection_s";
	private static final String TIMESTAMPF = "_c_timestamp_dt";
	private static final String RRTYPEF = "_c_rrtype_s";
	private static final String CUSTOMERIDF = "_c_customerid_s";
	private static final String APPF = "_c_app_s";
	private static final String STATUSF = "_c_status_i";

	private static String getPrefix(String ftype) {
		return String.format("_c_%s_", ftype);
	}
	private static String getSolrFieldName(String ftype, String fname) {
		String prefix = getPrefix(ftype);
		return String.format("%s%s%s", prefix, fname, FSUFFIX);
	}

	private static final String FSUFFIX = "_ss"; // set of strings in Solr
	// ensure that this pattern is consistent with the prefix and suffixes used above
	private static final String patternStr = "^_c_([^_]+)_(.*)_ss$";
	private static final Pattern pattern = Pattern.compile(patternStr);
	private static final String QPARAMS = "qp"; 
	private static final String FPARAMS = "fp"; 
	private static final String META = "meta"; 
	private static final String HDR = "hdr"; 

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

}
