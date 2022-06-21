package com.aegisql.conveyor.persistence.converters;

import static com.aegisql.conveyor.cart.LoadType.STATIC_PART;

import java.nio.ByteBuffer;
import java.util.Map;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.CreatingCart;
import com.aegisql.conveyor.cart.Load;
import com.aegisql.conveyor.cart.LoadType;
import com.aegisql.conveyor.cart.MultiKeyCart;
import com.aegisql.conveyor.cart.ResultConsumerCart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.persistence.core.ObjectConverter;
import com.aegisql.conveyor.persistence.core.PersistenceException;

public class CartToBytesConverter<K, V, L> implements ObjectConverter<Cart<K, V, L>, byte[]> {

	/*
	 * 
	 * Cart record format
	 * 
	 * 4b - total cart record size 1b - cart type (enum order) 8b - creation time 8b
	 * - expiration time
	 * 
	 * 1b - key type length kb - key type (string) 4b - key length Kb - key
	 * 
	 * 1b - label type length lb - label type (string) 4b - label length Lb - label
	 * (can be enum order?)
	 * 
	 * 1b - value type length vb - valuel type (string) 4b - Value length Vb - Value
	 * 
	 * 4b - properties length Pb - properties (JSON string)
	 * 
	 */

	private final ObjectToJsonBytesConverter<Map<String, Object>> propertiesConverter = new ObjectToJsonBytesConverter(
			Map.class);

	private final ConverterAdviser<L> adviser;

	public CartToBytesConverter(ConverterAdviser<L> adviser) {
		this.adviser = adviser;
	}

	public CartToBytesConverter() {
		this(new ConverterAdviser<>());
	}

	@Override
	public byte[] toPersistence(Cart<K, V, L> cart) {

		K key = cart.getKey();
		L label = cart.getLabel();
		V value = cart.getValue();

		byte type = (byte) cart.getLoadType().ordinal();
		byte[] keyBytes = convertObject(key);
		byte[] valueBytes = convertObject(value);
		byte[] labelBytes = convertObject(label);
		byte[] propertiesBytes = propertiesConverter.toPersistence(cart.getAllProperties());

		int totalSize = 4 + 1 + 8 + 8 + 8 + keyBytes.length + valueBytes.length + labelBytes.length + 4
				+ propertiesBytes.length;
		int pos = 0;

		byte[] buff = new byte[totalSize];
		ByteBuffer bb = ByteBuffer.wrap(buff);
		bb.putInt(pos,totalSize); pos+=4;
		bb.put(pos++,type);
		bb.putLong(pos, cart.getCreationTime());
		pos += 8;
		bb.putLong(pos, cart.getExpirationTime());
		pos += 8;
		bb.putLong(pos, cart.getPriority());
		pos += 8;
		for (byte keyByte : keyBytes) {
			bb.put(pos++, keyByte);
		}
		for (byte valueByte : valueBytes) {
			bb.put(pos++, valueByte);
		}
		for (byte labelByte : labelBytes) {
			bb.put(pos++, labelByte);
		}
		bb.putInt(pos, propertiesBytes.length);
		pos += 4;
		for (byte propertiesByte : propertiesBytes) {
			bb.put(pos++, propertiesByte);
		}

		return buff;
	}

	private byte[] convertObject(Object o) {
		if (o == null) {
			return new byte[] { 0 };
		} else {
			ObjectConverter<Object, byte[]> oc = adviser.getConverter(null, o.getClass());
			byte[] hint = oc.conversionHint().getBytes();
			assert hint.length < 256 : oc.conversionHint();
			byte hLength = (byte) hint.length;
			byte[] objBytes = oc.toPersistence(o);
			int total = 1 + hint.length + 4 + objBytes.length;
			byte[] buff = new byte[total];
			ByteBuffer bb = ByteBuffer.wrap(buff);
			int pos = 0;

			bb.put(pos, hLength);
			pos++;
			for (byte b : hint) {
				bb.put(pos++, b);
			}

			bb.putInt(pos, objBytes.length);
			pos += 4;
			for (byte objByte : objBytes) {
				bb.put(pos++, objByte);
			}

			return buff;
		}
	}

	@Override
	public Cart<K, V, L> fromPersistence(byte[] p) {

		ByteBuffer bb = ByteBuffer.wrap(p);

		int pos = 0;
		int totalSize = bb.getInt(pos);
		pos += 4;
		LoadType loadType = LoadType.values()[bb.get(pos++)];
		long creationTime = bb.getLong(pos);
		pos += 8;
		long expirationTime = bb.getLong(pos);
		pos += 8;
		long priority = bb.getLong(pos);
		pos += 8;
		// -------------- KEY
		byte keyHintLength = bb.get(pos++);
		Object key = null;
		if (keyHintLength > 0) {
			byte[] keyHintBytes = new byte[keyHintLength];
			for (int i = 0; i < keyHintLength; i++) {
				keyHintBytes[i] = bb.get(pos++);
			}
			String keyHint = new String(keyHintBytes);
			ObjectConverter<Object, byte[]> kc = adviser.getConverter(null, keyHint);
			int keyLength = bb.getInt(pos);
			pos += 4;
			byte[] keyBytes = new byte[keyLength];
			for (int i = 0; i < keyLength; i++) {
				keyBytes[i] = bb.get(pos++);
			}
			key = kc.fromPersistence(keyBytes);
		}
		// -------------- VALUE
		byte valHintLength = bb.get(pos++);
		byte[] valHintBytes = new byte[valHintLength];
		for (int i = 0; i < valHintLength; i++) {
			valHintBytes[i] = bb.get(pos++);
		}
		String valHint = new String(valHintBytes);
		ObjectConverter<Object, byte[]> vc = adviser.getConverter(null, valHint);
		int valLength = bb.getInt(pos);
		pos += 4;
		byte[] valBytes = new byte[valLength];
		for (int i = 0; i < valLength; i++) {
			valBytes[i] = bb.get(pos++);
		}
		Object val = vc.fromPersistence(valBytes);
		// -------------- LABEL
		byte lHintLength = bb.get(pos++);
		byte[] lHintBytes = new byte[lHintLength];
		for (int i = 0; i < lHintLength; i++) {
			lHintBytes[i] = bb.get(pos++);
		}
		String lHint = new String(lHintBytes);
		ObjectConverter<Object, byte[]> lc = adviser.getConverter(null, lHint);
		int lLength = bb.getInt(pos);
		pos += 4;
		byte[] lBytes = new byte[lLength];
		for (int i = 0; i < lLength; i++) {
			lBytes[i] = bb.get(pos++);
		}
		Object label = lc.fromPersistence(lBytes);
		// -------------- PROPERTIES
		int propLength = bb.getInt(pos);
		pos += 4;
		byte[] pBytes = new byte[propLength];
		for (int i = 0; i < propLength; i++) {
			pBytes[i] = bb.get(pos++);
		}
		Map<String, Object> properties = propertiesConverter.fromPersistence(pBytes);
		switch (loadType) {
		case PART:
			return new ShoppingCart(key, val, label, creationTime, expirationTime, properties, loadType,priority);
		case MULTI_KEY_PART:
			Load load = (Load) val;
			return new MultiKeyCart(load.getFilter(), load.getValue(), label, creationTime, expirationTime,load.getLoadType(), properties,priority);
		case STATIC_PART:
			return new ShoppingCart(null, val, label, creationTime, 0, properties, STATIC_PART, priority);
		case RESULT_CONSUMER:
			if(key != null) {
				return new ResultConsumerCart(key, (ResultConsumer) val, creationTime, expirationTime,priority);
			} else {
				Load load2 = (Load) val;
				return new MultiKeyCart(load2.getFilter(), load2.getValue(), label, creationTime, expirationTime,load2.getLoadType(), properties,priority);
			}
		case BUILDER:
			return new CreatingCart(key, (BuilderSupplier) val, creationTime, expirationTime,priority);
		case FUTURE:
			throw new PersistenceException("Unsupported cart converter " + loadType);
		case COMMAND:
			throw new PersistenceException("Unsupported cart converter " + loadType);
		default:
			throw new PersistenceException("Unsupported cart converter " + loadType);
		}
	}

	@Override
	public String conversionHint() {
		return "Cart:byte[]";
	}

}
