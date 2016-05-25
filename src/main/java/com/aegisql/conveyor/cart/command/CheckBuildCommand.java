package com.aegisql.conveyor.cart.command;

import com.aegisql.conveyor.CommandLabel;

public class CheckBuildCommand<K> extends AbstractCommand<K, String> {

	private static final long serialVersionUID = 4603066172969703859L;

	public CheckBuildCommand(K k) {
		super(k, null, CommandLabel.CHECK_BUILD);
	}

}
