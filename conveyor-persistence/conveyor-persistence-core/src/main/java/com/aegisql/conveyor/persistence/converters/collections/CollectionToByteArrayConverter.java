package com.aegisql.conveyor.persistence.converters.collections;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class CollectionToByteArrayConverter <O> implements ObjectConverter<Collection<O>, byte[]> {

	private final Supplier<Collection<O>> collectionSupplier;
	private final ObjectConverter<O, byte[]> objectConverter;
	private final String hint;
	
	public CollectionToByteArrayConverter(Supplier<Collection<O>> collectionSupplier, ObjectConverter<O, byte[]> objectConverter) {
		this.collectionSupplier = collectionSupplier;
		this.objectConverter    = objectConverter;
		Collection<O> col = collectionSupplier.get();
		this.hint = col.getClass().getCanonicalName()+"<"+objectConverter.conversionHint()+">:byte[]";
	}
	
	@Override
	public byte[] toPersistence(Collection<O> collection) {
		if(collection == null) {
			return null;
		}
		int size  = collection.size();
		if(size == 0) {
			return new byte[0];
		}
		int totalBytes = 0;
		int maxObjSize = 0;
		ArrayList<byte[]> allBytes = new ArrayList<>();
		for(O o:collection) {
			if(o==null) {
				allBytes.add(null);
			} else {
				byte[] bytes = objectConverter.toPersistence(o);
				maxObjSize = Math.max(maxObjSize, bytes.length);
				totalBytes += bytes.length;
				allBytes.add(bytes);
			}
		}
		
		byte width = 0;
		for(int x= maxObjSize; x > 0; x >>>= 8 ) {
			width++;
		}
		
		byte[] buff = new byte[ 1+size*width + totalBytes ];
		ByteBuffer bb = ByteBuffer.wrap(buff);
		bb.put(0,width);
		int index = 1;
		for(int i = 0; i < size; i++) {
			byte[] bytes = allBytes.get(i);
			if(bytes == null) {
				for(int j = 0; j < width; j++) {
					bb.put(index++, (byte) 0);
				}
			} else {
				for(int j = 0, s = 8*width-8; j < width; j++,s-=8) {
					bb.put(index++, (byte)(bytes.length >>> s));
				}
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
		byte width = bb.get(0);
		int index = 1;
		
		while(index < p.length) {
			
			ByteBuffer ib = ByteBuffer.allocate(4);
			for(int i = 4-width; i < 4; i++) {
				byte b = bb.get(index++);
				ib.put(i,b);
			}
			int size = ib.getInt();
			
			if(size == 0) {
				col.add(null);
			} else {
				byte[] dest = new byte[size];
				for(int i = 0; i < size; i++) {
					dest[i] = bb.get(index++);
				}
				col.add(objectConverter.fromPersistence(dest));
			}
		}
		return col;
	}

	@Override
	public String conversionHint() {
		return hint;
	}

}
