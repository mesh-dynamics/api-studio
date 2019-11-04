package com.cube.cache;


import java.util.Optional;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;

import com.cube.dao.Recording;

/**
 * Key against which the analysis template will be retrieved/cached
 */
public class TemplateKey {

    private String customerId;
    private String appId;
    private String serviceId;
    private String path;
    private String version; // this is the version of the TemplateSet
    private Type reqOrResp;

    public Type getReqOrResp() {
        return reqOrResp;
    }

    public enum Type {
        Request,
        Response
    }

    public TemplateKey(String version, String customerId, String appId, String serviceId, String path , Type reqOrResp) {
        this(customerId,appId,serviceId,path,reqOrResp);
        this.version = version;
    }

    private TemplateKey(String customerId, String appId, String serviceId, String path , Type reqOrResp) {
        this.customerId = customerId;
        this.appId = appId;
        this.serviceId = serviceId;
        this.path = path;
        this.reqOrResp = reqOrResp;
    }

    public TemplateKey() {

    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("customerId" , getCustomerId()).add("appId" , getAppId())
                .add("serviceId" , getServiceId()).add("path" , getPath())
                .add("version" , getVersion()).add("type" , getReqOrResp()).toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getCustomerId(), this.getAppId(), this.getServiceId()
                , this.getPath() , this.getVersion(), this.getReqOrResp());
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TemplateKey) {
            TemplateKey other = (TemplateKey) o;
            return Objects.equal(this.getCustomerId(), other.getCustomerId())
                && Objects.equal(this.getAppId(), other.getAppId())
                && Objects.equal(this.getServiceId(), other.getServiceId())
                && Objects.equal(this.getPath(), other.getPath())
                && Objects.equal(this.getVersion(), other.getVersion())
                && Objects.equal(this.getReqOrResp() , other.getReqOrResp());
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

    public String getVersion() { return version; }
}
