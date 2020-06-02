/**
 * Copyright Cube I O
 */
package com.cube.ws;

import java.util.Properties;
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

import io.cube.agent.CommonConfig;
import io.cube.agent.ConsoleRecorder;
import io.cube.agent.IntentResolver;
import io.cube.agent.Mocker;
import io.cube.agent.Recorder;
import io.cube.agent.SimpleMocker;
import io.cube.agent.TraceIntentResolver;
import io.md.utils.CommonUtils;
import io.md.utils.MeshDGsonProvider;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import com.cube.cache.RedisPubSub;
import com.cube.cache.TemplateCache;
import com.cube.cache.TemplateCacheRedis;
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
    public static int REDIS_DELETE_TTL; // redis key expiry timeout in seconds

	final Properties properties;
	public final SolrClient solr;
	public final ReqRespStore rrstore;
	// Adding a compare template cache
    public final TemplateCache templateCache;

    public final JedisPool jedisPool;

	public final ObjectMapper jsonMapper = CubeObjectMapperProvider.createDefaultMapper();

	public final long responseSize;

	//public final Tracer tracer = Utils.init("Cube");

	public final Recorder recorder;
	public final Mocker mocker;

	public final Gson gson;

    public IntentResolver intentResolver = new TraceIntentResolver();
    public CommonConfig commonConfig;

	public Config() throws Exception {
		LOGGER.info("Creating config");
		properties = new java.util.Properties();
		System.setProperty("io.md.intent" , "noop");
		commonConfig = CommonConfig.getInstance();
		String solrurl = null;
    int size = Integer.valueOf(fromEnvOrProperties("response_size", "1"));
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
            solr = new HttpSolrClient.Builder(solrurl).build();
            rrstore = new ReqRespStoreSolr(solr, this);
            templateCache = new TemplateCacheRedis(rrstore , this);
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
        recorder = new ConsoleRecorder(gson);
        mocker = new SimpleMocker(gson);

        try {
            String redisHost = fromEnvOrProperties("redis_host", "localhost");
            int redisPort = Integer.valueOf(fromEnvOrProperties("redis_port"
                , "6379"));
            String redisPassword = fromEnvOrProperties("redis_password" , null);
            jedisPool = new JedisPool(new JedisPoolConfig() , redisHost, redisPort , 2000,  redisPassword);
            REDIS_DELETE_TTL = Integer.parseInt(fromEnvOrProperties("redis_delete_ttl"
                , "15"));
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
			        Jedis jedis = jedisPool.getResource();
			        jedis.configSet("notify-keyspace-events" , "Ex");
			        jedis.psubscribe(new RedisPubSub(), "__key*__:*");
		        }
	        };
	        new Thread(subscribeThread).start();
        } catch (Exception e) {
            LOGGER.error("Error while initializing redis thread pool :: " + e.getMessage());
            throw e;
        }

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

}
