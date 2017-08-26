package com.aegisql.conveyor.persistence.ack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.AcknowledgeStatus;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.Expireable;
import com.aegisql.conveyor.Testing;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.persistence.core.Persistence;

public class AcknowledgeBuilder<K> implements Supplier<List<Long>>, Testing, Expireable {

	private static final long serialVersionUID = 1L;

	private final static Logger LOG = LoggerFactory.getLogger(AcknowledgeBuilder.class);

	private final Persistence<K> persistence;

	private Set<Long> cartIds = new LinkedHashSet<>();
	private final Conveyor<K, ?, ?> forward;
	private final AcknowledgeBuildingConveyor<K> ackConveyor;
	
	private boolean initializationMode = false;

	private boolean unloadEnabled      = false;

	private Long timestamp = Long.valueOf(0);
	
	private boolean complete = false;

	public AcknowledgeBuilder(Persistence<K> persistence, Conveyor<K, ?, ?> forward, AcknowledgeBuildingConveyor<K> ackConveyor) {
		this.persistence        = persistence;
		this.forward            = forward;
		this.ackConveyor        = ackConveyor;
	}

	@Override
	public List<Long> get() {
		if(cartIds == null) {
			return null;
		} else {
			return new ArrayList<>(cartIds);
		}
	}

	public static <K, L> void setMode(AcknowledgeBuilder<K> builder, Boolean mode) {
		builder.initializationMode = mode;
	}

	public static <K, L> void setUnloadMode(AcknowledgeBuilder<K> builder, Boolean mode) {
		builder.unloadEnabled = mode;
	}

	public static <K, L> void processCart(AcknowledgeBuilder<K> builder, Cart<K, ?, L> cart) {
		LOG.debug("CART " + cart);
		boolean save = false;
		K key = cart.getKey();
		Long id = null;
		builder.timestamp = System.nanoTime();
		if (!cart.getAllProperties().containsKey("#CART_ID")) {
			id = builder.persistence.nextUniquePartId();
			cart.addProperty("#CART_ID", id);
			cart.addProperty(id.toString(),"#CART_ID");
			save = true;
		} else {
			id = (Long) cart.getProperty("#CART_ID", Long.class);
		}
		
		if( ! builder.initializationMode ) {
			//savedIds.removeAll(builder.cartIds);
			if(builder.unloadEnabled && builder.cartIds.isEmpty()) {
				Set<Long> savedIds = new HashSet<>(builder.persistence.getAllPartIds(key));
				LOG.debug("RESTORE {}",savedIds);
				savedIds.forEach(i->{
				Cart<K,?,L> oldCart = builder.persistence.getPart(i);
				oldCart.addProperty("#TIMESTAMP", builder.timestamp);
				oldCart.addProperty(""+i, "#CART_ID");
				builder.forward.place((Cart) oldCart);
				builder.cartIds.add(i);
			});
			}
		} else {
			LOG.debug("INITIALIZING {}",cart.getKey());
		}
		
		if ( ! builder.cartIds.contains(id)) {
			if(save) {
				builder.persistence.savePart(id, cart);
				builder.persistence.savePartId(cart.getKey(), id);
			}
			builder.cartIds.add(id);
			cart.addProperty("#TIMESTAMP", builder.timestamp);
			if (builder.forward != null) {
				builder.forward.place((Cart) cart);
			}
		} else {
			LOG.debug("Duplicate cart {}",cart.getKey());
		}
	}

	public static <K, L> void unload(AcknowledgeBuilder<K> builder, AcknowledgeStatus<K> status) {
		Set<Long> siteIds    = new HashSet<>();
//		Set<Long> builderIds = new HashSet<>(builder.cartIds);
//		Set<Long> savedIds   = new HashSet<>(builder.persistence.getAllPartIds(status.getKey()));
		
		Long timestamp = Long.valueOf(0);
		
		if(status.getProperties() != null) {
			timestamp = (Long) status.getProperties().get("#TIMESTAMP");
			for(Map.Entry<String,Object> en : status.getProperties().entrySet()) {
				if("#CART_ID".equals(en.getValue())) {
					Long id = Long.parseLong(en.getKey());
					siteIds.add(id);
				}
			}
		}
		LOG.debug("UNLOAD {}={}",status.getKey(),siteIds);
		builder.complete = true;
		builder.cartIds = null;
		if(! builder.initializationMode && ! timestamp.equals(builder.timestamp)) {
			builder.ackConveyor.part().id(status.getKey()).value(status.getKey()).label(builder.ackConveyor.REPLAY).place();
		}

	}

	public static <K, L> void complete(AcknowledgeBuilder<K> builder, AcknowledgeStatus<K> status) {
		builder.complete  = true;
		Set<Long> siteIds = new HashSet<>();
		for(Map.Entry<String,Object> en : status.getProperties().entrySet()) {
			if("#CART_ID".equals(en.getValue())) {
				Long id = Long.parseLong(en.getKey());
				siteIds.add(id);
			}
		}
		builder.cartIds.addAll(siteIds);
		LOG.debug("COMPLETE {}  {}",siteIds,status);
	}

	public static <K, L> void replay(AcknowledgeBuilder<K> builder, K key) {
			Set<K> completed = builder.persistence.getCompletedKeys();
			if(completed.contains(key)) {
				return;
			} else {
				if(builder.timestamp.equals(Long.valueOf(0))) {
					builder.timestamp = System.nanoTime();
				}
				Collection<Long> cartIds = builder.persistence.getAllPartIds(key);
				cartIds.forEach(id -> {
					Cart cart = builder.persistence.getPart(id);
					cart.addProperty(""+id, "#CART_ID");
					cart.addProperty("#TIMESTAMP", builder.timestamp);
					builder.forward.place(cart);
				});
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
				 + ", complete=" + complete + "]";
	}

}
