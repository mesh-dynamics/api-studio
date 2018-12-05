/**
 * Copyright Cube I O
 */
package com.cube.dao;

import java.io.IOException;
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
			qr.id.ifPresent(reqid -> {
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

		LOGGER.info(String.format("Running Solr query %s", query.toQueryString()));

		QueryResponse response;
		try {
			response = solr.query(query);
		} catch (SolrServerException | IOException e) {
			LOGGER.error("Error in querying Solr", e);
			return Optional.empty();
		}
		final SolrDocumentList documents = response.getResults();

		documents.stream().findFirst().map(doc -> {
			return docToRequest(doc);
		});
		return Optional.empty();
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

		documents.stream().findFirst().map(doc -> {
			return docToResponse(doc);
		});
		return Optional.empty();
	}

	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#getRespForReq(com.cube.dao.ReqRespStore.Request)
	 */
	@Override
	public Optional<Response> getRespForReq(Request qr) {
		// Find request, without considering request id
		Optional<Request> req = getRequest(qr, true);
		return req.flatMap(reqv -> reqv.id).flatMap(idv -> {
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
	private static final String BODYF = "_c_body_s";
	private static final String STATUSF = "_status_i";

	private static String getPrefix(String ftype) {
		return String.format("_c_%s_", ftype);
	}
	private static String getSolrFieldName(String ftype, String fname) {
		String prefix = getPrefix(ftype);
		return String.format("%s%s%s", prefix, fname, FSUFFIX);
	}

	private static final String FSUFFIX = "_ss"; // set of strings in Solr
	// ensure that this pattern is consistent with the prefix and suffixes used above
	private static final String patternStr = "^_c_(\\w+)_(.*)_ss$";
	private static final Pattern pattern = Pattern.compile(patternStr);
	private static final String QPARAMS = "qp"; 
	private static final String FPARAMS = "fp"; 
	private static final String META = "meta"; 
	private static final String HDR = "hdr"; 
	private static final String CK = "ck"; 

	
	private static SolrInputDocument reqToSolrDoc(Request req) {
		final SolrInputDocument doc = new SolrInputDocument();
		
		doc.setField(TYPEF, Types.Request.toString());
		doc.setField(PATHF, req.path);
		req.id.ifPresent(id -> doc.setField(REQIDF, id));
		doc.setField(BODYF, req.body);
		addFieldsToDoc(doc, QPARAMS, req.qparams);
		addFieldsToDoc(doc, FPARAMS, req.fparams);
		addFieldsToDoc(doc, META, req.meta);
		addFieldsToDoc(doc, HDR, req.hdrs);
		addFieldsToDoc(doc, CK, req.cookies);
		
		return doc;
	}

	private static Optional<Request> docToRequest(SolrDocument doc) {
		
		Optional<String> type = Optional.empty();
		Optional<String> path = Optional.empty();
		Optional<String> id = Optional.empty();
		MultivaluedMap<String, String> qparams = new MultivaluedHashMap<String, String>(); // query params
		MultivaluedMap<String, String> fparams = new MultivaluedHashMap<String, String>(); // form params
		MultivaluedMap<String, String> meta = new MultivaluedHashMap<String, String>(); 
		MultivaluedMap<String, String> hdrs = new MultivaluedHashMap<String, String>();
		MultivaluedMap<String, String> cookies = new MultivaluedHashMap<String, String>();
		Optional<String> body = Optional.empty();
		
		for (Entry<String, Object> kv : doc) {
			String k = kv.getKey();
			Object v = kv.getValue();
			switch (k) {
			case TYPEF:
				if (v instanceof String) type = Optional.of((String)v);
				break;
			case PATHF:
				if (v instanceof String) path = Optional.of((String)v);
				break;
			case REQIDF:
				if (v instanceof String) id = Optional.of((String)v);
				break;
			case BODYF:
				if (v instanceof String) body = Optional.of((String)v);
				break;
			default:
				Matcher m = pattern.matcher(k);
				if (m.find()) {					
					switch (m.group(0)) {
					case QPARAMS:
						if (v instanceof String) {qparams.add(m.group(1), (String)v);};
						break;
					case FPARAMS:
						if (v instanceof String) {fparams.add(m.group(1), (String)v);};
						break;
					case META:
						if (v instanceof String) {meta.add(m.group(1), (String)v);};
						break;
					case HDR:
						if (v instanceof String) {hdrs.add(m.group(1), (String)v);};
						break;
					case CK:
						if (v instanceof String) {cookies.add(m.group(1), (String)v);};
						break;
					default:
					}
				}
			}
		};

		final String p = path.orElse("");
		final String b = body.orElse("");
		final Optional<String> idv = id; // this is just to avoid compiler from cribbing when accessing non final id in lambda function below
		return type.map(t -> {
			if (t.equals(Types.Request.toString()))
				return new Request(p, idv, qparams, fparams, meta, hdrs, cookies, b);
			else
				return null;
		});
	}

	
	private static SolrInputDocument respToSolrDoc(Response resp) {
		final SolrInputDocument doc = new SolrInputDocument();
		
		doc.setField(TYPEF, Types.Response.toString());
		resp.reqid.ifPresent(id -> doc.setField(REQIDF, id));
		doc.setField(BODYF, resp.body);
		doc.setField(STATUSF, resp.status);
		addFieldsToDoc(doc, META, resp.meta);
		addFieldsToDoc(doc, HDR, resp.hdrs);
		
		return doc;
	}

	
	private static Optional<Response> docToResponse(SolrDocument doc) {
		
		Optional<String> type = Optional.empty();
		Optional<Integer> status = Optional.empty();
		Optional<String> id = Optional.empty();
		MultivaluedMap<String, String> meta = new MultivaluedHashMap<String, String>(); 
		MultivaluedMap<String, String> hdrs = new MultivaluedHashMap<String, String>();
		Optional<String> body = Optional.empty();
		
		for (Entry<String, Object> kv : doc) {
			String k = kv.getKey();
			Object v = kv.getValue();
			switch (k) {
			case TYPEF:
				if (v instanceof String) type = Optional.of((String)v);
				break;
			case STATUSF:
				if (v instanceof Integer) status = Optional.of((Integer)v);
				break;
			case REQIDF:
				if (v instanceof String) id = Optional.of((String)v);
				break;
			case BODYF:
				if (v instanceof String) body = Optional.of((String)v);
				break;
			default:
				Matcher m = pattern.matcher(k);
				if (m.find()) {					
					switch (m.group(0)) {
					case META:
						if (v instanceof String) {meta.add(m.group(1), (String)v);};
						break;
					case HDR:
						if (v instanceof String) {hdrs.add(m.group(1), (String)v);};
						break;
					default:
					}
				}
			}
		};

		final String b = body.orElse("");
		final Optional<Integer> s = status;
		final Optional<String> idv = id; // this is just to avoid compiler from cribbing when accessing non final id in lambda function below
		return type.flatMap(t -> {
			if (t.equals(Types.Response.toString())) {
				s.map(sv ->{
					return new Response(idv, sv, meta, hdrs, b);
				});				
			} 
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
