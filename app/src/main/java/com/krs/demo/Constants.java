package com.krs.demo;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsResult;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

@RequiresApi(api = Build.VERSION_CODES.S)
public class Constants {
    public static final int REQUEST_BLUETOOTH_ENABLE_CODE = 101;
    public static final int REQUEST_LOCATION_ENABLE_CODE = 101;

    public static int SCAN_PERIOD = 15000;

    public static LocationRequest mLocationRequest;
    public static GoogleApiClient mGoogleApiClient;
    public static PendingResult<LocationSettingsResult> result;
    public static BluetoothAdapter mBluetoothAdapter;
    public static BluetoothLeScanner bluetoothLeScanner;
    public static String txt_srvalue_sp = "txt_srvalue";
    public static String txt_title1_sp = "txt_title1";
    public static String txt_title2_sp = "txt_title2";
    public static String txt_title3_sp = "txt_title3";
    public static String txt_title4_sp = "txt_title4";
    public static String txt_title5_sp = "txt_title5";
    public static String txt_title6_sp = "txt_title6";
    public static String txt_title7_sp = "txt_title7";
    public static String edt_gross_wt_sp = "edt_gross_wt";
    public static String edt_tare_wt_sp = "edt_tare_wt_wt";
    public static String edt_net_wt_sp = "edt_net_wt_wt";
    public static String edtLotno_sp = "edtTitle2_wt";
    public static String edtBaleno_sp = "edtTitle3_wt";
    public static String edtMaterial_sp = "edtMaterial_wt";
    public static String dev_address_sp = "dev_address_sp";
    public static String dev_name_sp = "dev_name_sp";

    public static SharedPreferences getSharedPreference(Context context) {
        return context.getSharedPreferences("superb", MODE_PRIVATE);
    }

    public static File getFile(String fileName) {
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "superb");
        Log.d("getFile", "directory: " + directory);
        //create directory if not exist
        if (!directory.isDirectory()) {
            try {
                if (directory.mkdir()) {
                    System.out.println("Directory created");
                } else {
                    System.out.println("Directory is not created");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //file path
        File file = new File(directory, fileName);
        Log.d("getFile", "file: " + file);
        return file;
    }

    public static void exportToExcel(JSONObject mjson, String fileName, boolean isCreate) {

        File file = getFile(fileName);
        try {
            WritableSheet sheet;
            WritableWorkbook copy;
            if (isCreate) {
                WorkbookSettings wbSettings = new WorkbookSettings();
                wbSettings.setLocale(new Locale("en", "EN"));
                copy = Workbook.createWorkbook(file, wbSettings);
                sheet = copy.createSheet("Superb Insturments", 0);
            } else {
                Workbook workbook = Workbook.getWorkbook(file);
                copy = Workbook.createWorkbook(file, workbook);
                sheet = copy.getSheet(0);
            }
            try {
                sheet.addCell(new Label(0, 0, MainActivity.txt_title1.getText().toString())); // column and row
                sheet.addCell(new Label(1, 0, MainActivity.txt_title4.getText().toString()));
                sheet.addCell(new Label(2, 0, MainActivity.txt_title5.getText().toString()));
                sheet.addCell(new Label(3, 0, MainActivity.txt_title6.getText().toString()));
                sheet.addCell(new Label(4, 0, MainActivity.txt_title2.getText().toString()));
                sheet.addCell(new Label(5, 0, MainActivity.txt_title3.getText().toString()));
                sheet.addCell(new Label(6, 0, MainActivity.txt_title7.getText().toString()));
                sheet.addCell(new Label(7, 0, "Date"));
                if (mjson != null) {
                    try {
                        String srno = mjson.getString(MainActivity.txt_title1.getText().toString());
                        String lot_no = mjson.getString(MainActivity.txt_title2.getText().toString());
                        String bale_no = mjson.getString(MainActivity.txt_title3.getText().toString());
                        String gross_wt = mjson.getString(MainActivity.txt_title4.getText().toString());
                        String tare_wt = mjson.getString(MainActivity.txt_title5.getText().toString());
                        String net_wt = mjson.getString(MainActivity.txt_title6.getText().toString());
                        String material = mjson.getString(MainActivity.txt_title7.getText().toString());
                        String date = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss a EEE", Locale.getDefault()).format(new Date());

                        sheet.addCell(new Label(0, Integer.parseInt(srno), srno));
                        sheet.addCell(new Label(1, Integer.parseInt(srno), gross_wt));
                        sheet.addCell(new Label(2, Integer.parseInt(srno), tare_wt));
                        sheet.addCell(new Label(3, Integer.parseInt(srno), net_wt));
                        sheet.addCell(new Label(4, Integer.parseInt(srno), lot_no));
                        sheet.addCell(new Label(5, Integer.parseInt(srno), bale_no));
                        sheet.addCell(new Label(6, Integer.parseInt(srno), material));
                        sheet.addCell(new Label(7, Integer.parseInt(srno), date));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (WriteException e) {
                e.printStackTrace();
            }
            copy.write();
            try {
                copy.close();
            } catch (WriteException e) {
                e.printStackTrace();
            }
        } catch (IOException | BiffException e) {
            e.getLocalizedMessage();
        }
    }

    public static void setupBluetooth(Activity activity) {
        mBluetoothAdapter = BluetoothUtils.getBluetoothAdapter(activity);
        if (mBluetoothAdapter == null) {
            Toast.makeText(activity, "Bluetooth not supported!", Toast.LENGTH_SHORT).show();
            return;
        }
        bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
    }

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_PRIVILEGED
    };

    @RequiresApi(api = Build.VERSION_CODES.S)
    public static void checkPermissions(Activity activity) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, 1);
        }
    }

    public static void setBluetooth(boolean enable) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean isEnabled = bluetoothAdapter.isEnabled();
        if (enable && !isEnabled) {
            bluetoothAdapter.enable();
        } else if (!enable && isEnabled) {
            bluetoothAdapter.disable();
        }
        // No need to change bluetooth state
    }

    public static void alert(Activity activity) {
        new android.app.AlertDialog.Builder(activity)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(activity.getResources().getString(R.string.app_name))
                .setMessage("Please turn on Bluetooth")
                .setPositiveButton("Ok", (dialogInterface, i) -> {
                    setBluetooth(true);
                }).show();
    }
}
