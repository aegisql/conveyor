package com.aegisql.conveyor.utils.map;

import java.util.Map;

import com.aegisql.conveyor.AssemblingConveyor;

public class MapConveyor<K,L,V> extends AssemblingConveyor<K, L, Map<L,V>> {

	public MapConveyor() {
		super();
		this.setName("MapConveyor");
		
		this.setDefaultCartConsumer((label,value,builder)->{
			MapBuilder<L,V> mb = (MapBuilder<L,V>)builder;
			if(label == null && value == null) {
				mb.complete();
			} else {
				mb.add(label, (V)value);
			}
		});
		
		
	}

}
