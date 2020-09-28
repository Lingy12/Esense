package com.sozolab.sumon;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.gson.Gson;
import com.sozolab.sumon.io.esense.esenselib.ESenseConfig;
import com.sozolab.sumon.io.esense.esenselib.ESenseManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {
    public static String DEFAULT_NAME = "eSense-0371";
    public static ESenseConfig DEFAULT_CONFIG = new ESenseConfig();
    public static int DEFAULT_SAMPLING_RATE = 10;
    private ESenseConfig config = DEFAULT_CONFIG;
    private String deviceName = DEFAULT_NAME;
    private int samplingRate = DEFAULT_SAMPLING_RATE;
    private ESenseManager eSenseManager;
    private String TAG = "Esense";
    private String activityName = "Activity";
    private int timeout = 30000;

    private ListView activityListView;
    private Chronometer chronometer;
    private ToggleButton recordButton;

    private TextView connectionTextView;
    private TextView deviceNameTextView;
    private TextView activityTextView;
    private TextView timerShow;
    private EditText newAct;
    private Switch maskSwitch;
    private Spinner positionSelector;
    private Spinner patternSelector;
    private Spinner handSelector;
    private Spinner touchingSelector;
    private ToggleButton timerSwitch;
    private ImageView statusImageView;
    private ProgressBar progressBar;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPrefEditor;
    private CountDownTimer timer;
   // private MediaPlayer mp = MediaPlayer.create(this, R.raw.beep);

    private String categorizedDirPath;
    Calendar currentTime;
    Activity activityObj;
    Intent audioRecordServiceIntent;
    DatabaseHandler databaseHandler;
    SensorListenerManager sensorListenerManager;
    ConnectionListenerManager connectionListenerManager;

    private static final int PERMISSION_REQUEST_CODE = 200;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate()");

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(R.mipmap.esense);

        //Update extract the configuration from preference book
        sharedPreferences = getSharedPreferences("eSenseSharedPrefs", Context.MODE_PRIVATE);
        sharedPrefEditor = sharedPreferences.edit();
        samplingRate = sharedPreferences.getInt("samplingRate", 50);
        deviceName = sharedPreferences.getString("deviceName", "eSense-0371");
        String parsedConfig = sharedPreferences.getString("eSenseConfig", "");

        //Parse the Configuration
        assert parsedConfig != null;
        if (parsedConfig.equals("")) {
            config = new ESenseConfig(); //default configuration
        } else {
            Gson gson = new Gson();
            config = gson.fromJson(parsedConfig, ESenseConfig.class);
            Log.d(TAG, "New configuration applied");
        }

        recordButton = (ToggleButton) findViewById(R.id.recordButton);
        newAct = (EditText) findViewById(R.id.new_activity);
        maskSwitch = (Switch) findViewById(R.id.mask_switch);
        positionSelector = (Spinner) findViewById(R.id.position_selector);
        patternSelector = (Spinner) findViewById(R.id.pattern_selector);
        handSelector = (Spinner) findViewById(R.id.hand_selector);
        timerShow = (TextView) findViewById(R.id.timer_show);
        timerSwitch = (ToggleButton) findViewById(R.id.timer_toggle);
        touchingSelector = (Spinner) findViewById(R.id.touching_switch);

        ArrayAdapter<String> positionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getPositionArray());
        ArrayAdapter<String> patternAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getPatternArray());
        ArrayAdapter<String> handAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getHandArray());
        ArrayAdapter<String> touchingAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, getTouchingArray());

        maskSwitch.setChecked(false);
        setAdpter(positionSelector, positionAdapter);
        setAdpter(patternSelector, patternAdapter);
        setAdpter(handSelector, handAdapter);
        setAdpter(touchingSelector, touchingAdapter);

        positionSelector.setSelection(0);
        handSelector.setSelection(0);
        patternSelector.setSelection(0);
        touchingSelector.setSelection(0);

        positionSelector.setOnItemSelectedListener(this);
        handSelector.setOnItemSelectedListener(this);
        patternSelector.setOnItemSelectedListener(this);
        touchingSelector.setOnItemSelectedListener(this);

        //Set up the timer
        timerShow.setText("Timer on");
        timerSwitch.setChecked(true);
        timerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                timerShow.setText("Timer on");
            } else {
                timerShow.setText("Timer off");
            }
        });

        createCountDownTimer();

        recordButton.setOnClickListener(this);


        statusImageView = (ImageView) findViewById(R.id.statusImage);
        connectionTextView = (TextView) findViewById(R.id.connectionTV);
        deviceNameTextView = (TextView) findViewById(R.id.deviceNameTV);
        activityTextView = (TextView) findViewById(R.id.activityTV);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        chronometer = (Chronometer) findViewById(R.id.chronometer);


        timerShow = (TextView) findViewById(R.id.timer_show);

        databaseHandler = new DatabaseHandler(this);
        activityListView = (ListView) findViewById(R.id.activityListView);

        ArrayList<Activity> activityHistory = databaseHandler.getAllActivities();

        if (activityHistory.size() > 0) {
            activityListView.setAdapter(new ActivityListAdapter(this, activityHistory));
        }

        audioRecordServiceIntent = new Intent(this, AudioRecordService.class);
        sensorListenerManager = new SensorListenerManager(this, config);
        connectionListenerManager = new ConnectionListenerManager(this, sensorListenerManager,
                connectionTextView, deviceNameTextView, statusImageView, progressBar, sharedPrefEditor);
        connectionListenerManager.setSamplingRate(samplingRate);
        eSenseManager = new ESenseManager(deviceName, MainActivity.this.getApplicationContext(), connectionListenerManager);
       // eSenseManager.setAdvertisementAndConnectiontInterval(80,100,80,100);
        sensorListenerManager.setCategorizedDirPath(categorizedDirPath);
        connectEarables();

        if (!checkPermission()) {
            requestPermission();
        } else {
            Log.d(TAG, "Permission already granted..");
        }
    }

    private void disconnect() {
        Log.d(TAG,"disconnect");
        eSenseManager.disconnect();
    }

    public static boolean isESenseDeviceConnected() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()
                && mBluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothHeadset.STATE_CONNECTED;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        this.moveTaskToBack(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.clear_menu:
                // Clear all the existing history
                Toast.makeText(this, "Clear history...", Toast.LENGTH_SHORT).show();
                databaseHandler.clearAll();
                FileHandler.deleteRecursive();
                FileHandler.deleteCategorizedRecursive();
                refreshActivityList();
                return true;
            case R.id.reset_menu:
                // Disconnect the device to reset
                Toast.makeText(this, "Reset connection..", Toast.LENGTH_SHORT).show();
                disconnect();
                return true;
            case R.id.setting_menu:
                // Go to setting
                disconnect();
                showSetting();
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.connectButton:
                progressBar.setVisibility(View.VISIBLE);
                connectEarables();
                break;
            case R.id.timer_toggle:
                timerSwitch.toggle();
            case R.id.new_act:
                showActivityCreation();
                break;
            case R.id.recordButton:
                if (!isESenseDeviceConnected()) {
                    showNotConnectedAlertMessage();
                    Log.d(TAG, "Not connected try to start");
                    break;
                }


//                if (activityName.equals("Activity")) {
//                    recordButton.setChecked(false);
//                    showAlertMessage();
//                }
                if (recordButton.isChecked()) {
                    if (timerSwitch.isChecked()) {
                        timer.start();
                    } else {
                        startCollection();
                    }

                    showCurrentSetting();
                } else {
                    stopCollection();
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        progressBar.setVisibility(View.GONE);

        boolean isConnected = isESenseDeviceConnected();

        if (!isConnected) {
            sharedPrefEditor.putString("status", "disconnected");
            sharedPrefEditor.commit();

            activityName = "Activity";
            sharedPrefEditor.putString("activityName", activityName);
            sharedPrefEditor.commit();

            Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
        }

        String isChecked = sharedPreferences.getString("checked", null);
        String status = sharedPreferences.getString("status", null);
        String activity = sharedPreferences.getString("activityName", null);

        if (activity != null) {
            activityName = activity;
            setActivityName();
        }

        if (status == null) {
            connectionTextView.setText("Disconnected");
            deviceNameTextView.setText(deviceName);
            statusImageView.setImageResource(R.drawable.disconnected);
            Toast.makeText(this, "Press connect to connect the device", Toast.LENGTH_SHORT).show();
        } else if (status.equals("connected")) {
            connectionTextView.setText("Connected");
            deviceNameTextView.setText(deviceName);
            statusImageView.setImageResource(R.drawable.connected);
        } else if (status.equals("disconnected")) {
            connectionTextView.setText("Disconnected");
            deviceNameTextView.setText(deviceName);
            statusImageView.setImageResource(R.drawable.disconnected);
        }

        if (isChecked == null) {
            recordButton.setChecked(false);
            recordButton.setBackgroundResource(R.drawable.start);
        } else if (isChecked.equals("on")) {
            recordButton.setChecked(true);
            recordButton.setBackgroundResource(R.drawable.stop);
        } else if (isChecked.equals("off")) {
            recordButton.setChecked(false);
            recordButton.setBackgroundResource(R.drawable.start);
        }

        Log.d(TAG, "onResume()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
    }

    //set the activity name given the spinner
    private void setActivityName() {
        //TODO: read the value from spinner
        if (touchingSelector.getSelectedItem().toString().equals("Touching")) {
            StringBuilder builder = new StringBuilder();
            builder.append(maskSwitch.isActivated() ? "mask_" : "no_mask_");
            builder.append(positionSelector.getSelectedItem().toString() + "_");
            builder.append(patternSelector.getSelectedItem().toString() + "_");
            builder.append(handSelector.getSelectedItem().toString());
            activityName = builder.toString();
            sharedPrefEditor.putString("activityName", activityName);
            sharedPrefEditor.commit();
        } else {
            activityName = touchingSelector.getSelectedItem().toString();
            sharedPrefEditor.putString("activityName", activityName);
            sharedPrefEditor.commit();
        }
    }

    //set the output path for categorized data
    private void setCategorizedDirPath() {
        StringBuilder builder = new StringBuilder();

        if (touchingSelector.getSelectedItem().toString().equals("Touching")) {
            builder.append(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "ESenseCategorized" + File.separator);
            builder.append(maskSwitch.isActivated() ? "mask_" : "no_mask_");
            builder.append(getConfigString() + File.separator);
            builder.append(positionSelector.getSelectedItem().toString() + File.separator);
            builder.append(patternSelector.getSelectedItem().toString() + File.separator);
            builder.append(handSelector.getSelectedItem().toString());
            categorizedDirPath = builder.toString();
            sensorListenerManager.setCategorizedDirPath(categorizedDirPath);
        } else {
            builder.append(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "ESenseCategorized" + File.separator);
            builder.append("no_touching" + File.separator);
            builder.append(touchingSelector.getSelectedItem().toString());
            categorizedDirPath = builder.toString();
            sensorListenerManager.setCategorizedDirPath(categorizedDirPath);
        }
    }

    public void connectEarables() {
        eSenseManager.connect(timeout);
    }

    public void startDataCollection(String activity) {
        sensorListenerManager.startDataCollection(activity);
    }

    public void stopDataCollection() {
        sensorListenerManager.stopDataCollection();
    }

    private boolean checkPermission() {
        int recordResult = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        int locationResult = ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION);
        int writeResult = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);

        return locationResult == PackageManager.PERMISSION_GRANTED &&
                writeResult == PackageManager.PERMISSION_GRANTED && recordResult == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {

        ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION,
                WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, PERMISSION_REQUEST_CODE);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {

                    boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean recordAccepted = grantResults[2] == PackageManager.PERMISSION_GRANTED;

                    if (locationAccepted && storageAccepted && recordAccepted) {
                        Log.d(TAG, "Permission granted");
                    } else {
                        Log.d(TAG, "Permission denied");

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION)) {
                                showMessageOKCancel("You need to allow access to all permissions",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                    requestPermissions(new String[]{ACCESS_FINE_LOCATION,
                                                            WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
                                                }
                                            }
                                        });
                                return;
                            }
                        }
                    }
                }
                break;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void showSetting() {
        Intent intent = new Intent(this, SettingActivity.class);
        startActivity(intent);
    }

    public void showAlertMessage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Please select an activityName !")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //do things
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void createCountDownTimer() {
        timer = new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerShow.setText(String.valueOf(millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                timerShow.setText("Start!");
                startCollection();
            }
        };
    }

    public void startCollection() {
        activityObj = new Activity();

        currentTime = Calendar.getInstance();
        int hour = currentTime.get(Calendar.HOUR_OF_DAY);
        int minute = currentTime.get(Calendar.MINUTE);
        int second = currentTime.get(Calendar.SECOND);

        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();

        if (activityObj != null) {
            String startTime = hour + " : " + minute + " : " + second;
            activityObj.setActivityName(activityName);
            activityObj.setStartTime(startTime);
        }

        sharedPrefEditor.putString("checked", "on");
        sharedPrefEditor.commit();
        recordButton.setBackgroundResource(R.drawable.stop);

        setActivityName();
        audioRecordServiceIntent.putExtra("activity", activityName);
        audioRecordServiceIntent.putExtra("categorizedpath", sensorListenerManager.getCategorizedDirPath());
        startDataCollection(activityName);
       // Toast.makeText( this,String.format("Current activity %s",activityName),Toast.LENGTH_LONG).show();
        startService(audioRecordServiceIntent);
        Log.d(TAG, "Start Collection!");
    }

    public void stopCollection() {
        currentTime = Calendar.getInstance();
        int hour = currentTime.get(Calendar.HOUR_OF_DAY);
        int minute = currentTime.get(Calendar.MINUTE);
        int second = currentTime.get(Calendar.SECOND);

        chronometer.stop();

        if (activityObj != null) {
            String stopTime = hour + " : " + minute + " : " + second;
            String duration = chronometer.getText().toString();
            activityObj.setStopTime(stopTime);
            activityObj.setDuration(duration);
        }

        sharedPrefEditor.putString("checked", "off");
        sharedPrefEditor.commit();

        recordButton.setBackgroundResource(R.drawable.start);

        stopDataCollection();
        stopService(audioRecordServiceIntent);

        if (activityObj != null) {
            databaseHandler.addActivity(activityObj);
        }

        createCountDownTimer();

        refreshActivityList();
        Log.d(TAG, "Stop Collection!");
        activityObj = null;
    }

    public void refreshActivityList() {
        if (databaseHandler != null) {
            ArrayList<Activity> activityHistory = databaseHandler.getAllActivities();
            activityListView.setAdapter(new ActivityListAdapter(this, activityHistory));

            for (Activity activity : activityHistory) {
                String activityLog = "Activity : " + activity.getActivityName() + " , Start Time : " + activity.getStartTime()
                        + " , Stop Time : " + activity.getStopTime() + " , Duration : " + activity.getDuration();
                Log.d(TAG, activityLog);
            }
        }
    }

    public void showNotConnectedAlertMessage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Device not connected !")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void showCurrentSetting() {

        if (eSenseManager.getSensorConfig()) {
            String builder = String.format("Current config: AccLPF: %s, AccRange: %s, GyroLPF: %s, GyroRange: %s", config.getAccLPF(), config.getAccRange(), config.getGyroLPF(), config.getGyroRange()) +
                    "Sampling rate: " + samplingRate;
         //   Toast.makeText(this, builder,Toast.LENGTH_LONG).show();
        }


    }

    private String[] getPositionArray() {
        return new String[]{"chin", "left_cheek", "left_ear", "left_eye", "left_forehead", "mouth",
                "nose", "right_cheek", "right_ear", "right_forehead"};
    }

    private String[] getPatternArray() {
        return new String[] {"scratch", "rub", "support"};
    }

    private String[] getHandArray() {
        return new String[] {"left", "right"};
    }

    private String[] getTouchingArray() {
        return new String[]{"Touching", "sit_still","look_left", "look_right", "clockwise_head_rotation",
        "anti_clockwise_head_rotation", "look_up", "look_down", "shake_head", "nod"};
    }

    //Get the config string to be used as activity name
    @SuppressLint("DefaultLocale")
    private String getConfigString() {
        return String.format("%s_%s_%s_%s_%d",config.getAccLPF(), config.getAccRange(), config.getGyroLPF(), config.getGyroRange(),samplingRate );
    }

    // Show the activity creation window
    private void showActivityCreation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View inflator = LayoutInflater.from(this).inflate(R.layout.add_act, null);

        final EditText newAct = (EditText) inflator.findViewById(R.id.new_activity);

        builder.setView(inflator)
                .setPositiveButton("Save", (dialog, which) -> {
                    activityName = newAct.getText().toString();
                    sharedPrefEditor.putString("activityName", activityName);
                    sharedPrefEditor.commit();
                    setActivityName();
                    dialog.cancel();
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        AlertDialog newActDialog = builder.create();
        newActDialog.show();
    }

    //set up the spinner with adapter
    private void setAdpter(Spinner spinner, ArrayAdapter adapter) {
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        setActivityName();
        setCategorizedDirPath();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}
