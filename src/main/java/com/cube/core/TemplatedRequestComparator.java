/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.core;

import com.cube.utils.Constants;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.cube.core.CompareTemplate.ComparisonType;
import com.cube.dao.Request;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-03-13
 * @author Prasad M D
 */
public class TemplatedRequestComparator extends TemplatedRRComparator implements RequestComparator {

    private final ComparisonType ctreqid;
    private final List<PathCT> ctmetaFields;
    private final List<PathCT> cthdrFields;
    private final ComparisonType ctbody;
    private final ComparisonType ctcollection;
    private final ComparisonType cttimestamp;
    private final ComparisonType ctrrtype;
    private final ComparisonType ctcustomerid;
    private final ComparisonType ctapp;
    private final ComparisonType ctpath;
    private final ComparisonType ctmethod;
    private final List<PathCT> ctqparamFields;
    private final List<PathCT> ctfparamFields;

    public TemplatedRequestComparator(CompareTemplate template, ObjectMapper jsonMapper) {
        super(template, jsonMapper);
        qparamFieldTemplate = template.subsetWithPrefix(Constants.QUERY_PARAMS_PATH);
        fparamFieldtemplate = template.subsetWithPrefix(Constants.FORM_PARAMS_PATH);
        ctreqid = template.getRule(Constants.REQ_ID_PATH).ct;
        ctmetaFields = metaFieldtemplate.getPathCTs();
        ctbody = template.getRule(Constants.BODY_PATH).ct;
        ctcollection = template.getRule(Constants.COLLECTION_PATH).ct;
        cttimestamp = template.getRule(Constants.TIMESTAMP_PATH).ct;
        ctrrtype = template.getRule(Constants.RUN_TYPE_PATH).ct;
        ctcustomerid = template.getRule(Constants.CUSTOMER_ID_PATH).ct;
        ctapp = template.getRule(Constants.APP_PATH).ct;
        cthdrFields = hdrFieldTemplate.getPathCTs();
        ctpath = template.getRule(Constants.PATH_PATH).ct;
        ctmethod = template.getRule(Constants.METHOD_PATH).ct;
        ctqparamFields = qparamFieldTemplate.getPathCTs();
        ctfparamFields = fparamFieldtemplate.getPathCTs();
    }

    @Override
    public Comparator.MatchType compare(Request lhs, Request rhs) {
        return lhs.compare(rhs, template, metaFieldtemplate, hdrFieldTemplate, bodyComparator, qparamFieldTemplate, fparamFieldtemplate);
    }

    @Override
    public ComparisonType getCTreqid() {
        return ctreqid;
    }

    @Override
    public List<PathCT> getCTMeta() {
        return ctmetaFields;
    }

    @Override
    public ComparisonType getCTbody() {
        return ctbody;
    }

    @Override
    public ComparisonType getCTcollection() {
        return ctcollection;
    }

    @Override
    public ComparisonType getCTtimestamp() {
        return cttimestamp;
    }

    @Override
    public ComparisonType getCTrrtype() {
        return ctrrtype;
    }

    @Override
    public ComparisonType getCTcustomerid() {
        return ctcustomerid;
    }

    @Override
    public ComparisonType getCTapp() {
        return ctapp;
    }

    @Override
    public List<PathCT> getCTHdrs() {
        return cthdrFields;
    }

    @Override
    public ComparisonType getCTpath() {
        return ctpath;
    }

    @Override
    public ComparisonType getCTmethod() {
        return ctmethod;
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
        return template;
    }

    protected final CompareTemplate qparamFieldTemplate;
    protected final CompareTemplate fparamFieldtemplate;

}
