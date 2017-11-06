package com.aegisql.conveyor.persistence.utils;

import java.math.BigDecimal;
import java.util.Objects;

import com.aegisql.conveyor.persistence.core.PersistenceException;

public enum DataSize {
	 B(1)
	,KB(1024)
	,MB(1024*1024)
	,GB(1024*1024*1024)
	,TB(GB.multiplier.multiply(BigDecimal.valueOf(1024)))
	,PB(TB.multiplier.multiply(BigDecimal.valueOf(1024)))
	,EB(PB.multiplier.multiply(BigDecimal.valueOf(1024)));
	
	private static class SDataSize {
		private DataSize ds  = B;
		private String   val = "-1";
		
		private final static String types = "BKMGTPE";
		
		public SDataSize(String s) {
			Objects.requireNonNull(s, "Expected non-null data size string");
			char[] chars = s.trim().toUpperCase().toCharArray();
			if(chars.length == 0) {
				throw new PersistenceException("Expected non-empty data size string");
			}
			StringBuilder valBulder     = new StringBuilder();
			StringBuilder dsTypeBulder  = new StringBuilder();
			boolean readingVal          = true;
			for(int i = 0; i < chars.length; i++) {
				char c = chars[i];
				if(Character.isDigit(c) || c=='.') {
					if(readingVal) {
						valBulder.append(c);
					} else {
						throw new RuntimeException("Unexpected digit after size type");
					}
				} else {
					readingVal = false;
					if(Character.isSpaceChar(c)) {
						continue;
					}
					if(types.indexOf(c)==-1) {
						throw new PersistenceException("UnExpected character "+c+" in DataSize string "+s);
					} else {
						dsTypeBulder.append(c);
					}
				}
			}
			
			this.ds  = DataSize.valueOf(dsTypeBulder.toString());
			this.val = valBulder.toString();
		}

		public DataSize getDs() {
			return ds;
		}

		public String getVal() {
			return val;
		}
		
	}
	
	BigDecimal multiplier;
	
	DataSize(BigDecimal multiplier) {
		this.multiplier = multiplier;
	}
	
	DataSize(long multiplier) {
		this.multiplier = BigDecimal.valueOf(multiplier);
	}

	//convert
	public BigDecimal convert(double size,DataSize from){
		return convert(from.multiplier.multiply(BigDecimal.valueOf(size)),from).divide(from.multiplier);
	}
	public BigDecimal convert(long size, DataSize from){
		return convert(BigDecimal.valueOf(size),from);
	}
	public BigDecimal convert(BigDecimal size, DataSize from){
		return size.multiply(from.multiplier).divide(multiplier);
	}
	
	//toBytes
	public BigDecimal toBytes(long size) {
		return B.convert(size, this);
	}
	public BigDecimal toBytes(double size) {
		return B.convert(size, this);
	}
	public BigDecimal toBytes(BigDecimal size) {
		return B.convert(size, this);
	}
	public static BigDecimal toBytes(String size) {
		SDataSize sds = new SDataSize(size);
		return B.convert(new BigDecimal(sds.getVal()), sds.getDs());
	}

	//toKiloBytes
	public BigDecimal toKBytes(long size) {
		return KB.convert(size, this);
	}
	public BigDecimal toKBytes(double size) {
		return KB.convert(size, this);
	}
	public BigDecimal toKBytes(BigDecimal size) {
		return KB.convert(size, this);
	}
	public static BigDecimal toKBytes(String size) {
		SDataSize sds = new SDataSize(size);
		return KB.convert(new BigDecimal(sds.getVal()), sds.getDs());
	}

	//toMegaBytes
	public BigDecimal toMBytes(long size) {
		return MB.convert(size, this);
	}
	public BigDecimal toMBytes(double size) {
		return MB.convert(size, this);
	}
	public BigDecimal toMBytes(BigDecimal size) {
		return MB.convert(size, this);
	}
	public static BigDecimal toMBytes(String size) {
		SDataSize sds = new SDataSize(size);
		return MB.convert(new BigDecimal(sds.getVal()), sds.getDs());
	}

	//toGigaBytes
	public BigDecimal toGBytes(long size) {
		return GB.convert(size, this);
	}
	public BigDecimal toGBytes(double size) {
		return GB.convert(size, this);
	}
	public BigDecimal toGBytes(BigDecimal size) {
		return GB.convert(size, this);
	}
	public static BigDecimal toGBytes(String size) {
		SDataSize sds = new SDataSize(size);
		return GB.convert(new BigDecimal(sds.getVal()), sds.getDs());
	}

	//toTeraBytes
	public BigDecimal toTBytes(long size) {
		return TB.convert(size, this);
	}
	public BigDecimal toTBytes(double size) {
		return TB.convert(size, this);
	}
	public BigDecimal toTBytes(BigDecimal size) {
		return TB.convert(size, this);
	}
	public static BigDecimal toTBytes(String size) {
		SDataSize sds = new SDataSize(size);
		return TB.convert(new BigDecimal(sds.getVal()), sds.getDs());
	}

	//toPetaBytes
	public BigDecimal toPBytes(long size) {
		return PB.convert(size, this);
	}
	public BigDecimal toPBytes(double size) {
		return PB.convert(size, this);
	}
	public BigDecimal toPBytes(BigDecimal size) {
		return PB.convert(size, this);
	}
	public static BigDecimal toPBytes(String size) {
		SDataSize sds = new SDataSize(size);
		return PB.convert(new BigDecimal(sds.getVal()), sds.getDs());
	}

	//toExaBytes
	public BigDecimal toEBytes(long size) {
		return EB.convert(size, this);
	}
	public BigDecimal toEBytes(double size) {
		return EB.convert(size, this);
	}
	public BigDecimal toEBytes(BigDecimal size) {
		return EB.convert(size, this);
	}
	public static BigDecimal toEBytes(String size) {
		SDataSize sds = new SDataSize(size);
		return EB.convert(new BigDecimal(sds.getVal()), sds.getDs());
	}

}
