/**
 * Copyright Cube I O
 */
package com.cube.ws;

import java.io.File;
import java.nio.file.FileSystems;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Pattern;

import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
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
import redis.embedded.RedisServer;

import com.cube.cache.RedisPubSub;
import com.cube.cache.TemplateCache;
import com.cube.dao.ReqRespStore;
import com.cube.dao.ReqRespStoreSolr;
import com.cube.pubsub.PubSubChannel;
import com.cube.pubsub.PubSubMgr;
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
    private static RedisServer redisServer;

    public static int REDIS_DELETE_TTL; // redis key expiry timeout in seconds
	public static int DISRUPTOR_QUEUE_SIZE;

	final Properties properties;
	public final SolrClient solr;
	public final ReqRespStore rrstore;
	// Adding a compare template cache
    public final TemplateCache templateCache;

	public final JedisConnResourceProvider jedisPool;

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

    public final PubSubMgr pubSubMgr;

    public static final String RUN_MODE_LOCAL_PROP = "local";
	public static final String RUN_MODE_CLOUD_PROP = "cloud";

	public Boolean isRunModeLocal = null;


	public static class JedisConnResourceProvider {
    	public static final Duration PING_WAIT = Duration.ofMillis(1000);
	    private final GenericObjectPoolConfig poolConfig;
	    private final String redisHost;
	    private final int redisPort;
	    private final int timeout;
	    private final String redisPassword;
    	private JedisPool pool;
    	private Instant lastPingTime = Instant.now();

	    private JedisPool getPool(){
			return new JedisPool(poolConfig , redisHost, redisPort , timeout,  redisPassword);
	    }
	    JedisConnResourceProvider(GenericObjectPoolConfig poolConfig, String host, int port, int timeout, String password){
	    	this.poolConfig = poolConfig;
	    	this.redisHost = host;
	    	this.redisPort = port;
	    	this.timeout = timeout;
	    	this.redisPassword = password;
	    	this.pool = getPool();
	    }

	    public Jedis getResource(){

	    	Jedis jedis;
		    try{
		    	jedis = pool.getResource();
		    	pingIfRequired(jedis);
		    } catch (Exception e) {
		    	LOGGER.error("Jedis pool resource fetch error "+e.getMessage() , e);
			    pool.destroy();
			    LOGGER.info("destroyed redis Pool. Creating again");
			    pool = getPool();
			    LOGGER.info("Created Jedil Pool again");
			    jedis = pool.getResource();
		    }
		    return jedis;
	    }

	    private void pingIfRequired(Jedis jedis){
	    	Instant now = Instant.now();
		    if(Duration.between(lastPingTime , now).compareTo(PING_WAIT)>0){
				jedis.ping();
				lastPingTime = now;
		    }
	    }

	    public JedisPool getJedisPool() {
		    return pool;
	    }
    }

	public Config() throws Exception {
		LOGGER.info("Creating config");
		properties = new java.util.Properties();
		System.setProperty("io.md.intent" , "noop");
		commonConfig = CommonConfig.getInstance();
		String solrurl = null;
		String runModeString;
    int size = Integer.valueOf(fromEnvOrProperties("response_size", "1"));
    pathsToKeepLimit = Long.valueOf(fromEnvOrProperties("paths_to_keep_limit", "1000"));
    responseSize =  size*1000000;
        try {
            properties.load(this.getClass().getClassLoader().
                    getResourceAsStream(CONFFILE));
            // TODO: SET run_mode as "local" when moved to final repo
	        runModeString = fromEnvOrProperties("run_mode" , Config.RUN_MODE_CLOUD_PROP);
	        isRunModeLocal = runModeString.equals(Config.RUN_MODE_LOCAL_PROP);
            String solrBaseUrl = fromEnvOrProperties("solr_base_url" , "http://18.222.86.142:8983/solr/");
            String solrCore = fromEnvOrProperties("solr_core" , "cube");
            solrurl = Utils.appendUrlPath(solrBaseUrl , solrCore);
        } catch(Exception eta){
            LOGGER.error(String.format("Not able to load config file %s; using defaults", CONFFILE), eta);
            eta.printStackTrace();
        }
        if (isRunModeLocal != null && !isRunModeLocal && solrurl != null) {
            solr = new MDHttpSolrClient.Builder(solrurl).build();
            LOGGER.info(String.format("Using solrurl IP %s", solrurl));

        } else {

        	//Embedded Solr
            String solrHome = fromEnvOrProperties("data_dir", "/var/lib/meshd/data") + "/solr";
            File solrXml = new File(solrHome + "/"+"solr.xml");
            //Check if the solr.xml exists. If yes do nothing
	        //If no that means it it is the first time container start. Copy from datasrc directory
	        if(!solrXml.exists()){
	        	String solrDataSrc = fromEnvOrProperties("datasrc_dir", "/var/lib/meshd/datasrc/embedded_solr_config");
		        File solrHomeDir = new File(solrHome);
		        if(!solrHomeDir.exists()){
			        solrHomeDir.mkdirs();
		        }
		        FileUtils.copyDirectory(new File(solrDataSrc) , solrHomeDir);
		        LOGGER.info(String.format("Copying solr data from %s to %s" , solrDataSrc , solrHome ));
	        }else{
	        	LOGGER.info("SolrXml already exists at location "+solrXml.getAbsolutePath());
	        }
            solr = new EmbeddedSolrServer(FileSystems.getDefault().getPath(solrHome), "cube");
            final String msg = String.format("Using embedded solr with home dir path %s", solrHome);
            LOGGER.info(msg);

            //Embedded Redis
	        int redisPort = Integer.parseInt(CommonUtils.fromEnvOrSystemProperties("redis_port").orElse("6379"));
	        redisServer = new RedisServer(redisPort);
	        redisServer.start();
        }

        ReqRespStoreSolr storeSolr = new ReqRespStoreSolr(solr, this);
        rrstore = storeSolr;
        templateCache = storeSolr.templateCache;
        ProtoDescriptorCacheProvider.instantiateCache(rrstore);
        protoDescriptorCache = ProtoDescriptorCacheProvider.getInstance()
            .orElseThrow(() -> new Exception("Cannot instantiate ProtoDescriptorCache"));

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
            poolConfig.setMaxWaitMillis(2000);
            //poolConfig.setTestOnReturn(true);
	        jedisPool = new JedisConnResourceProvider(poolConfig , redisHost, redisPort , 2000,  redisPassword);
            REDIS_DELETE_TTL = Integer.parseInt(fromEnvOrProperties("redis_delete_ttl"
                , "20"));
            LOGGER.info("REDIS TTL for record/replay after stop : " + REDIS_DELETE_TTL + " sec");
	        Runnable subscribeThread = () -> {
		        while (true) {
			        try(Jedis jedis = jedisPool.getResource()) {
				       jedis.configSet("notify-keyspace-events", "Ex");
				        jedis.psubscribe(new RedisPubSub(rrstore, jsonMapper, jedisPool.getJedisPool()),
					        "__key*__:*");
			        } catch (Throwable th) {
				        LOGGER.error("Redis Key Events PubSub Worker error " + th.getMessage(), th);
			        }
		        }
	        };

	        Runnable channelPubSubThread = () -> {
		        while(true){
			        try(Jedis jedis = jedisPool.getResource()) {
				        LOGGER.debug("starting the channel pubsub thread");
				        jedis.subscribe(PubSubChannel.getSingleton(this), PubSubChannel.MD_PUBSUB_CHANNEL_NAME);
				        LOGGER.error("channel pubsub thread stopped");
			        }catch (Throwable th){
				        LOGGER.error("Redis Channel PubSub Worker error "+th.getMessage() , th);
			        }
		        }
	        };

	        new Thread(subscribeThread).start();
	        new Thread(channelPubSubThread).start();

        } catch (Exception e) {
            LOGGER.error("Error while initializing redis thread pool :: " + e.getMessage() , e);
            throw e;
        }

        DISRUPTOR_QUEUE_SIZE = Integer.parseInt(fromEnvOrProperties("disruptor_queue_size"
	        , "16384"));

        disruptorEventQueue = new DisruptorEventQueue(rrstore, DISRUPTOR_QUEUE_SIZE, Optional.of(this.protoDescriptorCache));

        pubSubMgr = new PubSubMgr(jedisPool);

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
