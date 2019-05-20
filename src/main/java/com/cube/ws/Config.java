/**
 * Copyright Cube I O
 */
package com.cube.ws;

import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Singleton;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.params.AppendedSolrParams;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.agent.Mocker;
import io.cube.agent.Recorder;
import io.cube.agent.SimpleMocker;
import io.cube.agent.SimpleRecorder;
import io.opentracing.Scope;
import io.opentracing.Tracer;

import com.cube.cache.ReplayResultCache;
import com.cube.cache.RequestComparatorCache;
import com.cube.cache.ResponseComparatorCache;
import com.cube.cache.TemplateCache;
import com.cube.core.Utils;
import com.cube.dao.ReqRespStore;
import com.cube.dao.ReqRespStoreSolr;

/**
 * @author prasad
 *
 */
@Singleton
public class Config {

    private static final Logger LOGGER = LogManager.getLogger(Config.class);
    private static final String CONFFILE = "cube.conf";
	public static final String DEFAULT_TRACE_FIELD = "x-b3-traceid";
    public static final String DEFAULT_SPAN_FIELD = "x-b3-spanid";
    public static final String DEFAULT_PARENT_SPAN_FIELD = "x-b3-parentspanid";

	final Properties properties;
	final SolrClient solr;
	public final ReqRespStore rrstore;
	// Adding a compare template cache
    public final TemplateCache templateCache;

    public final RequestComparatorCache requestComparatorCache;

    public final ResponseComparatorCache responseComparatorCache;

    public final ReplayResultCache replayResultCache;

	public final ObjectMapper jsonmapper = CubeObjectMapperProvider.createDefaultMapper();

	public final Tracer tracer = Utils.init("Cube");

	public Recorder recorder = new SimpleRecorder();
	public Mocker mocker = new SimpleMocker();

	ReentrantReadWriteLock reentrantLock = new ReentrantReadWriteLock();

	public enum AppState {
        Mock , Record , Normal
    }

    public String customerId, app, instance, serviceName;

    public AppState state = AppState.Normal;

	public Config() throws Exception {
		LOGGER.info("Creating config");
		properties = new java.util.Properties();
		String solrurl = "http://18.191.135.125:8983/solr/cube";   // TODO: pass this default from kube conf
        try {
            properties.load(this.getClass().getClassLoader().
                    getResourceAsStream(CONFFILE));
            solrurl = properties.getProperty("solrurl");
            customerId = properties.getProperty("customer" , "ravivj");
            app = properties.getProperty("app" , "cubews");
            instance = properties.getProperty("instance" , "dev");
            serviceName = properties.getProperty("service" , "cube");
        } catch(Exception eta){
            LOGGER.error(String.format("Not able to load config file %s; using defaults", CONFFILE), eta);
            eta.printStackTrace();
            LOGGER.info(String.format("Using default solrurl IP %s", solrurl));
        }
        if (solrurl != null) {
            solr = new HttpSolrClient.Builder(solrurl).build();
            rrstore = new ReqRespStoreSolr(solr, this);
            templateCache = new TemplateCache(rrstore , this);
            requestComparatorCache = new RequestComparatorCache(templateCache , jsonmapper);
            responseComparatorCache = new ResponseComparatorCache(templateCache , jsonmapper);
            replayResultCache = new ReplayResultCache(rrstore, this);
        } else {
            final String msg = String.format("Solrurl missing in the config file %s", CONFFILE);
            LOGGER.error(msg);
            throw new Exception(msg);
        }

	}

	public void setState(AppState state) {
	    ReentrantReadWriteLock.WriteLock writeLock = reentrantLock.writeLock();
	    try {
            writeLock.lock();
            this.state = state;
        }  finally {
	        writeLock.unlock();
	    }
    }

    public AppState getState() {
	    ReentrantReadWriteLock.ReadLock readLock = reentrantLock.readLock();
	    try {
            readLock.lock();
            return this.state;
        } finally {
	        readLock.unlock();
        }
    }

	public String getProperty(String key)
	{
		String value = this.properties.getProperty(key);
		return value;
	}

	public Optional<String> getCurrentActionFromScope() {
        Scope scope =  tracer.scopeManager().active();
        Optional<String> action = Optional.empty();
        if (scope != null) {
            action = Optional.ofNullable(scope.span().getBaggageItem("action"));
        }
        return action;
    }

}
