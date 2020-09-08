package com.sozolab.sumon;

import android.os.Environment;

import com.github.mikephil.charting.utils.FileUtils;

import java.io.File;

public class FileHandler {
    static String dataDirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "ESenseData" + File.separator;

    public static void deleteRecursive() {
        File dir = new File(dataDirPath);

        for (String child: dir.list()) {
            File myFile = new File(dir, child);
            myFile.delete();
        }
    }
}
