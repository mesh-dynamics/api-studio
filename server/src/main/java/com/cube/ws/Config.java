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

    private static final Logger LOGGER = LogManager.getLogger(ReqRespStoreSolr.class);
    private static final String CONFFILE = "cube.conf";
	public static final String DEFAULT_TRACE_FIELD = "x-b3-traceid";

	final Properties properties;
	final SolrClient solr;
	public final ReqRespStore rrstore;

	public final ObjectMapper jsonmapper = CubeObjectMapperProvider.createDefaultMapper();
	
	public Config() throws Exception {
		LOGGER.info("Creating config");
		properties = new java.util.Properties();
		try {
			properties.load(this.getClass().getClassLoader().
					getResourceAsStream(CONFFILE));
			final String solrurl = properties.getProperty("solrurl");
			if (solrurl != null) {
				solr = new HttpSolrClient.Builder(solrurl).build();
				rrstore = new ReqRespStoreSolr(solr, this);
			} else {
				final String msg = String.format("Solrurl missing in the config file %s", CONFFILE);
				LOGGER.error(msg);
				throw new Exception(msg);
			}
		} catch(Exception eta){
			LOGGER.error(String.format("Not able to load config file %s", CONFFILE), eta);
			eta.printStackTrace();
			throw eta;
		}
	}

	public String getProperty(String key)
	{
		String value = this.properties.getProperty(key);
		return value;
	}

}
