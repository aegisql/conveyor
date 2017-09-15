package com.aegisql.conveyor.persistence.converters.collections;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class CollectionToByteArrayConverter <O> implements ObjectConverter<Collection<O>, byte[]> {

	private final Supplier<Collection<O>> collectionSupplier;
	private final ObjectConverter<O, byte[]> objectConverter;
	
	public CollectionToByteArrayConverter(Supplier<Collection<O>> collectionSupplier, ObjectConverter<O, byte[]> objectConverter) {
		this.collectionSupplier = collectionSupplier;
		this.objectConverter    = objectConverter;
	}
	
	@Override
	public byte[] toPersistence(Collection<O> obj) {
		if(obj == null) {
			return null;
		}
		int size  = obj.size();
		int nulls = 0;
		if(size == 0) {
			return new byte[0];
		}
		
		int objSize = 0;
		ArrayList<byte[]> allBytes = new ArrayList<>();
		for(O o:obj) {
			if(o==null) {
				allBytes.add(null);
				nulls++;
			} else {
				byte[] bytes = objectConverter.toPersistence(o);
				if(objSize == 0) {
					objSize = bytes.length;
				}
				allBytes.add(bytes);
			}
		}
		byte[] buff = new byte[ 4 + size+(size-nulls)*objSize ];
		ByteBuffer bb = ByteBuffer.wrap(buff);
		bb.putInt(0,objSize);
		int index = 4;
		for(int i = 0; i < size; i++) {
			byte[] bytes = allBytes.get(i);
			if(bytes == null) {
				bb.put(index++, (byte) 0);
			} else {
				bb.put(index++, (byte) 1);
				for(byte b:bytes) {
					bb.put(index++,b);
				}
			}
		}
		return buff;
	}

	@Override
	public Collection<O> fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		Collection<O> col = collectionSupplier.get();
		if(p.length == 0) {
			return col;
		}

		ByteBuffer bb = ByteBuffer.wrap(p);
		int objSize = bb.getInt(0);
		int index = 4;
		
		while(index < p.length) {
			byte isNull = bb.get(index++);
			if(isNull == 0) {
				col.add(null);
			} else {
				byte[] dest = new byte[objSize];
				for(int i = 0; i < objSize; i++) {
					dest[i] = bb.get(index++);
				}
				col.add(objectConverter.fromPersistence(dest));
			}
		}
		return col;
	}

}
