package org.library.http;

import java.io.IOException;
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
import org.apache.http.client.methods.HttpGet;
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
public class HttpGetAsyncTask extends AsyncTask<String, Integer, String> {

	/**
	 * 请求次数
	 */
	private int requestCount = 3;

	/**
	 * 超时时间
	 */
	private final int TIMEOUT = 15 * 1000;

	/**
	 * log标识
	 */
	private final String tag = "HttpAsyncTask";

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

	public boolean isBase64 = false;

	/**
	 * @param map
	 * @param callBack
	 */
	private HttpGetAsyncTask(Map<String, String> map, CallBack callBack) {
		super();
		this.map = map;
		this.callBack = callBack;
	}

	/**
	 * @param callBack
	 */
	private HttpGetAsyncTask(CallBack callBack) {
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
				callBack.onError("服务无响应！请稍后重试.");
				break;
			case ON_ERROR_CONNECT_TIMEOUT:
				callBack.onError("网络连接超时！请稍后重试.");
				break;
			}
		}

	};

	protected String doInBackground(String... params) {

		Log.i(tag, "---doInBackground---");

		requestCount++;// 自增请求次数

		Log.i(tag, "---请求次数:" + requestCount);

		String url = params[0];
		Log.i(tag, "请求地址:" + url);
		
		String requestContent = params[1];// 会数组越界
		Log.i(tag, "var data = " + requestContent);
		

		DefaultHttpClient client = null;
		HttpGet post = null;
		
		HttpParams httpParameters = null;
		String response = null;

		httpParameters = new BasicHttpParams();

		HttpConnectionParams.setConnectionTimeout(httpParameters, TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpParameters, TIMEOUT);

		// HttpConnectionParams.setTcpNoDelay(httpParameters, true);
		// HttpConnectionParams.setSocketBufferSize(httpParameters, 1024 * 8);
		// HttpProtocolParams.setVersion(httpParameters, HttpVersion.HTTP_1_1);

		client = new DefaultHttpClient(httpParameters);
		
		String requsestBase = null, result = null;
		StringEntity se = null;

		byte[] encode, decode;

		try {
			
			post = new HttpGet(url);
			

			HttpResponse resp = client.execute(post);

			int responseCode = resp.getStatusLine().getStatusCode();

			Log.i(tag, "--响应编码--" + responseCode);

			HttpEntity entity = resp.getEntity();
			
			response = EntityUtils.toString(entity, HTTP.UTF_8);// 返回结果
			
			if (responseCode == HttpStatus.SC_OK) {

				Log.i(tag, "------响应成功-------");

				if (isBase64) {
					decode = Base64.decode(response.getBytes(HTTP.UTF_8));
					// 将返回结果并解密
					result = new String(decode);
				} else {
					result = response;
				}

				Log.i(tag, "var data = " + result);
				
				return result;

			} else {

				Log.i(tag, "------响应失败-------");
				
				// 如果失败的话再请求1次
				if (requestCount > 2) {
					taskHandler.sendEmptyMessage(ON_ERROR_NO_RESPONSE);
					return null;
				} else {
					return doInBackground(params);
				}
			}

		} catch (java.net.SocketTimeoutException ste) {
			Log.e(tag, "----SocketTimeoutException连接超时-----");
			if (requestCount > 2) {
				taskHandler.sendEmptyMessage(ON_ERROR_CONNECT_TIMEOUT);
				return null;
			} else {
				return doInBackground(params);
			}
		} catch (ConnectTimeoutException cte) {
			Log.e(tag, "----ConnectTimeoutException连接超时-----");
			if (requestCount > 2) {
				taskHandler.sendEmptyMessage(ON_ERROR_CONNECT_TIMEOUT);
				return null;
			} else {
				return doInBackground(params);
			}
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
	}

	protected void onPostExecute(String re) {
		super.onPostExecute(result);
		if (re != null && !re.equals("")) {
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
	 * getParameter
	 * 
	 * 描述：
	 * 
	 * 开发人员:liuzhimin
	 * 
	 * 创建时间:-6-26
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
			Log.i(tag, key+"="+map.get(key));
		}
		return nameValuePairs;
	}

	/**
	 * post
	 * 
	 * 描述：已键值对的形式请求
	 * 
	 * 开发人员:liuzhimin
	 * 
	 * 创建时间:-7-1
	 * 
	 * @param url
	 * @param map
	 * @param callBack
	 */
	public static void post(String url, Map<String, String> map,
			CallBack callBack) {
		HttpGetAsyncTask asyncTask = new HttpGetAsyncTask(map, callBack);
		String[] param = { url, null };
		asyncTask.execute(param);
	}

	public static void post(String url, Map<String, String> map,
			CallBack callBack, boolean isBase64) {
		HttpGetAsyncTask asyncTask = new HttpGetAsyncTask(map, callBack);
		asyncTask.isBase64 = isBase64;
		String[] param = { url, null };
		asyncTask.execute(param);
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
	 * 
	 * 是因为空格的原因
	   url= url.replaceAll(" ", "%20");
	 * 
	 * @param url
	 * @param callBack
	 */
	public static void post(String url, CallBack callBack) {
		HttpGetAsyncTask asyncTask = new HttpGetAsyncTask(callBack);
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
		HttpGetAsyncTask asyncTask = new HttpGetAsyncTask(callBack);
		String[] param = { url, params };
		asyncTask.execute(param);
	}

	public static void post(String url, String params, CallBack callBack,
			boolean isBase64) {
		HttpGetAsyncTask asyncTask = new HttpGetAsyncTask(callBack);
		asyncTask.isBase64 = isBase64;
		String[] param = { url, params };
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
