package io.md.core;


import java.util.Objects;
import java.util.StringJoiner;

import org.jetbrains.annotations.NotNull;

/**
 * Key against which the analysis template will be retrieved/cached
 */
public class TemplateKey implements Comparable<TemplateKey> {

    private final String customerId;
    private final String appId;
    private final String serviceId;
    private final String path;
    private final String version; // this is the version of the TemplateSet
    private Type reqOrResp;

    public Type getReqOrResp() {
        return reqOrResp;
    }

    @Override
    public int compareTo(@NotNull TemplateKey o) {
        int ret = getCustomerId().compareTo(o.getCustomerId());
        if (ret == 0) ret = getAppId().compareTo(o.getAppId());
        if (ret == 0) ret = getServiceId().compareTo(o.getServiceId());
        if (ret == 0) ret = getPath().compareTo(o.getPath());
        if (ret == 0) ret = getVersion().compareTo(o.getVersion());
        if (ret == 0) ret = getReqOrResp().compareTo(o.getReqOrResp());
        return ret;
    }


    public enum Type {
        RequestMatch,
        RequestCompare,
        ResponseCompare,
        DontCare
    }

    /**
     * This constructor is only for jackson json deserialization
     **/
    private TemplateKey() {
        this.customerId = "";
        this.appId = "";
        this.serviceId = "";
        this.path = "";
        this.reqOrResp = Type.RequestMatch;
        this.version = "";
    }

    public TemplateKey(String version, String customerId, String appId, String serviceId, String path, Type reqOrResp) {
        this.customerId = customerId;
        this.appId = appId;
        this.serviceId = serviceId;
        this.path = path;
        this.reqOrResp = reqOrResp;
        this.version = version;
    }

    @Override
    public String toString() {
        // "TemplateKey{customerId=CubeCorp, appId=MovieInfo, serviceId=movieinfo,
        // path=minfo/listmovies, version=RespPartialMatch, type=RequestMatch}"
        return new StringJoiner(", ", this.getClass().getSimpleName() + "{", "}")
            .add("customerId=" + getCustomerId())
            .add("appId=" + getAppId())
            .add("serviceId=" + getServiceId())
            .add("path=" + getPath())
            .add("version=" + getVersion())
            .add("type=" + getReqOrResp())
            .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getCustomerId(), this.getAppId(), this.getServiceId()
                , this.getPath() , this.getVersion(), this.getReqOrResp());
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TemplateKey) {
            TemplateKey other = (TemplateKey) o;
            return Objects.equals(this.getCustomerId(), other.getCustomerId())
                && Objects.equals(this.getAppId(), other.getAppId())
                && Objects.equals(this.getServiceId(), other.getServiceId())
                && Objects.equals(this.getPath(), other.getPath())
                && Objects.equals(this.getVersion(), other.getVersion())
                && Objects.equals(this.getReqOrResp() , other.getReqOrResp());
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

    public boolean isResponseTemplate() {
        return this.reqOrResp == Type.ResponseCompare;
    }
}
