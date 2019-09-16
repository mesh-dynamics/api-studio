/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.core;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Generated;
import javax.ws.rs.core.MultivaluedMap;

import com.cube.core.Comparator.MatchType;
import com.cube.dao.Request;
import com.cube.core.CompareTemplate.ComparisonType;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-03-13
 * @author Prasad M D
 */
public class ReqMatchSpec extends RRMatchSpec implements RequestComparator {



    /**
     * @param mpath
     * @param mqparams
     * @param qparamfields
     * @param mfparams
     * @param fparamfields
     * @param mmethod
     */
    /*
    private ReqMatchSpec(ComparisonType mreqid, ComparisonType mmeta, List<String> metafields, ComparisonType mhdrs,
                         List<String> hdrfields, ComparisonType mbody, ComparisonType mcollection, ComparisonType mtimestamp, ComparisonType mrrtype,
                         ComparisonType mcustomerid, ComparisonType mapp, ComparisonType mpath, ComparisonType mqparams, List<String> qparamfields, ComparisonType mfparams,
                         List<String> fparamfields, ComparisonType mmethod) {
        super(mreqid, mmeta, metafields, mhdrs, hdrfields, mbody, mcollection, mtimestamp, mrrtype, mcustomerid, mapp);
        this.mpath = mpath;
        this.mqparams = mqparams;
        this.qparamfields = qparamfields;
        this.mfparams = mfparams;
        this.fparamfields = fparamfields;
        this.mmethod = mmethod;
    }
    */

    public final ComparisonType mpath;
    public final ComparisonType mqparams;
    public final List<String> qparamfields;
    public final ComparisonType mfparams;
    public final List<String> fparamfields;
    public final ComparisonType mmethod;
    private final List<PathCT> cthdrFields;
    private final List<PathCT> ctmetaFields;
    private final List<PathCT> ctqparamFields;
    private final List<PathCT> ctfparamFields;

    @Generated("SparkTools")
    private ReqMatchSpec(Builder builder) {
        super(builder);
        this.mpath = builder.mpath;
        this.mqparams = builder.mqparams;
        this.qparamfields = builder.qparamfields;
        this.mfparams = builder.mfparams;
        this.fparamfields = builder.fparamfields;
        this.mmethod = builder.mmethod;
        this.cthdrFields = this.hdrfields.stream().map(f -> new PathCT(f, this.mhdrs)).collect(Collectors.toList());
        this.ctmetaFields = this.metafields.stream().map(f -> new PathCT(f, this.mmeta)).collect(Collectors.toList());
        this.ctqparamFields = this.qparamfields.stream().map(f -> new PathCT(f, this.mqparams)).collect(Collectors.toList());
        this.ctfparamFields = this.fparamfields.stream().map(f -> new PathCT(f, this.mfparams)).collect(Collectors.toList());
    }


    /**
     * This function is used for checking if results returned from the store actually matched completely
     * Relevant when the matching spec allows for optional matching of some fields
     * "this" will be the query request
     * @param rhs the matched request from the store
     * @return
     */
    @Override
    public MatchType compare(Request lhs, Request rhs) {

        MatchType ret = MatchType.ExactMatch;

        if ((ret = ret.And(checkMatch(mreqid, lhs.reqid, rhs.reqid))) == MatchType.NoMatch) return ret;
        if ((ret = ret.And(checkMatch(mmeta, lhs.meta, rhs.meta, metafields))) == MatchType.NoMatch) return ret;
        if ((ret = ret.And(checkMatch(mhdrs, lhs.hdrs, rhs.hdrs, hdrfields))) == MatchType.NoMatch) return ret;
        if ((ret = ret.And(checkMatch(mbody, lhs.body, rhs.body))) == MatchType.NoMatch) return ret;
        if ((ret = ret.And(checkMatch(mcollection, lhs.collection, rhs.collection))) == MatchType.NoMatch) return ret;
        if ((ret = ret.And(checkMatch(mtimestamp, lhs.timestamp, rhs.timestamp))) == MatchType.NoMatch) return ret;
        if ((ret = ret.And(checkMatch(mrrtype, lhs.rrtype, rhs.rrtype))) == MatchType.NoMatch) return ret;
        if ((ret = ret.And(checkMatch(mcustomerid, lhs.customerid, rhs.customerid))) == MatchType.NoMatch) return ret;
        if ((ret = ret.And(checkMatch(mapp, lhs.app, rhs.app))) == MatchType.NoMatch) return ret;

        if ((ret = ret.And(checkMatch(mpath, lhs.path, rhs.path))) == MatchType.NoMatch) return ret;
        if ((ret = ret.And(checkMatch(mqparams, lhs.qparams, rhs.qparams, qparamfields))) == MatchType.NoMatch) return ret;
        if ((ret = ret.And(checkMatch(mfparams, lhs.fparams, rhs.fparams, fparamfields))) == MatchType.NoMatch) return ret;
        if ((ret = ret.And(checkMatch(mmethod, lhs.method, rhs.method))) == MatchType.NoMatch) return ret;

        return ret;

    }

    @Override
    public ComparisonType getCTreqid() {
        return mreqid;
    }

    @Override
    public List<PathCT> getCTMeta() {
        return ctmetaFields;
    }


    @Override
    public ComparisonType getCTbody() {
        return mbody;
    }

    @Override
    public ComparisonType getCTcollection() {
        return mcollection;
    }

    @Override
    public ComparisonType getCTtimestamp() {
        return mtimestamp;
    }

    @Override
    public ComparisonType getCTrrtype() {
        return mrrtype;
    }

    @Override
    public ComparisonType getCTcustomerid() {
        return mcustomerid;
    }

    @Override
    public ComparisonType getCTapp() {
        return mapp;
    }


    @Override
    public List<PathCT> getCTHdrs() {
        return cthdrFields;
    }


    @Override
    public ComparisonType getCTpath() {
        return mpath;
    }

    @Override
    public ComparisonType getCTmethod() {
        return mmethod;
    }

    @Override
    public List<PathCT> getCTQparams() {
        return ctqparamFields;
    }

    @Override
    public List<PathCT> getCTFparams() {
        return ctfparamFields;
    }

    @Override
    public CompareTemplate getCompareTemplate() {
        //  TODO: Not implemented. This class itself can be removed later
        return null;
    }

    /**
     * @param mt
     * @param thisfmap
     * @param fmap
     * @return
     */
    private MatchType checkMatch(ComparisonType mt, MultivaluedMap<String, String> thisfmap,
                                    MultivaluedMap<String, String> fmap, List<String> fieldstomatch) {

        if (mt == ComparisonType.Equal || mt == ComparisonType.EqualOptional) {
            MatchType ret = MatchType.ExactMatch;
            for (String f : fieldstomatch) {
                List<String> thisfvals = thisfmap.get(f);
                List<String> fvals = fmap.get(f);
                // check if all values match
                if (!fvals.containsAll(thisfvals)) {
                    if (mt == ComparisonType.EqualOptional) { // for soft match, its ok to not match on the field val
                        ret = MatchType.FuzzyMatch;
                    } else {
                        ret = MatchType.NoMatch;
                        break;
                    }
                }
            }
            return ret;
        }
        return MatchType.ExactMatch; // default is match
    }

    // This will work for both String and Optional<String> values
    private static MatchType checkMatch(ComparisonType mt,
                                           Object thisfval, Object fval) {

        if (mt == ComparisonType.Equal || mt == ComparisonType.EqualOptional) {
            if (thisfval.equals(fval)) {
                return MatchType.ExactMatch;
            }
            if (mt == ComparisonType.EqualOptional) {// for soft match, its ok to not match on the field val
                return MatchType.FuzzyMatch;
            }
            return MatchType.NoMatch;
        }
        return MatchType.ExactMatch; // default is match
    }

    /**
     * Creates builder to build {@link ReqMatchSpec}.
     * @return created builder
     */
    @Generated("SparkTools")
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link ReqMatchSpec}.
     */
    @Generated("SparkTools")
    public static final class Builder extends RRMatchSpec.Builder {
        private ComparisonType mpath = ComparisonType.Ignore;
        private ComparisonType mqparams = ComparisonType.Ignore;
        private List<String> qparamfields = Collections.emptyList();
        private ComparisonType mfparams = ComparisonType.Ignore;
        private List<String> fparamfields = Collections.emptyList();
        private ComparisonType mmethod = ComparisonType.Ignore;

        private Builder() {
            super();
        }

        public Builder withMpath(ComparisonType mpath) {
            this.mpath = mpath;
            return this;
        }

        public Builder withMqparams(ComparisonType mqparams) {
            this.mqparams = mqparams;
            return this;
        }

        public Builder withQparamfields(List<String> qparamfields) {
            this.qparamfields = qparamfields;
            return this;
        }

        public Builder withMfparams(ComparisonType mfparams) {
            this.mfparams = mfparams;
            return this;
        }

        public Builder withFparamfields(List<String> fparamfields) {
            this.fparamfields = fparamfields;
            return this;
        }

        public Builder withMmethod(ComparisonType mmethod) {
            this.mmethod = mmethod;
            return this;
        }

        public ReqMatchSpec build() {
            return new ReqMatchSpec(this);
        }
    }



}
