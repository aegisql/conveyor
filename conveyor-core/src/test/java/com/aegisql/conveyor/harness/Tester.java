package com.aegisql.conveyor.harness;

import org.apache.commons.io.FileUtils;

import java.io.*;

public class Tester {
    private static final String TEST_ARTIFACTS_DIR_PROPERTY = "conveyor.core.test.artifacts.dir";
    private static final String DEFAULT_TEST_ARTIFACTS_DIR = "test-artifacts";

    public static File testArtifactsDirectory() {
        File dir = new File(System.getProperty(TEST_ARTIFACTS_DIR_PROPERTY, DEFAULT_TEST_ARTIFACTS_DIR));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File testFile(String name) {
        File file = new File(name);
        if (!file.isAbsolute()) {
            file = new File(testArtifactsDirectory(), name);
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        return file;
    }

    public static void removeDirectory(String directory) {
        File f = testFile(directory);
        try {
            FileUtils.deleteDirectory(f);
            System.out.println("Directory "+directory+" has been deleted!");
        } catch (IOException e) {
            System.err.println("Problem occured when deleting the directory : " + directory);
            e.printStackTrace();
        }

    }

    public static void removeFile(String directory) {
        File f = testFile(directory);
        FileUtils.deleteQuietly(f);
        System.out.println("File "+directory+" has been deleted!");
    }

    public static <T extends Serializable> byte[] pickle(T obj)
            throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.close();
        return baos.toByteArray();
    }

    public static <T extends Serializable> T unpickle(byte[] b, Class<T> cl)
            throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        ObjectInputStream ois = new ObjectInputStream(bais);
        Object o = ois.readObject();
        return cl.cast(o);
    }

}
