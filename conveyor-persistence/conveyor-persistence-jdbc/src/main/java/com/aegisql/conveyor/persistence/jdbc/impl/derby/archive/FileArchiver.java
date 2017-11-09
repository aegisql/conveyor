package com.aegisql.conveyor.persistence.jdbc.impl.derby.archive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.derby.tools.sysinfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.persistence.archive.Archiver;
import com.aegisql.conveyor.persistence.archive.BinaryLogConfiguration;
import com.aegisql.conveyor.persistence.converters.CartToBytesConverter;
import com.aegisql.conveyor.persistence.converters.ConverterAdviser;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.utils.CartOutputStream;
import com.aegisql.conveyor.persistence.utils.PersistUtils;

public class FileArchiver<K> implements Archiver<K> {

	private final static Logger LOG = LoggerFactory.getLogger(FileArchiver.class);

	private final Class<K> keyClass;
	private final String partTable;
	private final String completedTable;
	private final DeleteArchiver<K> deleteArchiver;
	private final String archivePath;
	private final BinaryLogConfiguration bLogConf;

	private Persistence<K> persistence;

	private CartToBytesConverter<K, ?, ?> converter;

	private final int saveBucketSize;
	
	private long readBytes;

	public FileArchiver(Class<K> keyClass, String partTable, String completedTable, BinaryLogConfiguration bLogConf,
			ConverterAdviser<?> adviser) {
		this.partTable = partTable;
		this.completedTable = completedTable;
		this.keyClass = keyClass;
		this.bLogConf = bLogConf;
		this.deleteArchiver = new DeleteArchiver<>(keyClass, partTable, completedTable);
		this.archivePath = bLogConf.getPath();
		this.saveBucketSize = bLogConf.getBucketSize();
		this.converter = new CartToBytesConverter<>(adviser);

	}

	private CartOutputStream<K, ?> getCartOutputStream() {
		try {
			File f = new File(bLogConf.getFilePath());
			readBytes = f.length();
			FileOutputStream fos = new FileOutputStream(f, true);
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
			Collection<Collection<Long>> balanced = PersistUtils.balanceIdList(ids, saveBucketSize);
			for(Collection<Long> bucket: balanced) {
				Collection<Cart<K, ?, Object>> carts = persistence.getParts(bucket);
				for (Cart cart: carts) {
					readBytes += cos.writeCart(cart);
					if(readBytes > bLogConf.getMaxSize()) {
						cos.close();
						
						String originalName = bLogConf.getFilePath();
						String renameName = bLogConf.getStampedFilePath();
						if(bLogConf.isZipFile()) {
							String zipName = renameName + ".zip";
							File of = new File(originalName);
							FileOutputStream fileOS = new FileOutputStream(zipName);
							try (ZipOutputStream zipOS = new ZipOutputStream(fileOS)) {
								byte[] buf = new byte[1024];
								int len;
								try (FileInputStream in = new FileInputStream(of)) {
									zipOS.putNextEntry(new ZipEntry(renameName));
									while ((len = in.read(buf)) > 0) {
										zipOS.write(buf, 0, len);
									}
								}
								of.delete();
							}
						} else {
							FileUtils.moveFile(new File(originalName), new File(renameName));
						}
						cos = getCartOutputStream();
					}
				}
			}
			cos.close();

		} catch (IOException e) {
			e.printStackTrace();
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
