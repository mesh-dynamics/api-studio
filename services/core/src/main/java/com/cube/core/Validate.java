package com.cube.core;

import java.util.Optional;

public class Validate {
    protected final boolean isValid;
    protected final Optional<String> message;

    public Validate() {
        this.isValid = false;
        this.message = Optional.empty();
    }

    public Validate(boolean isValid, Optional<String> message) {
        this.isValid = isValid;
        this.message = message;
    }

    public boolean isValid() {
        return isValid;
    }

    public String getMessage() {
        return message.orElse("");
    }

    public boolean getStatus() {
        return isValid;
    }

}
