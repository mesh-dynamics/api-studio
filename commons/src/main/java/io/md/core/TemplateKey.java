package io.md.core;


import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final Optional<String> method;
    @JsonIgnore
    private String recordOrReplay;

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
        if (ret == 0) ret = getMethod().orElse("").compareTo(o.getMethod().orElse(""));
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
        method = Optional.empty();
        this.recordOrReplay = DEFAULT_RECORDING;
    }

    public static final String DEFAULT_RECORDING = "NA";

    public TemplateKey(String version, String customerId, String appId, String serviceId,
        String path, Type reqOrResp) {
        this(version, customerId, appId, serviceId, path, reqOrResp,
            Optional.empty(), DEFAULT_RECORDING);
    }

    public TemplateKey(String version, String customerId, String appId, String serviceId
        , String path, Type reqOrResp, Optional<String> method, String recordOrReplay) {
        this.customerId = customerId;
        this.appId = appId;
        this.serviceId = serviceId;
        this.path = path;
        this.reqOrResp = reqOrResp;
        this.version = version;
        this.method = method;
        this.recordOrReplay = recordOrReplay;
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
            .add("method=" + getMethod().orElse(""))
            .add("recordOrReplay=" + getRecording())
            .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getCustomerId(), this.getAppId(), this.getServiceId(),
            this.getPath() , this.getVersion(), this.getReqOrResp(), this.getMethod().orElse("") ,
            this.getRecording());
    }

    public boolean equals(Object o, boolean ignoreRecording) {
        if (o instanceof TemplateKey) {
            TemplateKey other = (TemplateKey) o;
            return Objects.equals(this.getCustomerId(), other.getCustomerId())
                && Objects.equals(this.getAppId(), other.getAppId())
                && Objects.equals(this.getServiceId(), other.getServiceId())
                && Objects.equals(this.getPath(), other.getPath())
                && Objects.equals(this.getVersion(), other.getVersion())
                && Objects.equals(this.getReqOrResp(), other.getReqOrResp())
                && Objects.equals(this.getMethod().orElse(""), other.getMethod().orElse(""))
                && (ignoreRecording || Objects.equals(this.getRecording(), other.getRecording()));
        } else {
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        return equals(o, false);
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

    public Optional<String> getMethod() {
        return this.method;
    }

    @JsonIgnore
    public String getRecording() {
        return recordOrReplay;
    }

    @JsonIgnore
    public String setRecording(String recording) {
        return recordOrReplay;
    }

    public String getVersion() { return version; }

    public boolean isResponseTemplate() {
        return this.reqOrResp == Type.ResponseCompare;
    }
}
