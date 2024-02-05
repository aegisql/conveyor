package com.aegisql.conveyor.persistence.ack;

import com.aegisql.conveyor.AcknowledgeStatus;
import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.persistence.core.Persistence;

import java.util.concurrent.TimeUnit;

// TODO: Auto-generated Javadoc
/**
 * The Class AcknowledgeBuildingConveyor.
 *
 * @param <K> the key type
 */
public class AcknowledgeBuildingConveyor <K> extends AssemblingConveyor<K, SmartLabel<AcknowledgeBuilder<K>>, Boolean> {
	
	/** The cart. */
	public final SmartLabel<AcknowledgeBuilder<K>> CART     = SmartLabel.of("CART", (b,cart)-> AcknowledgeBuilder.processCart(b, (Cart<K,?,?>)cart));
	
	/** The complete. */
	public final SmartLabel<AcknowledgeBuilder<K>> COMPLETE = SmartLabel.of("COMPLETE", (b,key)-> AcknowledgeBuilder.complete(b, (AcknowledgeStatus<K>)key));
	
	/** The replay. */
	public final SmartLabel<AcknowledgeBuilder<K>> REPLAY   = SmartLabel.of("REPLAY", (b,key)-> AcknowledgeBuilder.replay(b, (K)key));
	
	/** The mode. */
	public final SmartLabel<AcknowledgeBuilder<K>> MODE     = SmartLabel.of("MODE", (b,mode)-> AcknowledgeBuilder.setMode(b, (Boolean)mode));
	
	/** The unload enabled. */
	public final SmartLabel<AcknowledgeBuilder<K>> UNLOAD_ENABLED = SmartLabel.of("UNLOAD_ENABLED", (b,mode)-> AcknowledgeBuilder.setUnloadMode(b, (Boolean)mode));
	
	/** The unload. */
	public final SmartLabel<AcknowledgeBuilder<K>> UNLOAD   = SmartLabel.of("UNLOAD", (b,key)-> AcknowledgeBuilder.unload(b, (AcknowledgeStatus<K>)key));

	/** The compact. */
	public final SmartLabel<AcknowledgeBuilder<K>> COMPACT   = SmartLabel.of("COMPACT", (b,key)-> AcknowledgeBuilder.compact(b, (K)key));

	public final SmartLabel<AcknowledgeBuilder<K>> MIN_COMPACT   = SmartLabel.of("MIN_COMPACT", (b,key)-> AcknowledgeBuilder.minCompactSize(b, (Integer)key));

	/**
	 * Instantiates a new acknowledge building conveyor.
	 *
	 * @param <L> the generic type
	 * @param <OUT> the generic type
	 * @param persistence the persistence
	 * @param forward the forward
	 */
	public <L,OUT> AcknowledgeBuildingConveyor(Persistence<K> persistence, Conveyor<K, L, OUT> forward) {
		super();
		this.setName("AcknowledgeBuildingConveyor<"+(forward == null ? "":forward.getName())+">");
		this.setBuilderSupplier( () -> new AcknowledgeBuilder<>(persistence, forward, this)  );
		this.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		this.resultConsumer(bin-> LOG.debug("{} {}",bin.key,bin.product ?" COMPLETE":" UNLOADED")).set();
	}

}
