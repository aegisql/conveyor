package com.aegisql.conveyor;

public class ConveyorRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ConveyorRuntimeException() {
	}

	public ConveyorRuntimeException(String message) {
		super(message);
	}

	public ConveyorRuntimeException(Throwable cause) {
		super(cause);
	}

	public ConveyorRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConveyorRuntimeException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
