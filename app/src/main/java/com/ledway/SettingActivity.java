package com.ledway;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class SettingActivity extends Activity {
	private EditText srvAddress;
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setting);
		srvAddress = (EditText) findViewById(R.id.srvAddress);
		SharedPreferences prefs = getSharedPreferences ("Ledway",0);
		srvAddress.setText(prefs.getString("srvAddress", "http://www.ledway.com.tw/wayflow"));
		Button btnSave = (Button) findViewById(R.id.btnSaveSetting);
		btnSave.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				// TODO Auto-generated method stub
				 SharedPreferences.Editor editor = getSharedPreferences ("Ledway",0).edit();
			        editor.putString("srvAddress",SettingActivity.this.srvAddress.getText().toString());
			        editor.commit();
			        LoginInfo.getInstance().setServer(SettingActivity.this.srvAddress.getText().toString());
			        SettingActivity.this.finish();
			}
			
		});
		Button btnCancel = (Button) findViewById(R.id.btnCancelSetting);
		btnCancel.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				// TODO Auto-generated method stub
				SettingActivity.this.finish();
			}
			
		});
		
	}
}
