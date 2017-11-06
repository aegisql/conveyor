package com.aegisql.conveyor.persistence.jdbc.archive;

public class BinaryLogConfiguration {
	
	public static class BinaryLogConfigurationBuilder {
		private String path;
		private long maxSize = 0;	
		
		public BinaryLogConfiguration build() {
			return new BinaryLogConfiguration();
		}
	}
	
	private String path;
	private long maxSize = 0;
	
	private BinaryLogConfiguration() {
		
	}
	
	public static BinaryLogConfigurationBuilder builder() {
		return new BinaryLogConfigurationBuilder();
	}
	
	
}
