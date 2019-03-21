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

    static {
        EQUALITYTEMPLATE.addRule(EQUALITYRULE);
    };

    /**
     *
     * @param template
     * @param jsonmapper
     */
    public TemplatedResponseComparator(CompareTemplate template, ObjectMapper jsonmapper) {
        super(template, jsonmapper);
    }

    @Override
    public Comparator.Match compare(Response lhs, Response rhs) {
        return lhs.compare(rhs, template, metaFieldtemplate, hdrFieldTemplate, bodyComparator);
    }

}
