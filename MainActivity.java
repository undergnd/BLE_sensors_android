package com.example.bleapp;

import static java.util.Arrays.copyOfRange;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.widget.TextView;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Formatter;

import java.util.ArrayList;
import java.util.List;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import android.os.Handler;

public class MainActivity extends AppCompatActivity {

    int sensors_number = 16;

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int REQUEST_ENABLE_BLUETOOTH = 2;
    private BluetoothAdapter btAdapter;
    private BluetoothLeScanner bleScanner;
    private TextView deviceText;

    static byte sensor_number = 0;
    static short[] sensor_rssi = new short[16];
    static short[] sensor_rssi_prev = new short[16];
    static int[] sensor_alarm = new int[16];
    static int[] sensor_alarm_prev = new int[16];
    static float[] sensor_temp = new float[16];
    static float[] sensor_temp_prev = new float[16];
    static float[] sensor_hum = new float[16];
    static float[] sensor_hum_prev = new float[16];
    static float[] sensor_bat = new float[16];
    static float[] sensor_bat_prev = new float[16];
    Handler handler = new Handler();


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BLUETOOTH && resultCode == Activity.RESULT_OK) {
            startBleScan();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getBluetoothAdapter();
        }
        else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
        deviceText = findViewById(R.id.deviceText);
        deviceText.setMovementMethod(new ScrollingMovementMethod());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getBluetoothAdapter();
            }
        }
    }

    private void getBluetoothAdapter() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter.isEnabled()) {
            startBleScan();
        }
        else {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BLUETOOTH);
        }
    }

    private void startBleScan() {
        bleScanner = btAdapter.getBluetoothLeScanner();
        List<ScanFilter> scanFilterList = new ArrayList<>();
        //scanFilterList.add(new ScanFilter.Builder().setDeviceName("AVR-BLE_1B19").build());
        scanFilterList.add(new ScanFilter.Builder().setDeviceAddress("02:80:E1:88:77:00").build());
         //scanFilterList.add(new ScanFilter.Builder().setDeviceAddress("77:77:88:E1:80:02").build());

        // Start the initial runnable task by posting through the handler
        handler.post(runTask);
        ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        bleScanner.startScan(scanFilterList, scanSettings, scanCallback);
        //bleScanner.startScan(scanCallback);
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            byte[] scanRecord = result.getScanRecord().getBytes();
            byte[] sensorBytes = copyOfRange(scanRecord, 0, (scanRecord[0]+1)); // raw bytes of advertising packets

            ByteBuffer buffer = ByteBuffer.wrap(sensorBytes);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            sensor_number = buffer.get(1);
            int k = sensor_number;

            if((k >=0 ) && (k < 16 ))
            {
                sensor_rssi[sensor_number] = buffer.get(3);
                sensor_alarm[sensor_number] = buffer.get(4);
                sensor_temp[sensor_number] = buffer.getFloat(14);
                sensor_hum[sensor_number] = buffer.getFloat(18);
                sensor_bat[sensor_number] = buffer.getFloat(22);
            }
        }
    };
    private SpannableString string_preparation(@NonNull String sensor_data) {
        SpannableString spannableString = new SpannableString(sensor_data);
// Creating the spans to style the string
        ForegroundColorSpan foregroundColorSpanGreen = new ForegroundColorSpan(Color.BLUE);
     //   StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);

// Setting the spans on spannable string
        spannableString.setSpan(foregroundColorSpanGreen, 0,  sensor_data.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//        spannableString.setSpan(boldSpan, 0, sensor_data.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }
    private Runnable runTask = new Runnable() {
        @Override
        public void run() {
            // Execute tasks on main thread

/****************************************************************************************************
 *
 */
/****************************************************************************************************/
            String [] s_sensor_results = {
                    (String.format("%02d      ", sensor_number)),
                    (String.format("%d           ", sensor_rssi[sensor_number])),
                    (String.format("%03d          ", sensor_alarm[sensor_number])),
                    (String.format("%.02f           ", sensor_temp[sensor_number])),
                    (String.format("%.02f        ", sensor_hum[sensor_number])),
                    (String.format("%.02f        ", sensor_bat[sensor_number] / 1000)),
            };

            deviceText.setTextKeepState(
                            "SN: sensor's number"
                            + "\r\n"
                            + "RSSI: received signal strength indication"
                            + "\r\n"
                            + "ACC_al:  accelerometer alarm"
                            + "\r\n"
                            + "T: temperature"
                            + "\r\n"
                            + "H: humidity"
                            + "\r\n"
                            + "Vbat: battery voltage"
                            +"\r\n"
                            + "Blue: value has changed"
                            + "\r\n"
                            + "\r\n");

            deviceText.append(
                    "SN:    " + "RSSI:      " + "ACC_al:       " + "T:                  " + "H:           " + "Vbat:  "  + "\r\n");
            for(int l = 0; l < 16; l++) {
                s_sensor_results[0] = (String.format("%02d      ", l));
                s_sensor_results[1] = (String.format("%d           ", sensor_rssi[l]));
                s_sensor_results[2] = (String.format("%03d          ", sensor_alarm[l]));
                s_sensor_results[3] = (String.format("%.02f           ", sensor_temp[l]));
                s_sensor_results[4] = (String.format("%.02f        ", sensor_hum[l]));
                s_sensor_results[5] = (String.format("%.02f        ", sensor_bat[l] / 1000));

                for (int g = 0; g < 6; g++) {
                    switch (g)
                    {
                        case 0:
                            deviceText.append(s_sensor_results[g]);
                          break;


                        case 1:
                            if(sensor_rssi[l] != sensor_rssi_prev[l])
                            {
                                deviceText.append(string_preparation(s_sensor_results[g]));
                            }
                            else
                            {
                                deviceText.append(s_sensor_results[g]);
                            }
                            break;


                        case 2:
                            if(sensor_alarm[l] != sensor_alarm_prev[l])
                            {
                                deviceText.append(string_preparation(s_sensor_results[g]));
                            }
                            else
                            {
                                deviceText.append(s_sensor_results[g]);
                            }
                            break;

                        case 3:
                            if(sensor_temp[l] != sensor_temp_prev[l])
                            {
                                deviceText.append(string_preparation(s_sensor_results[g]));
                            }
                            else
                            {
                                deviceText.append(s_sensor_results[g]);
                            }
                            break;

                        case 4:
                            if(sensor_hum[l] != sensor_hum_prev[l])
                            {
                                deviceText.append(string_preparation(s_sensor_results[g]));
                            }
                            else
                            {
                                deviceText.append(s_sensor_results[g]);
                            }
                            break;

                        case 5:
                            if(sensor_bat[l] != sensor_bat_prev[l])
                            {
                                deviceText.append(string_preparation(s_sensor_results[g]));
                            }
                            else
                            {
                                deviceText.append(s_sensor_results[g]);
                            }
                            break;
                        default:
                            break;

                    }

                }
                deviceText.append("\r\n");

            }
            System.arraycopy(sensor_rssi, 0, sensor_rssi_prev, 0, sensors_number);
            System.arraycopy(sensor_alarm, 0, sensor_alarm_prev, 0, sensors_number);
            System.arraycopy(sensor_temp, 0, sensor_temp_prev, 0, sensors_number);
            System.arraycopy(sensor_hum, 0, sensor_hum_prev, 0, sensors_number);
            System.arraycopy(sensor_bat, 0, sensor_bat_prev, 0, sensors_number);
/****************************************************************************************************/

            // Repeat this task again another 2 seconds
            handler.postDelayed(this, 5000);
        }
    };
}

