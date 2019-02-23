package com.aegisql.conveyor.harness;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

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

}
