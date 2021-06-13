/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.md.utils;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.md.logger.LogMgr;
import org.slf4j.Logger;

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


	static Logger LOGGER = LogMgr.getLogger(MeshDGsonProvider.class);

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
