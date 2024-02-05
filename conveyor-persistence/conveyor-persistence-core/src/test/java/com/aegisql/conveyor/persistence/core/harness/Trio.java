package com.aegisql.conveyor.persistence.core.harness;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Trio {

	private final String text1;
	private final String text2;
	private final int number;
	@JsonCreator
	public Trio(@JsonProperty("text1") String text1, @JsonProperty("text2") String text2, @JsonProperty("number") int number) {
		super();
		this.text1 = text1;
		this.text2 = text2;
		this.number = number;
	}
	public int getNumber() {
		return number;
	}
	public String getText1() {
		return text1;
	}
	public String getText2() {
		return text2;
	}
	@Override
	public String toString() {
		return "Trio [text1=" + text1 + ", text2=" + text2 + ", number=" + number + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + number;
		result = prime * result + ((text1 == null) ? 0 : text1.hashCode());
		result = prime * result + ((text2 == null) ? 0 : text2.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Trio other = (Trio) obj;
		if (number != other.number)
			return false;
		if (text1 == null) {
			if (other.text1 != null)
				return false;
		} else if (!text1.equals(other.text1))
			return false;
		if (text2 == null) {
			if (other.text2 != null)
				return false;
		} else if (!text2.equals(other.text2))
			return false;
		return true;
	}
	
	public boolean test(String a, String b, int x) {
		return this.equals(new Trio(a,b,x));
	}
	
}
