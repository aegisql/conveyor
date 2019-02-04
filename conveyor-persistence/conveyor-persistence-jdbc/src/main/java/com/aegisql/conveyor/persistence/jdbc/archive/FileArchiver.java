package com.aegisql.conveyor.persistence.jdbc.archive;

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
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.persistence.archive.Archiver;
import com.aegisql.conveyor.persistence.archive.BinaryLogConfiguration;
import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo;
import com.aegisql.conveyor.persistence.utils.CartOutputStream;
import com.aegisql.conveyor.persistence.utils.PersistUtils;

public class FileArchiver<K> extends AbstractJdbcArchiver<K> {

	private final Archiver<K> deleteArchiver;
	private final BinaryLogConfiguration bLogConf;

	private long readBytes;


	public FileArchiver(EngineDepo<K> engine, BinaryLogConfiguration bLogConf) {
		
		super(engine);
		this.deleteArchiver = new DeleteArchiver<>(engine);
		this.bLogConf = bLogConf;
	}

	private CartOutputStream<K, ?> getCartOutputStream() {
		try {
			File f = new File(bLogConf.getFilePath());
			readBytes = f.length();
			FileOutputStream fos = new FileOutputStream(f, true);
			return new CartOutputStream<>(bLogConf.getCartConverter(), fos);
		} catch (FileNotFoundException e) {
			throw new PersistenceException("Error opening file " + bLogConf.getPath(), e);
		}
	}

	private void archiveCarts(Collection<Cart<K, ?, Object>> carts,CartOutputStream<K, ?> cos) throws IOException {
		for (Cart cart: carts) {
			readBytes += cos.writeCart(cart);
			if(readBytes > bLogConf.getMaxSize()) {
				cos.close();
				
				String originalName = bLogConf.getFilePath();
				String renameName   = bLogConf.getStampedFilePath();

				FileUtils.moveFile(new File(originalName), new File(renameName));

				if(bLogConf.isZipFile()) {
					String zipName = renameName + ".zip";
					FileOutputStream fileOS = new FileOutputStream(zipName);
					try (ZipOutputStream zipOS = new ZipOutputStream(fileOS)) {
						byte[] buf = new byte[1024];
						int len;
						try (FileInputStream in = new FileInputStream(renameName)) {
							zipOS.putNextEntry(new ZipEntry(renameName));
							while ((len = in.read(buf)) > 0) {
								zipOS.write(buf, 0, len);
							}
						}
					}
					new File(renameName).delete();
					LOG.debug("{} file reached limit of {} and were moved to {}",originalName,bLogConf.getMaxSize(),zipName);
				} else {
					LOG.debug("{} file reached limit of {} and were moved to {}",originalName,bLogConf.getMaxSize(),renameName);
				}
				cos = getCartOutputStream();
			}
		}

	}
	
	@Override
	public void archiveParts(Connection conn, Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return;
		}

		try {
			CartOutputStream<K, ?> cos = getCartOutputStream();
			Collection<Collection<Long>> balanced = PersistUtils.balanceIdList(ids, bLogConf.getBucketSize());
			for(Collection<Long> bucket: balanced) {
				Collection<Cart<K, ?, Object>> carts = persistence.getParts(bucket);
				archiveCarts(carts,cos);
			}
			cos.close();

		} catch (IOException e) {
			e.printStackTrace();
			throw new PersistenceException("Error saving carts", e);
		}
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
		try {
			CartOutputStream<K, ?> cos = getCartOutputStream();
			Collection<Cart<K, ?, Object>> carts = persistence.getExpiredParts();
			archiveCarts(carts,cos);
			cos.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new PersistenceException("Error saving expired carts", e);
		}
		deleteArchiver.archiveExpiredParts(conn);
	}

	@Override
	public void archiveAll(Connection conn) {
		try {
			CartOutputStream<K, ?> cos = getCartOutputStream();
			Collection<Cart<K, ?, Object>> carts = persistence.getAllParts();
			archiveCarts(carts,cos);
			cos.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new PersistenceException("Error saving expired carts", e);
		}
		deleteArchiver.archiveAll(conn);
	}

	@Override
	public String toString() {
		return "FileArchiver: "+bLogConf;
	}
	
	

}
