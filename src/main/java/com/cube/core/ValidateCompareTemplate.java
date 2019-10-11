package com.cube.core;

import java.util.Optional;

public class ValidateCompareTemplate extends Validate {
    public ValidateCompareTemplate() {
     super();
    }

    public ValidateCompareTemplate(boolean isValid, Optional<String> message) {
        super(isValid, message);
    }

}
