package com.aegisql.conveyor.exception;

// TODO: Auto-generated Javadoc

import java.io.Serial;

/**
 * The Class KeepRunningConveyorException.
 */
public class KeepRunningConveyorException extends ConveyorRuntimeException {

	/** The Constant serialVersionUID. */
	@Serial
    private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new keep running conveyor exception.
	 */
	public KeepRunningConveyorException() {
	}

	/**
	 * Instantiates a new keep running conveyor exception.
	 *
	 * @param message the message
	 */
	public KeepRunningConveyorException(String message) {
		super(message);
	}

	/**
	 * Instantiates a new keep running conveyor exception.
	 *
	 * @param cause the cause
	 */
	public KeepRunningConveyorException(Throwable cause) {
		super(cause);
	}

	/**
	 * Instantiates a new keep running conveyor exception.
	 *
	 * @param message the message
	 * @param cause the cause
	 */
	public KeepRunningConveyorException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Instantiates a new keep running conveyor exception.
	 *
	 * @param message the message
	 * @param cause the cause
	 * @param enableSuppression the enable suppression
	 * @param writableStackTrace the writable stack trace
	 */
	public KeepRunningConveyorException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
