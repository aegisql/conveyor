package com.aegisql.conveyor.utils.caching;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.BuildingSite;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.consumers.scrap.LogScrap;

// TODO: Auto-generated Javadoc
/**
 * The Class CachingConveyor.
 *
 * @param <K> the key type
 * @param <L> the generic type
 * @param <OUT> the generic type
 */
public class CachingConveyor<K, L, OUT> extends AssemblingConveyor<K, L, OUT> {
	
	/** The Constant LOG. */
	private final static Logger LOG = LoggerFactory.getLogger(CachingConveyor.class);
	
	/**
	 * Instantiates a new caching conveyor.
	 */
	public CachingConveyor() {
		super();
		this.setReadinessEvaluator( (k,l) -> false);
		this.setName("CachingConveyor");
		this.setOnTimeoutAction(builder-> LOG.debug("Cache Timeout {}", builder));
		this.scrapConsumer().first(LogScrap.debug(this)).set();
	}

	/**
	 * Gets the product supplier.
	 *
	 * @param key the key
	 * @return the product supplier
	 */
	public Supplier<? extends OUT> getProductSupplier(K key) {
		BuildingSite<K, L, Cart<K, ?, L>, ? extends OUT> site = collector.get(key);
		if(site == null) {
			return null;
		} else {
			return site.getProductSupplier();
		}
	}
	
}
