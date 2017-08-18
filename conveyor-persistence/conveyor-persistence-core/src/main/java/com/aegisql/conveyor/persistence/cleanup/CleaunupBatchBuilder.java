package com.aegisql.conveyor.persistence.cleanup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.Testing;
import com.aegisql.conveyor.TimeoutAction;
import com.aegisql.conveyor.persistence.core.Persistence;

public class CleaunupBatchBuilder <K> implements Supplier<Runnable>, Testing, TimeoutAction {
	
	private static Logger LOG = LoggerFactory.getLogger(CleaunupBatchBuilder.class);

	private boolean ready = false;
	
	private final Persistence<K> persistence;
	private final int summBatchSize;
	private final Collection<Long> cartIds = new ArrayList<>();
	private final Collection<K> keys    = new ArrayList<>();
	
	public CleaunupBatchBuilder(Persistence<K> persistence, int summBatchSize) {
		this.persistence   = persistence;
		this.summBatchSize = summBatchSize;
	}
	
	@Override
	public Runnable get() {
		return ()->{
			LOG.debug("Archiving data: keys:{} ids:{}",keys, cartIds);
			persistence.archiveParts(cartIds);
			persistence.archiveKeys(keys);
			persistence.archiveCompleteKeys(keys);
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

	public static <K> void addCartId(CleaunupBatchBuilder <K> builder, long id) {
		builder.cartIds.add(id);
	}

	public static <K> void addCartIds(CleaunupBatchBuilder <K> builder, Collection<Long> ids) {
		builder.cartIds.addAll(ids);
	}

	public static <K> void addKey(CleaunupBatchBuilder <K> builder, K key) {
		builder.keys.add(key);
	}

}