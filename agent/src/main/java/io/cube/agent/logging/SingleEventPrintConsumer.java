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

package io.cube.agent.logging;


import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

import io.md.logger.LogMgr;
import org.clapper.util.io.RollingFileWriter;
import org.clapper.util.io.RollingFileWriter.Compression;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.lmax.disruptor.EventHandler;

import io.cube.agent.CommonConfig;
import io.cube.agent.Utils;
import io.md.utils.CubeObjectMapperProvider;

public class SingleEventPrintConsumer {
	private static final Logger LOGGER = LogMgr.getLogger(SingleEventPrintConsumer.class);


	private ObjectWriter objectWriter;
	private PrintWriter eventWriter;
	private JsonGenerator generator;

	public SingleEventPrintConsumer() {
		System.out.println("SingleEventPrintConsumer Constructor Called");
		if (CommonConfig.getInstance().disruptorOutputLocation.equals("stdout")) {
			eventWriter = new PrintWriter(System.out);
		} else {
			try {
				eventWriter = new RollingFileWriter(
					CommonConfig.getInstance().disruptorFileOutName
						+ "-" + UUID.randomUUID().toString() + ".log${n}"
					, CommonConfig.getInstance().disruptorLogFileMaxSize
					, CommonConfig.getInstance().disruptorLogMaxBackup, Compression.DONT_COMPRESS_BACKUPS);
			} catch (Exception e) {
				eventWriter = Utils.nullPrintWriter(LOGGER);
				LOGGER.debug("Unable to find outstream file. Setting outstream as nullOutputStream ", e);
			}
		}
		try {
			ObjectMapper jsonMapper = CubeObjectMapperProvider.getInstance();
			objectWriter = jsonMapper.writer();
			generator = new JsonFactory().createGenerator(eventWriter);
		} catch (IOException e) {
			LOGGER.error("Unable to initialize json generator" ,e);
			throw new RuntimeException(e);
		}

	}

	public EventHandler<ValueEvent> getEventHandler() {
		return (event, sequence, endOfBatch  )
			-> {
			objectWriter.writeValue(generator, event);
			generator.flush();
			eventWriter.println();
			//generator.writeRaw("\n");
		};
	}
}
