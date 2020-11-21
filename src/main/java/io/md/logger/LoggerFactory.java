package io.md.logger;

import org.slf4j.Logger;

public interface LoggerFactory {

    public Logger getLogger(Class<?> clazz);
}
