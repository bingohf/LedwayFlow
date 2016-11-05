package com.ledway;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class WorkItem extends Activity implements OnClickListener, Runnable,
		DialogInterface.OnKeyListener, DialogInterface.OnDismissListener {
	private final int UPDATE_SHOWTEXT = 1;
	private final int UPDATE_DISSMIS = 2;
	private final int UPDATE_START = 3;
	private HashMap<String, String> map;
	private String approveType = "";
	private EditText comment;
	private ProgressDialog mydialog;
	private Button btnApprove, btnReject;
	private Thread thread;
	private Intent intent;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.workitem);
		Intent intent = getIntent();
		map = (HashMap<String, String>) intent.getSerializableExtra("data");
		((TextView) findViewById(R.id.FEFORMNO)).setText(map.get("FEFORMNO"));
		((TextView) findViewById(R.id.CANUSE)).setText(map.get("CANUSE"));
		((TextView) findViewById(R.id.FEFORMNAME)).setText(map
				.get("FEFORMNAME"));
		((TextView) findViewById(R.id.FEMNAME)).setText(map.get("FEMNAME"));
		((TextView) findViewById(R.id.FSTARTDATE)).setText(map
				.get("FSTARTDATE"));
		comment = (EditText) findViewById(R.id.edtComment);
		btnApprove = (Button) findViewById(R.id.btnApprove);
		btnReject = (Button) findViewById(R.id.btnReject);

		btnApprove.setOnClickListener(this);
		btnReject.setOnClickListener(this);
		Button button = (Button) findViewById(R.id.btnPDF);
		button.setOnClickListener(this);

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) { // 按下的如果是BACK，同时没有重复
			finish();
		}

		return super.onKeyDown(keyCode, event);

	}

	public void onClick(View v) {
		// TODO Auto-generated method stub
		// Intent intent = new Intent(this,WebViewActivity.class);
		// intent.putExtra("url", map.get("PDFURL"));
		// startActivity(intent);
		switch (v.getId()) {
			case R.id.btnPDF: {
				thread = new Thread(new Runnable() {

					public void run() {
						// TODO Auto-generated method stub
						String urlStr = map.get("PDFURL");
						String path = "file";
						String fileName = System.currentTimeMillis() +".pdf";
						String SDCard = Environment.getExternalStorageDirectory() + "";
						String pathName = SDCard + "/" + path + "/" + fileName;// 文件存储路径
						OutputStream output = null;
						try {
						/*
						 * 通过URL取得HttpURLConnection 要网络连接成功，需在AndroidMainfest.xml中进行权限配置
						 * <uses-permission android:name="android.permission.INTERNET"
						 * />
						 */
							URL url = new URL(urlStr);
							HttpURLConnection conn = (HttpURLConnection) url
									.openConnection();

							File file = new File(pathName);
							InputStream input = conn.getInputStream();
							if (file.exists()) {
								System.out.println("exits");
								return;
							} else {
								String dir = SDCard + "/" + path;
								new File(dir).mkdir();// 新建文件夹
								file.createNewFile();// 新建文件
								output = new FileOutputStream(file);
								// 读取大文件
								byte[] buffer = new byte[4];
								while (input.read(buffer) != -1) {
									output.write(buffer);
								}
								output.flush();
							}
							conn.disconnect();
							intent = new Intent("android.intent.action.VIEW");
							intent.addCategory("android.intent.category.DEFAULT");
							intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							Uri uri = Uri.fromFile(new File(pathName));
							intent.setDataAndType(uri, "application/pdf");
							// Uri uri = Uri.parse(map.get("PDFURL"));
							// Intent intent = new Intent(Intent.ACTION_VIEW, uri);

							//startActivity(intent);
							mHandler.sendEmptyMessage(UPDATE_START);
						} catch (MalformedURLException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						} finally {
							try {
								output.close();
								System.out.println("success");
								mHandler.sendEmptyMessage(UPDATE_DISSMIS);
							} catch (IOException e) {
								System.out.println("fail");
								e.printStackTrace();
							}
						}
					}
				});

				mydialog = ProgressDialog.show(this, "请稍等...", "Loading...", true);
				mydialog.setOnKeyListener(this);
				mydialog.setOnDismissListener(this);
				thread.start();

				break;
			}
			case R.id.btnApprove: {
				approveType = "SendEForm";
				mydialog = ProgressDialog.show(this, "请稍等...", "Loading...", true);
				mydialog.setOnKeyListener(this);
				thread = new Thread(this);
				mydialog.setOnDismissListener(this);
				thread.start();
				break;
			}
			case R.id.btnReject: {
				approveType = "BackEForm";
				mydialog = ProgressDialog.show(this, "请稍等...", "Loading...", true);
				mydialog.setOnKeyListener(this);
				mydialog.setOnDismissListener(this);
				thread = new Thread(this);
				thread.start();
				break;
			}
		}

	}

	public void run() {
		// TODO Auto-generated method stub

		try {

			PostMethod postMethod = new PostMethod(
					LoginInfo.getInstance().getServer()+ "/webservice.asmx");


			String soapRequestData = String
					.format("<?xml version=\"1.0\" encoding=\"utf-8\"?> "
							+ " <soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"> "
							+ " <soap:Body> "
							+ "<ProcEForm xmlns=\"http://tempuri.org/\"> "
							+ "<FEFormID>%s</FEFormID> "
							+ " <FEFormNo>%s</FEFormNo> "
							+ "<FEmID>%s</FEmID> " + "<FComment>%s</FComment> "
							+ " <ProcType>%s</ProcType> " + " </ProcEForm> "
							+ " </soap:Body>" + "</soap:Envelope>", map
							.get("FEFORMID"), map.get("FEFORMNO"), LoginInfo
							.getInstance().getName(), comment.getText()
							.toString().replaceAll("<", "&lt;"), approveType);
			System.out.println(soapRequestData);
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
			String result = doc.getRootElement().element("Body")
					.element("ProcEFormResponse")
					.elementText("ProcEFormResult");
			System.out.println(result);
			if (result.startsWith("-1")) {
				Message msg = new Message();
				msg.obj = result;
				msg.what = UPDATE_SHOWTEXT;
				mHandler.sendMessage(msg);
			} else {
				Message msg = new Message();
				msg.obj = "Done";
				msg.what = UPDATE_SHOWTEXT;
				mHandler.sendMessage(msg);
				this.finish();
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

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case UPDATE_SHOWTEXT: {
					Toast.makeText(WorkItem.this, msg.obj.toString(), 1).show();
					break;
				}
				case UPDATE_DISSMIS: {
					mydialog.dismiss();
					break;
				}
				case UPDATE_START:{
					startActivity(intent);
				}
			}
		}
	};

	public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent arg2) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK && mydialog.isShowing()) {
			mydialog.dismiss();
			Log.v("", "dismiss");
			return true;
		}
		return false;
	}

	public void onDismiss(DialogInterface arg0) {
		// TODO Auto-generated method stub
		Log.v("", "dismiss2");
		if (thread != null && thread.isAlive()) {
			thread.stop();

		}

	}
}
