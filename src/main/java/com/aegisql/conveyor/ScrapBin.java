package com.aegisql.conveyor;

public class ScrapBin<K,O> {
	public final K key;
	public final String comment;
	public final O scrap;
	public final Throwable error;
	
	public ScrapBin(K key, O scrap, String comment) {
		this.key     = key;
		this.comment = comment;
		this.scrap   = scrap;
		this.error   = null;
	}

	public ScrapBin(K key, O scrap, String comment, Throwable error) {
		this.key     = key;
		this.comment = comment;
		this.scrap   = scrap;
		this.error   = error;
	}

	@Override
	public String toString() {
		return "ScrapBin [key=" + key + ": " + comment + "; " + scrap + (error == null ? "": " error="+error.getMessage() ) +"]";
	}
	
}
