package io.cube.agent;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Optional;

import io.md.utils.FnKey;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-06
 * @author Prasad M D
 */
public interface Mocker {

    /**
     * @param fnKey The key to the function to mocked
     * @param prevRespTS The timestamp of the previous response
     * @param args The argument values as Java objects
     * @return The response value as Java object, along with a timestamp
     */
	FnResponseObj mock(FnKey fnKey,
		Optional<Instant> prevRespTS, Optional<Type> retType, Object... args);
}
