package com.aegisql.conveyor.persistence.core;

import java.io.Closeable;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.cart.Cart;

// TODO: Auto-generated Javadoc
/**
 * The Interface Persistence.
 *
 * @param <K> the key type
 */
public interface Persistence <K> extends Closeable {

	/**
	 * Next unique part id.
	 *
	 * @return the long
	 */
	//SETTERS
	long nextUniquePartId();
	
	/**
	 * Save part.
	 *
	 * @param <L> the generic type
	 * @param id the id
	 * @param cart the cart
	 */
	<L> void savePart(long id,Cart<K,?,L> cart);
	
	/**
	 * Save part id.
	 *
	 * @param key the key
	 * @param partId the part id
	 */
	void savePartId(K key, long partId);
	
	/**
	 * Save completed build key.
	 *
	 * @param key the key
	 */
	void saveCompletedBuildKey(K key);
	
	/**
	 * Gets the part.
	 *
	 * @param <L> the generic type
	 * @param id the id
	 * @return the part
	 */
	default <L> Cart<K, ?, L> getPart(long id) {
		Collection<Cart<K, ?, L>> res = getParts(List.of(id));
		return switch (res.size()) {
			case 1 -> res.iterator().next();
			case 0 -> null;
			default -> throw new PersistenceException("Unexpected number of results for a single ID=" + id + " " + res);
		};
	}

	/**
	 * Gets the parts.
	 *
	 * @param <L> the generic type
	 * @param id the id
	 * @return the parts
	 */
	<L> Collection<Cart<K,?,L>> getParts(Collection<Long> id);
	
	
	/**
	 * Gets the all part ids.
	 *
	 * @param key the key
	 * @return the all part ids
	 */
	Collection<Long> getAllPartIds(K key);
	
	/**
	 * Gets the all parts.
	 *
	 * @param <L> the generic type
	 * @return the all parts
	 */
	<L> Collection<Cart<K,?,L>> getAllParts();

	/**
	 * Gets the expired parts.
	 *
	 * @param <L> the generic type
	 * @return the expired parts
	 */
	<L> Collection<Cart<K,?,L>> getExpiredParts();

	/**
	 * Gets the all static parts.
	 *
	 * @param <L> the generic type
	 * @return the all static parts
	 */
	<L> Collection<Cart<K,?,L>> getAllStaticParts();
	
	/**
	 * Gets the completed keys.
	 *
	 * @return the completed keys
	 */
	Set<K> getCompletedKeys();
	
	/**
	 * Archive parts.
	 *
	 * @param ids the ids
	 */
	//ARCHIVE OPERATIONS
	void archiveParts(Collection<Long> ids);
	
	/**
	 * Archive keys.
	 *
	 * @param keys the keys
	 */
	void archiveKeys(Collection<K> keys);
	
	/**
	 * Archive complete keys.
	 *
	 * @param keys the keys
	 */
	void archiveCompleteKeys(Collection<K> keys);
	
	/**
	 * Archive expired parts.
	 */
	void archiveExpiredParts();
	
	/**
	 * Archive all.
	 */
	void archiveAll();
	
	/**
	 * Gets the max archive batch size.
	 *
	 * @return the max archive batch size
	 */
	//BATCH 
	int getMaxArchiveBatchSize();
	
	/**
	 * Gets the max archive batch time.
	 *
	 * @return the max archive batch time
	 */
	long getMaxArchiveBatchTime();
	
	/**
	 * Gets the number of parts.
	 *
	 * @return the number of parts
	 */
	//HELP
	long getNumberOfParts();

	int getMinCompactSize();

	/**
	 * Absorb.
	 *
	 * @param <L> the generic type
	 * @param old the old
	 */
	default <L> void absorb(Persistence<K> old) {
		Set<K> completed                 = old.getCompletedKeys();
		Collection<Cart<K,?,L>> oldParts = old.getAllParts();
		oldParts.forEach(cart->{
			K key = cart.getKey();
			if( ! completed.contains(key) ) {
				long nextId = nextUniquePartId();
				cart.addProperty("#CART_ID", nextId);
				cart.addProperty(nextId+"","#CART_ID");
				Integer recoveryAttempt = cart.getProperty("RECOVERY_ATTEMPT", Integer.class);
				if(recoveryAttempt == null) {
					cart.addProperty("RECOVERY_ATTEMPT", 1);
				} else {
					cart.addProperty("RECOVERY_ATTEMPT", recoveryAttempt+1);
				}
				savePart(nextId, cart);
				savePartId(key,nextId);
			}
		});
	}
	
	/**
	 * Copy.
	 *
	 * @return the persistence
	 */
	Persistence<K> copy();
	
	/**
	 * Non persistent properties.
	 *
	 * @param property the property
	 * @return true, if is persistent property
	 */
	boolean isPersistentProperty(String property);
	
	/**
	 * Wrap conveyor.
	 *
	 * @param <L> the generic type
	 * @param <OUT> the generic type
	 * @param conveyor the conveyor
	 * @return the persistent conveyor
	 */
	default <L,OUT> PersistentConveyor<K,L,OUT> wrapConveyor(Conveyor<K,L,OUT> conveyor) {
		return new PersistentConveyor<>(this,conveyor);
	}

	/**
	 * Gets the assembling conveyor.
	 *
	 * @param <L> the generic type
	 * @param <OUT> the generic type
	 * @return the assembling conveyor
	 */
	default <L,OUT> PersistentConveyor<K,L,OUT> getConveyor() {
		return new PersistentConveyor<>(this,new AssemblingConveyor<>());
	}

	/**
	 * Gets the conveyor.
	 *
	 * @param <L> the generic type
	 * @param <OUT> the generic type
	 * @param conveyor the conveyor
	 * @return the conveyor
	 */
	default <L,OUT> PersistentConveyor<K,L,OUT> getConveyor(Supplier<Conveyor<K,L,OUT>> conveyor) {
		return new PersistentConveyor<>(this,conveyor.get());
	}

	/** The Constant mBeanServer. */
	MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
	
	/**
	 * By name.
	 *
	 * @param name the name
	 * @return the persistence
	 */
	static Persistence byName(String name) {
		ObjectName objectName = null;
		try {
			if(name.startsWith("com.aegisql.conveyor.persistence.")) {
				objectName = new ObjectName(name);
			} else {
				String[] parts = name.split("\\.");
				if(parts.length != 3) {
					throw new PersistenceException("Expected persistence full name type.schema.part");
				}
				objectName = new ObjectName("com.aegisql.conveyor.persistence."+parts[0]+"."+parts[1]+":type="+parts[2]);
			}
			Object res = mBeanServer.invoke(objectName, "get", null, null);
			return (Persistence) res;
		} catch (Exception e) {
			throw new RuntimeException("Persistence with name '"+name +"' not found",e);
		}
	}
	
	/**
	 * Lazy supplier.
	 *
	 * @param name the name
	 * @return the supplier
	 */
	static Supplier<Persistence> lazySupplier(String name) {
		return new LazyPersistenceSupplier(name);
	}
}
