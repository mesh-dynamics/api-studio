/*
 *
 *    Copyright Cube I O
 *
 */

package io.md.services;

import java.util.Optional;

import javax.ws.rs.core.MultivaluedMap;

import io.md.dao.Event;

/*
 * Created by IntelliJ IDEA.
 * Date: 14/05/20
 */
public interface Mocker {

    FnResponse mockFunction(Event event) throws MockerException;

    Optional<Event> getResp(MultivaluedMap<String, String> queryParams, String path,
                            MultivaluedMap<String, String> formParams,
                            String customerId, String app, String instanceId,
                            String service, String method, String body,
                            MultivaluedMap<String, String> headers);

    class MockerException extends Exception {
        public final String errorType;

        public MockerException(String errorType, String message) {
            super(message);
            this.errorType = errorType;
        }
    }

}
