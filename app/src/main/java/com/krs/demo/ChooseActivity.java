package com.krs.demo;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class ChooseActivity extends AppCompatActivity {

    private SharedPreferences mSharedPreference;


    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose);
        mSharedPreference =Constants.getSharedPreference(this);
        Button btnList=findViewById(R.id.btnlist);
        Button btnConnect=findViewById(R.id.btnConnect);
        String name= mSharedPreference.getString(Constants.dev_name_sp,"");
        Constants.checkPermissions(this);

        if(name.isEmpty()){
           Intent mIntent=new Intent(ChooseActivity.this,WeightListActivity.class);
           startActivity(mIntent);
           finish();
        }else{
            btnConnect.setText("Connect to "+name);
            btnList.setOnClickListener(v -> {
                Intent mIntent=new Intent(ChooseActivity.this,WeightListActivity.class);
                startActivity(mIntent);
            });

            btnConnect.setOnClickListener(v->{
                Intent mIntent=new Intent(ChooseActivity.this,MainActivity.class);
                Bundle mBundle=new Bundle();
                String address= mSharedPreference.getString(Constants.dev_address_sp,"");
                mBundle.putString("Address",address);
                mBundle.putString("Name",name);
                mIntent.putExtras(mBundle);
                startActivity(mIntent);
            });
        }
    }
}