package com.aegisql.conveyor.cart.command;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.CommandLabel;
import com.aegisql.conveyor.cart.Cart;

import java.io.Serial;
import java.util.function.Supplier;

// TODO: Auto-generated Javadoc
/**
 * The Class CreateCommand.
 *
 * @param <K> the key type
 * @param <OUT> the generic type
 */
public class CreateCommand<K, OUT> extends GeneralCommand<K, BuilderSupplier<OUT>> implements Supplier<BuilderSupplier<OUT>> {

	/** The Constant serialVersionUID. */
	@Serial
    private static final long serialVersionUID = 4603066172969708346L;

	public CreateCommand(K k, BuilderSupplier<OUT> builderSupplier, long creation, long expiration) {
		super(k, builderSupplier, CommandLabel.CREATE_BUILD, creation, expiration);
	}

	public CreateCommand(K k, long creation, long expiration) {
		super(k, null, CommandLabel.CREATE_BUILD, creation, expiration);
	}

	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	@Override
	public BuilderSupplier<OUT> get() {
		return getValue();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#copy()
	 */
	@Override
	public Cart<K, BuilderSupplier<OUT>, CommandLabel> copy() {
		return new CreateCommand<>(getKey(), getValue(), getCreationTime(), getExpirationTime());
	}
	
}
