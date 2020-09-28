package com.sozolab.sumon;

import android.os.Environment;

import com.github.mikephil.charting.utils.FileUtils;

import java.io.File;

public class FileHandler {
    static String dataDirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "ESenseData" + File.separator;
    static String categorizedDirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "ESenseCategorized" + File.separator;

    public static void deleteRecursive() {
        File dir = new File(dataDirPath);

        for (String child: dir.list()) {
            File myFile = new File(dir, child);
            myFile.delete();
        }
    }

    public static void deleteCategorizedRecursive() {
        File dir = new File(categorizedDirPath);

        deleteRec(dir);
    }

    private static void deleteRec(File file) {
        if (!file.exists())
            return;

        //if directory, go inside and call recursively
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                //call recursively
                deleteRec(f);
            }
        }
        //call delete to delete files and empty directory
        file.delete();
    }
}
