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

package io.cube;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.cube.agent.ConsoleRecorder;
import io.cube.agent.IntentResolver;
import io.cube.agent.Mocker;
import io.cube.agent.Recorder;
import io.cube.agent.SimpleMocker;
import io.cube.agent.TraceIntentResolver;
import io.md.gsonadapters.DateTypeAdapter;
import io.md.gsonadapters.TimeTypeAdapter;
import io.md.gsonadapters.TimestampTypeAdapter;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;

// TODO make this config file singleton and inject it
public class Config {

	public IntentResolver intentResolver = new TraceIntentResolver();

	public final Recorder recorder;
	public final Mocker mocker;

	public Config() {
		Gson gson = new GsonBuilder().registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory())
			.registerTypeAdapter(Timestamp.class, new TimestampTypeAdapter())
			.registerTypeAdapter(Time.class, new TimeTypeAdapter())
			.registerTypeAdapter(Date.class, new DateTypeAdapter())
			.create();
		mocker = new SimpleMocker(gson);
		recorder = new ConsoleRecorder(gson);
	}
}
