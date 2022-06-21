package com.aegisql.conveyor.persistence.cleanup;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.loaders.FutureLoader;
import com.aegisql.conveyor.loaders.PartLoader;
import com.aegisql.conveyor.persistence.core.Persistence;


// TODO: Auto-generated Javadoc
/**
 * The Class PersistenceCleanupBatchConveyor.
 *
 * @param <K> the key type
 */
public class PersistenceCleanupBatchConveyor <K> extends AssemblingConveyor<SingleKey, SmartLabel<CleaunupBatchBuilder<K>>, Runnable> {

	/**
	 * The Class Batch.
	 */
	public static class Batch {}
	
	/** The cart id. */
	public final SmartLabel<CleaunupBatchBuilder<K>> CART_ID  = SmartLabel.of("CART_ID", (b,id)->CleaunupBatchBuilder.addCartId(b, (Long)id));
	
	/** The cart ids. */
	public final SmartLabel<CleaunupBatchBuilder<K>> CART_IDS = SmartLabel.of("CART_IDS", (b,ids)->CleaunupBatchBuilder.addCartIds(b, (Collection<Long>)ids ));
	
	/** The key. */
	public final SmartLabel<CleaunupBatchBuilder<K>> KEY      = SmartLabel.of("KEY", (b,key)->CleaunupBatchBuilder.addKey(b, (K)key));

	/**
	 * Instantiates a new persistence cleanup batch conveyor.
	 *
	 * @param persistence the persistence
	 */
	public PersistenceCleanupBatchConveyor(Persistence<K> persistence) {
		super();
		this.setName("PersistenceCleanupBatchConveyor");
		this.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		this.setBuilderSupplier( () -> new CleaunupBatchBuilder<>(persistence)  );
		this.resultConsumer(bin-> bin.product.run()).set();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.AssemblingConveyor#part()
	 */
	@Override
	public PartLoader<SingleKey, SmartLabel<CleaunupBatchBuilder<K>>> part() {
		return super.part().id(SingleKey.BATCH);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.AssemblingConveyor#future()
	 */
	@Override
	public FutureLoader<SingleKey, Runnable> future() {
		return super.future().id(SingleKey.BATCH);
	}
	
	
}
