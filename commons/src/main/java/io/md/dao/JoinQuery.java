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

package io.md.dao;

import io.md.constants.Constants;

import java.util.Collections;
import java.util.Map;

public class JoinQuery {
    public String getJoinFrom() {
        return joinFrom;
    }

    public String getJoinTo() {
        return joinTo;
    }

    public Map<String, String> getOrConds() {
        return orConds;
    }

    public Map<String, String> getAndConds() {
        return andConds;
    }

    public Map<String, String> getJoinParams() {
        return joinParams;
    }


    private final String joinFrom;
    private final String joinTo;
    private final Map<String,String> joinParams;

    private final Map<String , String> orConds;
    private final Map<String , String> andConds;

    private JoinQuery(Builder builder){
        this.joinFrom = builder.joinFrom;
        this.joinTo = builder.joinTo;
        this.orConds = builder.orConds;
        this.andConds = builder.andConds;
        this.joinParams = builder.joinParams;
    }

    public static class Builder{

        private String joinFrom = Constants.REQ_ID_FIELD;
        private String joinTo = Constants.REQ_ID_FIELD;
        private Map<String,String> joinParams =  Collections.EMPTY_MAP;

        private  Map<String , String> orConds = Collections.EMPTY_MAP;
        private  Map<String , String> andConds = Collections.EMPTY_MAP;

        public Builder(){}

        public Builder withJoinFrom(String from){
            joinFrom = from;
            return this;
        }

        public Builder withJoinTo(String to){
            joinTo = to;
            return this;
        }

        public Builder withOrConds(Map<String,String> or){
            orConds = or;
            return this;
        }

        public Builder withAndConds(Map<String,String> and){
            andConds = and;
            return this;
        }

        public Builder withJoinParams(Map<String,String> params){
            joinParams = params;
            return this;
        }

        public JoinQuery build(){
            return new JoinQuery(this);
        }

    }
}
