package com.aegisql.conveyor.persistence.cleanup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.Expireable;
import com.aegisql.conveyor.Testing;
import com.aegisql.conveyor.TimeoutAction;
import com.aegisql.conveyor.persistence.core.Persistence;

// TODO: Auto-generated Javadoc
/**
 * The Class CleaunupBatchBuilder.
 *
 * @param <K> the key type
 */
public class CleaunupBatchBuilder <K> implements Supplier<Runnable>, Testing, TimeoutAction, Expireable {
	
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The log. */
	private static Logger LOG = LoggerFactory.getLogger(CleaunupBatchBuilder.class);

	/** The ready. */
	private boolean ready = false;
	
	/** The persistence. */
	private final Persistence<K> persistence;
	
	/** The summ batch size. */
	private final int summBatchSize;
	
	/** The expiration time. */
	private final long expirationTime;
	
	/** The cart ids. */
	private final Collection<Long> cartIds = new ArrayList<>();
	
	/** The keys. */
	private final Collection<K> keys    = new ArrayList<>();
	
	/**
	 * Instantiates a new cleaunup batch builder.
	 *
	 * @param persistence the persistence
	 */
	public CleaunupBatchBuilder(Persistence<K> persistence) {
		this.persistence   = persistence;
		this.summBatchSize = persistence.getMaxArchiveBatchSize();
		this.expirationTime = System.currentTimeMillis() + persistence.getMaxArchiveBatchTime();
	}
	
	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	@Override
	public Runnable get() {
		return ()->{
			LOG.debug("Archiving data: keys:{} ids:{}",keys, cartIds);
			persistence.archiveParts(cartIds);
			persistence.archiveKeys(keys);
			persistence.archiveCompleteKeys(keys);
			persistence.archiveExpiredParts();
		};
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Testing#test()
	 */
	@Override
	public boolean test() {
		return ready  || (cartIds.size()+keys.size() >= summBatchSize);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.TimeoutAction#onTimeout()
	 */
	@Override
	public void onTimeout() {
		ready = true;
	}

	/**
	 * Adds the cart id.
	 *
	 * @param <K> the key type
	 * @param builder the builder
	 * @param id the id
	 */
	public static <K> void addCartId(CleaunupBatchBuilder <K> builder, long id) {
		builder.cartIds.add(id);
	}

	/**
	 * Adds the cart ids.
	 *
	 * @param <K> the key type
	 * @param builder the builder
	 * @param ids the ids
	 */
	public static <K> void addCartIds(CleaunupBatchBuilder <K> builder, Collection<Long> ids) {
		builder.cartIds.addAll(ids);
	}

	/**
	 * Adds the key.
	 *
	 * @param <K> the key type
	 * @param builder the builder
	 * @param key the key
	 */
	public static <K> void addKey(CleaunupBatchBuilder <K> builder, K key) {
		builder.keys.add(key);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Expireable#getExpirationTime()
	 */
	@Override
	public long getExpirationTime() {
		return expirationTime;
	}

}
