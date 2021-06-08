package io.md.core;

import java.util.Optional;

public class Validate {
	public final boolean isValid;
	public final Optional<String> message;

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