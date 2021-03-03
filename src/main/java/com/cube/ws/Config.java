/**
 * Copyright Cube I O
 */
package com.cube.ws;

import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Pattern;

import javax.inject.Singleton;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.MDHttpSolrClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.cube.agent.CommonConfig;
import io.cube.agent.IntentResolver;
import io.cube.agent.ProxyMocker;
import io.cube.agent.Recorder;
import io.cube.agent.TraceIntentResolver;
import io.md.cache.ProtoDescriptorCache;
import io.md.utils.CommonUtils;
import io.md.utils.CubeObjectMapperProvider;
import io.md.utils.MeshDGsonProvider;
import io.md.core.Utils;
import io.md.utils.ProtoDescriptorCacheProvider;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import com.cube.cache.RedisPubSub;
import com.cube.cache.TemplateCache;
import com.cube.cache.TemplateCacheRedis;
import com.cube.dao.ReqRespStore;
import com.cube.dao.ReqRespStoreSolr;
import com.cube.queue.DisruptorEventQueue;
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
    public static int REDIS_DELETE_TTL; // redis key expiry timeout in seconds
	public static int DISRUPTOR_QUEUE_SIZE;

	final Properties properties;
	public final SolrClient solr;
	public final ReqRespStore rrstore;
	// Adding a compare template cache
    public final TemplateCache templateCache;

    public final JedisPool jedisPool;

	public final ObjectMapper jsonMapper = CubeObjectMapperProvider.getInstance();

	public final long responseSize;

	public final long pathsToKeepLimit;

	//public final Tracer tracer = ServerUtils.init("Cube");

	public final Recorder recorder;
	public final ProxyMocker mocker;

	public final Gson gson;

    public IntentResolver intentResolver = new TraceIntentResolver();
    public CommonConfig commonConfig;

    public final DisruptorEventQueue disruptorEventQueue;

    public final ProtoDescriptorCache protoDescriptorCache;

    public final static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	public Config() throws Exception {
		LOGGER.info("Creating config");
		properties = new java.util.Properties();
		System.setProperty("io.md.intent" , "noop");
		commonConfig = CommonConfig.getInstance();
		String solrurl = null;
    int size = Integer.valueOf(fromEnvOrProperties("response_size", "1"));
    pathsToKeepLimit = Long.valueOf(fromEnvOrProperties("paths_to_keep_limit", "1000"));
    responseSize =  size*1000000;
        try {
            properties.load(this.getClass().getClassLoader().
                    getResourceAsStream(CONFFILE));
            String solrBaseUrl = fromEnvOrProperties("solr_base_url" , "http://18.222.86.142:8983/solr/");
            String solrCore = fromEnvOrProperties("solr_core" , "cube");
            solrurl = Utils.appendUrlPath(solrBaseUrl , solrCore);
        } catch(Exception eta){
            LOGGER.error(String.format("Not able to load config file %s; using defaults", CONFFILE), eta);
            eta.printStackTrace();
            LOGGER.info(String.format("Using default solrurl IP %s", solrurl));
        }
        if (solrurl != null) {
            solr =  new MDHttpSolrClient.Builder(solrurl).build();
            rrstore = new ReqRespStoreSolr(solr, this);
            templateCache = new TemplateCacheRedis(rrstore , this);
            ProtoDescriptorCacheProvider.instantiateCache(rrstore);
	        protoDescriptorCache = ProtoDescriptorCacheProvider.getInstance()
		        .orElseThrow(() -> new Exception("Cannot instantiate ProtoDescriptorCache"));
        } else {
            final String msg = String.format("Solrurl missing in the config file %s", CONFFILE);
            LOGGER.error(msg);
            throw new Exception(msg);
        }

        gson = new GsonBuilder().registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory())
            .registerTypeAdapter(Pattern.class, new GsonPatternSerializer())
            .registerTypeAdapter(SolrDocumentList.class, new GsonSolrDocumentListSerializer())
            .registerTypeAdapter(SolrDocument.class, new GsonSolrDocumentSerializer())
            .create();
        MeshDGsonProvider.setInstance(gson);
        recorder = commonConfig.getRecorder();
        mocker = new ProxyMocker();

        try {
            String redisHost = fromEnvOrProperties("redis_host", "localhost");
            int redisPort = Integer.valueOf(fromEnvOrProperties("redis_port"
                , "6379"));
            String redisPassword = fromEnvOrProperties("redis_password" , null);
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setTestOnBorrow(true);
            //poolConfig.setTestOnReturn(true);
            jedisPool = new JedisPool(poolConfig , redisHost, redisPort , 2000,  redisPassword);
            REDIS_DELETE_TTL = Integer.parseInt(fromEnvOrProperties("redis_delete_ttl"
                , "5"));
	        Runnable subscribeThread = new Runnable() {
		        /**
		         * When an object implementing interface <code>Runnable</code> is
		         * used to create a thread, starting the thread causes the object's
		         * <code>run</code> method to be called in that separately
		         * executing
		         * thread.
		         * <p>
		         * The general contract of the method <code>run</code> is that it
		         * may take any action whatsoever.
		         *
		         * @see Thread#run()
		         */
		        @Override
		        public void run() {
		        	while(true){
		        		try{
					        Jedis jedis = jedisPool.getResource();
					        jedis.configSet("notify-keyspace-events" , "Ex");
					        jedis.psubscribe(new RedisPubSub(rrstore, jsonMapper, jedisPool), "__key*__:*");
				        }catch (Throwable th){
		        			LOGGER.error("Redis PubSub Worker error "+th.getMessage() , th);
				        }
			        }

		        }
	        };
	        new Thread(subscribeThread).start();
        } catch (Exception e) {
            LOGGER.error("Error while initializing redis thread pool :: " + e.getMessage());
            throw e;
        }

        DISRUPTOR_QUEUE_SIZE = Integer.parseInt(fromEnvOrProperties("disruptor_queue_size"
	        , "16384"));

        disruptorEventQueue = new DisruptorEventQueue(rrstore, DISRUPTOR_QUEUE_SIZE, Optional.of(this.protoDescriptorCache));

	}

    private String fromEnvOrProperties(String propertyName, String defaultValue) {
	    return CommonUtils.fromEnvOrSystemProperties(propertyName)
		    .orElse(properties.getProperty(propertyName , defaultValue));
    }


	public String getProperty(String key)
	{
		String value = this.properties.getProperty(key);
		return value;
	}

	public long getResponseSize() {
	  return responseSize;
  }

  public long getPathsToKeepLimit() {
	  return pathsToKeepLimit;
  }

}
