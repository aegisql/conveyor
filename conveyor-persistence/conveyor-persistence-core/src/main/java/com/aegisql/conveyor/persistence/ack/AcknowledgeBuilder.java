package com.aegisql.conveyor.persistence.ack;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.AcknowledgeStatus;
import com.aegisql.conveyor.CommandLabel;
import com.aegisql.conveyor.BuildingSite.Memento;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.Expireable;
import com.aegisql.conveyor.Testing;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.LoadType;
import com.aegisql.conveyor.cart.command.GeneralCommand;
import com.aegisql.conveyor.consumers.result.ForwardResult.ForwardingConsumer;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistenceCart;
import com.aegisql.conveyor.persistence.utils.CartInputStream;

// TODO: Auto-generated Javadoc
/**
 * The Class AcknowledgeBuilder.
 *
 * @param <K> the key type
 */
public class AcknowledgeBuilder<K> implements Supplier<Boolean>, Testing, Expireable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The Constant LOG. */
	private final static Logger LOG = LoggerFactory.getLogger(AcknowledgeBuilder.class);

	/** The persistence. */
	private final Persistence<K> persistence;

	/** The cart ids. */
	private final Set<Long> cartIds = new LinkedHashSet<>();
	
	/** The forward. */
	private final Conveyor<K, ?, ?> forward;
	
	/** The ack conveyor. */
	private final AcknowledgeBuildingConveyor<K> ackConveyor;
	
	/** The initialization mode. */
	private boolean initializationMode = false;

	/** The unload enabled. */
	private boolean unloadEnabled      = false;

	/** The timestamp. */
	private Long timestamp = Long.valueOf(0);
	
	/** The complete. */
	private boolean complete = false;
	
	/** The complete result. */
	private Boolean completeResult = null;
	
	private K key;
	
	private int minCompactSize = 0;

	/**
	 * Instantiates a new acknowledge builder.
	 *
	 * @param persistence the persistence
	 * @param forward the forward
	 * @param ackConveyor the ack conveyor
	 */
	public AcknowledgeBuilder(Persistence<K> persistence, Conveyor<K, ?, ?> forward, AcknowledgeBuildingConveyor<K> ackConveyor) {
		this.persistence        = persistence;
		this.forward            = forward;
		this.ackConveyor        = ackConveyor;
	}

	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	@Override
	public Boolean get() {
		if(completeResult == null) {
			return null;
		} else {
			return completeResult;
		}
	}

	/**
	 * Sets the mode.
	 *
	 * @param <K> the key type
	 * @param <L> the generic type
	 * @param builder the builder
	 * @param mode the mode
	 */
	public static <K, L> void setMode(AcknowledgeBuilder<K> builder, Boolean mode) {
		builder.initializationMode = mode;
	}

	/**
	 * Sets the unload mode.
	 *
	 * @param <K> the key type
	 * @param <L> the generic type
	 * @param builder the builder
	 * @param mode the mode
	 */
	public static <K, L> void setUnloadMode(AcknowledgeBuilder<K> builder, Boolean mode) {
		builder.unloadEnabled = mode;
	}

	/**
	 * Process cart.
	 *
	 * @param <K> the key type
	 * @param <L> the generic type
	 * @param builder the builder
	 * @param cart the cart
	 */
	public static <K, L> void processCart(AcknowledgeBuilder<K> builder, Cart<K, ?, L> cart) {
		LOG.debug("Process CART " + cart);
		
		if(cart instanceof PersistenceCart) {
			cart = (Cart<K, ?, L>) cart.getValue();
		}
		
		boolean save = false;
		K key = cart.getKey();
		builder.key = key;
		Long id = null;
		builder.timestamp = System.nanoTime();
		if (!cart.getAllProperties().containsKey("#CART_ID")) {
			id = builder.persistence.nextUniquePartId();
			cart.addProperty("#CART_ID", id);
			cart.addProperty(id.toString(),"#CART_ID");
			save = true;
			LOG.debug("NEW ID {}",id);
		} else {
			LOG.debug("RESTORED ID {}",id);
			id = (Long) cart.getProperty("#CART_ID", Long.class);
		}
		CompletableFuture<Boolean> f = CompletableFuture.completedFuture(true);
		if( ! builder.initializationMode ) {
			if(builder.unloadEnabled && builder.cartIds.isEmpty()) {
				Set<Long> savedIds = new HashSet<>(builder.persistence.getAllPartIds(key));
				LOG.debug("RESTORE {}",savedIds);
				for(long i: savedIds) {
					Cart<K,?,L> oldCart = builder.persistence.getPart(i);
					oldCart.addProperty("#TIMESTAMP", builder.timestamp);
					oldCart.addProperty(""+i, "#CART_ID");
					f = builder.forward.place((Cart) oldCart);
					builder.cartIds.add(i);
				}
			}
		} else {
			LOG.debug("INITIALIZING {} {}",cart.getKey(),id);
		}
		
		if ( ! builder.cartIds.contains(id)) {
			if(save) {
				try {
					builder.persistence.savePart(id, cart);
				} catch(Exception e) {
					cart.getFuture().completeExceptionally(e);
					throw e;
				}
				builder.persistence.savePartId(cart.getKey(), id);
			}
			builder.cartIds.add(id);
			cart.addProperty("#TIMESTAMP", builder.timestamp);
			if (builder.forward != null) {
				f = builder.forward.place((Cart) cart);
			}
		} else {
			LOG.debug("Duplicate cart {} {}",cart.getKey(),id);
		}
		if(builder.minCompactSize > 0 && builder.cartIds.size() >= builder.minCompactSize) {
			if(f.join()) {
				compact(builder, key);
			}
		}
		
	}

	public static <K, L> void compact(AcknowledgeBuilder<K> builder, K k) {
		K key = k != null ? k : builder.key;
		LOG.debug("Compact " + key);
		Memento m = builder.forward.command().id(key).memento().join();
		if(m != null) {
			long id = builder.persistence.nextUniquePartId();
			Cart cart = new GeneralCommand(key, m, CommandLabel.RESTORE_BUILD, 0);
			try {
				builder.persistence.savePart(id, cart);
			} catch (Exception e) {
				cart.getFuture().completeExceptionally(e);
				throw e;
			}
			builder.persistence.archiveParts(builder.cartIds);
			builder.cartIds.clear();
			builder.cartIds.add(id);
		} else {
			LOG.debug("Memento not forund for {}",key);
		}
	}
	
	/**
	 * Unload.
	 *
	 * @param <K> the key type
	 * @param <L> the generic type
	 * @param builder the builder
	 * @param status the status
	 */
	public static <K, L> void unload(AcknowledgeBuilder<K> builder, AcknowledgeStatus<K> status) {
		Set<Long> siteIds    = new HashSet<>();
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
		builder.completeResult = Boolean.FALSE;
		if(! builder.initializationMode && ! timestamp.equals(builder.timestamp)) {
			builder.ackConveyor.part().id(status.getKey()).value(status.getKey()).label(builder.ackConveyor.REPLAY).place();
		}

	}

	/**
	 * Complete.
	 *
	 * @param <K> the key type
	 * @param <L> the generic type
	 * @param builder the builder
	 * @param status the status
	 */
	public static <K, L> void complete(AcknowledgeBuilder<K> builder, AcknowledgeStatus<K> status) {
		builder.complete  = true;
		builder.completeResult = Boolean.TRUE;
		LOG.debug("COMPLETE {}",status);
	}

	public static <K, L> void minCompactSize(AcknowledgeBuilder<K> builder, Integer minSize) {
		builder.minCompactSize  = minSize != null? minSize:0;
		LOG.debug("MIN COMPACT SIZE {}",minSize);
	}

	/**
	 * Replay.
	 *
	 * @param <K> the key type
	 * @param <L> the generic type
	 * @param builder the builder
	 * @param key the key
	 */
	public static <K, L> void replay(AcknowledgeBuilder<K> builder, K key) {
			builder.key = key;
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

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Testing#test()
	 */
	@Override
	public boolean test() {
		return complete;// && keyReady != null;
	}

	// This build never expires.
	// Expiration is managed by receiving the COMPLETED message
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Expireable#getExpirationTime()
	 */
	// From the working conveyor.
	@Override
	public long getExpirationTime() {
		return Long.MAX_VALUE;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "AcknowledgeBuilder [persistence=" + persistence + ", cartIds=" + cartIds + ", forward=" + forward
				 + ", complete=" + complete + "]";
	}

}
