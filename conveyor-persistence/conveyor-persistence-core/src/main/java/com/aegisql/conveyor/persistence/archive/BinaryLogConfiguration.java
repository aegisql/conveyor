package com.aegisql.conveyor.persistence.archive;

import com.aegisql.conveyor.persistence.converters.CartToBytesConverter;
import com.aegisql.conveyor.persistence.converters.ConverterAdviser;
import com.aegisql.conveyor.persistence.utils.DataSize;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

// TODO: Auto-generated Javadoc
/**
 * The Class BinaryLogConfiguration.
 */
public class BinaryLogConfiguration {
	
	/** The Constant dateFormat. */
	private final static String dateFormat = "yyyy-MM-dd'T'HH_mm_ss.SSS";
	
	/**
	 * The Class BinaryLogConfigurationBuilder.
	 */
	public static class BinaryLogConfigurationBuilder {
		
		
		/** The file. */
		private String file     = "part";
		
		/** The path. */
		private String path     = "."+File.separator;
		
		/** The move topath. */
		private String moveTopath  = null;
		
		/** The max size. */
		private long maxSize    = Long.MAX_VALUE;
		
		/** The bucket size. */
		private int bucketSize  = 100;
		
		/** The zip file. */
		private boolean zipFile = false;
		
		/** The cart converter. */
		private CartToBytesConverter cartConverter = new CartToBytesConverter<>();
		
		/**
		 * Builds the.
		 *
		 * @return the binary log configuration
		 */
		public BinaryLogConfiguration build() {
			return new BinaryLogConfiguration(
					 path
					,moveTopath == null ? path : moveTopath
					,file
					,maxSize
					,bucketSize
					,zipFile
					,cartConverter
					);
		}
		
		/**
		 * Path.
		 *
		 * @param path the path
		 * @return the binary log configuration builder
		 */
		public BinaryLogConfigurationBuilder path(String path) {
			
			if(path == null || "".equals(path)) {
				this.path = "."+File.separator;
			} else {
				this.path = path.endsWith(File.separator) ? path : path+File.separator;
			}
			return this;
		}
		
		/**
		 * Move to path.
		 *
		 * @param path the path
		 * @return the binary log configuration builder
		 */
		public BinaryLogConfigurationBuilder moveToPath(String path) {
			if(path == null || "".equals(path)) {
				this.moveTopath = "."+File.separator;
			} else {
				this.moveTopath = path.endsWith(File.separator) ? path : path+File.separator;
			}
			return this;
		}
		
		/**
		 * Max file size.
		 *
		 * @param size the size
		 * @param ds the ds
		 * @return the binary log configuration builder
		 */
		public BinaryLogConfigurationBuilder maxFileSize(long size,DataSize ds) {
			this.maxSize = ds.toBytes(size).longValue();
			return this;
		}
		
		/**
		 * Max file size.
		 *
		 * @param size the size
		 * @param ds the ds
		 * @return the binary log configuration builder
		 */
		public BinaryLogConfigurationBuilder maxFileSize(double size,DataSize ds) {
			this.maxSize = ds.toBytes(size).longValue();
			return this;
		}
		
		/**
		 * Max file size.
		 *
		 * @param size the size
		 * @return the binary log configuration builder
		 */
		public BinaryLogConfigurationBuilder maxFileSize(String size) {
			this.maxSize = DataSize.toBytes(size).longValue();
			return this;
		}
		
		/**
		 * Part table name.
		 *
		 * @param part the part
		 * @return the binary log configuration builder
		 */
		public BinaryLogConfigurationBuilder partTableName(String part) {
			this.file = part;
			return this;
		}
		
		/**
		 * Bucket size.
		 *
		 * @param size the size
		 * @return the binary log configuration builder
		 */
		public BinaryLogConfigurationBuilder bucketSize(int size) {
			this.bucketSize = size;
			return this;
		}
		
		/**
		 * Zip file.
		 *
		 * @param zip the zip
		 * @return the binary log configuration builder
		 */
		public BinaryLogConfigurationBuilder zipFile(boolean zip) {
			this.zipFile = zip;
			return this;
		}
		
		/**
		 * Zip file.
		 *
		 * @param adviser the adviser
		 * @return the binary log configuration builder
		 */
		public BinaryLogConfigurationBuilder zipFile(ConverterAdviser<?> adviser) {
			this.cartConverter = new CartToBytesConverter<>(adviser);
			return this;
		}
		
	}
	
	/** The path. */
	private final String path;
	
	/** The move to path. */
	private final String moveToPath;
	
	/** The max size. */
	private final long maxSize;
	
	/** The file. */
	private final String file;
	
	/** The bucket size. */
	private final int bucketSize;
	
	/** The zip file. */
	private final boolean zipFile;
	
	/** The cart converter. */
	private final CartToBytesConverter cartConverter;

	/**
	 * Instantiates a new binary log configuration.
	 *
	 * @param path the path
	 * @param moveToPath the move to path
	 * @param file the file
	 * @param maxSize the max size
	 * @param bucketSize the bucket size
	 * @param zipFile the zip file
	 * @param cartConverter the cart converter
	 */
	private BinaryLogConfiguration(
			 String path
			,String moveToPath
			,String file
			,long maxSize
			,int bucketSize
			,boolean zipFile
			,CartToBytesConverter cartConverter

			) {
		this.path       = path;
		this.moveToPath = moveToPath;
		this.file       = file;
		this.maxSize    = maxSize;
		this.bucketSize = bucketSize;
		this.zipFile    = zipFile;
		this.cartConverter = cartConverter;
	}
	
	/**
	 * Builder.
	 *
	 * @return the binary log configuration builder
	 */
	public static BinaryLogConfigurationBuilder builder() {
		return new BinaryLogConfigurationBuilder();
	}

	public static BinaryLogConfigurationBuilder builder(String partTable) {
		return new BinaryLogConfigurationBuilder().partTableName(partTable);
	}

	/**
	 * Gets the path.
	 *
	 * @return the path
	 */
	public String getPath() {
		return path;
	}
	
	/**
	 * Gets the move to path.
	 *
	 * @return the move to path
	 */
	public String getMoveToPath() {
		return moveToPath;
	}

	/**
	 * Gets the max size.
	 *
	 * @return the max size
	 */
	public long getMaxSize() {
		return maxSize;
	}
	
	/**
	 * Gets the file path.
	 *
	 * @return the file path
	 */
	public String getFilePath() {
		return path+file+".blog";
	}

	/**
	 * Gets the stamped file path.
	 *
	 * @return the stamped file path
	 */
	public String getStampedFilePath() {
		Date date = new Date();
		SimpleDateFormat format = new SimpleDateFormat(dateFormat);
		String datetime = format.format(date);
		return moveToPath+file+"."+datetime+".blog";
	}

	/**
	 * Gets the bucket size.
	 *
	 * @return the bucket size
	 */
	public int getBucketSize() {
		return bucketSize;
	}

	/**
	 * Checks if is zip file.
	 *
	 * @return true, if is zip file
	 */
	public boolean isZipFile() {
		return zipFile;
	}

	/**
	 * Gets the cart converter.
	 *
	 * @return the cart converter
	 */
	public CartToBytesConverter getCartConverter() {
		return cartConverter;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "path=" + path + ", moveToPath=" + moveToPath + ", maxSize=" + maxSize
				+ ", file=" + file + ", bucketSize=" + bucketSize + ", zipFile=" + zipFile;
	}

	
	
}
