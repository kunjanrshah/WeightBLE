package com.krs.demo;

import static com.krs.demo.Constants.bluetoothLeScanner;
import static com.krs.demo.Constants.mBluetoothAdapter;
import static com.krs.demo.Constants.mGoogleApiClient;
import static com.krs.demo.Constants.mLocationRequest;
import static com.krs.demo.Constants.setupBluetooth;
import static com.krs.demo.MainActivity.REQUEST_LOCATION;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.ArrayList;
import java.util.List;

public class WeightListActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "WeightListActivity";
    ArrayList<BluetoothDevice> WeightModelArrayList;
    RecyclerView deviceRV;
    WeightAdapter WeightAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weight_list);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);

        Constants.checkPermissions(this);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Your devices that don't support BLE", Toast.LENGTH_LONG).show();
            finish();
        }

        MemoryAllocation();

        WeightAdapter = new WeightAdapter(this, WeightModelArrayList);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);

        deviceRV.setLayoutManager(linearLayoutManager);
        deviceRV.setAdapter(WeightAdapter);
        startScanning(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void MemoryAllocation() {
        WeightModelArrayList = new ArrayList<>();
        deviceRV = findViewById(R.id.idRVDevice);
        mSwipeRefreshLayout = findViewById(R.id.container);

        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            //startScanning(false);
            WeightModelArrayList.clear();
            WeightAdapter.notifyDataSetChanged();
            startScanning(true);
        });

        Constants.setupBluetooth(this);
    }

    public boolean containsName(final List<BluetoothDevice> list, final String name) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return list.stream().map(BluetoothDevice::getName).anyMatch(name::equals);
        }
        return false;
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice bluetoothDevice = result.getDevice();
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(WeightListActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
            Log.d(TAG, "name: " + bluetoothDevice.getName() + " Addr: " + bluetoothDevice.getAddress());
            if (WeightModelArrayList != null && bluetoothDevice.getName() != null) {
                if (!containsName(WeightModelArrayList, bluetoothDevice.getName())) {
                    WeightModelArrayList.add(bluetoothDevice);
                    WeightAdapter.notifyDataSetChanged();
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d(TAG, "Scanning Failed " + errorCode);
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void startScanning(final boolean enable) {

        Handler mHandler = new Handler();
        if (enable) {
            List<ScanFilter> scanFilters = new ArrayList<>();
            final ScanSettings settings = new ScanSettings.Builder().build();
            //  ScanFilter scanFilter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(SampleGattAttributes.UUID_BATTERY_LEVEL_UUID)).build();
            // scanFilters.add(scanFilter);
            mHandler.postDelayed(() -> {
                mSwipeRefreshLayout.clearAnimation();
                mSwipeRefreshLayout.clearFocus();
                mSwipeRefreshLayout.setRefreshing(false);
                if (bluetoothLeScanner != null) {
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                    }
                    bluetoothLeScanner.stopScan(scanCallback);
                }
            }, Constants.SCAN_PERIOD);

            setupBluetooth(this);

            if (bluetoothLeScanner != null) {
                bluetoothLeScanner.startScan(scanFilters, settings, scanCallback);
            }
        } else {
            mHandler.post(() -> {
                if (bluetoothLeScanner != null) {
                    bluetoothLeScanner.stopScan(scanCallback);
                }
            });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(30 * 1000);
        mLocationRequest.setFastestInterval(5 * 1000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);

        Constants.result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

        Constants.result.setResultCallback(result -> {
            final Status status = result.getStatus();
            //final LocationSettingsStates state = result.getLocationSettingsStates();
            switch (status.getStatusCode()) {
                case LocationSettingsStatusCodes.SUCCESS:
                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                    break;
                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                    try {
                        status.startResolutionForResult(WeightListActivity.this, REQUEST_LOCATION);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("onActivityResult()", Integer.toString(resultCode));

        //final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        if (requestCode == REQUEST_LOCATION) {
            switch (resultCode) {
                case Activity.RESULT_OK: {
                    // All required changes were successfully made
                    startScanning(true);
                    Toast.makeText(WeightListActivity.this, "Location enabled by user!", Toast.LENGTH_LONG).show();
                    break;
                }
                case Activity.RESULT_CANCELED: {
                    // The user was asked to change settings, but chose not to
                    Toast.makeText(WeightListActivity.this, "Location not enabled, user cancelled.", Toast.LENGTH_LONG).show();
                    break;
                }
                default: {
                    break;
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning(true);
            } else {
                Toast.makeText(WeightListActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onResume() {
        super.onResume();
        Constants.checkPermissions(this);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        if (!mBluetoothAdapter.enable()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, Constants.REQUEST_BLUETOOTH_ENABLE_CODE);
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(LocationServices.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
        mGoogleApiClient.connect();

        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(mReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        try{
            unregisterReceiver(mReceiver);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.S)
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                    Constants.alert(WeightListActivity.this);
                }
            }
            if (LocationManager.PROVIDERS_CHANGED_ACTION.equals(action)) {
                mGoogleApiClient = new GoogleApiClient.Builder(WeightListActivity.this).addApi(LocationServices.API).addConnectionCallbacks(WeightListActivity.this).addOnConnectionFailedListener(WeightListActivity.this).build();
                mGoogleApiClient.connect();
            }
        }
    };
}
