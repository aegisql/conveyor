package com.aegisql.conveyor.persistence.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;

import com.aegisql.conveyor.cart.Cart;

public class PersistFiles {

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
}
