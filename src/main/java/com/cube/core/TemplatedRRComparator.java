/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.core;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.cube.dao.RRBase;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-03-14
 * @author Prasad M D
 */
public class TemplatedRRComparator {


    public static CompareTemplate EQUALITYTEMPLATE = new CompareTemplate();
    static TemplateEntry EQUALITYRULE = new TemplateEntry("/body", CompareTemplate.DataType.Str,
        CompareTemplate.PresenceType.Required, CompareTemplate.ComparisonType.Equal, CompareTemplate.ExtractionMethod.Default);
    protected final CompareTemplate template;
    protected final CompareTemplate bodytemplate; // to be used for the body field
    protected final CompareTemplate hdrFieldTemplate;
    protected final CompareTemplate metaFieldtemplate;
    protected final Comparator bodyComparator;

    public TemplatedRRComparator(CompareTemplate template, ObjectMapper jsonmapper) {
        this.template = template;


        // the fields below are computed and stored for efficiency purposes, so that we don't
        // have to redo the work on each response compare
        bodytemplate = template.subsetWithPrefix(RRBase.BODYPATH);
        hdrFieldTemplate = template.subsetWithPrefix(RRBase.HDRPATH);
        metaFieldtemplate = template.subsetWithPrefix(RRBase.METAPATH);
        bodyComparator = new JsonComparator(bodytemplate, jsonmapper);
    }
}
