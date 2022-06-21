package com.aegisql.conveyor.persistence.core;

import java.io.Serial;

public class PersistenceException extends RuntimeException {

	@Serial
    private static final long serialVersionUID = 1L;

	public PersistenceException() {
	}

	public PersistenceException(String arg0) {
		super(arg0);
	}

	public PersistenceException(Throwable arg0) {
		super(arg0);
	}

	public PersistenceException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public PersistenceException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}

}
