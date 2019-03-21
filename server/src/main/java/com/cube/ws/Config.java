/**
 * Copyright Cube I O
 */
package com.cube.ws;

import java.util.Properties;

import javax.inject.Singleton;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

import com.cube.dao.ReqRespStore;
import com.cube.dao.ReqRespStoreSolr;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author prasad
 *
 */
@Singleton
public class Config {

    private static final Logger LOGGER = LogManager.getLogger(Config.class);
    private static final String CONFFILE = "cube.conf";
	public static final String DEFAULT_TRACE_FIELD = "x-b3-traceid";

	final Properties properties;
	final SolrClient solr;
	public final ReqRespStore rrstore;

	public final ObjectMapper jsonmapper = CubeObjectMapperProvider.createDefaultMapper();
	
	public Config() throws Exception {
		LOGGER.info("Creating config");
		properties = new java.util.Properties();
		String solrurl = "http://18.191.135.125:8983/solr/cube";   // TODO: pass this default from kube conf
		try {
            properties.load(this.getClass().getClassLoader().
                    getResourceAsStream(CONFFILE));
            solrurl = properties.getProperty("solrurl");
        } catch(Exception eta){
            LOGGER.error(String.format("Not able to load config file %s; using defaults", CONFFILE), eta);
            eta.printStackTrace();
            LOGGER.info(String.format("Using default solrulr IP %s", solrurl));
        }
        if (solrurl != null) {
            solr = new HttpSolrClient.Builder(solrurl).build();
            rrstore = new ReqRespStoreSolr(solr, this);
        } else {
            final String msg = String.format("Solrurl missing in the config file %s", CONFFILE);
            LOGGER.error(msg);
            throw new Exception(msg);
        }

	}

	public String getProperty(String key)
	{
		String value = this.properties.getProperty(key);
		return value;
	}

}
