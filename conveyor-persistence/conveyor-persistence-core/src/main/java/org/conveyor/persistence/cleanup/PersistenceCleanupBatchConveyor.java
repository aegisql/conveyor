package org.conveyor.persistence.cleanup;

import java.util.Collection;

import org.conveyor.persistence.core.Persistence;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.loaders.FutureLoader;
import com.aegisql.conveyor.loaders.PartLoader;


public class PersistenceCleanupBatchConveyor <K> extends AssemblingConveyor<SingleKey, SmartLabel<CleaunupBatchBuilder<K>>, Runnable> {

	public static class Batch {}
	
	public final SmartLabel<CleaunupBatchBuilder<K>> CART_ID  = SmartLabel.of("CART_ID", (b,id)->CleaunupBatchBuilder.addCartId(b, (Long)id));
	public final SmartLabel<CleaunupBatchBuilder<K>> CART_IDS = SmartLabel.of("CART_IDS", (b,ids)->CleaunupBatchBuilder.addCartIds(b, (Collection<Long>)ids ));
	public final SmartLabel<CleaunupBatchBuilder<K>> KEY      = SmartLabel.of("KEY", (b,key)->CleaunupBatchBuilder.addKey(b, (K)key));

	public PersistenceCleanupBatchConveyor(Persistence<K> persistence,int batchSize) {
		super();
		this.setName("PersistenceCleanupBatchConveyor");
		this.setBuilderSupplier( () -> new CleaunupBatchBuilder<K>(persistence,batchSize)  );
		this.resultConsumer(bin->{
			bin.product.run();
		}).set();
	}

	@Override
	public PartLoader<SingleKey, SmartLabel<CleaunupBatchBuilder<K>>, ?, Runnable, Boolean> part() {
		return super.part().id(SingleKey.BATCH);
	}

	@Override
	public FutureLoader<SingleKey, Runnable> future() {
		return super.future().id(SingleKey.BATCH);
	}
	
	
}
