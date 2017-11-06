package com.aegisql.conveyor.persistence.jdbc.impl.derby.archive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.persistence.converters.CartToBytesConverter;
import com.aegisql.conveyor.persistence.converters.ConverterAdviser;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.jdbc.archive.Archiver;
import com.aegisql.conveyor.persistence.utils.CartOutputStream;

public class FileArchiver<K> implements Archiver<K> {

	private final static Logger LOG = LoggerFactory.getLogger(FileArchiver.class);

	private final Class<K> keyClass;
	private final String partTable;
	private final String completedTable;
	private final DeleteArchiver<K> deleteArchiver;
	private final String archivePath;

	private final String fileNameTmpl;

	private Persistence<K> persistence;

	private CartToBytesConverter<K, ?, ?> converter;

	public FileArchiver(Class<K> keyClass, String partTable, String completedTable, String path,
			ConverterAdviser<?> adviser) {
		this.partTable = partTable;
		this.completedTable = completedTable;
		this.keyClass = keyClass;
		this.deleteArchiver = new DeleteArchiver<>(keyClass, partTable, completedTable);
		this.archivePath = path.endsWith(File.separator) ? path : path + File.separator;

		this.fileNameTmpl = archivePath + partTable + ".blog";
		this.converter = new CartToBytesConverter<>(adviser);

	}

	private CartOutputStream<K, ?> getCartOutputStream() {
		try {
			FileOutputStream fos = new FileOutputStream(fileNameTmpl, true);
			return new CartOutputStream<>(converter, fos);
		} catch (FileNotFoundException e) {
			throw new PersistenceException("Error opening file " + archivePath, e);
		}
	}

	@Override
	public void archiveParts(Connection conn, Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return;
		}

		try {
			CartOutputStream<K, ?> cos = getCartOutputStream();

			for (long id : ids) {
				Cart cart = persistence.getPart(id);
				cos.writeCart(cart);
			}
			cos.close();
		} catch (IOException e) {
			throw new PersistenceException("Error saving carts", e);
		}
		LOG.debug("Archived parts successfully. About to delete data from {}", partTable);
		deleteArchiver.archiveParts(conn, ids);
	}

	@Override
	public void archiveKeys(Connection conn, Collection<K> keys) {
		if (keys == null || keys.isEmpty()) {
			return;
		}
		ArrayList<Long> ids = new ArrayList<>();
		for(K key:keys) {
			ids.addAll(persistence.getAllPartIds(key));
		}
		archiveParts(conn, ids);
		LOG.debug("Archived parts for keys successfully. About to delete data from {}", partTable);
		deleteArchiver.archiveKeys(conn, keys);
	}

	@Override
	public void archiveCompleteKeys(Connection conn, Collection<K> keys) {
		if (keys == null || keys.isEmpty()) {
			return;
		}
		// nothing else required. Just delete completed keys
		deleteArchiver.archiveCompleteKeys(conn, keys);
	}

	@Override
	public void archiveExpiredParts(Connection conn) {
		// TODO - impl body
		LOG.debug("Archived expired parts successfully. About to delete data from {}", partTable);
		deleteArchiver.archiveExpiredParts(conn);
	}

	@Override
	public void archiveAll(Connection conn) {
		// TODO - impl body
		LOG.debug("Archived all parts successfully. About to delete data from {}", partTable);
		deleteArchiver.archiveAll(conn);
	}

	@Override
	public void setPersistence(Persistence<K> persistence) {
		this.persistence = persistence;
	}

}
