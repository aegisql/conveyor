package org.conveyor.persistence.ack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.conveyor.persistence.core.Persist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.Testing;
import com.aegisql.conveyor.cart.Cart;

public class AcknowledgeBuilder <K,I> implements Supplier<List<I>>, Testing {
	
	private final static Logger LOG = LoggerFactory.getLogger(AcknowledgeBuilder.class);

	private final Persist<K,I> persistence;
	
	private final List<I> cartIds = new ArrayList<>();
	private final Conveyor<K, ?, ?> forward;
	
	private K ackKey = null;
	
	private boolean complete = false;
	
	public AcknowledgeBuilder(Persist<K,I> persistence, Conveyor<K, ?, ?> forward) {
		this.persistence = persistence;
		this.forward     = forward;
		LOG.debug("Created");
	}
	
	@Override
	public List<I> get() {
		return cartIds;
	}
	
	public static <K,I,L> void processCart(AcknowledgeBuilder <K,I> builder, Cart cart) {
		LOG.debug("CART "+cart);
		I id = null;
		if( ! cart.getAllProperties().containsKey("CART_ID") ) {
			id = builder.persistence.getUniqueId();
			cart.addProperty("CART_ID", id);
		} else {
			id = (I)cart.getProperty("CART_ID", Object.class);
		}
		builder.persistence.saveCart(id, cart);
		builder.cartIds.add(id);		
		if( builder.ackKey == null && builder.forward != null ) {
			builder.forward.place(cart);
		}			
	}
	
	public static <K,I,L> void setAckKey(AcknowledgeBuilder <K,I> builder, K key) {
		LOG.debug("ACK "+key);
		builder.ackKey = key;
	}

	public static <K,I,L> void complete(AcknowledgeBuilder <K,I> builder, Status status) {
		LOG.debug("COMPLETE "+status);
		builder.complete  = true;
	}

	@Override
	public boolean test() {
		return complete && ackKey != null;
	}

}
