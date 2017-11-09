package com.aegisql.conveyor.persistence.archive;

import java.io.File;
import java.sql.Timestamp;

import com.aegisql.conveyor.persistence.utils.DataSize;

public class BinaryLogConfiguration {
	
	public static class BinaryLogConfigurationBuilder {
		private String file = "part";
		private String path = "."+File.separator;
		private long maxSize = 0;
		private int bucketSize = 100;
		
		public BinaryLogConfiguration build() {
			return new BinaryLogConfiguration(
					 path
					,file
					,maxSize
					,bucketSize
					);
		}
		
		public BinaryLogConfigurationBuilder path(String path) {
			
			if(path == null || "".equals(path)) {
				this.path = "."+File.separator;
			} else {
				this.path = path.endsWith(File.separator) ? path : path+File.separator;
			}
			return this;
		}
		public BinaryLogConfigurationBuilder maxFileSize(long size,DataSize ds) {
			this.maxSize = ds.toBytes(size).longValue();
			return this;
		}
		public BinaryLogConfigurationBuilder maxFileSize(double size,DataSize ds) {
			this.maxSize = ds.toBytes(size).longValue();
			return this;
		}
		public BinaryLogConfigurationBuilder maxFileSize(String size) {
			this.maxSize = DataSize.toBytes(size).longValue();
			return this;
		}
		public BinaryLogConfigurationBuilder partTableName(String part) {
			this.file = part;
			return this;
		}
		public BinaryLogConfigurationBuilder bucketSize(int size) {
			this.bucketSize = size;
			return this;
		}

		
	}
	
	private final String path;
	private final long maxSize;
	private final String file;
	private final int bucketSize;
	
	private BinaryLogConfiguration(
			 String path
			,String file
			,long maxSize
			,int bucketSize
			) {
		this.path       = path;
		this.file       = file;
		this.maxSize    = maxSize;
		this.bucketSize = bucketSize;
	}
	
	public static BinaryLogConfigurationBuilder builder() {
		return new BinaryLogConfigurationBuilder();
	}

	public String getPath() {
		return path;
	}

	public long getMaxSize() {
		return maxSize;
	}
	public String getFilePath() {
		return path+file+".blog";
	}

	public String getStampedFilePath() {
		Timestamp ts = new Timestamp(System.currentTimeMillis());
		String datetime = ts.toString().replace(" ", "T");
		return path+file+"."+datetime+".blog";
	}

	public int getBucketSize() {
		return bucketSize;
	}

	
}
