/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.core;

import java.util.Collections;
import java.util.List;

import javax.annotation.Generated;

import com.cube.core.CompareTemplate.ComparisonType;


/*
 * Created by IntelliJ IDEA.
 * Date: 2019-03-14
 * @author Prasad M D
 */
public class RRMatchSpec {



    /**
     * @param mreqid
     * @param mmeta
     * @param metafields
     * @param mhdrs
     * @param hdrfields
     * @param mbody
     * @param mcollection
     * @param mtimestamp
     * @param mrrtype
     * @param mcustomerid
     * @param mapp
     */
    protected RRMatchSpec(ComparisonType mreqid, ComparisonType mmeta, List<String> metafields, ComparisonType mhdrs,
                          List<String> hdrfields, ComparisonType mbody, ComparisonType mcollection, ComparisonType mtimestamp, ComparisonType mrrtype,
                          ComparisonType mcustomerid, ComparisonType mapp) {
        super();
        this.mreqid = mreqid;
        this.mmeta = mmeta;
        this.metafields = metafields;
        this.mhdrs = mhdrs;
        this.hdrfields = hdrfields;
        this.mbody = mbody;
        this.mcollection = mcollection;
        this.mtimestamp = mtimestamp;
        this.mrrtype = mrrtype;
        this.mcustomerid = mcustomerid;
        this.mapp = mapp;
    }


    // indicates whether these fields should be matched
    protected final ComparisonType mreqid;
    protected final ComparisonType mmeta;
    protected final List<String> metafields;
    protected final ComparisonType mhdrs;
    protected final List<String> hdrfields;
    protected final ComparisonType mbody;
    protected final ComparisonType mcollection;
    protected final ComparisonType mtimestamp;
    protected final ComparisonType mrrtype;
    protected final ComparisonType mcustomerid;
    protected final ComparisonType mapp;
    @Generated("SparkTools")
    protected RRMatchSpec(Builder builder) {
        this.mreqid = builder.mreqid;
        this.mmeta = builder.mmeta;
        this.metafields = builder.metafields;
        this.mhdrs = builder.mhdrs;
        this.hdrfields = builder.hdrfields;
        this.mbody = builder.mbody;
        this.mcollection = builder.mcollection;
        this.mtimestamp = builder.mtimestamp;
        this.mrrtype = builder.mrrtype;
        this.mcustomerid = builder.mcustomerid;
        this.mapp = builder.mapp;
    }
    /**
     * Creates builder to build {@link RRMatchSpec}.
     * @return created builder
     */
    @Generated("SparkTools")
    public static Builder builder() {
        return new Builder();
    }


    /**
     * Builder to build {@link RRMatchSpec}.
     */
    @Generated("SparkTools")
    public static class Builder {
        private ComparisonType mreqid = ComparisonType.Ignore;
        private ComparisonType mmeta = ComparisonType.Ignore;
        private List<String> metafields = Collections.emptyList();
        private ComparisonType mhdrs = ComparisonType.Ignore;
        private List<String> hdrfields = Collections.emptyList();
        private ComparisonType mbody = ComparisonType.Ignore;
        private ComparisonType mcollection = ComparisonType.Ignore;
        private ComparisonType mtimestamp = ComparisonType.Ignore;
        private ComparisonType mrrtype = ComparisonType.Ignore;
        private ComparisonType mcustomerid = ComparisonType.Ignore;
        private ComparisonType mapp = ComparisonType.Ignore;

        protected Builder() {
        }

        public Builder withMreqid(ComparisonType mreqid) {
            this.mreqid = mreqid;
            return this;
        }

        public Builder withMmeta(ComparisonType mmeta) {
            this.mmeta = mmeta;
            return this;
        }

        public Builder withMetafields(List<String> metafields) {
            this.metafields = metafields;
            return this;
        }

        public Builder withMhdrs(ComparisonType mhdrs) {
            this.mhdrs = mhdrs;
            return this;
        }

        public Builder withHdrfields(List<String> hdrfields) {
            this.hdrfields = hdrfields;
            return this;
        }

        public Builder withMbody(ComparisonType mbody) {
            this.mbody = mbody;
            return this;
        }

        public Builder withMcollection(ComparisonType mcollection) {
            this.mcollection = mcollection;
            return this;
        }

        public Builder withMtimestamp(ComparisonType mtimestamp) {
            this.mtimestamp = mtimestamp;
            return this;
        }

        public Builder withMrrtype(ComparisonType mrrtype) {
            this.mrrtype = mrrtype;
            return this;
        }

        public Builder withMcustomerid(ComparisonType mcustomerid) {
            this.mcustomerid = mcustomerid;
            return this;
        }

        public Builder withMapp(ComparisonType mapp) {
            this.mapp = mapp;
            return this;
        }

        public RRMatchSpec build() {
            return new RRMatchSpec(this);
        }
    }



}
