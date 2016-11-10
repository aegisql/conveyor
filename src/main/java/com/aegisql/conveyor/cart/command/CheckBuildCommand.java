package com.aegisql.conveyor.cart.command;

import com.aegisql.conveyor.CommandLabel;

// TODO: Auto-generated Javadoc
/**
 * The Class CheckBuildCommand.
 *
 * @param <K> the key type
 */
public class CheckBuildCommand<K> extends GeneralCommand<K, String> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4603066172969703859L;

	/**
	 * Instantiates a new check build command.
	 *
	 * @param k the k
	 */
	public CheckBuildCommand(K k) {
		super(k, null, CommandLabel.CHECK_BUILD);
	}

}
