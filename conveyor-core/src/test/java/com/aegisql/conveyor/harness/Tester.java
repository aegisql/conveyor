package com.aegisql.conveyor.harness;

import org.apache.commons.io.FileUtils;

import java.io.*;

public class Tester {
    public static void removeDirectory(String directory) {
        File f = new File(directory);
        try {
            FileUtils.deleteDirectory(f);
            System.out.println("Directory "+directory+" has been deleted!");
        } catch (IOException e) {
            System.err.println("Problem occured when deleting the directory : " + directory);
            e.printStackTrace();
        }

    }

    public static void removeFile(String directory) {
        File f = new File(directory);
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
