package com.aegisql.conveyor.persistence.converters.collections;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class MapToByteArrayConverter <K,V> implements ObjectConverter<Map<K,V>, byte[]> {

	private final Supplier<Map<K,V>> mapSupplier;
	private final ObjectConverter<K, byte[]> keyConverter;
	private final ObjectConverter<V, byte[]> valueConverter;
	
	public MapToByteArrayConverter(Supplier<Map<K,V>> mapSupplier, ObjectConverter<K, byte[]> keyConverter, ObjectConverter<V, byte[]> valueConverter) {
		this.mapSupplier = mapSupplier;
		this.keyConverter       = keyConverter;
		this.valueConverter     = valueConverter;
	}
	
	@Override
	public byte[] toPersistence(Map<K,V> map) {
		if(map == null) {
			return null;
		}
		int size  = map.size();
		if(size == 0) {
			return new byte[0];
		}
		int totalBytes = 0;
		int maxKeySize = 0;
		int maxValSize = 0;
		ArrayList<byte[]> keyBytesList = new ArrayList<>();
		ArrayList<byte[]> valBytesList = new ArrayList<>();
		for(Entry<K,V> o:map.entrySet()) {
			if(o==null) {
				keyBytesList.add(null);
				valBytesList.add(null);
			} else {
				K key = o.getKey();
				V val = o.getValue();
				byte[] keyBytes = keyConverter.toPersistence(key);
				byte[] valBytes = valueConverter.toPersistence(val);
				
				maxKeySize = Math.max(maxKeySize, keyBytes.length);
				maxValSize = Math.max(maxValSize, valBytes.length);
				totalBytes += keyBytes.length;
				totalBytes += valBytes.length;
				keyBytesList.add(keyBytes);
				valBytesList.add(valBytes);
			}
		}
		
		byte keyWidth = 0;
		for(int x= maxKeySize; x > 0; x >>>= 8 ) {
			keyWidth++;
		}
		byte valWidth = 0;
		for(int x= maxValSize; x > 0; x >>>= 8 ) {
			valWidth++;
		}
		
		byte[] buff = new byte[ 2+size*keyWidth +size*valWidth + totalBytes ];
		ByteBuffer bb = ByteBuffer.wrap(buff);
		bb.put(0,keyWidth);
		bb.put(1,valWidth);
		int index = 2;
		for(int i = 0; i < size; i++) {
			byte[] keyBytes = keyBytesList.get(i);
			byte[] valBytes = valBytesList.get(i);
			if(keyBytes == null) {
				for(int j = 0; j < keyWidth; j++) {
					bb.put(index++, (byte) 0);
				}
			} else {
				for(int j = 0, s = 8*keyWidth-8; j < keyWidth; j++,s-=8) {
					bb.put(index++, (byte)(keyBytes.length >>> s));
				}
				for(byte b:keyBytes) {
					bb.put(index++,b);
				}
			}

			if(valBytes == null) {
				for(int j = 0; j < valWidth; j++) {
					bb.put(index++, (byte) 0);
				}
			} else {
				for(int j = 0, s = 8*valWidth-8; j < valWidth; j++,s-=8) {
					bb.put(index++, (byte)(valBytes.length >>> s));
				}
				for(byte b:valBytes) {
					bb.put(index++,b);
				}
			}
		
		}
		return buff;
	}

	@Override
	public Map<K,V> fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		Map<K,V> map = mapSupplier.get();
		if(p.length == 0) {
			return map;
		}

		ByteBuffer bb = ByteBuffer.wrap(p);
		byte keyWidth = bb.get(0);
		byte valWidth = bb.get(1);
		int index = 2;
		
		while(index < p.length) {

			K key = null;
			V val = null;
			
			ByteBuffer keyb = ByteBuffer.allocate(4);
			for(int i = 4-keyWidth; i < 4; i++) {
				byte b = bb.get(index++);
				keyb.put(i,b);
			}
			int keySize = keyb.getInt();

			if(keySize > 0) {
				byte[] dest = new byte[keySize];
				for(int i = 0; i < keySize; i++) {
					dest[i] = bb.get(index++);
				}
				key = keyConverter.fromPersistence(dest);
			}
			
			ByteBuffer valb = ByteBuffer.allocate(4);
			for(int i = 4-valWidth; i < 4; i++) {
				byte b = bb.get(index++);
				valb.put(i,b);
			}
			int valSize = valb.getInt();

			if(valSize > 0) {
				byte[] dest = new byte[valSize];
				for(int i = 0; i < valSize; i++) {
					dest[i] = bb.get(index++);
				}
				val = valueConverter.fromPersistence(dest);
			}
			map.put(key, val);
		}
		return map;
	}

}
