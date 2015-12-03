package com.aegisql.conveyor;

public class ScrapBin<K,O> {
	private final K key;
	private final String comment;
	private final O scrap;
	
	private Throwable error = null;
	
	public ScrapBin(K key, O scrap, String comment) {
		this.key     = key;
		this.comment = comment;
		this.scrap   = scrap;
	}

	public ScrapBin(K key, O scrap, String comment, Throwable error) {
		this.key     = key;
		this.comment = comment;
		this.scrap   = scrap;
		this.error   = error;
	}

	public String getComment() {
		return comment;
	}
	public O getScrap() {
		return scrap;
	}
	public K getKey() {
		return key;
	}
	public Throwable getError() {
		return error;
	}

	@Override
	public String toString() {
		return "ScrapBin [key=" + key + ": " + comment + "; " + scrap + (error == null ? "": " error="+error.getMessage() ) +"]";
	}
	
}
