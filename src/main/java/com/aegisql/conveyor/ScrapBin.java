package com.aegisql.conveyor;

public class ScrapBin<K,O> {
	private final K key;
	private final String comment;
	private final O scrap;
	public ScrapBin(K key, O scrap, String comment) {
		this.key     = key;
		this.comment = comment;
		this.scrap   = scrap;
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
	@Override
	public String toString() {
		return "ScrapBin [key=" + key + ": " + comment + "; " + scrap + "]";
	}
	
}
