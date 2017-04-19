package org.library.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * ������HttpAsyncTask
 * 
 * ������com.ordersystem.http
 * 
 * ������http�첽������
 * 
 * �����汾�ţ�
 * 
 * ������Ա�� liuzhimin
 * 
 * �������� �� -6-20
 */
public class DWRHttpAsyncTask extends AsyncTask<String, Integer, String> {

	/**
	 * �������
	 */
	private int requestCount = 0;

	/**
	 * ��ʱʱ��
	 */
	private final int TIMEOUT = 10 * 1000;

	/**
	 * log��ʶ
	 */
	private final String tag = "DWRHttpAsyncTask";

	/** ��ֵ�Բ��� */
	private Map<String, String> map;

	/** �ɹ� */
	private final int ON_SUCCESS = 0x7f070000;

	/** ����Ӧ */
	private final int ON_ERROR_NO_RESPONSE = 0x7f070002;

	/** ���ӳ�ʱ */
	private final int ON_ERROR_CONNECT_TIMEOUT = 0x7f070003;

	/** �����ʧ�� */
	private final int ON_ERROR = 0x7f070004;

	/** ���ؽ�� */
	private String result = null;

	/** �ص� */
	private CallBack callBack;

	public boolean isBase64 = true;

	/**
	 * @param map
	 * @param callBack
	 */
	private DWRHttpAsyncTask(Map<String, String> map, CallBack callBack) {
		super();
		this.map = map;
		this.callBack = callBack;
	}

	/**
	 * @param callBack
	 */
	private DWRHttpAsyncTask(CallBack callBack) {
		this.callBack = callBack;
	}

	/** handler */
	private Handler taskHandler = new Handler() {

		public void handleMessage(Message msg) {
			switch (msg.what) {
			case ON_SUCCESS:
				callBack.onSuccess(result);
				break;
			case ON_ERROR:
				callBack.onError("������Ӧ�������Ժ�����.");
				break;
			case ON_ERROR_NO_RESPONSE:
				callBack.onError("������Ӧʧ�ܣ����Ժ�����.");
				break;
			case ON_ERROR_CONNECT_TIMEOUT:
				callBack.onError("�������ӳ�ʱ�����Ժ�����.");
				break;
			}
		}

	};

	protected String doInBackground(String... params) {

		requestCount++;// �����������

		String url = params[0];
		Log.i(tag, "�����ַ:" + url);

		String requestContent = params[1];// ������Խ��

		DefaultHttpClient client = null;
		HttpPost post = null;
		HttpParams httpParameters = null;
		String response = null;

		httpParameters = new BasicHttpParams();

		HttpConnectionParams.setConnectionTimeout(httpParameters, TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpParameters, TIMEOUT);

		client = new DefaultHttpClient(httpParameters);

		post = new HttpPost(url);
	
		String result = null;

		try {

			// ͨ��json�ַ������Ͳ���

			Header[] headers = {
					new BasicHeader("Accept-Encoding", "gzip, deflate"),
					new BasicHeader("Accept-Language", "zh-CN,zh;q=0.8"),
					new BasicHeader("Connection", "keep-alive"),
					new BasicHeader("Content-Type", "text/plain") };

			post.setHeaders(headers);

			List<NameValuePair> parameters = new ArrayList<NameValuePair>();
			parameters.add(new BasicNameValuePair("callCount", "1"));
			parameters.add(new BasicNameValuePair("c0-id", "0"));
			parameters.add(new BasicNameValuePair("batchId", "0"));
			parameters.add(new BasicNameValuePair("instanceId", "0"));

			parameters.add(new BasicNameValuePair("windowName", ""));
			parameters.add(new BasicNameValuePair("scriptSessionId", ""));

			if (requestContent != null && !requestContent.equals("")) {
				parameters.add(new BasicNameValuePair("c0-param0", "string:" + requestContent));
				Log.i(tag, "var data = " + requestContent);
			}
			
//			c0-e1=string:1
//			c0-e2=string:2
//			c0-e3=string:1
//			c0-e4=string:111
//			c0-e5=string:sss
			
//			c0-param0=Object_Object:{account:reference:c0-e1, name:reference:c0-e2, password:reference:c0-e3, tel:reference:c0-e4, email:reference:c0-e5}
			
			String dwr = url.substring(url.lastIndexOf("/") + 1);

			String[] array = dwr.split("\\.");

			String page = "/" + array[2] + "/test/" + array[0];

			String scriptName = array[0];

			String methodName = array[1];

			parameters.add(new BasicNameValuePair("page", page));
			parameters.add(new BasicNameValuePair("c0-scriptName", scriptName));
			parameters.add(new BasicNameValuePair("c0-methodName", methodName));

			UrlEncodedFormEntity encodedFormEntity = new UrlEncodedFormEntity(parameters, HTTP.UTF_8);

			post.setEntity(encodedFormEntity);// ֱ�������ַ�������

			HttpResponse resp = client.execute(post);

			int responseCode = resp.getStatusLine().getStatusCode();

			Log.i(tag, "--��Ӧ����--" + responseCode);

			if (responseCode == HttpStatus.SC_OK) {

				Log.i(tag, "------��Ӧ�ɹ�-------");

				HttpEntity entity = resp.getEntity();

				response = EntityUtils.toString(entity, "UTF-8");// ���ؽ��
				
				Log.i(tag, "response��"+response);
				
				String temp = Base64.decodeUnicode(response);// Unicodeװ��

				String temp1 = temp.substring(temp.indexOf("handleCallback"));

				result = temp1.substring(temp1.indexOf("(") + 9,temp1.indexOf(");"));

				Log.i(tag, "var data = " + result);

			} else {
				Log.i(tag, "------��Ӧʧ��-------");
				taskHandler.sendEmptyMessage(ON_ERROR_NO_RESPONSE);
				return null;
			}

		} catch (java.net.SocketTimeoutException ste) {
			Log.e(tag, "----SocketTimeoutException���ӳ�ʱ-----");
			taskHandler.sendEmptyMessage(ON_ERROR_CONNECT_TIMEOUT);
			return null;
		} catch (ConnectTimeoutException cte) {
			Log.e(tag, "----ConnectTimeoutException���ӳ�ʱ-----");
			taskHandler.sendEmptyMessage(ON_ERROR_CONNECT_TIMEOUT);
			return null;
		} catch (UnsupportedEncodingException uee) {
			Log.e(tag, "----ת�������쳣------");
			taskHandler.sendEmptyMessage(ON_ERROR);
			return null;
		} catch (IOException ioe) {
			Log.e(tag, "doInBackground Exception" + ioe.toString());
			taskHandler.sendEmptyMessage(ON_ERROR);
			return null;
		} catch (Exception e) {
			Log.e(tag, "doInBackground Exception" + e.toString());
			taskHandler.sendEmptyMessage(ON_ERROR);
			return null;
		} finally {
			if (client != null) {
				client.getConnectionManager().shutdown();// �ر�
				client = null;
			}
		}
		return result;
	}

	protected void onPostExecute(String re) {
		super.onPostExecute(result);
		boolean flag1 = re != null && !re.equals("") && re.startsWith("{")
				&& re.endsWith("}");
		boolean flag2 = re != null && !re.equals("") && re.startsWith("[")
				&& re.endsWith("]");
		if (flag1 || flag2) {
			this.result = re;
			this.taskHandler.sendEmptyMessage(ON_SUCCESS);
		} else {
			this.result = re;
			this.taskHandler.sendEmptyMessage(ON_ERROR_NO_RESPONSE);
		}
	}

	protected void onPreExecute() {
		super.onPreExecute();
	}

	protected void onProgressUpdate(Integer... values) {
		super.onProgressUpdate(values);
	}

	/**
	 * post
	 * 
	 * �������޲�������
	 * 
	 * ������Ա:liuzhimin
	 * 
	 * ����ʱ��:-7-1
	 * 
	 * @param url
	 * @param callBack
	 */
	public static void post(String url, CallBack callBack) {
		DWRHttpAsyncTask asyncTask = new DWRHttpAsyncTask(callBack);
		String[] param = { url, null };
		asyncTask.execute(param);
	}

	/**
	 * post
	 * 
	 * ��������json��ʽ���ַ���Ϊ��������
	 * 
	 * ������Ա:liuzhimin
	 * 
	 * ����ʱ��:-7-17
	 * 
	 * @param url
	 * @param params
	 * @param callBack
	 */
	public static void post(String url, String params, CallBack callBack) {
		DWRHttpAsyncTask asyncTask = new DWRHttpAsyncTask(callBack);
		String[] param = { url, params };
		asyncTask.execute(param);
	}
	
	public static void post(String url, Map map, CallBack callBack) {
		DWRHttpAsyncTask asyncTask = new DWRHttpAsyncTask(callBack);
		String[] param = { url, null };
		asyncTask.execute(param);
	}

	/**
	 * ������CallBack
	 * 
	 * ������com.example.http
	 * 
	 * �������ص�
	 * 
	 * �����汾�ţ�
	 * 
	 * ������Ա�� liuzhimin
	 * 
	 * �������� �� -7-1
	 */
	public interface CallBack {

		/**
		 * onSuccess
		 * 
		 * ������
		 * 
		 * ������Ա:liuzhimin
		 * 
		 * ����ʱ��:-7-1
		 * 
		 * @param result
		 */
		public void onSuccess(String result);

		/**
		 * onError
		 * 
		 * ������ �������
		 * 
		 * ������Ա:liuzhimin
		 * 
		 * ����ʱ��:-6-30
		 * 
		 * @param msg
		 *            ���������Ϣ
		 */
		public void onError(String msg);

	}

}
