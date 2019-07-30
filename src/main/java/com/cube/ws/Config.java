/**
 * Copyright Cube I O
 */
package com.cube.ws;

import java.util.Properties;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import javax.inject.Singleton;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.cube.agent.IntentResolver;
import io.cube.agent.Mocker;
import io.cube.agent.Recorder;
import io.cube.agent.SimpleMocker;
import io.cube.agent.SimpleRecorder;
import io.cube.agent.TraceIntentResolver;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import com.cube.cache.ReplayResultCache;
import com.cube.cache.RequestComparatorCache;
import com.cube.cache.ResponseComparatorCache;
import com.cube.cache.TemplateCache;
import com.cube.core.Utils;
import com.cube.dao.ReqRespStore;
import com.cube.dao.ReqRespStoreSolr;
import com.cube.serialize.GsonPatternSerializer;
import com.cube.serialize.GsonSolrDocumentListSerializer;
import com.cube.serialize.GsonSolrDocumentSerializer;

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

    public final JedisPool jedisPool;

	public final ObjectMapper jsonmapper = CubeObjectMapperProvider.createDefaultMapper();

	//public final Tracer tracer = Utils.init("Cube");

	public final Recorder recorder;
	public final Mocker mocker;

	ReentrantReadWriteLock reentrantLock = new ReentrantReadWriteLock();

    public String customerId, app, instance, serviceName;

    public IntentResolver intentResolver = new TraceIntentResolver();

	public Config() throws Exception {
		LOGGER.info("Creating config");
		properties = new java.util.Properties();
		String solrurl = "http://18.191.135.125:8983/solr/cube";   // TODO: pass this default from kube conf
        try {
            properties.load(this.getClass().getClassLoader().
                    getResourceAsStream(CONFFILE));
            solrurl = properties.getProperty("solrurl");
            customerId = fromEnvOrProperties("customer_dogfood" , "ravivj");
            app = fromEnvOrProperties("app_dogfood" , "cubews");
            instance = fromEnvOrProperties("instance_dogfood" , "dev");
            serviceName = fromEnvOrProperties("service_dogfood" , "cube");
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

        Gson gson = new GsonBuilder().registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory())
            .registerTypeAdapter(Pattern.class, new GsonPatternSerializer())
            .registerTypeAdapter(SolrDocumentList.class, new GsonSolrDocumentListSerializer())
            .registerTypeAdapter(SolrDocument.class, new GsonSolrDocumentSerializer())
            .create();
        recorder = new SimpleRecorder(gson);
        mocker = new SimpleMocker(gson);

        Tracer tracer = Utils.init("tracer");
        try {
            GlobalTracer.register(tracer);
        } catch (IllegalStateException e) {
            LOGGER.error("Trying to register a tracer when one is already registered");
        }

        try {
            String redisHost = fromEnvOrProperties("redis_host", "localhost");
            int redisPort = Integer.valueOf(fromEnvOrProperties("redis_port"
                , "6379"));
            String redisPassword = fromEnvOrProperties("redis_password" , null);
            jedisPool = new JedisPool(new JedisPoolConfig() , redisHost, redisPort , 2000,  redisPassword);
        } catch (Exception e) {
            LOGGER.error("Error while initializing redis thread pool :: " + e.getMessage());
            throw e;
        }

	}

    private String fromEnvOrProperties(String propertyName, String defaultValue) {
        String fromEnv =  System.getenv(propertyName);
        if (fromEnv != null) {
            return fromEnv;
        }
        return  properties.getProperty(propertyName , defaultValue);
    }


	public String getProperty(String key)
	{
		String value = this.properties.getProperty(key);
		return value;
	}

}
