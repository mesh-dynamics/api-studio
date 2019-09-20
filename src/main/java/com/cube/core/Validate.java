package com.cube.core;

import java.util.Optional;

public class Validate {
    protected boolean isValid;
    protected Optional<String> message;

    public boolean isValid() {
        return isValid;
    }

    public String getMessage() {
        return message.orElse("");
    }

    public void setMessage(Optional<String> message) {
        this.message = message;
    }

    public boolean getStatus() {
        return isValid;
    }

    public void setValid(boolean valid) {
        this.isValid = valid;
    }

}
