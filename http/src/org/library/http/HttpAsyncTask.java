package org.library.http;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.xmlpull.v1.XmlPullParser;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.util.Xml;

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
public class HttpAsyncTask extends AsyncTask<String, Integer, String> {

	/**
	 * �������
	 */
	private int requestCount = 0;

	/**
	 * ��ʱʱ��
	 */
	private final int TIMEOUT = 15 * 1000;

	/**
	 * log��ʶ
	 */
	private final String tag = "HttpAsyncTask";

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
	private HttpAsyncTask(Map<String, String> map, CallBack callBack) {
		super();
		this.map = map;
		this.callBack = callBack;
	}

	/**
	 * @param callBack
	 */
	private HttpAsyncTask(CallBack callBack) {
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
				callBack.onError("��������Ӧ�����Ժ�����.");
				break;
			case ON_ERROR_CONNECT_TIMEOUT:
				callBack.onError("�������ӳ�ʱ�����Ժ�����.");
				break;
			}
		}

	};

	protected String doInBackground(String... params) {

		Log.i(tag, "---doInBackground---");

		requestCount++;// �����������

		Log.i(tag, "---�������:" + requestCount);

		String url = params[0];
		Log.i(tag, "�����ַ:" + url);

		String requestContent = params[1];// ������Խ��
		Log.i(tag, "var data = " + requestContent);

		DefaultHttpClient client = null;
		HttpPost post = null;
		HttpParams httpParameters = null;
		String response = null;

		httpParameters = new BasicHttpParams();

		HttpConnectionParams.setConnectionTimeout(httpParameters, TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpParameters, TIMEOUT);

		// HttpConnectionParams.setTcpNoDelay(httpParameters, true);
		// HttpConnectionParams.setSocketBufferSize(httpParameters, 1024 * 8);
		// HttpProtocolParams.setVersion(httpParameters, HttpVersion.HTTP_1_1);

		client = new DefaultHttpClient(httpParameters);

		post = new HttpPost(url);

		String requsestBase = null, result = null;
		StringEntity se = null;

		byte[] encode, decode;

		try {

			// ͨ��json�ַ������Ͳ���
			if (requestContent != null && !requestContent.equals("")) {
				if (isBase64) {
					// encode =
					// Base64.encode(requestContent.getBytes(HTTP.UTF_8));
					// �������Base64���ܴ�����s
					requsestBase = android.util.Base64.encodeToString(
							requestContent.getBytes(HTTP.UTF_8),
							android.util.Base64.DEFAULT);
					se = new StringEntity(requsestBase, HTTP.UTF_8);
				} else {
					se = new StringEntity(requestContent, HTTP.UTF_8);
				}
				se.setContentType("application/json");
				post.setEntity(se);// ֱ�������ַ�������
			}

			// ͨ����ֵ�Է��Ͳ���
			if (this.map != null && this.map.size() > 0) {
				post.setEntity(new UrlEncodedFormEntity(getParameter(map),
						"UTF-8"));
			}

			HttpResponse resp = client.execute(post);

			resp.addHeader("", "");

			int responseCode = resp.getStatusLine().getStatusCode();

			Log.i(tag, "--��Ӧ����--" + responseCode);

			if (responseCode == HttpStatus.SC_OK) {

				Log.i(tag, "------��Ӧ�ɹ�-------");

				HttpEntity entity = resp.getEntity();

				response = EntityUtils.toString(entity, "UTF-8");// ���ؽ��

				if (isBase64) {
					decode = android.util.Base64.decode(response,
							Base64.DEFAULT);
					// �����ؽ��������
					result = new String(decode);
				} else {
					result = response;
				}

				Log.i(tag, "var data = " + result);

				return result;

			} else {
				Log.i(tag, "------��Ӧʧ��-------");
				// ���ʧ�ܵĻ�������1��
				if (requestCount > 2) {
					taskHandler.sendEmptyMessage(ON_ERROR_NO_RESPONSE);
					return null;
				} else {
					return doInBackground(params);
				}
			}

		} catch (java.net.SocketTimeoutException ste) {
			Log.e(tag, "----SocketTimeoutException���ӳ�ʱ-----");
			if (requestCount > 2) {
				taskHandler.sendEmptyMessage(ON_ERROR_CONNECT_TIMEOUT);
				return null;
			} else {
				return doInBackground(params);
			}
		} catch (ConnectTimeoutException cte) {
			Log.e(tag, "----ConnectTimeoutException���ӳ�ʱ-----");
			if (requestCount > 2) {
				taskHandler.sendEmptyMessage(ON_ERROR_CONNECT_TIMEOUT);
				return null;
			} else {
				return doInBackground(params);
			}
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
		// return result;
	}

	protected void onPostExecute(String re) {
		super.onPostExecute(result);
		if (re != null && !re.equals("") && re.startsWith("{")
				&& re.endsWith("}")) {
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

	private String parseXML(String xml) {
		XmlPullParser parser = Xml.newPullParser();
		String url = "";
		try {
			parser.setInput(new StringReader(xml));
			int eventType = parser.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
				switch (eventType) {

				case XmlPullParser.START_DOCUMENT:// �ĵ���ʼ�¼�,���Խ������ݳ�ʼ������
					break;
				case XmlPullParser.START_TAG:// ��ʼԪ���¼�
					String tag = parser.getName();
					Log.i("CDCLogs", tag);
					if (tag.equals("urls")) {
						url = parser.getText();
					}
					break;
				case XmlPullParser.END_TAG:// ����Ԫ���¼�
					break;
				}
				eventType = parser.next();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return url;
	}

	/**
	 * getParameter
	 * 
	 * ������
	 * 
	 * ������Ա:liuzhimin
	 * 
	 * ����ʱ��:-6-26
	 * 
	 * @param map
	 * @return
	 */
	private List<NameValuePair> getParameter(Map<String, String> map) {
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		Set<String> set = map.keySet();
		for (String key : set) {
			NameValuePair name = new BasicNameValuePair(key, map.get(key));
			nameValuePairs.add(name);
		}
		return nameValuePairs;
	}

	/**
	 * post
	 * 
	 * �������Ѽ�ֵ�Ե���ʽ����
	 * 
	 * ������Ա:liuzhimin
	 * 
	 * ����ʱ��:-7-1
	 * 
	 * @param url
	 * @param map
	 * @param callBack
	 */
	public static void post(String url, Map<String, String> map,
			CallBack callBack) {
		HttpAsyncTask asyncTask = new HttpAsyncTask(map, callBack);
		String[] param = { url, null };
		asyncTask.execute(param);
	}

	public static void post(String url, Map<String, String> map,
			CallBack callBack, boolean isBase64) {
		HttpAsyncTask asyncTask = new HttpAsyncTask(map, callBack);
		asyncTask.isBase64 = isBase64;
		String[] param = { url, null };
		asyncTask.execute(param);
	}
	
	public static void postWithHeader(String url, String json,
			CallBack callBack, Map<String, String> map) {
		HttpAsyncTask asyncTask = new HttpAsyncTask(map, callBack);
		asyncTask.isBase64 = false;
		String[] param = { url, null };
		asyncTask.execute(param);
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
		HttpAsyncTask asyncTask = new HttpAsyncTask(callBack);
		String[] param = { url, null };
		asyncTask.execute(param);
	}

	public static void post(String url, CallBack callBack, boolean isBase64) {
		HttpAsyncTask asyncTask = new HttpAsyncTask(callBack);
		asyncTask.isBase64 = isBase64;
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
		HttpAsyncTask asyncTask = new HttpAsyncTask(callBack);
		String[] param = { url, params };
		asyncTask.execute(param);
	}

	public static void post(String url, String params, CallBack callBack,
			boolean isBase64) {
		HttpAsyncTask asyncTask = new HttpAsyncTask(callBack);
		asyncTask.isBase64 = isBase64;
		String[] param = { url, params };
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

	private void addRequestInterceptor(DefaultHttpClient httpClient) {
		httpClient.addRequestInterceptor(new HttpRequestInterceptor() {
			public void process(HttpRequest httpRequest, HttpContext httpContext)
					throws HttpException, IOException {
				Log.i(tag, "addRequestInterceptor");
			}
		});
	}

	private void addResponseInterceptor(DefaultHttpClient httpClient) {
		httpClient.addResponseInterceptor(new HttpResponseInterceptor() {
			public void process(HttpResponse response, HttpContext httpContext)
					throws HttpException, IOException {
				Log.i(tag, "addResponseInterceptor");
				final HttpEntity entity = response.getEntity();
				if (entity == null) {
					return;
				}
				final Header encoding = entity.getContentEncoding();
				if (encoding != null) {
					for (HeaderElement element : encoding.getElements()) {
						if (element.getName().equalsIgnoreCase("gzip")) {
							// response.setEntity(new
							// GZipDecompressingEntity(response.getEntity()));
							return;
						}
					}
				}
			}
		});
	}
}
