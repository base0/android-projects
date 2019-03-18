package com.vac.wasd.myapplication;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    // BluetoothAdapter for scanning => device
    // device.connectGatt
    // bluetoothGatt.discoverServices
    // List<BluetoothGattService> gattServices
    // List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
    private BluetoothAdapter bluetoothAdapter;
    String BT_ADDRESS = "D8:80:39:F7:8E:93";
    int REQUEST_ENABLE_BT = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO : change request code
        // TODO : scan in activity result
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    0);
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not support", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 1 Get the BluetoothAdapter
        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // 2 Enable Bluetooth
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }


        List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder().setDeviceAddress(BT_ADDRESS).build());

        ScanSettings.Builder builderScanSettings = new ScanSettings.Builder();
        builderScanSettings.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
        builderScanSettings.setReportDelay(0);

        BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        bluetoothLeScanner.startScan(filters, builderScanSettings.build(), scanCallback);
        //new Scan().scanLeDevice(true);
    }


    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);


            result.getDevice().connectGatt(MainActivity.this, false, bluetoothGattCallback);
            Log.i("vac", "onScanResult: ");
        }
    };

    BluetoothGatt btgatt;
    BluetoothGattCharacteristic btchar;

    BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.i("vac", "onConnectionStateChange: CONNECTED");
                gatt.discoverServices();
                btgatt = gatt;
            }
        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            Log.i("vac", "onServicesDiscovered: #services " + gatt.getServices().size());
            for (BluetoothGattService s : gatt.getServices()) {
                Log.i("vac", String.format("onServicesDiscovered: service %s", s.getUuid().toString()));
            }
            for (BluetoothGattService s : gatt.getServices()) {
                for (BluetoothGattCharacteristic c : s.getCharacteristics()) {
                    if (c.getUuid().toString().equals("49535343-1e4d-4bd9-ba61-23c647249616")) {
                        if (btchar == null) {
                            btchar = c;


                            // https://stackoverflow.com/questions/27068673/subscribe-to-a-ble-gatt-notification-android
                            gatt.setCharacteristicNotification(c, true);

                            UUID uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
                            BluetoothGattDescriptor descriptor = c.getDescriptor(uuid);
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                            Log.i("vac", "onServicesDiscovered: remembered");
                        }
                    }
                    Log.i("vac", String.format("onServicesDiscovered: %s %s %d", s.getUuid().toString(), c.getUuid().toString(), c.getProperties()));
//                     gatt.readCharacteristic(c);

//                    byte[] a = c.getValue();
//                    Log.i("vac", "onServicesDiscovered: " + Arrays.toString(a));
                    //Log.i("vac", String.format("onServicesDiscovered: %x", d.getPermissions()));
//                    for (BluetoothGattDescriptor d : c.getDescriptors()) {
//                    Log.i("vac", "onServicesDiscovered: " + c.getUuid().toString());
//                    Log.i("vac", String.format("onServicesDiscovered: %x", d.getPermissions()));
//                }
                }
            }

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            //gatt.readCharacteristic(characteristic);
            Log.i("vac", "onCharacteristicWrite: ");
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            Log.i("vac", "onCharacteristicRead: " + characteristic.getUuid().toString() +  Arrays.toString(characteristic.getValue()));

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            Log.i("vac", "onCharacteristicChanged: "  +  Arrays.toString(characteristic.getValue()));
        }
    };

    byte i;
    public void clk(View view) {
        btchar.setValue(new byte[] {i++});
        btgatt.writeCharacteristic(btchar);
    }

    public void read(View view) {
        btgatt.readCharacteristic(btchar);
    }
}
