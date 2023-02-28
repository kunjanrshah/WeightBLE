package com.krs.demo;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class WeightAdapter extends RecyclerView.Adapter<WeightAdapter.Viewholder> {

    private final Context context;
    private final ArrayList<BluetoothDevice> courseModelArrayList;

    // Constructor
    public WeightAdapter(Context context, ArrayList<BluetoothDevice> courseModelArrayList) {
        this.context = context;
        this.courseModelArrayList = courseModelArrayList;
    }

    @NonNull
    @Override
    public WeightAdapter.Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // to inflate the layout for each item of recycler view.
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_layout, parent, false);
        return new Viewholder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WeightAdapter.Viewholder holder, int position) {
        // to set data to textview and imageview of each card layout
        BluetoothDevice mDevice = courseModelArrayList.get(position);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        holder.txtName.setText(mDevice.getName());
        holder.txtAddr.setText(mDevice.getAddress());
        holder.btnConnect.setOnClickListener(v -> {
            Intent mIntent=new Intent(context,MainActivity.class);
            Bundle mBundle=new Bundle();
            mBundle.putString("Address",mDevice.getAddress());
            mBundle.putString("Name",mDevice.getName());
            mIntent.putExtras(mBundle);
            context.startActivity(mIntent);
            ((Activity) context).finish();
        });
    }

    @Override
    public int getItemCount() {
        // this method is used for showing number of card items in recycler view
        return courseModelArrayList.size();
    }

    // View holder class for initializing of your views such as TextView and Imageview
    public static class Viewholder extends RecyclerView.ViewHolder {
        private final TextView txtName;
        private final TextView txtAddr;
        private final Button btnConnect;
        public Viewholder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtName);
            txtAddr = itemView.findViewById(R.id.txtAddr);
            btnConnect = itemView.findViewById(R.id.btnConnect);
        }
    }
}

