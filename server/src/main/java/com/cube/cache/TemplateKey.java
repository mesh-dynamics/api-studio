package com.cube.cache;


import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;

/**
 * Key against which the analysis template will be retrieved/cached
 */
public class TemplateKey {
    private String customerId;
    private String appId;
    private String serviceId;
    private String path;

    public TemplateKey(String customerId, String appId, String serviceId, String path) {
        this.customerId = customerId;
        this.appId = appId;
        this.serviceId = serviceId;
        this.path = path;
    }


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("customerId" , getCustomerId()).add("appId" , getAppId())
                .add("serviceId" , getServiceId()).add("path" , getPath()).toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getCustomerId(), this.getAppId(), this.getServiceId(), this.getPath());
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TemplateKey) {
            TemplateKey other = (TemplateKey) o;
            return (ComparisonChain.start().
                    compare(this.getCustomerId(), other.getCustomerId()).compare(this.getAppId(), other.getAppId()).
                    compare(this.getServiceId(), other.getServiceId()).compare(this.getPath(), other.getPath()).result() == 0);
        } else {
            return false;
        }
    }


    public String getCustomerId() {
        return customerId;
    }

    public String getAppId() {
        return appId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getPath() {
        return path;
    }
}