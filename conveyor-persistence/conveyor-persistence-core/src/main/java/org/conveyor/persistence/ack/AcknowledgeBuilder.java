package org.conveyor.persistence.ack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.conveyor.persistence.core.Persist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.Testing;
import com.aegisql.conveyor.cart.Cart;

public class AcknowledgeBuilder <K> implements Supplier<List<Long>>, Testing {
	
	private final static Logger LOG = LoggerFactory.getLogger(AcknowledgeBuilder.class);

	private final Persist<K> persistence;
	
	private final List<Long> cartIds = new ArrayList<>();
	private final Conveyor<K, ?, ?> forward;
	
	private K keyReady = null;
	
	private boolean complete = false;
	
	public AcknowledgeBuilder(Persist<K> persistence, Conveyor<K, ?, ?> forward) {
		this.persistence = persistence;
		this.forward     = forward;
		LOG.debug("Created");
	}
	
	@Override
	public List<Long> get() {
		return cartIds;
	}
	
	public static <K,L> void processCart(AcknowledgeBuilder <K> builder, Cart cart) {
		LOG.debug("CART "+cart);
		Long id = null;
		if( ! cart.getAllProperties().containsKey("CART_ID") ) {
			id = builder.persistence.getUniqueId();
			cart.addProperty("CART_ID", id);
		} else {
			id = (Long) cart.getProperty("CART_ID", Long.class);
		}
		builder.persistence.saveCart(id, cart);
		builder.cartIds.add(id);		
		if( builder.keyReady == null && builder.forward != null ) {
			builder.forward.place(cart);
		}			
	}
	
	public static <K,L> void keyReady(AcknowledgeBuilder <K> builder, K key) {
		LOG.debug("ACK "+key);
		builder.keyReady = key;
	}

	public static <K,L> void complete(AcknowledgeBuilder <K> builder, Status status) {
		LOG.debug("COMPLETE "+status);
		builder.complete  = true;
	}

	public static <K,L> void replay(AcknowledgeBuilder <K> builder, K key) {
		if(builder.keyReady != null) {
			LOG.debug("REPLAY "+key);
			Collection<Long> cartIds = builder.persistence.getAllCartIds(key);
			cartIds.forEach(id -> {
				Cart cart = builder.persistence.getCart(id);
				builder.forward.place(cart);
			});
		} else {
			LOG.debug("REPLAY NOT READY FOR "+key);
		}
	}

	@Override
	public boolean test() {
		return complete && keyReady != null;
	}

}
