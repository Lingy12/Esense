package com.sozolab.sumon;

import android.content.Intent;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.sozolab.sumon.io.esense.esenselib.ESenseConfig;

public class SettingActivity extends AppCompatActivity implements View.OnClickListener {
    private String TAG = "Setting";
    EditText deviceName;
    EditText sampleRate;
    Spinner accSelector;
    Spinner gyroSelecttor;
    Spinner lpfGyro;
    Spinner lpfAcc;
    Button saveButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_menu);
        Intent intent = getIntent();

        deviceName = (EditText) findViewById(R.id.device_id);
        sampleRate = (EditText) findViewById(R.id.sensing_frq);
//        accSelector = (EditText) findViewById(R.id.acc_selector);
//        gyroSelecttor = (EditText) findViewById(R.id.hyro_selector);
        saveButton = (Button) findViewById(R.id.save_button);
        accSelector = (Spinner) findViewById(R.id.acc_g);
        gyroSelecttor = (Spinner) findViewById(R.id.gyro_deg);
        lpfAcc = (Spinner) findViewById(R.id.acc_lpf);
        lpfGyro = (Spinner) findViewById(R.id.gyro_lpf);

        ArrayAdapter accAdapter = new ArrayAdapter(this,android.R.layout.simple_spinner_item,ESenseConfig.AccRange.values());
        ArrayAdapter lpfaAdapter = new ArrayAdapter(this,android.R.layout.simple_spinner_item,ESenseConfig.AccLPF.values());
        ArrayAdapter lpfgAdpter = new ArrayAdapter(this,android.R.layout.simple_spinner_item,ESenseConfig.GyroLPF.values());
        ArrayAdapter gyrpAdpter = new ArrayAdapter(this,android.R.layout.simple_spinner_item,ESenseConfig.GyroRange.values());

        setAdpter(accSelector,accAdapter);
        setAdpter(gyroSelecttor,gyrpAdpter);
        setAdpter(lpfAcc,lpfaAdapter);
        setAdpter(lpfGyro,lpfgAdpter);

        deviceName.setText(MainActivity.DEFAULT_NAME);
        sampleRate.setText("5");
        saveButton.setOnClickListener(this);
//        accSelector.setSelection(0);
//        gyroSelecttor.setSelection(0);
    }

    private void setAdpter(Spinner spinner, ArrayAdapter adapter) {
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this,MainActivity.class);
        String deviceName = this.deviceName.getText().toString();
        String sampleRate = this.sampleRate.getText().toString();
        ESenseConfig.GyroLPF gyroLPF = (ESenseConfig.GyroLPF) this.lpfGyro.getSelectedItem();
        ESenseConfig.AccLPF accLPF = (ESenseConfig.AccLPF) this.lpfAcc.getSelectedItem();
        ESenseConfig.AccRange accRange = (ESenseConfig.AccRange) this.accSelector.getSelectedItem();
        ESenseConfig.GyroRange gyroRange = (ESenseConfig.GyroRange) this.gyroSelecttor.getSelectedItem();

        Log.d(TAG,"Onclick()");
        MainActivity.deviceName = deviceName;

        MainActivity.config = new ESenseConfig(accRange,gyroRange,accLPF,gyroLPF);
        MainActivity.samplingRate = Integer.parseInt(sampleRate);

        startActivity(intent);
    }
}
