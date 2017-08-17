package com.aegisql.conveyor.persistence.ack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.Expireable;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.Testing;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.persistence.core.Persistence;

public class AcknowledgeBuilder<K> implements Supplier<List<Long>>, Testing, Expireable {

	private final static Logger LOG = LoggerFactory.getLogger(AcknowledgeBuilder.class);

	private final Persistence<K> persistence;

	private final Set<Long> cartIds = new LinkedHashSet<>();
	private final Conveyor<K, ?, ?> forward;

	private K keyReady = null;

	private boolean complete = false;

	public AcknowledgeBuilder(Persistence<K> persistence, Conveyor<K, ?, ?> forward) {
		this.persistence = persistence;
		this.forward = forward;
	}

	@Override
	public List<Long> get() {
		return new ArrayList<>(cartIds);
	}

	public static <K, L> void processCart(AcknowledgeBuilder<K> builder, Cart<K, ?, L> cart) {
		LOG.debug("CART " + cart);
		boolean save = false;
		Long id = null;
		if (!cart.getAllProperties().containsKey("CART_ID")) {
			id = builder.persistence.nextUniquePartId();
			cart.addProperty("CART_ID", id);
			save = true;
		} else {
			id = (Long) cart.getProperty("CART_ID", Long.class);
		}
		if (!builder.cartIds.contains(id)) {
			if(save) {
				LOG.debug("---- SAVING " + cart);
				builder.persistence.savePart(id, cart);
				builder.persistence.savePartId(cart.getKey(), id);
			}
			builder.cartIds.add(id);
			if (builder.keyReady == null && builder.forward != null) {
				builder.forward.place((Cart) cart);
			}
		} else {
			LOG.debug("Duplicate cart {}",cart.getKey());
		}
	}

	public static <K, L> void keyReady(AcknowledgeBuilder<K> builder, K key) {
		LOG.debug("ACK " + key);
		builder.keyReady = key;
	}

	public static <K, L> void complete(AcknowledgeBuilder<K> builder, Status status) {
		LOG.debug("COMPLETE " + status);
		builder.complete = true;
	}

	public static <K, L> void replay(AcknowledgeBuilder<K> builder, K key) {
		if (builder.keyReady != null) {
			LOG.debug("REPLAY " + key);
			//builder.cartIds.clear();
			Collection<Long> cartIds = builder.persistence.getAllPartIds(key);
			cartIds.forEach(id -> {
				Cart cart = builder.persistence.getPart(id);
				builder.forward.place(cart);
			});
		} else {
			LOG.debug("REPLAY NOT READY FOR " + key);
		}
	}

	@Override
	public boolean test() {
		return complete;// && keyReady != null;
	}

	// This build never expires.
	// Expiration is managed by receiving the COMPLETED message
	// From the working conveyor.
	@Override
	public long getExpirationTime() {
		return Long.MAX_VALUE;
	}

	@Override
	public String toString() {
		return "AcknowledgeBuilder [persistence=" + persistence + ", cartIds=" + cartIds + ", forward=" + forward
				+ ", keyReady=" + keyReady + ", complete=" + complete + "]";
	}

}
