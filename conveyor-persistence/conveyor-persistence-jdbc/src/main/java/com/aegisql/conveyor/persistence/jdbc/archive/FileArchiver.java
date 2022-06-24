package com.aegisql.conveyor.persistence.jdbc.archive;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.persistence.archive.Archiver;
import com.aegisql.conveyor.persistence.archive.BinaryLogConfiguration;
import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo;
import com.aegisql.conveyor.persistence.utils.CartOutputStream;
import com.aegisql.conveyor.persistence.utils.PersistUtils;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

// TODO: Auto-generated Javadoc
/**
 * The Class FileArchiver.
 *
 * @param <K> the key type
 */
public class FileArchiver<K> extends AbstractJdbcArchiver<K> {

	/** The delete archiver. */
	private final Archiver<K> deleteArchiver;
	
	/** The b log conf. */
	private final BinaryLogConfiguration bLogConf;

	/** The read bytes. */
	private long readBytes;


	/**
	 * Instantiates a new file archiver.
	 *
	 * @param engine the engine
	 * @param bLogConf the b log conf
	 */
	public FileArchiver(EngineDepo<K> engine, BinaryLogConfiguration bLogConf) {
		super(engine);
		this.deleteArchiver = new DeleteArchiver<>(engine);
		this.bLogConf = bLogConf;
	}

	/**
	 * Gets the cart output stream.
	 *
	 * @return the cart output stream
	 */
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

	private void truncateFile(String name) {
		File f = new File(name);
		//noinspection EmptyTryBlock
		try(FileOutputStream fos = new FileOutputStream(f, false)) {

		} catch (IOException e) {
			throw new PersistenceException("Error truncating file " + bLogConf.getPath(), e);
		}
	}

	/**
	 * Archive carts.
	 *
	 * @param carts the carts
	 * @param cos the cos
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void archiveCarts(Collection<Cart<K, ?, Object>> carts,CartOutputStream<K, ?> cos) throws IOException {
		for (Cart cart: carts) {
			readBytes += cos.writeCart(cart);
			if(readBytes > bLogConf.getMaxSize()) {
				cos.close();
				
				String originalName = bLogConf.getFilePath();
				String renameName   = bLogConf.getStampedFilePath();

				FileUtils.copyFile(new File(originalName), new File(renameName));
				truncateFile(originalName);
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
					//noinspection ResultOfMethodCallIgnored
					new File(renameName).delete();
					LOG.debug("{} file reached limit of {} and were moved to {}",originalName,bLogConf.getMaxSize(),zipName);
				} else {
					LOG.debug("{} file reached limit of {} and were moved to {}",originalName,bLogConf.getMaxSize(),renameName);
				}
				cos = getCartOutputStream();
			}
		}

	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveParts(java.util.Collection)
	 */
	@Override
	public void archiveParts(Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return;
		}

		try(CartOutputStream<K, ?> cos = getCartOutputStream()) {
			Collection<Collection<Long>> balanced = PersistUtils.balanceIdList(ids, bLogConf.getBucketSize());
			for(Collection<Long> bucket: balanced) {
				Collection<Cart<K, ?, Object>> carts = persistence.getParts(bucket);
				archiveCarts(carts,cos);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new PersistenceException("Error saving carts", e);
		}
		deleteArchiver.archiveParts(ids);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveKeys(java.util.Collection)
	 */
	@Override
	public void archiveKeys( Collection<K> keys) {
		if (keys == null || keys.isEmpty()) {
			return;
		}
		ArrayList<Long> ids = new ArrayList<>();
		for(K key:keys) {
			ids.addAll(persistence.getAllPartIds(key));
		}
		archiveParts(ids);
		deleteArchiver.archiveKeys(keys);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveCompleteKeys(java.util.Collection)
	 */
	@Override
	public void archiveCompleteKeys(Collection<K> keys) {
		if (keys == null || keys.isEmpty()) {
			return;
		}
		// nothing else required. Just delete completed keys
		deleteArchiver.archiveCompleteKeys(keys);
	}

	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveExpiredParts()
	 */
	@Override
	public void archiveExpiredParts() {
		try(CartOutputStream<K, ?> cos = getCartOutputStream()) {
			Collection<Cart<K, ?, Object>> carts = persistence.getExpiredParts();
			archiveCarts(carts,cos);
		} catch (IOException e) {
			e.printStackTrace();
			throw new PersistenceException("Error saving expired carts", e);
		}
		deleteArchiver.archiveExpiredParts();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveAll()
	 */
	@Override
	public void archiveAll() {
		try(CartOutputStream<K, ?> cos = getCartOutputStream()) {
			Collection<Cart<K, ?, Object>> carts = persistence.getAllParts();
			archiveCarts(carts,cos);
		} catch (IOException e) {
			e.printStackTrace();
			throw new PersistenceException("Error saving expired carts", e);
		}
		deleteArchiver.archiveAll();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.archive.AbstractJdbcArchiver#toString()
	 */
	@Override
	public String toString() {
		return super.toString()+" bLogConf="+bLogConf;
	}

}
