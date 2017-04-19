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

	/** ���ؽ�� */
	private String result = null;

	private Map<String, String> files;
	private Map<String, String> values;
	private boolean isZIP = false, isBase64 = false;

	/**
	 * ��ʱʱ��
	 */
	private final int TIMEOUT = 60 * 1000;

	/** �ɹ� */
	private final int ON_SUCCESS = 0x7f070000;

	/** ����Ӧ */
	private final int ON_ERROR_NO_RESPONSE = 0x7f070002;

	private final int RESPONSE_ERROR = 0x7f070009;

	/** ���ӳ�ʱ */
	private final int ON_ERROR_CONNECT_TIMEOUT = 0x7f070003;

	/** δ֪�����ʧ�� */
	private final int ON_ERROR = 0x7f070004;

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

		String targetURL = null;// TODO ָ��URL

		File targetFile = null;// TODO ָ���ϴ��ļ�

		targetURL = url; // servleturl

		try {

			FileToZip.fileToZip(sourceFilePath, zipFilePath, fileName);

			// ע�⣺Ҫ����commons-httpclient-3.1.jar commons-codec.jar
			// commons-logging.jar��������

			targetFile = new File(zip);

			PostMethod filePost = new PostMethod(targetURL);

			// ͨ�����·�������ģ��ҳ������ύ
			// filePost.setParameter("name", "����");
			// filePost.setParameter("pass", "1234");

			// filePost.setRequestBody("");

			// 1. javadoc��˵�� ���Ƽ�ʹ�á�ʹ��setRequestEntity��RequestEntity��
			// RequestEntity�кܶ�
			// 1��ByteArrayRequestEntity��
			// 2��FileRequestEntity��
			// 3��InputStreamRequestEntity��
			// 4��MultipartRequestEntity��
			// 5��StringRequestEntity
			// ʹ���ʺ�����ˣ�
			// ���XML��һ��String��ʹ��StringRequestEntity�������һ���ļ��У�ʹ��FileRequestEntity�ȡ�
			// ���磬post.setRequestEntity( new StringRequestEntity( xml ) );

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
				Log.i(tag, "------��Ӧ�ɹ�-------");
				result = "success";
				return result;
			} else {
				Log.i(tag, "------��Ӧʧ��-------");
				taskHandler.sendEmptyMessage(RESPONSE_ERROR);
				return null;
			}

		} catch (ConnectTimeoutException cte) {
			Log.e(tag, "----ConnectTimeoutException���ӳ�ʱ-----");
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
		// ����doOutput����Ϊtrue��ʾ��ʹ�ô�urlConnectionд������
		urlConnection.setDoOutput(true);
		// �����д�����ݵ��������ͣ���������Ϊapplication/x-www-form-urlencoded����
		urlConnection.setRequestProperty("content-type",
				"application/x-www-form-urlencoded");
		// �õ���������������
		OutputStreamWriter out = new OutputStreamWriter(
				urlConnection.getOutputStream());

		// ������д�������Body
		// out.write("message = Hello World chszs");

		IOUtils.copy(input, out);

		out.flush();
		out.close();

		// �ӷ�������ȡ��Ӧ
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
		int temp = 0; // ���ж�ȡ�����ݶ�ʹ��temp����
		while ((temp = in.read()) != -1) { // ��û�ж�ȡ��ʱ��������ȡ
			b[len] = (byte) temp;
			len++;
		}
		return in;
	}

	public boolean uploadFile(String filePath, String url) throws Exception {

		// ע�⣺Ҫ����commons-httpclient-3.1.jar commons-codec.jar
		// commons-logging.jar��������

		String targetURL = null;// TODO ָ��URL

		File targetFile = null;// TODO ָ���ϴ��ļ�

		targetFile = new File(filePath);
		targetURL = url; // servleturl
		PostMethod filePost = new PostMethod(targetURL);

		// ͨ�����·�������ģ��ҳ������ύ
		// filePost.setParameter("name", "����");
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
