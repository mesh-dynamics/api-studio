/*
 *
 *    Copyright Cube I O
 *
 */

package io.md.services;

import io.md.dao.MockWithCollection;
import java.time.Instant;
import java.util.Optional;

import javax.ws.rs.core.MultivaluedMap;

import io.md.dao.Event;

/*
 * Created by IntelliJ IDEA.
 * Date: 14/05/20
 */
public interface Mocker {

    MockResponse mock(Event reqEvent, Optional<Instant> lowerBoundForMatching, Optional<MockWithCollection> mockWithCollections) throws MockerException;

    class MockerException extends Exception {
        public final String errorType;

        public MockerException(String errorType, String message) {
            super(message);
            this.errorType = errorType;
        }
    }

}
