package com.sozolab.sumon;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.sozolab.sumon.io.esense.esenselib.ESenseConfig;
import com.sozolab.sumon.io.esense.esenselib.ESenseEvent;
import com.sozolab.sumon.io.esense.esenselib.ESenseSensorListener;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SensorListenerManager implements ESenseSensorListener {

    private final String TAG = "SensorListenerManager";
    private long timeStamp;
    private long cacheStamp = 0;
    private double[] accel;
    private double[] gyro;
    private boolean dataCollecting;

    Context context;
    Workbook excelWorkbook;
    int rowIndex;
    String sheetName;
    Sheet excelSheet;
    File excelFile;
    String sensorDataFile;
    ESenseConfig eSenseConfig;
    String dataDirPath;
    String categorizedDirPath;
    String activityName;
    ESenseEvent eventCache;
    long timeTouch;
    boolean isStart;
    int samplingRate;

    public SensorListenerManager(Context context){
        this.context = context;
        eSenseConfig = new ESenseConfig();
        rowIndex = 1;
        activityName = "";
        sheetName = "";
        excelSheet = null;
        excelFile = null;
        samplingRate = ConnectionListenerManager.getSamplingRate();

        dataDirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "ESenseData" + File.separator;
        Log.d(TAG, "Sensor Data Path : " + dataDirPath);
    }

    /**
     * Set up the sensor with given configuration.
     *
     * @param context Application context.
     * @param eSenseConfig Configuration of sensor.
     */
    public SensorListenerManager(Context context, ESenseConfig eSenseConfig) {
        this(context);
        this.eSenseConfig = eSenseConfig;
    }

    /**
     * Called when there is new sensor data available
     *
     * @param evt object containing the sensor samples received
     */
    @Override
    public void onSensorChanged(ESenseEvent evt) {
        Log.d(TAG, "onSensorChanged()");
        eventCache = evt;
        if (dataCollecting){

            if(excelSheet != null){
                rowIndex++;

                if (isStart) {
                    timeStamp = evt.getTimestamp();
                    isStart = false;
                } else {
                    timeStamp = (long) (cacheStamp + (1.0 / samplingRate) * 1000 - 1);
                }
//                timeStamp = evt.getTimestamp();

                if (timeStamp == cacheStamp) {
                    Log.e(TAG, "Time stamp error");
//                    return;
                }

                cacheStamp = timeStamp;

                accel = evt.convertAccToG(eSenseConfig);
                gyro = evt.convertGyroToDegPerSecond(eSenseConfig);

                Row dataRow = excelSheet.createRow(rowIndex);
                Cell dataCell = null;

                dataCell = dataRow.createCell(0);
                dataCell.setCellValue(evt.getPacketIndex());

                dataCell = dataRow.createCell(1);
                dataCell.setCellValue(timeStamp);

                dataCell = dataRow.createCell(2);
                dataCell.setCellValue(accel[0]);

                dataCell = dataRow.createCell(3);
                dataCell.setCellValue(accel[1]);

                dataCell = dataRow.createCell(4);
                dataCell.setCellValue(accel[2]);

                dataCell = dataRow.createCell(5);
                dataCell.setCellValue(gyro[0]);

                dataCell = dataRow.createCell(6);
                dataCell.setCellValue(gyro[1]);

                dataCell = dataRow.createCell(7);
                dataCell.setCellValue(gyro[2]);

                dataCell = dataRow.createCell(8);
                dataCell.setCellValue(activityName);



                String sensorData = " Activity : " + activityName + " Row : " + rowIndex + " Time : " + timeStamp
                        + " accel : " + accel[0] + " " + accel[1] + " " + accel[2] + " gyro : " + gyro[0] + " " + gyro[1] + " " + gyro[2];
                Log.d(TAG, sensorData);
            }
        }
    }

    public void setColumnWidth(Sheet sheet){
        sheet.setColumnWidth(0, (15 * 300));
        sheet.setColumnWidth(1, (15 * 300));
        sheet.setColumnWidth(2, (15 * 300));
        sheet.setColumnWidth(3, (15 * 300));
        sheet.setColumnWidth(4, (15 * 300));
        sheet.setColumnWidth(5, (15 * 300));
        sheet.setColumnWidth(6, (15 * 300));
        sheet.setColumnWidth(7, (15 * 300));
        sheet.setColumnWidth(8, (15 * 300));
    }

    public void startDataCollection(String activity) {
        isStart = true;
        samplingRate = ConnectionListenerManager.getSamplingRate();
        
        Log.i(TAG, String.format("Current rate %d",samplingRate));
        this.activityName = activity; //activity is built in MainActivity

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd_MM_yyyy_hh_mm_ss_a", Locale.getDefault());
        String currentDateTime = simpleDateFormat.format(new Date());

        sensorDataFile = activityName + "_" + currentDateTime + ".xls";
        File dataDir = new File(dataDirPath);

        if (!dataDir.exists()) {
            dataDir.mkdir();
        }

        excelFile = new File(dataDirPath, sensorDataFile);

        sheetName = activityName;
        excelWorkbook = new HSSFWorkbook();
        excelSheet = excelWorkbook.createSheet(sheetName);

        Row dataRow = excelSheet.createRow(rowIndex);
        Cell dataCell = null;

        dataCell = dataRow.createCell(0);
        dataCell.setCellValue("Packet Index");

        dataCell = dataRow.createCell(1);
        dataCell.setCellValue("Time stamp");

        dataCell = dataRow.createCell(2);
        dataCell.setCellValue("Ax");

        dataCell = dataRow.createCell(3);
        dataCell.setCellValue("Ay");

        dataCell = dataRow.createCell(4);
        dataCell.setCellValue("Az");

        dataCell = dataRow.createCell(5);
        dataCell.setCellValue("Gx");

        dataCell = dataRow.createCell(6);
        dataCell.setCellValue("Gy");

        dataCell = dataRow.createCell(7);
        dataCell.setCellValue("Gz");

        dataCell = dataRow.createCell(8);
        dataCell.setCellValue("Activity Label");

        setColumnWidth(excelSheet);
        dataCollecting = true;
        Log.i(TAG, "Listener start");
    }

    public void stopDataCollection(){
        Row dataRow = excelSheet.createRow(++rowIndex);
        Cell dataCell = null;

        Log.i(TAG, "Time stamp for touching is: " + timeTouch);

        dataCell = dataRow.createCell(0);
        dataCell.setCellValue(timeTouch);

        rowIndex = 1;
        dataCollecting = false;
        FileOutputStream accelOutputStream = null;

        try {
            accelOutputStream = new FileOutputStream(excelFile);
            excelWorkbook.write(accelOutputStream);

            File categorizedDir = new File(categorizedDirPath);
            File categorizedFile = new File(categorizedDirPath, sensorDataFile);

            Log.i(TAG, "categorized path:" + categorizedDirPath);
            if (!categorizedDir.exists()) {
                categorizedDir.mkdirs();
                Log.i(TAG, "The directory exist? " + categorizedDir.exists());
                Log.i(TAG, "make the directory");
                categorizedFile = new File(categorizedDirPath, sensorDataFile);
            }

            FileOutputStream outStream = new FileOutputStream(categorizedFile);
            excelWorkbook.write(outStream);
            Log.i(TAG, "Writing categorized file: " + categorizedFile);

            Log.w(TAG, "Writing excelFile : " + excelFile);
            Log.w(TAG,"Write success");
        } catch (IOException e) {
            Log.w(TAG, "Error writing : " + excelFile, e);
        } catch (Exception e) {
            Log.w(TAG, "Failed to save data file", e);
        } finally {
            try {
                if (null != accelOutputStream){
                    accelOutputStream.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        Log.i(TAG,"listener stop");
    }

    public void setCatchTime() {
        timeTouch = eventCache.getTimestamp();
    }

    public void setCategorizedDirPath(String path) {
        categorizedDirPath = path;
    }

    public String getCategorizedDirPath() {
        return categorizedDirPath;
    }
}
