package com.ledway;

import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
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
import org.apache.commons.io.FileUtils;
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
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class WorkItem extends AppCompatActivity implements OnClickListener, Runnable,
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
		getSupportActionBar().setTitle(map.get("FEFORMNO"));
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


				mydialog = ProgressDialog.show(this, "请稍等...", "Loading...", true);


				final Subscription subscription =
						Observable.just(map.get("PDFURL"))
								.flatMap(new Func1<String, Observable<File>>() {
									public Observable<File> call(final String url) {
										return Observable.create(new Observable.OnSubscribe<File>() {
											public void call(Subscriber<? super File> subscriber) {
												String fileName = Environment.getExternalStorageDirectory().getAbsolutePath() +"/file/" + System.currentTimeMillis() +".pdf";
												File outFile = new File(fileName);
												outFile.getParentFile().mkdirs();
												try {
													FileUtils.copyURLToFile(new URL(url),outFile);
													subscriber.onNext(outFile);
													subscriber.onCompleted();
												} catch (IOException e) {
													e.printStackTrace();
													subscriber.onError(e);
												}
											}
										});

									}
								})
								.subscribeOn(Schedulers.io())
								.observeOn(AndroidSchedulers.mainThread())
								.subscribe(new Subscriber<File>() {
									public void onCompleted() {
										mydialog.dismiss();
									}

									public void onError(Throwable e) {
										Log.e("error", e.getMessage(), e);
										Toast.makeText(WorkItem.this, e.getMessage(), Toast.LENGTH_LONG).show();
										mydialog.dismiss();
									}

									public void onNext(File file) {
										Toast.makeText(WorkItem.this, file.getAbsolutePath(), Toast.LENGTH_LONG).show();
										Intent intent = new Intent(WorkItem.this, PDFViewerActivity.class);
										intent.putExtra("pdf_file_uri", file.getAbsolutePath());
										startActivity(intent);
									}
								});
				mydialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
					public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
						if(keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK){
							subscription.unsubscribe();
						}
						return false;
					}
				});
				mydialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					public void onDismiss(DialogInterface dialogInterface) {
						subscription.unsubscribe();
					}
				});
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
