package com.ledway;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class LedwayWorkflowActivity extends Activity implements
		DialogInterface.OnKeyListener, Runnable, OnClickListener,
		DialogInterface.OnDismissListener {
	/** Called when the activity is first created. */
	private ProgressDialog mydialog;
	private Thread myThread;
	private final int UPDATE_SHOWTEXT = 1;
	private final int UPDATE_DISSMIS = 2;
	private final int SHOW_WORKlIST = 3;
	private EditText username, password;
	private CheckBox cbSave;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences prefs = getSharedPreferences ("Ledway",0);
		LoginInfo.getInstance().setServer(prefs.getString("srvAddress", "http://www.ledway.com.tw/wayflow"));



		// System.out.println("On create");
		setContentView(R.layout.main);
		Button button = (Button) findViewById(R.id.btnLogin);
		username = (EditText) findViewById(R.id.username);
		password = (EditText) findViewById(R.id.password);
		cbSave = (CheckBox) findViewById(R.id.cbSave);
		button.setOnClickListener(this);

		prefs = getPreferences(0);
		cbSave.setChecked(prefs.getBoolean("Save Password", false));
		if (cbSave.isChecked()){
			username.setText(prefs.getString("User Name", ""));
			password.setText(prefs.getString("Password", ""));
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) { // 按下的如果是BACK，同时没有重复
			SysUtil mSysUtil= new SysUtil(LedwayWorkflowActivity.this);
			mSysUtil.exit();
			System.exit(0);
		}

		return super.onKeyDown(keyCode, event);

	}

	public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent arg2) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK && mydialog.isShowing()) {
			mydialog.dismiss();
			Log.v("", "dismiss");
			return true;
		}
		return false;
	}

	//这里用来接受退出程序的指令
	@Override
	protected void onStart() {



		int flag = getIntent().getIntExtra("flag", 0);
		if(flag == SysUtil.EXIT_APPLICATION){
			finish();
		}
		super.onResume();

	}

	//当activity是单例的时候,再次启动该activity就不会再调用 oncreate->onstart这些方法了
	@Override
	protected void onNewIntent(Intent intent) {
		int flag = getIntent().getIntExtra("flag", 0);
		if(flag == SysUtil.EXIT_APPLICATION){
			finish();
		}
		super.onNewIntent(intent);
	}

	public void run() {
		// TODO Auto-generated method stub

		try {

			PostMethod postMethod = new PostMethod(
					LoginInfo.getInstance().getServer()+ "/webservice.asmx");


			String soapRequestData = String
					.format("<?xml version=\"1.0\" encoding=\"utf-8\"?> "
									+ "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"> "
									+ "<soap:Body>"
									+ "<Login xmlns=\"http://tempuri.org/\"> "
									+ " <FEmID>%s</FEmID>" + "<FEmPswd>%s</FEmPswd> "
									+ " </Login>" + " </soap:Body> "
									+ "</soap:Envelope>", username.getText(),
							password.getText());

			byte[] b = soapRequestData.getBytes("utf-8");
			InputStream is = new ByteArrayInputStream(b, 0, b.length);
			RequestEntity re = new InputStreamRequestEntity(is, b.length,
					"text/xml; charset=utf-8");
			postMethod.setRequestEntity(re);

			HttpClient httpClient = new HttpClient();
			int statusCode = httpClient.executeMethod(postMethod);
			String soapResponseData = postMethod.getResponseBodyAsString();
			System.out.println(soapResponseData);
			Document doc = DocumentHelper.parseText(soapResponseData);
			String result = doc.getRootElement().element("Body").element("LoginResponse").elementText("LoginResult");
			System.out.println(result);
			if (result.startsWith("-1")){
				Message msg = new Message();
				msg.obj = result;
				msg.what = UPDATE_SHOWTEXT;
				mHandler.sendMessage(msg);
			}else{
				mHandler.sendEmptyMessage(SHOW_WORKlIST);
				LoginInfo.getInstance().setName(result);
			}
			mHandler.sendEmptyMessage(UPDATE_DISSMIS);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Message msg = new Message();
			msg.obj = e.toString();
			msg.what = UPDATE_SHOWTEXT;
			mHandler.sendMessage(msg);
			mHandler.sendEmptyMessage(UPDATE_DISSMIS);
			// Toast.makeText(LedwayWorkflowActivity.this, e.toString(),
			// 1).show();
		}
	}

	public void onClick(View v) {
		// TODO Auto-generated method stub


		if (myThread != null && myThread.isAlive()) {
			myThread.stop();

		}
		mydialog = ProgressDialog.show(this, "请稍等...", "正在登录...", true);
		mydialog.setOnKeyListener(this);
		myThread = new Thread(this);
		myThread.start();
	}

	public void onDismiss(DialogInterface arg0) {
		// TODO Auto-generated method stub
		Log.v("", "dismiss2");
		if (myThread != null && myThread.isAlive()) {
			myThread.stop();

		}

	}
	@Override
	protected void onPause() {
		super.onPause();

		SharedPreferences.Editor editor = getPreferences(0).edit();
		editor.putBoolean("Save Password", cbSave.isChecked());

		editor.putString("User Name",cbSave.isChecked()? username.getText().toString():"");
		editor.putString("Password", cbSave.isChecked()?password.getText().toString():"");
		editor.commit();
	}
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case UPDATE_SHOWTEXT: {
					Toast.makeText(LedwayWorkflowActivity.this, msg.obj.toString(),
							1).show();
					break;
				}
				case UPDATE_DISSMIS: {
					mydialog.dismiss();
					break;
				}
				case SHOW_WORKlIST:{
					Intent intent = new Intent(LedwayWorkflowActivity.this,WorklistActivity.class);
					LedwayWorkflowActivity.this.startActivity(intent);

				}
			}
		}
	};
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0,1,0,"Setting");
		return true;
	}
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent = new Intent(LedwayWorkflowActivity.this,SettingActivity.class);
		LedwayWorkflowActivity.this.startActivity(intent);
		return false;
	}
}