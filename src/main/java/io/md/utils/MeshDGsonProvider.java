package io.md.utils;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.md.gsonadapters.DateTypeAdapter;
import io.md.gsonadapters.TimeTypeAdapter;
import io.md.gsonadapters.TimestampTypeAdapter;

public class MeshDGsonProvider {

	private static Gson singleInstance = (new GsonBuilder())
		.registerTypeAdapter(
			Timestamp.class, new TimestampTypeAdapter())
		.registerTypeAdapter(Time.class, new TimeTypeAdapter())
		.registerTypeAdapter(Date.class, new DateTypeAdapter())
		.create();


	static Logger LOGGER = LoggerFactory.getLogger(MeshDGsonProvider.class);

	private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	public static Gson getInstance() {
		return singleInstance;
	}

	public static void setInstance(Gson gson) {
		lock.writeLock().lock();
		try {
			LOGGER.info("Re-setting gson instance in agent");
			singleInstance = gson;
		} finally {
			lock.writeLock().unlock();
		}
	}


}
