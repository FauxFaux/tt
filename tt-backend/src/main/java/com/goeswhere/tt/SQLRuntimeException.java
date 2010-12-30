package com.goeswhere.tt;

import java.sql.SQLException;

public class SQLRuntimeException extends RuntimeException {

	public SQLRuntimeException(SQLException cause) {
		super(cause);
	}

	public SQLRuntimeException(String cause) {
		super(cause);
	}

}
