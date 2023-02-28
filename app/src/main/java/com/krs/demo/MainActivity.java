package com.krs.demo;

import static com.krs.demo.Constants.bluetoothLeScanner;
import static com.krs.demo.Constants.mBluetoothAdapter;
import static com.krs.demo.Constants.mGoogleApiClient;
import static com.krs.demo.Constants.mLocationRequest;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import org.json.JSONObject;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    final static int REQUEST_LOCATION = 199;
    private static final String TAG = "MainActivity";
    public static TextView txtSrNo = null, txt_title1 = null, txt_title2 = null, txt_title3 = null, txt_title4 = null, txt_title5 = null, txt_title6 = null, txt_title7 = null;
    private static final DecimalFormat df2 = new DecimalFormat(".##");

    // private BluetoothDevice bluetoothDevice;
    private Button btnScan, btnTare, btnMode, btnInc, btnShift;
    private Button btncount;
    private int count = 0, isShow = 0;
    private SharedPreferences mSharedPreference;

    private EditText edt_net_wt = null, edtLotno = null, edtBaleno = null, edtMaterial = null, edt_tare_wt = null;
    private TextView txt_gross_wt = null;
    private TextClock textClock = null;
    private BluetoothLEService mBluetoothLEService;
    private SharedPreferences.Editor mEditor;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    String dev_address = "", dev_name = "";

    public final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.S)
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                    Constants.alert(MainActivity.this);
                }
            }
            if (LocationManager.PROVIDERS_CHANGED_ACTION.equals(action)) {
                mGoogleApiClient = new GoogleApiClient.Builder(MainActivity.this).addApi(LocationServices.API).addConnectionCallbacks(MainActivity.this).addOnConnectionFailedListener(MainActivity.this).build();
                mGoogleApiClient.connect();
            }
        }
    };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLEService = ((BluetoothLEService.LocalBinder) service).getService();
            if (!mBluetoothLEService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            btnScan.setText("Connecting device...");
            btnTare.setEnabled(true);
            btnMode.setEnabled(true);
            btnInc.setEnabled(true);
            btnShift.setEnabled(true);

            mBluetoothLEService.connect(dev_address);
            startScanning(false);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLEService = null;
            btnTare.setEnabled(false);
            btnMode.setEnabled(false);
            btnInc.setEnabled(false);
            btnShift.setEnabled(false);
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLEService.ACTION_GATT_CONNECTED.equals(action)) {
                updateConnectionState(getString(R.string.connected));
                invalidateOptionsMenu();
            } else if (BluetoothLEService.ACTION_GATT_DISCONNECTED.equals(action)) {
                updateConnectionState(getString(R.string.scan));
            } else if (BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                displayGattServices(mBluetoothLEService.getSupportedGattServices());
            } else if (BluetoothLEService.ACTION_DATA_AVAILABLE.equals(action)) {
                byte[] dataInput = mNotifyCharacteristic.getValue();
                displayData(dataInput);
            }
        }
    };


    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

//                bluetoothDevice = result.getDevice();
//                Intent gattServiceIntent = new Intent(MainActivity.this, BluetoothLEService.class);
//                bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
//                Log.d(TAG,"address: "+bluetoothDevice.getAddress());
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

    private static IntentFilter GattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLEService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void updateConnectionState(final String status) {
        runOnUiThread(() -> btnScan.setText(status));
    }

    private void displayData(byte[] data) {
        try {
            if (data != null) {
                String output = "";
                for (int i = 0; i < data.length; i++) {
                    output = output + (char) data[i];
                }
                if (count == 0) {
                    txt_gross_wt.setText(output);
                } else if (count == 1) {
                    dotAtLastEnd(output);
                } else if (count == 2) {
                    dotAtBeforeLastEnd(output);
                } else if (count == 3) {
                    dotAtEnd_3(output);
                }
                Double net = 0.0d;
                if (!edt_tare_wt.getText().toString().isEmpty() && !txt_gross_wt.getText().toString().isEmpty()) {
                    net = Double.parseDouble(txt_gross_wt.getText().toString()) - Double.parseDouble(edt_tare_wt.getText().toString());
                }
                edt_net_wt.setText("" + df2.format(net));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void dotAtLastEnd(String output) {
        try {
            output = output.replace(".", "");
            output = output.replace("\n\r", "");
            boolean flag = false;
            if (output.contains("-")) {
                flag = true;
                output = output.replace("-", "");
            }
            output = output.trim();
            int len = output.length();
            if (len > 4) {
                if (len == 5) {
                    output = output.substring(0, 4) + "." + output.substring(4);
                } else if (len == 6) {
                    output = output.substring(0, 5) + "." + output.substring(5);
                }

                if (output.substring(0, 3).equalsIgnoreCase("000")) {
                    output = output.substring(3);
                } else if (output.substring(0, 2).equalsIgnoreCase("00")) {
                    output = output.substring(2);
                } else if (output.substring(0, 1).equalsIgnoreCase("0")) {
                    output = output.substring(1);
                }
                if (flag) {
                    output = "-".concat(output);
                }
                txt_gross_wt.setText(output);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void dotAtBeforeLastEnd(String output1) {
        try {
            output1 = output1.replace(".", "");
            boolean flag = false;
            if (output1.contains("-")) {
                flag = true;
                output1 = output1.replace("-", "");
            }
            output1 = output1.replace("\n\r", "");
            output1 = output1.trim();
            int len = output1.length();
            if (len > 4) {
                if (len == 5) {
                    output1 = output1.substring(0, 3) + "." + output1.substring(3);
                } else if (len == 6) {
                    output1 = output1.substring(0, 4) + "." + output1.substring(4);
                }

                if (output1.substring(0, 2).equalsIgnoreCase("00")) {
                    output1 = output1.substring(2);
                } else if (output1.substring(0, 1).equalsIgnoreCase("0")) {
                    output1 = output1.substring(1);
                }
                if (flag) {
                    output1 = "-".concat(output1);
                }
                txt_gross_wt.setText("" + output1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void dotAtEnd_3(String output1) {
        try {
            output1 = output1.replace(".", "");
            boolean flag = false;
            if (output1.contains("-")) {
                flag = true;
                output1 = output1.replace("-", "");
            }
            output1 = output1.replace("\n\r", "");
            output1 = output1.trim();
            int len = output1.length();
            if (len > 4) {
                if (len == 5) {
                    output1 = output1.substring(0, 2) + "." + output1.substring(2);
                } else if (len == 6) {
                    output1 = output1.substring(0, 3) + "." + output1.substring(3);
                }

                if (output1.substring(0, 2).equalsIgnoreCase("00")) {
                    output1 = output1.substring(2);
                } else if (output1.substring(0, 1).equalsIgnoreCase("0")) {
                    output1 = output1.substring(1);
                }
                if (flag) {
                    output1 = "-".concat(output1);
                }
                txt_gross_wt.setText("" + output1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String serviceString = "unknown service";
        String charaString = "unknown characteristic";

        for (BluetoothGattService gattService : gattServices) {

            uuid = gattService.getUuid().toString();
            serviceString = BluetoothUtils.lookup(uuid);

            if (serviceString != null) {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    HashMap<String, String> currentCharaData = new HashMap<String, String>();
                    uuid = gattCharacteristic.getUuid().toString();
                    charaString = BluetoothUtils.lookup(uuid);
                    if (charaString != null) {
                        //  serviceName.setText(charaString);
                    }
                    mNotifyCharacteristic = gattCharacteristic;
                    if (mNotifyCharacteristic != null) {
                        final int charaProp = mNotifyCharacteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mBluetoothLEService.setCharacteristicNotification(mNotifyCharacteristic, true);
                        }
                    }
                    return;
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        Constants.checkPermissions(this);
        MemoryAllocation();
        dev_address = getIntent().getExtras().getString("Address");
        dev_name = getIntent().getExtras().getString("Name");
        if (dev_address != null && !dev_address.isEmpty()) {
            mEditor.putString(Constants.dev_address_sp, dev_address);
            mEditor.putString(Constants.dev_name_sp, dev_name);
            mEditor.apply();
            if (mBluetoothLEService != null) {
                mBluetoothLEService.connect(dev_address);
            }
        } else {
            dev_address = mSharedPreference.getString(Constants.dev_address_sp, "");
        }
        if (dev_address.isEmpty()) {
            finish();
        }


        Constants.setupBluetooth(this);
        Intent gattServiceIntent = new Intent(MainActivity.this, BluetoothLEService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        btnScan.setOnClickListener(view -> {
            if (btnScan.getText().toString().equalsIgnoreCase(getString(R.string.scan))) {
                alert("Are you want to Scan?", getString(R.string.scan));
            } else if (btnScan.getText().toString().equalsIgnoreCase(getString(R.string.connected))) {
                alert("Are you want to DISCONNECT?", getString(R.string.connected));
            }
        });
        btnTare.setOnClickListener(view -> {
            if (mNotifyCharacteristic != null) {
                mBluetoothLEService.writeCharacteristic(mNotifyCharacteristic, "T");
            } else {
                Toast.makeText(MainActivity.this, "Please connect again!", Toast.LENGTH_SHORT).show();
            }
        });

        btnMode.setOnClickListener(view -> {
            if (mNotifyCharacteristic != null) {
                mBluetoothLEService.writeCharacteristic(mNotifyCharacteristic, "M");
            } else {
                Toast.makeText(MainActivity.this, "Please connect again!", Toast.LENGTH_SHORT).show();
            }
        });

        btnInc.setOnClickListener(view -> {
            if (mNotifyCharacteristic != null) {
                mBluetoothLEService.writeCharacteristic(mNotifyCharacteristic, "I");
            } else {
                Toast.makeText(MainActivity.this, "Please connect again!", Toast.LENGTH_SHORT).show();
            }
        });

        btnShift.setOnClickListener(view -> {
            if (mNotifyCharacteristic != null) {
                mBluetoothLEService.writeCharacteristic(mNotifyCharacteristic, "S");
            } else {
                Toast.makeText(MainActivity.this, "Please connect again!", Toast.LENGTH_SHORT).show();
            }
        });


        findViewById(R.id.btnNext).setOnClickListener(v -> {
            JSONObject mjsonobject = new JSONObject();
            try {
                mjsonobject.put(txt_title1.getText().toString(), txtSrNo.getText().toString());
                mjsonobject.put(txt_title2.getText().toString(), edtLotno.getText().toString());
                mjsonobject.put(txt_title3.getText().toString(), edtBaleno.getText().toString());
                mjsonobject.put(txt_title4.getText().toString(), txt_gross_wt.getText().toString());
                mjsonobject.put(txt_title5.getText().toString(), edt_tare_wt.getText().toString());
                mjsonobject.put(txt_title6.getText().toString(), edt_net_wt.getText().toString());
                mjsonobject.put(txt_title7.getText().toString(), edtMaterial.getText().toString());
            } catch (Exception e) {
                e.getMessage();
            }
            String filename = "superb_" + edtLotno.getText().toString().trim() + "_lot.xls";
            Constants.exportToExcel(mjsonobject, filename, txtSrNo.getText().toString().equalsIgnoreCase("1"));
            int no = Integer.parseInt(txtSrNo.getText().toString()) + 1;
            txtSrNo.setText("" + no);
            int bale_no = Integer.parseInt(edtBaleno.getText().toString()) + 1;
            edtBaleno.setText("" + bale_no);
            Toast.makeText(MainActivity.this, "Written in " + filename, Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnSubmit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alert("Are you sure to Submit ?", "submit");

            }
        });

        findViewById(R.id.btnExport1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                LayoutInflater li = LayoutInflater.from(MainActivity.this);
                View promptsView = li.inflate(R.layout.prompts, null);
                android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(MainActivity.this);
                alertDialogBuilder.setView(promptsView);
                final EditText userInput = promptsView.findViewById(R.id.edtInput);
                final TextView textView1 = promptsView.findViewById(R.id.textView1);
                String str = "Type " + txt_title2.getText().toString() + " number: ";
                textView1.setText(str);
                userInput.setText(edtLotno.getText().toString());
                // set dialog message
                alertDialogBuilder.setCancelable(false).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String filename = "superb_" + userInput.getText().toString().trim() + "_lot.xls";
                        File file = Constants.getFile(filename);
                        ExportAlert(file);
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

                // create alert dialog
                android.app.AlertDialog alertDialog = alertDialogBuilder.create();

                // show it
                alertDialog.show();
            }
        });

        btncount.setOnClickListener(v -> {
            if (count == 0) {
                count = 1;
            } else if (count == 1) {
                count = 2;
            } else if (count == 2) {
                count = 3;
            } else if (count == 3) {
                count = 0;
            }
            btncount.setText("Count " + count);
            mEditor.putInt("count", count);
            mEditor.apply();
        });

        textClock.setOnClickListener(v -> {
            isShow++;
            if (isShow > 4) {
                btncount.setText("Count " + count);
                btncount.setVisibility(View.VISIBLE);
            } else {
                btncount.setVisibility(View.GONE);
            }
        });

        textClock.setFormat12Hour("dd/MM/yyyy hh:mm:ss a EEE");
        textClock.setFormat24Hour(null);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        btncount.setVisibility(View.GONE);

    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void MemoryAllocation() {

        mSharedPreference = Constants.getSharedPreference(this);
        mEditor = mSharedPreference.edit();
        count = mSharedPreference.getInt("count", 0);
        textClock = (TextClock) findViewById(R.id.textClock);
        txtSrNo = (TextView) findViewById(R.id.txtSrNo);
        txt_title1 = (TextView) findViewById(R.id.txt_title1);
        txt_title2 = (TextView) findViewById(R.id.txt_title2);
        txt_title3 = (TextView) findViewById(R.id.txt_title3);
        txt_title4 = (TextView) findViewById(R.id.txt_title4);
        txt_title5 = (TextView) findViewById(R.id.txt_title5);
        txt_title6 = (TextView) findViewById(R.id.txt_title6);
        txt_title7 = (TextView) findViewById(R.id.txt_title7);
        txt_gross_wt = (TextView) findViewById(R.id.edt_gross_wt);
        edt_net_wt = (EditText) findViewById(R.id.edt_net_wt);
        edt_tare_wt = (EditText) findViewById(R.id.edt_tare_wt);
        edtLotno = (EditText) findViewById(R.id.edt_lotno);
        edtBaleno = (EditText) findViewById(R.id.edt_baleno);
        edtMaterial = (EditText) findViewById(R.id.edtMaterial);
        btnScan = (Button) findViewById(R.id.btnScan);
        btnTare = (Button) findViewById(R.id.btnTare);

        btnMode = (Button) findViewById(R.id.btnMode);
        btnInc = (Button) findViewById(R.id.btnInc);
        btnShift = (Button) findViewById(R.id.btnShift);

        btncount = (Button) findViewById(R.id.btncount);
        txt_title1.setOnClickListener(this);
        txt_title2.setOnClickListener(this);
        txt_title3.setOnClickListener(this);
        txt_title4.setOnClickListener(this);
        txt_title5.setOnClickListener(this);
        txt_title6.setOnClickListener(this);
        txt_title7.setOnClickListener(this);


    }

    private void alert(String message, final String state) {

        new android.app.AlertDialog.Builder(this)

                .setIcon(R.drawable.ic_helix)
                .setTitle(getResources().getString(R.string.app_name))
                .setMessage(message)
                .setPositiveButton("Ok", (dialogInterface, i) -> {
                    if (state.equalsIgnoreCase("submit")) {
                        txtSrNo.setText("1");
                        edtBaleno.setText("1");
                        if (!edtLotno.getText().toString().isEmpty()) {
                            int lot = Integer.parseInt(edtLotno.getText().toString()) + 1;
                            edtLotno.setText("" + lot);
                        } else {
                            edtLotno.setText("1");
                        }
                    } else if (state.equalsIgnoreCase(getString(R.string.scan))) {
                        try {
//                            if(bluetoothDevice!=null){
//                                mBluetoothLEService.connect(bluetoothDevice.getAddress());
//                            }else{
//                                startScanning(true);
//                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (state.equalsIgnoreCase(getString(R.string.connected))) {
                        mBluetoothLEService.disconnect();
                        mBluetoothLEService.close();
                        btnScan.setText(getString(R.string.scan));
                        txt_gross_wt.setText("0.0");
                        edt_tare_wt.setText("0.0");
                        edt_net_wt.setText("0.0");
                        Toast.makeText(MainActivity.this, "disconnected", Toast.LENGTH_SHORT).show();
                    }

                })
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                .show();
    }


    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onResume() {
        super.onResume();

        if (mBluetoothAdapter == null) {
            Toast.makeText(MainActivity.this, "Bluetooth not supported!", Toast.LENGTH_LONG).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, Constants.REQUEST_LOCATION_ENABLE_CODE);
        }
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Your devices that don't support BLE", Toast.LENGTH_LONG).show();
            finish();
        }
        if (!mBluetoothAdapter.enable()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, Constants.REQUEST_BLUETOOTH_ENABLE_CODE);
        }

        if (mBluetoothLEService != null) {
            final boolean result = mBluetoothLEService.connect(dev_address);
            Log.d(TAG, "Connect request result=" + result);
        }

        registerReceiver(mGattUpdateReceiver, GattUpdateIntentFilter());
        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(mReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));

        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(LocationServices.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
        mGoogleApiClient.connect();

        String stvalue = mSharedPreference.getString(Constants.txt_srvalue_sp, "1");
        String title1 = mSharedPreference.getString(Constants.txt_title1_sp, getResources().getString(R.string.sr_no));
        String title2 = mSharedPreference.getString(Constants.txt_title2_sp, getResources().getString(R.string.lot_no));
        String title3 = mSharedPreference.getString(Constants.txt_title3_sp, getResources().getString(R.string.bale_no));
        String title4 = mSharedPreference.getString(Constants.txt_title4_sp, getResources().getString(R.string.gross_wt));
        String title5 = mSharedPreference.getString(Constants.txt_title5_sp, getResources().getString(R.string.tare_wt));
        String title6 = mSharedPreference.getString(Constants.txt_title6_sp, getResources().getString(R.string.net_wt));
        String title7 = mSharedPreference.getString(Constants.txt_title7_sp, getResources().getString(R.string.material));
        //String gross_wt = mSharedPreference.getString(Constants.edt_gross_wt_sp, "0.0");
        //String tare_wt = mSharedPreference.getString(Constants.edt_tare_wt_sp, "0.0");
        //String net_wt = mSharedPreference.getString(Constants.edt_net_wt_sp, "0.0");
        String lot_no = mSharedPreference.getString(Constants.edtLotno_sp, "1");
        String bale_no = mSharedPreference.getString(Constants.edtBaleno_sp, "1");
        String Material = mSharedPreference.getString(Constants.edtMaterial_sp, "");

        txtSrNo.setText(stvalue);
        txt_title1.setText(title1);
        txt_title2.setText(title2);
        txt_title3.setText(title3);
        txt_title4.setText(title4);
        txt_title5.setText(title5);
        txt_title6.setText(title6);
        txt_title7.setText(title7);
        txt_gross_wt.setText("0.0");
        edt_tare_wt.setText("0.0");
        edt_net_wt.setText("0.0");
        edtLotno.setText(lot_no);
        edtBaleno.setText(bale_no);
        edtMaterial.setText(Material);
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onStop() {
        super.onStop();

        mEditor.putString(Constants.txt_srvalue_sp, txtSrNo.getText().toString());
        mEditor.putString(Constants.txt_title1_sp, txt_title1.getText().toString());
        mEditor.putString(Constants.txt_title2_sp, txt_title2.getText().toString());
        mEditor.putString(Constants.txt_title3_sp, txt_title3.getText().toString());
        mEditor.putString(Constants.txt_title4_sp, txt_title4.getText().toString());
        mEditor.putString(Constants.txt_title5_sp, txt_title5.getText().toString());
        mEditor.putString(Constants.txt_title6_sp, txt_title6.getText().toString());
        mEditor.putString(Constants.txt_title7_sp, txt_title7.getText().toString());

        mEditor.putString(Constants.edt_gross_wt_sp, txt_gross_wt.getText().toString());
        mEditor.putString(Constants.edt_tare_wt_sp, edt_tare_wt.getText().toString());
        mEditor.putString(Constants.edt_net_wt_sp, edt_net_wt.getText().toString());
        mEditor.putString(Constants.edtLotno_sp, edtLotno.getText().toString());
        mEditor.putString(Constants.edtBaleno_sp, edtBaleno.getText().toString());
        mEditor.putString(Constants.edtMaterial_sp, edtMaterial.getText().toString());
        mEditor.apply();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, yay! Do the
                // contacts-related task you need to do.
            } else {
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                Toast.makeText(MainActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
            }
            return;
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
        unregisterReceiver(mGattUpdateReceiver);
    }

    private void ExportAlert(final File file) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this);
        builder.setTitle(getString(R.string.app_name));

        builder.setMessage("Data Exported in a Excel Sheet");
        builder.setNegativeButton("Share", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(@NonNull DialogInterface dialog, int which) {


                Uri photoURI = FileProvider.getUriForFile(MainActivity.this, getApplicationContext().getPackageName() + ".provider", file);

                Intent intentShareFile = new Intent(Intent.ACTION_SEND);
                intentShareFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                if (file.exists()) {
                    intentShareFile.setType("application/xls");
                    intentShareFile.putExtra(Intent.EXTRA_STREAM, photoURI);
                    intentShareFile.putExtra(Intent.EXTRA_SUBJECT, "Sharing File...");
                    intentShareFile.putExtra(Intent.EXTRA_TEXT, "Sharing File...");
                    startActivity(Intent.createChooser(intentShareFile, "Share File"));
                } else {
                    Toast.makeText(MainActivity.this, "File is not exist", Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            }
        });
        builder.setPositiveButton("View", (dialog, which) -> {
            //File file = new File(Environment.getExternalStorageDirectory()+ "/superb/" + filename);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.ms-excel");
            startActivity(intent);
            dialog.dismiss();
        }).show();
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void startScanning(final boolean enable) {

        Handler mHandler = new Handler();
        if (enable) {
            List<ScanFilter> scanFilters = new ArrayList<>();
            final ScanSettings settings = new ScanSettings.Builder().build();
            //  ScanFilter scanFilter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(SampleGattAttributes.UUID_BATTERY_LEVEL_UUID)).build();
            // scanFilters.add(scanFilter);
            mHandler.postDelayed(() -> {

                if (!btnScan.getText().toString().equalsIgnoreCase(getString(R.string.connected))) {
                    btnScan.setText(getString(R.string.scan));
                }
                if (bluetoothLeScanner != null) {
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                    }
                    bluetoothLeScanner.stopScan(scanCallback);
                }
            }, Constants.SCAN_PERIOD);


            btnScan.setText("Scanning...");
            if (bluetoothLeScanner != null) {
                bluetoothLeScanner.startScan(scanFilters, settings, scanCallback);
            }

        } else {

            mHandler.post(() -> {
                btnScan.setText(getString(R.string.scan));
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
                    break;
                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                    try {
                        status.startResolutionForResult(MainActivity.this, REQUEST_LOCATION);
                    } catch (Exception e) {
                    }
                    break;
                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("onActivityResult()", Integer.toString(resultCode));

        //final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        switch (requestCode) {
            case REQUEST_LOCATION:
                switch (resultCode) {
                    case Activity.RESULT_OK: {
                        // All required changes were successfully made
                        Toast.makeText(MainActivity.this, "Location enabled by user!", Toast.LENGTH_LONG).show();
                        break;
                    }
                    case Activity.RESULT_CANCELED: {
                        // The user was asked to change settings, but chose not to
                        Toast.makeText(MainActivity.this, "Location not enabled, user cancelled.", Toast.LENGTH_LONG).show();
                        break;
                    }
                    default: {
                        break;
                    }
                }
                break;
        }
    }


    private void textChangeDialog(String title, final TextView textView) {
        LayoutInflater li = LayoutInflater.from(MainActivity.this);
        View promptsView = li.inflate(R.layout.prompts, null);
        android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setView(promptsView);
        final EditText userInput = promptsView.findViewById(R.id.edtInput);
        final TextView textView1 = promptsView.findViewById(R.id.textView1);
        textView1.setText(title);
        userInput.setText(textView.getText().toString());
        // set dialog message
        alertDialogBuilder.setCancelable(false).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                textView.setText("" + userInput.getText().toString());
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.txt_title1:
                textChangeDialog("Type a Title1", (TextView) v);
                break;
            case R.id.txt_title2:
                textChangeDialog("Type a Title2", (TextView) v);
                break;
            case R.id.txt_title3:
                textChangeDialog("Type a Title3", (TextView) v);
                break;
            case R.id.txt_title4:
                textChangeDialog("Type a Title4", (TextView) v);
                break;
            case R.id.txt_title5:
                textChangeDialog("Type a Title5", (TextView) v);
                break;
            case R.id.txt_title6:
                textChangeDialog("Type a Title6", (TextView) v);
                break;
            case R.id.txt_title7:
                textChangeDialog("Type a Title7", (TextView) v);
                break;

        }
    }
}
