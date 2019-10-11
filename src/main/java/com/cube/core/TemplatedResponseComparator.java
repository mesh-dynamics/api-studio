/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.core;

import com.cube.dao.Response;
import com.fasterxml.jackson.databind.ObjectMapper;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-03-06
 * @author Prasad M D
 */
public class TemplatedResponseComparator extends TemplatedRRComparator implements ResponseComparator {


    // Removing static hard coded analysis template
    /*static TemplateEntry EQUALITYRULE = new TemplateEntry("/body", ResponseCompareTemplate.DataType.Str,
        ResponseCompareTemplate.PresenceType.Required, ResponseCompareTemplate.ComparisonType.Equal);
    public static ResponseCompareTemplate EQUALITYTEMPLATE = new ResponseCompareTemplate();
    static {
        EQUALITYTEMPLATE.addRule(EQUALITYRULE);
    };*/

    /**
     *
     * @param template
     * @param jsonMapper
     */
    public TemplatedResponseComparator(CompareTemplate template, ObjectMapper jsonMapper) {
        super(template, jsonMapper);
    }

    @Override
    public Comparator.Match compare(Response lhs, Response rhs) {
        return lhs.compare(rhs, template, metaFieldtemplate, hdrFieldTemplate, bodyComparator);
    }

}
