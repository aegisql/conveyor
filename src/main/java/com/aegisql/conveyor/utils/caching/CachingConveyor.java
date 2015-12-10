package com.aegisql.conveyor.utils.caching;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.BuildingSite;
import com.aegisql.conveyor.BuildingSite.Status;
import com.aegisql.conveyor.cart.Cart;

public class CachingConveyor<K, L, OUT> extends AssemblingConveyor<K, L, OUT> {
	
	private final static Logger LOG = LoggerFactory.getLogger(CachingConveyor.class);
	
	public CachingConveyor() {
		super();
		this.setReadinessEvaluator( (k,l) -> false);
		this.setName("CachingConveyor");
		this.setOnTimeoutAction(builder->{
			LOG.debug("Evicting builder {}",builder);			
		});
		this.setResultConsumer(bin->{
			LOG.debug("Evicting key {}",bin.key);
		});
		this.setScrapConsumer(bin->{
			LOG.debug("Evicting key {}",bin.getKey());
		});
	}

	public Supplier<? extends OUT> getProductSupplier(K key) {
		BuildingSite<K, L, Cart<K, ?, L>, ? extends OUT> site = collector.get(key);
		if(site == null) {
			return null;
		} else {
			Supplier<OUT> s = new Supplier<OUT>(){
				@Override
				public OUT get() {
					if( ! site.getStatus().equals(Status.WAITING_DATA)) {
						throw new IllegalStateException("Supplier is in a wrong state: "+site.getStatus());
					}
					return site.getProductSupplier().get();
				}
				
			};
			return s;
		}
	}
	
}
