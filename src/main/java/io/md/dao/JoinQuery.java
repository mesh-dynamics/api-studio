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

    private final String joinFrom;
    private final String joinTo;

    private final Map<String , String> orConds;
    private final Map<String , String> andConds;

    private JoinQuery(Builder builder){
        this.joinFrom = builder.joinFrom;
        this.joinTo = builder.joinTo;
        this.orConds = builder.orConds;
        this.andConds = builder.andConds;
    }

    public static class Builder{

        private String joinFrom = Constants.REQ_ID_FIELD;
        private String joinTo = Constants.REQ_ID_FIELD;

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

        public JoinQuery build(){
            return new JoinQuery(this);
        }

    }
}
