package com.sozolab.sumon;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.gson.Gson;
import com.sozolab.sumon.io.esense.esenselib.ESenseConfig;
import com.sozolab.sumon.io.esense.esenselib.ESenseManager;

import org.apache.poi.ss.formula.functions.Count;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
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

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private Button connectButton;
    private Button headShakeButton;
    private Button speakingButton;
    private Button noddingButton;
    private Button eatingButton;
    private Button walkButton;
    private Button stayButton;
    private Button speakWalkButton;
    private Button addButton;
    private ListView activityListView;
    private Chronometer chronometer;
    private ToggleButton recordButton;

    private TextView connectionTextView;
    private TextView deviceNameTextView;
    private TextView activityTextView;
    private TextView timerShow;
    private EditText newAct;
    private ToggleButton timerSwitch;
    private ImageView statusImageView;
    private ProgressBar progressBar;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPrefEditor;
    private NavigationView navigationView;
    private CountDownTimer timer;

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
        deviceName = sharedPreferences.getString("deviceName", "eSense-0371");
        samplingRate = sharedPreferences.getInt("samplingRate", 10);
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
        connectButton = (Button) findViewById(R.id.connectButton);
        headShakeButton = (Button) findViewById(R.id.headShakeButton);
        speakingButton = (Button) findViewById(R.id.speakingButton);
        noddingButton = (Button) findViewById(R.id.noddingButton);
        eatingButton = (Button) findViewById(R.id.eatingButton);
        walkButton = (Button) findViewById(R.id.walkButton);
        stayButton = (Button) findViewById(R.id.stayButton);
        speakWalkButton = (Button) findViewById(R.id.speak_walk_button);
        addButton = (Button) findViewById(R.id.new_act);
        newAct = (EditText) findViewById(R.id.new_activity);

        timerShow = (TextView) findViewById(R.id.timer_show);
        timerSwitch = (ToggleButton) findViewById(R.id.timer_toggle);


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
        connectButton.setOnClickListener(this);
        headShakeButton.setOnClickListener(this);
        speakingButton.setOnClickListener(this);
        noddingButton.setOnClickListener(this);
        eatingButton.setOnClickListener(this);
        walkButton.setOnClickListener(this);
        stayButton.setOnClickListener(this);
        speakWalkButton.setOnClickListener(this);
        addButton.setOnClickListener(this);


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
        eSenseManager = new ESenseManager(deviceName, MainActivity.this.getApplicationContext(), connectionListenerManager);

        eSenseManager.registerSensorListener(sensorListenerManager, samplingRate);

        connectEarables();
        //Toast.makeText(this,String.format("Sampling rate is %dHz",samplingRate), Toast.LENGTH_SHORT).show();

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
                Toast.makeText(this, "Clear history...", Toast.LENGTH_SHORT).show();
                databaseHandler.clearAll();
                FileHandler.deleteRecursive();
                refreshActivityList();
                return true;
            case R.id.reset_menu:
                Toast.makeText(this, "Reset connection..", Toast.LENGTH_SHORT).show();
                disconnect();
                return true;
            case R.id.setting_menu:
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

            case R.id.headShakeButton:
                activityName = "Head Shake";
                sharedPrefEditor.putString("activityName", activityName);
                sharedPrefEditor.commit();
                setActivityName();
                break;

            case R.id.speakingButton:
                activityName = "Speaking";
                sharedPrefEditor.putString("activityName", activityName);
                sharedPrefEditor.commit();
                setActivityName();
                break;

            case R.id.noddingButton:
                activityName = "Nodding";
                sharedPrefEditor.putString("activityName", activityName);
                sharedPrefEditor.commit();
                setActivityName();
                break;

            case R.id.eatingButton:
                activityName = "Eating";
                sharedPrefEditor.putString("activityName", activityName);
                sharedPrefEditor.commit();
                setActivityName();
                break;

            case R.id.walkButton:
                activityName = "Walking";
                sharedPrefEditor.putString("activityName", activityName);
                sharedPrefEditor.commit();
                setActivityName();
                break;

            case R.id.stayButton:
                activityName = "Staying";
                sharedPrefEditor.putString("activityName", activityName);
                sharedPrefEditor.commit();
                setActivityName();
                break;

            case R.id.speak_walk_button:
                activityName = "Speak and Walk";
                sharedPrefEditor.putString("activityName", activityName);
                sharedPrefEditor.commit();
                setActivityName();
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


                if (activityName.equals("Activity")) {
                    recordButton.setChecked(false);
                    showAlertMessage();
                }

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

    public void setActivityName() {
        activityTextView.setText(activityName);
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

        audioRecordServiceIntent.putExtra("activity", activityName);

        startDataCollection(activityName);
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
            Toast.makeText(this, builder,Toast.LENGTH_LONG).show();
        }


    }

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
}
