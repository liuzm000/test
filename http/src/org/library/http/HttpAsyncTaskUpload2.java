package org.library.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class HttpAsyncTaskUpload2 extends AsyncTask<String, Integer, String> {

	private final String tag = "HttpAsyncTaskUpload";
	private Context context;
	private String filePath;
	private long totalSize;
	private CallBack callBack;

	/** 返回结果 */
	private String result = null;

	private Map<String, String> files;
	private Map<String, String> values;
	private boolean isZIP = false, isBase64 = false;

	/**
	 * 超时时间
	 */
	private final int TIMEOUT = 60 * 1000;

	/** 成功 */
	private final int ON_SUCCESS = 0x7f070000;

	/** 无响应 */
	private final int ON_ERROR_NO_RESPONSE = 0x7f070002;

	private final int RESPONSE_ERROR = 0x7f070009;

	/** 连接超时 */
	private final int ON_ERROR_CONNECT_TIMEOUT = 0x7f070003;

	/** 未知出错或失败 */
	private final int ON_ERROR = 0x7f070004;

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

	public HttpAsyncTaskUpload2(Context context) {
		this.context = context;
	}

	public HttpAsyncTaskUpload2(Context context, CallBack callBack,
			Map<String, String> files, Map<String, String> values) {
		this.context = context;
		this.callBack = callBack;
		this.files = files;
		this.values = values;
	}

	public HttpAsyncTaskUpload2(Context context, CallBack callBack,
			Map<String, String> files, Map<String, String> values, boolean isZIP) {
		this.context = context;
		this.callBack = callBack;
		this.files = files;
		this.values = values;
		this.isZIP = isZIP;
	}

	public HttpAsyncTaskUpload2(Context context, CallBack callBack,
			Map<String, String> files, Map<String, String> values,
			boolean isZIP, boolean isBase64) {
		this.context = context;
		this.callBack = callBack;
		this.files = files;
		this.values = values;
		this.isZIP = isZIP;
		this.isBase64 = isBase64;
	}

	public HttpAsyncTaskUpload2(Context context, CallBack callBack,
			Map<String, String> files, boolean isBase64) {
		this.context = context;
		this.callBack = callBack;
		this.files = files;
		this.isBase64 = isBase64;
	}

	boolean f = true;

	protected String doInBackground(String... params) {

		String result = null;
		String url = params[0];
		filePath = params[1];
		String sourceFilePath = filePath;
		String zipFilePath = params[2];
		String fileName = "fileName";

		String zip = zipFilePath + fileName + ".zip";

		String targetURL = null;// TODO 指定URL

		File targetFile = null;// TODO 指定上传文件

		targetURL = url; // servleturl

		try {

			FileToZip.fileToZip(sourceFilePath, zipFilePath, fileName);

			// 注意：要载入commons-httpclient-3.1.jar commons-codec.jar
			// commons-logging.jar这三个包

			targetFile = new File(zip);

			PostMethod filePost = new PostMethod(targetURL);

			// 通过以下方法可以模拟页面参数提交
			// filePost.setParameter("name", "中文");
			// filePost.setParameter("pass", "1234");

			// filePost.setRequestBody("");

			// 1. javadoc中说： 不推荐使用。使用setRequestEntity（RequestEntity）
			// RequestEntity有很多
			// 1、ByteArrayRequestEntity，
			// 2、FileRequestEntity，
			// 3、InputStreamRequestEntity，
			// 4、MultipartRequestEntity，
			// 5、StringRequestEntity
			// 使用适合你的人：
			// 如果XML是一个String，使用StringRequestEntity如果是在一个文件中，使用FileRequestEntity等。
			// 例如，post.setRequestEntity( new StringRequestEntity( xml ) );

			Part[] parts = { new FilePart(targetFile.getName(), targetFile) };

			// use StringRequestEntity(String content, String contentType ,
			// String charset) instead
			// filePost.setRequestEntity(new
			// StringRequestEntity("{name:'tome'}","json","utf-8"));

			filePost.setRequestEntity(new MultipartRequestEntity(parts,
					filePost.getParams()));

			org.apache.commons.httpclient.HttpClient client = new org.apache.commons.httpclient.HttpClient();

			client.getHttpConnectionManager().getParams()
					.setConnectionTimeout(10 * 1000);

			int status = client.executeMethod(filePost);

			boolean upload = status == HttpStatus.SC_OK;

			if (upload) {
				Log.i(tag, "------响应成功-------");
				result = "success";
				return result;
			} else {
				Log.i(tag, "------响应失败-------");
				taskHandler.sendEmptyMessage(RESPONSE_ERROR);
				return null;
			}

		} catch (ConnectTimeoutException cte) {
			Log.e(tag, "----ConnectTimeoutException连接超时-----");
			taskHandler.sendEmptyMessage(ON_ERROR_CONNECT_TIMEOUT);
			return null;
		} catch (IOException ioe) {
			Log.e(tag, "doInBackground Exception" + ioe.toString());
			taskHandler.sendEmptyMessage(ON_ERROR);
			return null;
		} finally {
		}
	}

	protected void onPreExecute() {
		this.callBack.onStart();
	}

	protected void onProgressUpdate(Integer... progress) {
	}

	protected void onPostExecute(String re) {
		super.onPostExecute(result);
		if (re != null) {
			this.result = re;
			this.taskHandler.sendEmptyMessage(ON_SUCCESS);
		} else {
			this.result = re;
			this.taskHandler.sendEmptyMessage(RESPONSE_ERROR);
		}
	}

	protected void onCancelled() {
	}

	public interface CallBack {

		public void onStart();

		public void onSuccess(String result);

		public void onError(String msg);

		public void uploading(int progress);

	}

	public String main1(String url_, InputStream input) throws Exception {
		// Configure and open a connection to the site you will send the
		// request
		URL url = new URL(url_);
		URLConnection urlConnection = url.openConnection();
		// 设置doOutput属性为true表示将使用此urlConnection写入数据
		urlConnection.setDoOutput(true);
		// 定义待写入数据的内容类型，我们设置为application/x-www-form-urlencoded类型
		urlConnection.setRequestProperty("content-type",
				"application/x-www-form-urlencoded");
		// 得到请求的输出流对象
		OutputStreamWriter out = new OutputStreamWriter(
				urlConnection.getOutputStream());

		// 把数据写入请求的Body
		// out.write("message = Hello World chszs");

		IOUtils.copy(input, out);

		out.flush();
		out.close();

		// 从服务器读取响应
		InputStream inputStream = urlConnection.getInputStream();
		String encoding = urlConnection.getContentEncoding();
		String body = IOUtils.toString(inputStream, encoding);
		return body;

	}

	private InputStream getInputStream(String s) throws Exception {
		File f = new File(s);
		InputStream in = new FileInputStream(f);
		byte b[] = new byte[1024];
		int len = 0;
		int temp = 0; // 所有读取的内容都使用temp接收
		while ((temp = in.read()) != -1) { // 当没有读取完时，继续读取
			b[len] = (byte) temp;
			len++;
		}
		return in;
	}

	public boolean uploadFile(String filePath, String url) throws Exception {

		// 注意：要载入commons-httpclient-3.1.jar commons-codec.jar
		// commons-logging.jar这三个包

		String targetURL = null;// TODO 指定URL

		File targetFile = null;// TODO 指定上传文件

		targetFile = new File(filePath);
		targetURL = url; // servleturl
		PostMethod filePost = new PostMethod(targetURL);

		// 通过以下方法可以模拟页面参数提交
		// filePost.setParameter("name", "中文");
		// filePost.setParameter("pass", "1234");

		Part[] parts = { new FilePart(targetFile.getName(), targetFile) };

		filePost.setRequestEntity(new MultipartRequestEntity(parts, filePost
				.getParams()));

		org.apache.commons.httpclient.HttpClient client = new org.apache.commons.httpclient.HttpClient();

		client.getHttpConnectionManager().getParams()
				.setConnectionTimeout(5000);

		int status = client.executeMethod(filePost);

		return status == HttpStatus.SC_OK;

	}
}
