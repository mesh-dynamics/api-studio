package com.cube.core;

import java.util.Optional;

public class ValidateCompareTemplate extends Validate {
    public ValidateCompareTemplate() {
        this.isValid = false;
        this.message = Optional.empty();
    }

    public ValidateCompareTemplate(boolean isValid, Optional<String> message) {
        this.isValid = isValid;
        this.message = message;
    }

}
