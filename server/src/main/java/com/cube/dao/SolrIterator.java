/**
 * Copyright Cube I O
 */
package com.cube.dao;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

/**
 * @author prasad
 *
 */
public class SolrIterator implements Iterator<SolrDocument> {

    private static final Logger LOGGER = LogManager.getLogger(SolrIterator.class);

	
	/**
	 * @param query
	 */
	private SolrIterator(SolrClient solr, SolrQuery query, Optional<Integer> maxresults) {
		super();
		this.solr = solr;
		this.query = query;
		this.start = 0;
		this.maxresults = maxresults;

		numresults = 0;
		iterator = Optional.empty();
		numread = 0;						

		int toread = maxresults.map(mr -> Math.min(BATCHSIZE, mr)).orElse(BATCHSIZE);
		
		query.setRows(toread);
		results = query();
		results.ifPresent(r -> {
			numresults = maxresults.map(mr -> Math.min(r.getNumFound(), mr)).orElse(r.getNumFound());
			numFound = r.getNumFound();
			iterator = Optional.ofNullable(r.iterator());
			numread = r.size();			
		});
		
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#hasNext()
	 */
	@Override
	public boolean hasNext() {
		return iterator.map(iter -> {
			if (iter.hasNext()) {
				return true; 
			} else {
				// try to read the next batch
				if (numread >= numresults) {
					return false;
				}
				int toread = maxresults.map(mr -> Math.min(BATCHSIZE, mr-numread)).orElse(BATCHSIZE);
				if (toread > 0) {
					query.setRows(toread);
					query.setStart(numread);
					results = query();
					return results.map(res -> {
						// Note - res.iterator can be null if there are no more result documents 
						iterator = Optional.ofNullable(res.iterator());
						numread += res.size();
						return iterator.map(it -> it.hasNext()).orElse(false);
					}).orElse(false);					
				} 
				return false;
			}
		}).orElse(false);
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#next()
	 */
	@Override
	public SolrDocument next() {
		return iterator.map(iter -> iter.next()).orElseThrow(() -> new NoSuchElementException());
	}

	private Optional<SolrDocumentList> query() {
		return runQuery(solr, query).map(r -> r.getResults());
	}

	private Stream<SolrDocument> toStream() {
		Iterable<SolrDocument> iterable = () -> this;
		return StreamSupport.stream(iterable.spliterator(), false);		
	}

	final SolrClient solr;
	final SolrQuery query;
	int start;
	Optional<SolrDocumentList> results;
	Optional<Iterator<SolrDocument>> iterator;
	long numresults;
	final Optional<Integer> maxresults;
	int numread;
	static final int BATCHSIZE = 20;
	long numFound; // total number of results matching the query, i.e. the max number of results available if no
	// maxresults is set
	
	static public Stream<SolrDocument> getStream(SolrClient solr, SolrQuery query, Optional<Integer> maxresults) {
		SolrIterator iter = new SolrIterator(solr, query, maxresults);
		return iter.toStream();
	}

	static public <R> Result<R> getResults(SolrClient solr, SolrQuery query, 
			Optional<Integer> maxresults,
			Function<SolrDocument, Optional<R>> transform) {
		SolrIterator iter = new SolrIterator(solr, query, maxresults);
		return new Result<R>(iter.toStream().flatMap(d -> transform.apply(d).stream()), iter.numresults,
				iter.numresults);
	}

	static Optional<QueryResponse> runQuery(SolrClient solr, SolrQuery query) {
		LOGGER.info(String.format("Running Solr query %s", query.toQueryString()));

		QueryResponse response;
		try {
			response = solr.query(query);
		} catch (SolrServerException | IOException e) {
			LOGGER.error("Error in querying Solr", e);
			return Optional.empty();
		}
		return Optional.ofNullable(response);		
	}
}
