package com.sozolab.sumon;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.gson.Gson;
import com.sozolab.sumon.io.esense.esenselib.ESenseConfig;

import java.util.Locale;

public class SettingActivity extends AppCompatActivity implements View.OnClickListener {
    private String TAG = "Setting";
    EditText deviceName;
    EditText sampleRate;
    Spinner accSelector;
    Spinner gyroSelecttor;
    Spinner lpfGyro;
    Spinner lpfAcc;
    Button saveButton;
    Button backButton;
    ActionBar actionBar;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    @SuppressLint("DefaultLocale")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_menu);
        Intent intent = getIntent();
        preferences = getSharedPreferences("eSenseSharedPrefs", Context.MODE_PRIVATE);
        Log.d(TAG, preferences.getString("deviceName", ""));
        editor = preferences.edit();
        actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP);

        deviceName = (EditText) findViewById(R.id.device_id);
        sampleRate = (EditText) findViewById(R.id.sensing_frq);
//        accSelector = (EditText) findViewById(R.id.acc_selector);
//        gyroSelecttor = (EditText) findViewById(R.id.hyro_selector);
        saveButton = (Button) findViewById(R.id.save_button);
        accSelector = (Spinner) findViewById(R.id.acc_g);
        gyroSelecttor = (Spinner) findViewById(R.id.gyro_deg);
        lpfAcc = (Spinner) findViewById(R.id.acc_lpf);
        lpfGyro = (Spinner) findViewById(R.id.gyro_lpf);
        backButton = (Button) findViewById(R.id.back_button);

        ArrayAdapter<ESenseConfig.AccRange> accAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, ESenseConfig.AccRange.values());
        ArrayAdapter<ESenseConfig.AccLPF> lpfaAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, ESenseConfig.AccLPF.values());
        ArrayAdapter<ESenseConfig.GyroLPF> lpfgAdpter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, ESenseConfig.GyroLPF.values());
        ArrayAdapter<ESenseConfig.GyroRange> gyrpAdpter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, ESenseConfig.GyroRange.values());

        setAdpter(accSelector, accAdapter);
        setAdpter(gyroSelecttor, gyrpAdpter);
        setAdpter(lpfAcc, lpfaAdapter);
        setAdpter(lpfGyro, lpfgAdpter);

        String currentJson = preferences.getString("eSenseConfig", "");
        ESenseConfig currentCofig;

        assert currentJson != null;
        if (currentJson.equals("")) {
            currentCofig = new ESenseConfig();
        } else {
            Gson gson = new Gson();
            currentCofig = gson.fromJson(currentJson, ESenseConfig.class);
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setHomeButtonEnabled(true);


        accSelector.setSelection(indexOf(ESenseConfig.AccRange.values(), currentCofig.getAccRange()));
        gyroSelecttor.setSelection(indexOf(ESenseConfig.GyroRange.values(), currentCofig.getGyroRange()));
        lpfAcc.setSelection(indexOf(ESenseConfig.AccLPF.values(), currentCofig.getAccLPF()));
        lpfGyro.setSelection(indexOf(ESenseConfig.GyroLPF.values(), currentCofig.getGyroLPF()));

        deviceName.setText(preferences.getString("deviceName", MainActivity.DEFAULT_NAME));
        sampleRate.setText(String.format("%d", preferences.getInt("samplingRate",MainActivity.DEFAULT_SAMPLING_RATE)));
        saveButton.setOnClickListener(this);
        backButton.setOnClickListener(this);
    }

    private void setAdpter(Spinner spinner, ArrayAdapter adapter) {
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, MainActivity.class);
        switch (v.getId()) {
            case R.id.save_button:

                String deviceName = this.deviceName.getText().toString();
                String sampleRate = this.sampleRate.getText().toString();
                ESenseConfig.GyroLPF gyroLPF = (ESenseConfig.GyroLPF) this.lpfGyro.getSelectedItem();
                ESenseConfig.AccLPF accLPF = (ESenseConfig.AccLPF) this.lpfAcc.getSelectedItem();
                ESenseConfig.AccRange accRange = (ESenseConfig.AccRange) this.accSelector.getSelectedItem();
                ESenseConfig.GyroRange gyroRange = (ESenseConfig.GyroRange) this.gyroSelecttor.getSelectedItem();

                Log.d(TAG, "Onclick()");
                editor.putInt("samplingRate", Integer.parseInt(sampleRate));
                editor.putString("deviceName", deviceName);
                MainActivity.DEFAULT_NAME = deviceName;
                editor.commit();

                Log.d(TAG, preferences.getString("deviceName", ""));
                ESenseConfig newConfig = new ESenseConfig(accRange, gyroRange, accLPF, gyroLPF);

                Gson gson = new Gson();
                String json = gson.toJson(newConfig);
                editor.putString("eSenseConfig", json);
                editor.commit();

                startActivity(intent);
            case R.id.back_button:
                startActivity(intent);

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this,MainActivity.class);
                startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    private <T> int indexOf(T[] arr, T element) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(element)) {
                return i;
            }
        }

        return -1;
    }


}
