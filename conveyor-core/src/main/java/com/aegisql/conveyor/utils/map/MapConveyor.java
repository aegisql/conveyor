package com.aegisql.conveyor.utils.map;

import com.aegisql.conveyor.AssemblingConveyor;

import java.util.Map;

// TODO: Auto-generated Javadoc
/**
 * The Class MapConveyor.
 *
 * @param <K> the key type
 * @param <L> the generic type
 * @param <V> the value type
 */
public class MapConveyor<K,L,V> extends AssemblingConveyor<K, L, Map<L,V>> {

	/**
	 * Instantiates a new map conveyor.
	 */
	public MapConveyor() {
		super();
		this.setName("MapConveyor");
		
		this.setDefaultCartConsumer((label,value,builder)->{
			var mb = (MapBuilder<L,V>)builder;
			if(label == null && value == null) {
				mb.complete();
			} else {
				mb.add(label, (V)value);
			}
		});
	}

}
