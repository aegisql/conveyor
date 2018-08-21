package com.aegisql.conveyor;

public class KeepRunningConveyorException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public KeepRunningConveyorException() {
	}

	public KeepRunningConveyorException(String message) {
		super(message);
	}

	public KeepRunningConveyorException(Throwable cause) {
		super(cause);
	}

	public KeepRunningConveyorException(String message, Throwable cause) {
		super(message, cause);
	}

	public KeepRunningConveyorException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
