package org.conveyor.persistence.ack;

import java.util.List;

import org.conveyor.persistence.cleanup.PersistenceCleanupBatchConveyor;
import org.conveyor.persistence.core.Persist;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.cart.Cart;

public class AcknowledgeBuildingConveyor <I, K> extends AssemblingConveyor<K, SmartLabel<AcknowledgeBuilder<K, I>>, List<I>> {
	
	public final SmartLabel<AcknowledgeBuilder<K, I>> CART     = SmartLabel.of("CART", (b,cart)->{ AcknowledgeBuilder.processCart(b, (Cart<K,?,?>)cart); });
	public final SmartLabel<AcknowledgeBuilder<K, I>> ACK      = SmartLabel.of("ACK", (b,key)->{ AcknowledgeBuilder.setAckKey(b, (K)key); });
	public final SmartLabel<AcknowledgeBuilder<K, I>> COMPLETE = SmartLabel.of("COMPLETE", (b,status)->{ AcknowledgeBuilder.complete(b, (Status)status); });

	public <L,OUT> AcknowledgeBuildingConveyor(Persist<K,I> persistence, Conveyor<K, L, OUT> forward, PersistenceCleanupBatchConveyor<K, I> cleaner) {
		super();
		this.setName("AcknowledgeBuildingConveyor<"+(forward == null ? "":forward.getName())+">");
		this.setBuilderSupplier( () -> new AcknowledgeBuilder<>(persistence, forward)  );
		this.resultConsumer(bin->{
			if(cleaner != null) {
				cleaner.part().label(cleaner.KEY).value(bin.key).place();
				cleaner.part().label(cleaner.CART_IDS).value(bin.product).place();
			}
		}).set();
	}
	
}
