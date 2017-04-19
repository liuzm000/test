package org.library.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

import org.apache.http.conn.ConnectTimeoutException;

import android.app.ActivityManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.text.format.Formatter;
import android.util.Log;

/**
 * ������HttpDownloadAsyncTask
 * 
 * ������com.example.http
 * 	http://www.liaohuqiu.net/cn/posts/storage-in-android/
 * ������
 * 
 * �����汾�ţ�
 * 
 * ������Ա�� liuzhimin
 * 
 * �������� �� 2014-7-17
 */
public class HttpDownloadAsyncTask extends AsyncTask<String, Integer, String> {

	/** log��ʶ */
	private final String tag = "HttpDownloadAsyncTask";
	/**
	 * ��������������
	 */
	private int alreadyDownloadSize;

	/** ���ӳ�ʱ */
	private final int TIME_OUT = 0x7f040011;

	/** ��Ӧʧ�� */
	private final int NO_RESPONSE = 0x7f040012;

	/** �ļ������쳣 */
	private final int ON_IO_ERROR = 0x7f040013;

	/** �洢�ռ䲻�� */
	private final int NO_SPACE = 0x7f040014;

	/** ���سɹ� */
	private final int ON_SUCCESS = 0x7f040015;

	/** ���ص�ַ�쳣 */
	private final int ON_URL_ERROR = 0x7f040019;
	
	private final int ON_STOP = 0x7f040059;

	/** ����ʧ�� */
	private final int DOWNLOAD_ERROR = 0x7f040029;

	/** �ص� */
	private CallBack callBack = null;

	/** �ص���� */
	private String result = null;

	/** ��������ʱ */
	private final int connect_time_out = 150 * 1000;

	/** ��ȡ��ʱ */
	private final int read_time_out = 150 * 1000;

	/** ������ */
	private Context context = null;

	/** �����ļ���С */
	private int fileSize;

	final int CANCEL = 0x7f041029;

	final int DOINBACKGROUND = 0x7f241029;

	/** handler */
	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			Log.i(tag, " handleMessage ");
			switch (msg.what) {
			case ON_STOP:
				callBack.onError("10010");
			case TIME_OUT:
				callBack.onError("���ӳ�ʱ.");
				break;
			case NO_RESPONSE:
				callBack.onError("������Ӧʧ��.");
				break;
			case ON_IO_ERROR:
				callBack.onError("�ļ������쳣.");
				break;
			case NO_SPACE:
				callBack.onError("�洢�ռ䲻��.");
				break;
			case ON_URL_ERROR:
				callBack.onError("���ص�ַ�쳣.");
				break;
			case DOWNLOAD_ERROR:
				callBack.onError("����ʧ��.");
				break;
			case ON_SUCCESS:
				callBack.onSuccess(result);
				break;
			}
		};
	};

	protected String doInBackground(String... params) {
		Log.i(tag, "doInBackground time:"+System.currentTimeMillis());
		HttpURLConnection conn = null;
		int code = 0;
		InputStream is = null;
		FileOutputStream fos = null;
		String contentType = null;

		try {

			File file = new File(params[1]);
			File parent = file.getParentFile();
			if (!parent.exists()) {
				parent.mkdirs();
			}
			if (file.exists()) {// ɾ���Ѵ��ڵ��ļ�
				file.delete();
			}
			conn = (HttpURLConnection) new URL(params[0]).openConnection();
			conn.setConnectTimeout(connect_time_out);// ���ӳ�ʱ
			conn.setReadTimeout(read_time_out);// ��ȡ��ʱ
			// Ҫȡ�ó�����Ҫ��http����Ҫgzipѹ���������������£�Ҫ��Ȼ����length=-1�����
			conn.setRequestProperty("Accept-Encoding", "identity");
			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.connect();
			
			
			code = conn.getResponseCode();
			if (code == HttpURLConnection.HTTP_OK) {
				URL absUrl = conn.getURL();// �����ʵUrl
				Log.i(tag, "absUrl:" + absUrl.toString());
				fileSize = conn.getContentLength();
				String filename = conn.getHeaderField("Content-Disposition");
				// ͨ��Content-Disposition��ȡ�ļ����������������йأ���Ҫ����ͨ
				if (filename == null || filename.length() < 1) {
					filename = absUrl.getFile();
				}
				contentType = conn.getContentType();
				
				Log.i(tag, "contentType:" + contentType);
				Log.i(tag, "filename:" + filename);
				
				if (isMemoryEnough(fileSize)) {
					handler.sendEmptyMessage(NO_SPACE);
					return null;
				}
				
				Log.i(tag, "fileSize:" + Formatter.formatFileSize(context, fileSize));

				if (fileSize <= 0) {
					handler.sendEmptyMessage(DOWNLOAD_ERROR);
					return null;
				}

				fos = new FileOutputStream(file);
				is = conn.getInputStream();
				int temp = -1;
				byte[] bytes = new byte[1024];
				do {
					temp = is.read(bytes);
					fos.write(bytes, 0, temp);
					alreadyDownloadSize = alreadyDownloadSize + temp;
					publishProgress(alreadyDownloadSize);// ����ֵ.....
					if (alreadyDownloadSize >= fileSize) {
						// ��ֹ���ֽڼ������±�Խ������
						break;
					}
				} while (true);

				return (params[1]);// �����������ļ��ľ���·��

			} else {
				handler.sendEmptyMessage(NO_RESPONSE);
				return null;
			}
		} catch (java.net.MalformedURLException mue) {
			handler.sendEmptyMessage(TIME_OUT);
			mue.printStackTrace();
			return null;
		} catch (SocketTimeoutException ste) {
			handler.sendEmptyMessage(TIME_OUT);
			return null;
		} catch (ConnectTimeoutException cte) {
			handler.sendEmptyMessage(TIME_OUT);
			return null;
		} catch (ArrayIndexOutOfBoundsException ao) {
			handler.sendEmptyMessage(ON_IO_ERROR);
			// alreadyDownloadSize
			Log.i(tag, "ArrayIndexOutOfBoundsException:" + ao.getMessage());
			Log.i(tag, "alreadyDownloadSize:" + alreadyDownloadSize);
			return null;
		} catch (IOException ioe) {
			handler.sendEmptyMessage(ON_IO_ERROR);
			ioe.printStackTrace();
			return null;
		} finally {
			try {
				if (is != null) {
					is.close();
				}
				if (fos != null) {
					fos.close();
				}
			} catch (IOException e) {
				handler.sendEmptyMessage(ON_IO_ERROR);
				e.printStackTrace();
				return null;
			}
		}
	}

	protected void onPostExecute(String re) {
		super.onPostExecute(re);
		Log.i(tag, "----ִ�����----");// û��ִ�е���һ��
		if(re==null){
			return ;	
		}
		this.result = re;
		handler.sendEmptyMessage(ON_SUCCESS);
	}

	protected void onPreExecute() {
		super.onPreExecute();
		Log.i(tag, "onPreExecute time:"+System.currentTimeMillis());
	}

	protected void onProgressUpdate(Integer... values) {
		super.onProgressUpdate(values);
		double temp = (double) values[0] / fileSize;
		int progress = (int) (temp * 100);
		callBack.updateProgress(progress);
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
	 * �������� �� 2014-7-1
	 */
	public interface CallBack {

		/**
		 * ������:onSuccess
		 * 
		 * ������
		 * 
		 * ������Ա:liuzhimin
		 * 
		 * ����ʱ��:2014-7-1
		 * 
		 * @param result
		 */
		public void onSuccess(String result);

		/**
		 * ������:onError
		 * 
		 * ������ �������
		 * 
		 * ������Ա:liuzhimin
		 * 
		 * ����ʱ��:2014-6-30
		 * 
		 * @param msg
		 *            ���������Ϣ
		 */
		public void onError(String msg);

		
		public void updateProgress(int progress);
		
		
	}

	/**
	 * ������:isMemoryEnough
	 * 
	 * ������ �ж��ڴ��Ƿ����
	 * 
	 * ������Ա:liuzhimin
	 * 
	 * ����ʱ��:2014-7-17
	 * 
	 * @param fileSize
	 * @return
	 */
	private boolean isMemoryEnough(int fileSize) {
		// �õ�ActivityManager
		ActivityManager am = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		// ����ActivityManager.MemoryInfo����
		ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
		am.getMemoryInfo(mi);
		Log.i(tag, " ���̿��Կռ�  " + mi.availMem);
		return fileSize >= mi.availMem;
	}

	/**
	 * ������:ExistSDCard
	 * 
	 * ������ �ж�SD���Ƿ����
	 * 
	 * ������Ա:liuzhimin
	 * 
	 * ����ʱ��:2014-9-28
	 * 
	 * @return
	 */
	private boolean ExistSDCard() {
		return (android.os.Environment.getExternalStorageState()
				.equals(android.os.Environment.MEDIA_MOUNTED));
	}

	/**
	 * ������:getSDFreeSize
	 * 
	 * ������ �鿴SD����ʣ��ռ�
	 * 
	 * ������Ա:liuzhimin
	 * 
	 * ����ʱ��:2014-9-28
	 * 
	 * @return
	 */
	private long getSDFreeSize() {
		// ȡ��SD���ļ�·��
		File path = Environment.getExternalStorageDirectory();
		StatFs sf = new StatFs(path.getPath());
		// ��ȡ�������ݿ�Ĵ�С(Byte)
		long blockSize = sf.getBlockSize();
		// ���е����ݿ������
		long freeBlocks = sf.getAvailableBlocks();
		// ����SD�����д�С
		// return freeBlocks * blockSize; //��λByte
		// return (freeBlocks * blockSize)/1024; //��λKB
		return (freeBlocks * blockSize) / 1024 / 1024; // ��λMB
	}

	/**
	 * ������:getSDAllSize
	 * 
	 * �������鿴SD��������
	 * 
	 * ������Ա:liuzhimin
	 * 
	 * ����ʱ��:2014-9-28
	 * 
	 * @return
	 */
	private long getSDAllSize() {
		// ȡ��SD���ļ�·��
		File path = Environment.getExternalStorageDirectory();
		StatFs sf = new StatFs(path.getPath());
		// ��ȡ�������ݿ�Ĵ�С(Byte)
		long blockSize = sf.getBlockSize();
		// ��ȡ�������ݿ���
		long allBlocks = sf.getBlockCount();
		// ����SD����С
		// return allBlocks * blockSize; //��λByte
		// return (allBlocks * blockSize)/1024; //��λKB
		return (allBlocks * blockSize) / 1024 / 1024; // ��λMB
	}

	public HttpDownloadAsyncTask(CallBack callBack, Context context,
			String fileName) {
		super();
		this.callBack = callBack;
		this.context = context;
		Log.i(tag, "HttpDownloadAsyncTask time:"+System.currentTimeMillis());
	}

	public static void download(CallBack callBack, String url, Context context,
			String fileName) {
		HttpDownloadAsyncTask asyncTask = new HttpDownloadAsyncTask(callBack,
				context, fileName);
		String[] params = { url, fileName };
		asyncTask.execute(params);
	}
}
