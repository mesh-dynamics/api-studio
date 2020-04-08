package io.md.utils;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MeshDGsonProvider {

	private static Gson singleInstance = (new GsonBuilder()).create();

	static Logger LOGGER = LoggerFactory.getLogger(MeshDGsonProvider.class);

	private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	public static Gson getInstance() {
		lock.readLock().lock();
		try {
			return singleInstance;
		} finally {
			lock.readLock().unlock();
		}
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
