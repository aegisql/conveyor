package com.aegisql.conveyor.persistence.converters;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.persistence.converters.arrays.BigDecimalsToBytesConverter;
import com.aegisql.conveyor.persistence.converters.arrays.BigIntegersToBytesConverter;
import com.aegisql.conveyor.persistence.converters.arrays.BoolPrimToBytesConverter;
import com.aegisql.conveyor.persistence.converters.arrays.BooleansToBytesConverter;
import com.aegisql.conveyor.persistence.converters.arrays.BytesPrimToBytesConverter;
import com.aegisql.conveyor.persistence.converters.arrays.BytesToBytesConverter;
import com.aegisql.conveyor.persistence.converters.arrays.CharPrimToBytesConverter;
import com.aegisql.conveyor.persistence.converters.arrays.CharactersToBytesConverter;
import com.aegisql.conveyor.persistence.converters.arrays.DatesToBytesConverter;
import com.aegisql.conveyor.persistence.converters.arrays.DoublePrimToBytesConverter;
import com.aegisql.conveyor.persistence.converters.arrays.DoublesToBytesConverter;
import com.aegisql.conveyor.persistence.converters.arrays.FloatPrimToBytesConverter;
import com.aegisql.conveyor.persistence.converters.arrays.FloatsToBytesConverter;
import com.aegisql.conveyor.persistence.converters.arrays.IntPrimToBytesConverter;
import com.aegisql.conveyor.persistence.converters.arrays.IntegersToBytesConverter;
import com.aegisql.conveyor.persistence.converters.arrays.LongPrimToBytesConverter;
import com.aegisql.conveyor.persistence.converters.arrays.LongsToBytesConverter;
import com.aegisql.conveyor.persistence.converters.arrays.ShortPrimToBytesConverter;
import com.aegisql.conveyor.persistence.converters.arrays.ShortsToBytesConverter;
import com.aegisql.conveyor.persistence.converters.arrays.StringsToBytesConverter;
import com.aegisql.conveyor.persistence.converters.arrays.UuidsToBytesConverter;
import com.aegisql.conveyor.persistence.converters.arrays.sql.SqlDatesToBytesConverter;
import com.aegisql.conveyor.persistence.converters.arrays.sql.SqlTimesToBytesConverter;
import com.aegisql.conveyor.persistence.converters.arrays.sql.SqlTimestampsToBytesConverter;
import com.aegisql.conveyor.persistence.converters.sql.SqlDateToBytesConverter;
import com.aegisql.conveyor.persistence.converters.sql.SqlTimeToBytesConverter;
import com.aegisql.conveyor.persistence.converters.sql.SqlTimestampToBytesConverter;
import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class ConverterAdviser <L> {

	private final static Logger LOG = LoggerFactory.getLogger(ConverterAdviser.class);
	
	private final Map<String,ObjectConverter<Object, byte[]>> primeConverters = new HashMap<>();
	private final Map<L,ObjectConverter<Object, byte[]>> labelConverters = new HashMap<>();
	
	private final NullConverter nc = new NullConverter();
	
	private ObjectConverter<Object, byte[]> defaultConverter = (ObjectConverter)new SerializableToBytesConverter<>();
	private final static ObjectConverter<Object, byte[]> NULL_CONVERTER = new NullConverter();
	
	private EncryptingConverter encryptor = null;
	
	public void setEncryptor(SecretKey key, Cipher cipher) {
		this.encryptor = new EncryptingConverter(key, cipher);
		primeConverters.put(this.encryptor.conversionHint(), (ObjectConverter)this.encryptor);
	}

	public ConverterAdviser() {
		
		this.addConverter(UUID.class, new UuidToBytesConverter());
		this.addConverter(String.class, new StringToBytesConverter());
		this.addConverter(Short.class, new ShortToBytesConverter());
		this.addConverter(Long.class, new LongToBytesConverter());
		this.addConverter(Integer.class, new IntegerToBytesConverter());
		this.addConverter(Float.class, new FloatToBytesConverter());
		this.addConverter(Double.class, new DoubleToBytesConverter());
		this.addConverter(java.util.Date.class, new DateToBytesConverter());
		this.addConverter(Character.class, new CharToBytesConverter());
		this.addConverter(Byte.class, new ByteToBytesConverter());
		this.addConverter(Boolean.class, new BooleanToBytesConverter());
		this.addConverter(BigInteger.class, new BigIntegerToBytesConverter());
		this.addConverter(BigDecimal.class, new BigDecimalToBytesConverter());
		this.addConverter(java.sql.Date.class, new SqlDateToBytesConverter());
		this.addConverter(java.sql.Time.class, new SqlTimeToBytesConverter());
		this.addConverter(java.sql.Timestamp.class, new SqlTimestampToBytesConverter());
		this.addConverter(String.class, new StringToBytesConverter());
		this.addConverter(BigInteger.class, new BigIntegerToBytesConverter());
		this.addConverter(BigDecimal.class, new BigDecimalToBytesConverter());

		this.addConverter(UUID[].class, new UuidsToBytesConverter());
		this.addConverter(Short[].class, new ShortsToBytesConverter());
		this.addConverter(short[].class, new ShortPrimToBytesConverter());
		this.addConverter(Long[].class, new LongsToBytesConverter());
		this.addConverter(long[].class, new LongPrimToBytesConverter());
		this.addConverter(Integer[].class, new IntegersToBytesConverter());
		this.addConverter(int[].class, new IntPrimToBytesConverter());
		this.addConverter(Float[].class, new FloatsToBytesConverter());
		this.addConverter(float[].class, new FloatPrimToBytesConverter());
		this.addConverter(Double[].class, new DoublesToBytesConverter());
		this.addConverter(double[].class, new DoublePrimToBytesConverter());
		this.addConverter(java.util.Date[].class, new DatesToBytesConverter());
		this.addConverter(Character[].class, new CharactersToBytesConverter());
		this.addConverter(char[].class, new CharPrimToBytesConverter());
		this.addConverter(Byte[].class, new BytesToBytesConverter());
		this.addConverter(byte[].class, new BytesPrimToBytesConverter());
		this.addConverter(Boolean[].class, new BooleansToBytesConverter());
		this.addConverter(boolean[].class, new BoolPrimToBytesConverter());
		this.addConverter(java.sql.Date[].class, new SqlDatesToBytesConverter());
		this.addConverter(java.sql.Time[].class, new SqlTimesToBytesConverter());
		this.addConverter(java.sql.Timestamp[].class, new SqlTimestampsToBytesConverter());
		
		this.addConverter(String[].class, new StringsToBytesConverter());
		this.addConverter(BigInteger[].class, new BigIntegersToBytesConverter());
		this.addConverter(BigDecimal[].class, new BigDecimalsToBytesConverter());

	}
	
	public void addConverter(Class<?> clas,ObjectConverter<?, byte[]> conv) {
		primeConverters.put(clas.getCanonicalName(), (ObjectConverter<Object, byte[]>) conv);
		primeConverters.put(conv.conversionHint(), (ObjectConverter<Object, byte[]>) conv);
	}

	public void addConverter(L label,ObjectConverter<?, byte[]> conv) {
		labelConverters.put(label, (ObjectConverter<Object, byte[]>) conv);
		primeConverters.put(conv.conversionHint(), (ObjectConverter<Object, byte[]>) conv);
	}

	public ObjectConverter<Object, byte[]> getConverter(L label, String name) {
		if(name == null) {
			return NULL_CONVERTER ;
		}
		ObjectConverter<Object, byte[]> converter = defaultConverter;
		if(labelConverters.containsKey(label)) {
			converter = labelConverters.get(label);
		} else {
			ObjectConverter<Object, byte[]> conv = primeConverters.get(name.replace("__##", ""));
			if(conv != null) {
				converter = conv;
			}	
		}
		if(encryptor == null) {
			if(name.startsWith("__##")) {
				throw new RuntimeException("Encryption is not set "+name);
			}
			return converter;
		} else {
			final ObjectConverter<byte[], byte[]> eConverter = encryptor;
			final ObjectConverter<Object, byte[]> oConverter = converter;
			return new ObjectConverter<Object, byte[]>(){

				@Override
				public byte[] toPersistence(Object obj) {
					return eConverter.toPersistence(oConverter.toPersistence(obj));
				}

				@Override
				public Object fromPersistence(byte[] p) {
					return oConverter.fromPersistence(eConverter.fromPersistence(p));
				}

				@Override
				public String conversionHint() {
					return "__##"+oConverter.conversionHint();
				}};
		}
	}

	public ObjectConverter<Object, byte[]> getConverter(L label, Class<?> clas) {
		return (ObjectConverter<Object, byte[]>) getConverter(label,clas.getCanonicalName());
	}

	public ObjectConverter<?, byte[]> getDefaultConverter() {
		return defaultConverter;
	}

	public void setDefaultConverter(ObjectConverter<Object, byte[]> defaultConverter) {
		this.defaultConverter = defaultConverter;
	}
	

}
