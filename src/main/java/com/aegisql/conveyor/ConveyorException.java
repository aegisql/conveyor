package com.aegisql.conveyor;

public class ConveyorException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ConveyorException() {
		super();
	}

	public ConveyorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ConveyorException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConveyorException(String message) {
		super(message);
	}

	public ConveyorException(Throwable cause) {
		super(cause);
	}

}
