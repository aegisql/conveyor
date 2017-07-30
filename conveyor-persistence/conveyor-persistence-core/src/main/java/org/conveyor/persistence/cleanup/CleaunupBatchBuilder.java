package org.conveyor.persistence.cleanup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import org.conveyor.persistence.core.Persist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.Testing;
import com.aegisql.conveyor.TimeoutAction;

public class CleaunupBatchBuilder <K,I> implements Supplier<Runnable>, Testing, TimeoutAction {
	
	private static Logger LOG = LoggerFactory.getLogger(CleaunupBatchBuilder.class);

	private boolean ready = false;
	
	private final Persist<K,I> persistence;
	private final int summBatchSize;
	private final Collection<I> cartIds = new ArrayList<>();
	private final Collection<K> keys    = new ArrayList<>();
	
	public CleaunupBatchBuilder(Persist<K,I> persistence, int summBatchSize) {
		this.persistence   = persistence;
		this.summBatchSize = summBatchSize;
	}
	
	@Override
	public Runnable get() {
		return ()->{
			LOG.debug("Archiving data: keys:{} ids:{}",keys, cartIds);
			persistence.archiveData(cartIds);
			persistence.archiveKeys(keys);
			persistence.archiveAckKeys(keys);
		};
	}

	@Override
	public boolean test() {
		return ready  || (cartIds.size()+keys.size() >= summBatchSize);
	}

	@Override
	public void onTimeout() {
		ready = true;
	}

	public static <K,I> void addCartId(CleaunupBatchBuilder <K,I> builder, I id) {
		builder.cartIds.add(id);
	}

	public static <K,I> void addCartIds(CleaunupBatchBuilder <K,I> builder, Collection<I> ids) {
		builder.cartIds.addAll(ids);
	}

	public static <K,I> void addKey(CleaunupBatchBuilder <K,I> builder, K key) {
		builder.keys.add(key);
	}

}
