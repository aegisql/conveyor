package com.aegisql.conveyor.config;

public class ConveyorConfigurationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ConveyorConfigurationException() {
		super();
	}

	public ConveyorConfigurationException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ConveyorConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConveyorConfigurationException(String message) {
		super(message);
	}

	public ConveyorConfigurationException(Throwable cause) {
		super(cause);
	}

}
