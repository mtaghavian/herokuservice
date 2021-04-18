package com.bcom.nsplacer.misc;

import java.io.File;

public class FileUtils {

    public static boolean deleteDirectory(File dir) {
        File[] allContents = dir.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return dir.delete();
    }
}
