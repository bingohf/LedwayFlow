package com.ledway;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class WorklistActivity extends ListActivity implements Runnable,
		OnClickListener, DialogInterface.OnKeyListener,DialogInterface.OnDismissListener  {
	private final int UPDATE_SHOWTEXT = 1;
	private final int UPDATE_DISSMIS = 2;
	private List list = new ArrayList<Map>();
	private SimpleAdapter adp ;
	private ProgressDialog mydialog;
	private Thread myThread;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adp = new SimpleAdapter(this, list,
				android.R.layout.simple_list_item_1,
				new String[] { "LIST_TITLE" }, new int[] { android.R.id.text1 });
		setListAdapter(adp);

	}

	@Override
	protected void onStart() {
		super.onStart();
		System.out.println("onStart");

		mydialog = ProgressDialog.show(this, "请稍等...", "Loading...", true);
		mydialog.setOnKeyListener(this);
		myThread = new Thread(this);
		myThread.start();

	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) { // 按下的如果是BACK，同时没有重复
			finish();
		}

		return super.onKeyDown(keyCode, event);

	}
	public void run() {
		// TODO Auto-generated method stub
		try {

			PostMethod postMethod = new PostMethod(
					LoginInfo.getInstance().getServer()+ "/webservice.asmx");

			String soapRequestData = String
					.format("<?xml version=\"1.0\" encoding=\"utf-8\"?>"
							+ "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"> "
							+ "<soap:Body>"
							+ "<GetEFormFlow1 xmlns=\"http://tempuri.org/\">"
							+ " <FEmID>%s</FEmID>" + "<FMinute>-1</FMinute> "
							+ " </GetEFormFlow1>" + " </soap:Body> "
							+ "</soap:Envelope>", LoginInfo.getInstance()
							.getName());

			byte[] b = soapRequestData.getBytes("utf-8");
			InputStream is = new ByteArrayInputStream(b, 0, b.length);
			RequestEntity re = new InputStreamRequestEntity(is, b.length,
					"text/xml; charset=utf-8");
			postMethod.setRequestEntity(re);

			HttpClient httpClient = new HttpClient();
			int statusCode = httpClient.executeMethod(postMethod);
			String soapResponseData = postMethod.getResponseBodyAsString();
			Document doc = DocumentHelper.parseText(soapResponseData);
			String result = doc.getRootElement().element("Body")
					.element("GetEFormFlow1Response")
					.elementText("GetEFormFlow1Result");
			System.out.println(result);
			if (result.startsWith("-1")) {
				Message msg = new Message();
				msg.obj = result;
				msg.what = UPDATE_SHOWTEXT;
				mHandler.sendMessage(msg);
			} else {
				Document tdoc = DocumentHelper.parseText(result);
				Iterator i = tdoc.getRootElement().elementIterator(
						"WayFlow.EFormFlow1");
				list.clear();
				while (i.hasNext()) {
					Element e = (Element) i.next();
					Map<String, String> map = new HashMap<String, String>();
					Iterator j = e.elementIterator();
					while (j.hasNext()) {
						Element ee = (Element) j.next();
						map.put(ee.getName(), ee.getText());
					}
					map.put("LIST_TITLE",
							map.get("FEFORMNAME") + " " + map.get("FEFORMNO"));
					list.add(map);

				}
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
			Toast.makeText(WorklistActivity.this, e.toString(),
					1).show();
		}
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			adp.notifyDataSetChanged();
			adp.notifyDataSetInvalidated();
			switch (msg.what) {
				case UPDATE_SHOWTEXT: {
					Toast.makeText(WorklistActivity.this, msg.obj.toString(), 1)
							.show();
					break;
				}
				case UPDATE_DISSMIS: {
					mydialog.dismiss();
					break;
				}
			}
		}
	};

	public void onClick(View v) {
		// TODO Auto-generated method stub

	}

	public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK && mydialog.isShowing()) {
			mydialog.dismiss();
			return true;
		}
		return false;
	}

	public void onDismiss(DialogInterface dialog) {
		// TODO Auto-generated method stub
		if (myThread != null && myThread.isAlive()) {
			myThread.stop();

		}
	}
	protected void onListItemClick(ListView l, View v, int position, long id) {
		System.out.println(position);
		Intent intent = new Intent(this,WorkItem.class);
		Bundle extras = new Bundle();
		extras.putSerializable("data", (HashMap)list.get(position));
		intent.putExtras(extras);

		startActivity(intent);
	}
}
