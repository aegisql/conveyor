package org.conveyor.persistence.ack;

import java.util.List;

import org.conveyor.persistence.core.Persist;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.cart.Cart;

public class AcknowledgeBuildingConveyor <I, K, L> extends AssemblingConveyor<K, SmartLabel<AcknowledgeBuilder<K, I, L>>, List<I>> {
	
	public final SmartLabel<AcknowledgeBuilder<K, I, L>> CART     = SmartLabel.of("CART", (b,cart)->{ AcknowledgeBuilder.processCart(b, (Cart<K,?,L>)cart); });
	public final SmartLabel<AcknowledgeBuilder<K, I, L>> ACK      = SmartLabel.of("ACK", (b,key)->{ AcknowledgeBuilder.setAckKey(b, (K)key); });
	public final SmartLabel<AcknowledgeBuilder<K, I, L>> COMPLETE = SmartLabel.of("COMPLETE", (b,key)->{ AcknowledgeBuilder.complete(b, (K)key); });

	public AcknowledgeBuildingConveyor(Persist<K,I> persistence, Conveyor<K, L, ?> forward, Conveyor<K, L, ?> cleaner) {
		super();
		this.setName("AcknowledgeBuildingConveyor<"+(forward == null ? "":forward.getName())+">");
		this.setBuilderSupplier( () -> new AcknowledgeBuilder<>(persistence, forward)  );
		this.resultConsumer(bin->{}).set();
		if(cleaner != null) {
			this.forwardResultTo(cleaner, null);
		}
		
	}
	
}
