package com.aegisql.conveyor.persistence.utils;

import com.aegisql.conveyor.cart.Cart;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.zip.*;

public class PersistUtils {

	private static String tempDir;

	public static String getTempDirectory() {
		if (tempDir == null) {
			String ioTempDir = System.getProperty("java.io.tmpdir");
			String convPath = "aegisql" + File.separator + "conveyor" + File.separator;
			if (ioTempDir.endsWith(File.separator)) {
				tempDir = ioTempDir + convPath;
			} else {
				tempDir = ioTempDir + File.separator + convPath;
			}
		}
		return tempDir;
	}

	public static boolean createTempDirectory() {
		String tmp = getTempDirectory();
		File tmpFile = new File(tmp);
		return tmpFile.mkdirs();
	}

	public static void cleanTempDirectory() throws IOException {
		String tmp = getTempDirectory();
		File tmpFile = new File(tmp);
		FileUtils.cleanDirectory(tmpFile);
	}

	public static <K, V, L> void saveCart(String filePath, Cart<K, V, L> cart) throws IOException {
		FileOutputStream fos = new FileOutputStream(filePath);
		try (ObjectOutputStream os = new ObjectOutputStream(fos)) {
			os.writeObject(cart);
			os.flush();
		}
	}

	public static <K, V, L> Cart<K, V, L> readCart(String filePath) throws IOException, ClassNotFoundException {
		byte[] array = FileUtils.readFileToByteArray(new File(filePath));
		ByteArrayInputStream bis = new ByteArrayInputStream(array);
		ObjectInputStream ois = new ObjectInputStream(bis);
		return (Cart<K, V, L>) ois.readObject();
	}

	public static void zipDirectory(String srcDir, String zipFile) throws Exception {
		FileOutputStream fileOS = new FileOutputStream(zipFile);
		try (ZipOutputStream zipOS = new ZipOutputStream(fileOS)) {
			addFolderToZip("", srcDir, zipOS);
			zipOS.flush();
		}
	}

	private static void addFileToZip(String path, String srcFile, ZipOutputStream zipOS) throws Exception {
		File folder = new File(srcFile);
		if (folder.isDirectory()) {
			addFolderToZip(path, srcFile, zipOS);
		} else {
			byte[] buf = new byte[1024];
			int len;
			try (FileInputStream in = new FileInputStream(srcFile)) {
				zipOS.putNextEntry(new ZipEntry(path + File.separator + folder.getName()));
				while ((len = in.read(buf)) > 0) {
					zipOS.write(buf, 0, len);
				}
			}
		}
	}

	private static void addFolderToZip(String path, String srcFolder, ZipOutputStream zipOS) throws Exception {
		File folder = new File(srcFolder);
		for (String fileName : folder.list()) {
			if (path.equals("")) {
				addFileToZip(folder.getName(), srcFolder + File.separator + fileName, zipOS);
			} else {
				addFileToZip(path + File.separator + folder.getName(), srcFolder + File.separator + fileName, zipOS);
			}
		}
	}

	public static byte[] compress(byte[] data) throws IOException {
		Deflater deflater = new Deflater();
		deflater.setInput(data);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
		deflater.finish();
		byte[] buffer = new byte[1024];
		while (!deflater.finished()) {
			int count = deflater.deflate(buffer); // returns the generated code... index
			outputStream.write(buffer, 0, count);
		}
		outputStream.close();
		byte[] output = outputStream.toByteArray();
		return output;
	}

	public static byte[] decompress(byte[] data) throws IOException, DataFormatException {
		Inflater inflater = new Inflater();
		inflater.setInput(data);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
		byte[] buffer = new byte[1024];
		while (!inflater.finished()) {
			int count = inflater.inflate(buffer);
			outputStream.write(buffer, 0, count);
		}
		outputStream.close();
		byte[] output = outputStream.toByteArray();
		return output;
	}

	public static Collection<Collection<Long>> balanceIdList(Collection<Long> col, int partSize) {
		int buckets = col.size() / partSize;
		if( col.size() % partSize > 0) {
			buckets++;
		}
		Collection<Collection<Long>> res = new ArrayList<>(buckets);
		Iterator<Long> it = col.iterator();
		for(int i = 0; i < buckets; i++) {
			Collection<Long> bucket = new ArrayList<>(partSize);
			for(int j = 0; j < partSize; j++) {
				if(! it.hasNext()) {
					break;
				}
				bucket.add(it.next());
			}
			res.add(bucket);
		}
		return res;
	}
	
}
