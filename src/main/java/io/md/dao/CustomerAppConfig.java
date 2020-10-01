package io.md.dao;

public class CustomerAppConfig {

    public final String tracer;

    private CustomerAppConfig(String tracer){
        this.tracer = tracer;
    }

    public static class Builder{
        private String tracer;

        public Builder withTracer(String tracer){
            this.tracer = tracer;
            return this;
        }

        public CustomerAppConfig build(){
            return new CustomerAppConfig(this.tracer);
        }
    }

}

