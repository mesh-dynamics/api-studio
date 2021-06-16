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

package io.cube.agent.logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.md.logger.LogMgr;
import io.md.logger.LogStoreDTO;
import io.md.utils.CubeObjectMapperProvider;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.slf4j.Logger;

public class LogUtils {
    private static Logger LOGGER = LogMgr.getLogger(LogUtils.class);

    private static ObjectMapper mapper = CubeObjectMapperProvider.getInstance();
    private static ObjectMapper msgPacker = CubeObjectMapperProvider.createMapper(new MessagePackFactory());

    public static String toJson(LogStoreDTO logStoreDTO){
        try{
            return mapper.writeValueAsString(logStoreDTO);
        }catch (Exception e){
          LOGGER.error("toJson error ", e);
          return logStoreDTO.logMessage;
        }
    }

    public static byte[] toMsgPack(LogStoreDTO logStoreDTO){
        try{
            return msgPacker.writeValueAsBytes(logStoreDTO);
        }catch (Exception e){
            LOGGER.error("toMsgPack error ", e);
            return logStoreDTO.logMessage.getBytes();
        }
    }


}