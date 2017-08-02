package org.conveyor.persistence.ack;

import java.util.List;

import org.conveyor.persistence.cleanup.PersistenceCleanupBatchConveyor;
import org.conveyor.persistence.core.Persistence;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.cart.Cart;

public class AcknowledgeBuildingConveyor <K> extends AssemblingConveyor<K, SmartLabel<AcknowledgeBuilder<K>>, List<Long>> {
	
	public final SmartLabel<AcknowledgeBuilder<K>> CART     = SmartLabel.of("CART", (b,cart)->{ AcknowledgeBuilder.processCart(b, (Cart<K,?,?>)cart); });
	public final SmartLabel<AcknowledgeBuilder<K>> READY    = SmartLabel.of("READY", (b,key)->{ AcknowledgeBuilder.keyReady(b, (K)key); });
	public final SmartLabel<AcknowledgeBuilder<K>> COMPLETE = SmartLabel.of("COMPLETE", (b,status)->{ AcknowledgeBuilder.complete(b, (Status)status); });
	public final SmartLabel<AcknowledgeBuilder<K>> REPLAY   = SmartLabel.of("REPLAY", (b,key)->{ AcknowledgeBuilder.replay(b, (K)key); });

	public <L,OUT> AcknowledgeBuildingConveyor(Persistence<K> persistence, Conveyor<K, L, OUT> forward, PersistenceCleanupBatchConveyor<K> cleaner) {
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
