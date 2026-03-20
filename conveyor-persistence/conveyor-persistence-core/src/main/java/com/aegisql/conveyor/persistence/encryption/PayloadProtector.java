package com.aegisql.conveyor.persistence.encryption;

/**
 * Protects serialized payload bytes before they are stored and restores them on read.
 */
public interface PayloadProtector {

	byte[] encrypt(byte[] plainBytes);

	byte[] decrypt(byte[] protectedBytes);

}
