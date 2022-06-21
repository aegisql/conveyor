package com.aegisql.conveyor.exception;

// TODO: Auto-generated Javadoc

import java.io.Serial;

/**
 * The Class ConveyorRuntimeException.
 */
public class ConveyorRuntimeException extends RuntimeException {

	/** The Constant serialVersionUID. */
	@Serial
    private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new conveyor runtime exception.
	 */
	public ConveyorRuntimeException() {
	}

	/**
	 * Instantiates a new conveyor runtime exception.
	 *
	 * @param message the message
	 */
	public ConveyorRuntimeException(String message) {
		super(message);
	}

	/**
	 * Instantiates a new conveyor runtime exception.
	 *
	 * @param cause the cause
	 */
	public ConveyorRuntimeException(Throwable cause) {
		super(cause);
	}

	/**
	 * Instantiates a new conveyor runtime exception.
	 *
	 * @param message the message
	 * @param cause the cause
	 */
	public ConveyorRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Instantiates a new conveyor runtime exception.
	 *
	 * @param message the message
	 * @param cause the cause
	 * @param enableSuppression the enable suppression
	 * @param writableStackTrace the writable stack trace
	 */
	public ConveyorRuntimeException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
