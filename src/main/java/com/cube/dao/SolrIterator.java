/**
 * Copyright Cube I O
 */
package com.cube.dao;

import java.io.IOException;
import java.lang.reflect.Method;
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
import org.apache.solr.common.params.SolrParams;

import io.cube.agent.FnKey;

import com.cube.ws.Config;

/**
 * @author prasad
 *
 */
public class SolrIterator implements Iterator<SolrDocument> {

    private static final Logger LOGGER = LogManager.getLogger(SolrIterator.class);

    private static Config config;

    public static void setConfig(Config config) {
        SolrIterator.config = config;
    }

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

	/**
	 * Utility function to
	 * a) query solr (in batches) for a given query
	 * b) convert the obtained result stream to a another stream by applying the transformer function
	 * Note that the transformer function is applied lazily (when get next on stream is called)
	 * c) wrap the resulting stream in a Result object (containing total/absolute number of results)
	 * @param solr
	 * @param query
	 * @param maxresults
	 * @param transformToStream
	 * @param <R>
	 * @return
	 */
	static public <R> Result<R> getResultsWithTransformStream
			(SolrClient solr, SolrQuery query, Optional<Integer> maxresults,
										   Function<SolrDocument, Stream<R>> transformToStream) {
		SolrIterator iter = new SolrIterator(solr , query , maxresults);
		// just want to understand that this flatMap will be called on demand and the entire stream
		// won't be transformed in the constructor of the Result object itself
		// also num results only indicates top-level results , as the trasnformStream could be a
		// one to many function
		return new Result<R>(iter.toStream().flatMap(d -> transformToStream.apply(d)) , iter.numresults,
				iter.numresults);

	}


    // TODO mock this function for all solr queries
    private static FnKey queryFnKey;

    static Optional<QueryResponse> runQuery(SolrClient solr, SolrQuery query) {
        Optional<String> action = config.getCurrentActionFromScope();
        if (queryFnKey == null) {
            try {
                Method currentMethod = solr.getClass().getMethod("query", SolrParams.class);
                queryFnKey = new FnKey(config.customerId, config.app, config.instance,
                    config.serviceName, currentMethod);
            } catch (Exception e) {
                LOGGER.error("Unable to find solr query method by reflection :: " + e.getMessage());
            }
        }
        if (config.getState() == Config.AppState.Mock && !action.orElse("").equals("func")) {
            return Optional.ofNullable((QueryResponse) config.mocker.mock(queryFnKey, Optional.empty(), Optional.empty()
                , Optional.empty(), Optional.empty(), query).retVal);
        }

        QueryResponse response = null;
        Optional<QueryResponse> toReturn = Optional.empty();
        try {
            response = solr.query(query);
            toReturn = Optional.of(response);
        } catch (SolrServerException | IOException e) {
            LOGGER.error("Error in querying Solr", e);
        }

        if (config.getState() == Config.AppState.Record && !action.orElse("").equals("func")) {
            config.recorder.record(queryFnKey, Optional.empty(), Optional.empty(), Optional.empty(),
                response, query);
        }
        return toReturn;
    }
}
