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

package com.cubeio.logcollector.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.md.logger.LogStoreDTO;
import io.md.utils.CubeObjectMapperProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

import java.util.Optional;

@Component
public class TextConverter {

    private static Logger LOGGER = LoggerFactory.getLogger(TextConverter.class);

    private ObjectMapper mapper = CubeObjectMapperProvider.getInstance();

    public Optional<LogStoreDTO> toLogStore(TextMessage message) {
        try{
            return Optional.ofNullable(mapper.readValue(message.getPayload() , LogStoreDTO.class)) ;
        }catch (Exception e){
            LOGGER.error("Text Message Parsing Failed ", e);
            return Optional.empty();
        }
    }
}
