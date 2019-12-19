package io.md.core;

public enum ReplayTypeEnum {
	HTTP,
	THRIFT,
	GRPC;


	public static ReplayTypeEnum fromString(String replayType) {
		// Default
		if (replayType == null) {
			return HTTP;
		}

		switch (replayType.toUpperCase()) {
			case "THRIFT":
				return THRIFT;
			case "GRPC":
				return GRPC;
			case "HTTP":
			default:
				return HTTP;
		}
	}
}
