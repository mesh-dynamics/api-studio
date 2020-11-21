package io.md.logger;

import org.slf4j.Logger;

public class Slf4jLoggerFactory implements LoggerFactory {

    @Override
    public Logger getLogger(Class<?> clazz) {
        return org.slf4j.LoggerFactory.getLogger(clazz);
    }
}
