package com.aegisql.conveyor.persistence.ack;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aegisql.conveyor.AcknowledgeStatus;
import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.persistence.cleanup.PersistenceCleanupBatchConveyor;
import com.aegisql.conveyor.persistence.core.Persistence;

public class AcknowledgeBuildingConveyor <K> extends AssemblingConveyor<K, SmartLabel<AcknowledgeBuilder<K>>, List<Long>> {
	
	public final SmartLabel<AcknowledgeBuilder<K>> CART     = SmartLabel.of("CART", (b,cart)->{ AcknowledgeBuilder.processCart(b, (Cart<K,?,?>)cart); });
	public final SmartLabel<AcknowledgeBuilder<K>> COMPLETE = SmartLabel.of("COMPLETE", (b,key)->{ AcknowledgeBuilder.complete(b, (AcknowledgeStatus<K>)key); });
	public final SmartLabel<AcknowledgeBuilder<K>> REPLAY   = SmartLabel.of("REPLAY", (b,key)->{ AcknowledgeBuilder.replay(b, (K)key); });
	public final SmartLabel<AcknowledgeBuilder<K>> MODE     = SmartLabel.of("MODE", (b,mode)->{ AcknowledgeBuilder.setMode(b, (Boolean)mode); });
	public final SmartLabel<AcknowledgeBuilder<K>> UNLOAD_ENABLED = SmartLabel.of("UNLOAD_ENABLED", (b,mode)->{ AcknowledgeBuilder.setUnloadMode(b, (Boolean)mode); });
	public final SmartLabel<AcknowledgeBuilder<K>> UNLOAD   = SmartLabel.of("UNLOAD", (b,key)->{ AcknowledgeBuilder.unload(b, (AcknowledgeStatus<K>)key); });
	
	private final AtomicBoolean initializationMode = new AtomicBoolean(true);

	public <L,OUT> AcknowledgeBuildingConveyor(Persistence<K> persistence, Conveyor<K, L, OUT> forward, PersistenceCleanupBatchConveyor<K> cleaner) {
		super();
		this.setName("AcknowledgeBuildingConveyor<"+(forward == null ? "":forward.getName())+">");
		this.setBuilderSupplier( () -> new AcknowledgeBuilder<>(persistence, forward, this)  );
		this.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		this.resultConsumer(bin->{
			if(cleaner != null) {
				if( bin.product != null) {
					cleaner.part().label(cleaner.KEY).value(bin.key).place();
					cleaner.part().label(cleaner.CART_IDS).value(bin.product).place();
				}
			}
		}).set();
	}
	
	public void setInitializationMode(boolean mode) {
		this.initializationMode.set(mode);
	}
	
}
