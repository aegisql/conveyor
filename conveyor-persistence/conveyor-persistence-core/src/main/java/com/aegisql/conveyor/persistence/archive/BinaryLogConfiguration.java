package com.aegisql.conveyor.persistence.archive;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.aegisql.conveyor.persistence.utils.DataSize;

public class BinaryLogConfiguration {
	
	private final static String dateFormat = "yyyy-MM-dd'T'HH_mm_ss.SSS";
	public static class BinaryLogConfigurationBuilder {
		
		
		private String file     = "part";
		private String path     = "."+File.separator;
		private String moveTopath  = null;
		private long maxSize    = Long.MAX_VALUE;
		private int bucketSize  = 100;
		private boolean zipFile = false;
		
		public BinaryLogConfiguration build() {
			return new BinaryLogConfiguration(
					 path
					,moveTopath == null ? path : moveTopath
					,file
					,maxSize
					,bucketSize
					,zipFile
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
		public BinaryLogConfigurationBuilder moveToPath(String path) {
			if(path == null || "".equals(path)) {
				this.moveTopath = "."+File.separator;
			} else {
				this.moveTopath = path.endsWith(File.separator) ? path : path+File.separator;
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
		public BinaryLogConfigurationBuilder zipFile(boolean zip) {
			this.zipFile = zip;
			return this;
		}

		
	}
	
	private final String path;
	private final String moveToPath;
	private final long maxSize;
	private final String file;
	private final int bucketSize;
	private final boolean zipFile;
	
	private BinaryLogConfiguration(
			 String path
			,String moveToPath
			,String file
			,long maxSize
			,int bucketSize
			,boolean zipFile
			) {
		this.path       = path;
		this.moveToPath = moveToPath;
		this.file       = file;
		this.maxSize    = maxSize;
		this.bucketSize = bucketSize;
		this.zipFile    = zipFile;
	}
	
	public static BinaryLogConfigurationBuilder builder() {
		return new BinaryLogConfigurationBuilder();
	}

	public String getPath() {
		return path;
	}
	public String getMoveToPath() {
		return moveToPath;
	}

	public long getMaxSize() {
		return maxSize;
	}
	public String getFilePath() {
		return path+file+".blog";
	}

	public String getStampedFilePath() {
		Date date = new Date();
		SimpleDateFormat format = new SimpleDateFormat(dateFormat);
		String datetime = format.format(date);
		return moveToPath+file+"."+datetime+".blog";
	}

	public int getBucketSize() {
		return bucketSize;
	}

	public boolean isZipFile() {
		return zipFile;
	}

	
}
