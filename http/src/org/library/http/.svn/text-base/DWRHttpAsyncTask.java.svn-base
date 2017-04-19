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
 * 类名：HttpAsyncTask
 * 
 * 包名：com.ordersystem.http
 * 
 * 描述：http异步请求类
 * 
 * 发布版本号：
 * 
 * 开发人员： liuzhimin
 * 
 * 创建日期 ： -6-20
 */
public class DWRHttpAsyncTask extends AsyncTask<String, Integer, String> {

	/**
	 * 请求次数
	 */
	private int requestCount = 0;

	/**
	 * 超时时间
	 */
	private final int TIMEOUT = 10 * 1000;

	/**
	 * log标识
	 */
	private final String tag = "DWRHttpAsyncTask";

	/** 键值对参数 */
	private Map<String, String> map;

	/** 成功 */
	private final int ON_SUCCESS = 0x7f070000;

	/** 无响应 */
	private final int ON_ERROR_NO_RESPONSE = 0x7f070002;

	/** 连接超时 */
	private final int ON_ERROR_CONNECT_TIMEOUT = 0x7f070003;

	/** 出错或失败 */
	private final int ON_ERROR = 0x7f070004;

	/** 返回结果 */
	private String result = null;

	/** 回调 */
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
				callBack.onError("服务响应错误！请稍后重试.");
				break;
			case ON_ERROR_NO_RESPONSE:
				callBack.onError("服务响应失败！请稍后重试.");
				break;
			case ON_ERROR_CONNECT_TIMEOUT:
				callBack.onError("网络连接超时！请稍后重试.");
				break;
			}
		}

	};

	protected String doInBackground(String... params) {

		requestCount++;// 自增请求次数

		String url = params[0];
		Log.i(tag, "请求地址:" + url);

		String requestContent = params[1];// 会数组越界

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

			// 通过json字符流发送参数

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

			post.setEntity(encodedFormEntity);// 直接推送字符流参数

			HttpResponse resp = client.execute(post);

			int responseCode = resp.getStatusLine().getStatusCode();

			Log.i(tag, "--响应编码--" + responseCode);

			if (responseCode == HttpStatus.SC_OK) {

				Log.i(tag, "------响应成功-------");

				HttpEntity entity = resp.getEntity();

				response = EntityUtils.toString(entity, "UTF-8");// 返回结果
				
				Log.i(tag, "response："+response);
				
				String temp = Base64.decodeUnicode(response);// Unicode装换

				String temp1 = temp.substring(temp.indexOf("handleCallback"));

				result = temp1.substring(temp1.indexOf("(") + 9,temp1.indexOf(");"));

				Log.i(tag, "var data = " + result);

			} else {
				Log.i(tag, "------响应失败-------");
				taskHandler.sendEmptyMessage(ON_ERROR_NO_RESPONSE);
				return null;
			}

		} catch (java.net.SocketTimeoutException ste) {
			Log.e(tag, "----SocketTimeoutException连接超时-----");
			taskHandler.sendEmptyMessage(ON_ERROR_CONNECT_TIMEOUT);
			return null;
		} catch (ConnectTimeoutException cte) {
			Log.e(tag, "----ConnectTimeoutException连接超时-----");
			taskHandler.sendEmptyMessage(ON_ERROR_CONNECT_TIMEOUT);
			return null;
		} catch (UnsupportedEncodingException uee) {
			Log.e(tag, "----转换编码异常------");
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
				client.getConnectionManager().shutdown();// 关闭
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
	 * 描述：无参数请求
	 * 
	 * 开发人员:liuzhimin
	 * 
	 * 创建时间:-7-1
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
	 * 描述：以json格式的字符流为参数请求
	 * 
	 * 开发人员:liuzhimin
	 * 
	 * 创建时间:-7-17
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
	 * 类名：CallBack
	 * 
	 * 包名：com.example.http
	 * 
	 * 描述：回调
	 * 
	 * 发布版本号：
	 * 
	 * 开发人员： liuzhimin
	 * 
	 * 创建日期 ： -7-1
	 */
	public interface CallBack {

		/**
		 * onSuccess
		 * 
		 * 描述：
		 * 
		 * 开发人员:liuzhimin
		 * 
		 * 创建时间:-7-1
		 * 
		 * @param result
		 */
		public void onSuccess(String result);

		/**
		 * onError
		 * 
		 * 描述： 请求错误
		 * 
		 * 开发人员:liuzhimin
		 * 
		 * 创建时间:-6-30
		 * 
		 * @param msg
		 *            请求错误信息
		 */
		public void onError(String msg);

	}

}
