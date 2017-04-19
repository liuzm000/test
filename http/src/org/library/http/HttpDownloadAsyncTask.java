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
 * 类名：HttpDownloadAsyncTask
 * 
 * 包名：com.example.http
 * 	http://www.liaohuqiu.net/cn/posts/storage-in-android/
 * 描述：
 * 
 * 发布版本号：
 * 
 * 开发人员： liuzhimin
 * 
 * 创建日期 ： 2014-7-17
 */
public class HttpDownloadAsyncTask extends AsyncTask<String, Integer, String> {

	/** log标识 */
	private final String tag = "HttpDownloadAsyncTask";
	/**
	 * 描述：已下载数
	 */
	private int alreadyDownloadSize;

	/** 连接超时 */
	private final int TIME_OUT = 0x7f040011;

	/** 响应失败 */
	private final int NO_RESPONSE = 0x7f040012;

	/** 文件操作异常 */
	private final int ON_IO_ERROR = 0x7f040013;

	/** 存储空间不足 */
	private final int NO_SPACE = 0x7f040014;

	/** 下载成功 */
	private final int ON_SUCCESS = 0x7f040015;

	/** 下载地址异常 */
	private final int ON_URL_ERROR = 0x7f040019;
	
	private final int ON_STOP = 0x7f040059;

	/** 下载失败 */
	private final int DOWNLOAD_ERROR = 0x7f040029;

	/** 回调 */
	private CallBack callBack = null;

	/** 回调结果 */
	private String result = null;

	/** 描述：超时 */
	private final int connect_time_out = 150 * 1000;

	/** 读取超时 */
	private final int read_time_out = 150 * 1000;

	/** 上下文 */
	private Context context = null;

	/** 下载文件大小 */
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
				callBack.onError("连接超时.");
				break;
			case NO_RESPONSE:
				callBack.onError("服务响应失败.");
				break;
			case ON_IO_ERROR:
				callBack.onError("文件操作异常.");
				break;
			case NO_SPACE:
				callBack.onError("存储空间不足.");
				break;
			case ON_URL_ERROR:
				callBack.onError("下载地址异常.");
				break;
			case DOWNLOAD_ERROR:
				callBack.onError("下载失败.");
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
			if (file.exists()) {// 删除已存在的文件
				file.delete();
			}
			conn = (HttpURLConnection) new URL(params[0]).openConnection();
			conn.setConnectTimeout(connect_time_out);// 连接超时
			conn.setReadTimeout(read_time_out);// 读取超时
			// 要取得长度则，要求http请求不要gzip压缩，具体设置如下，要不然出现length=-1的情况
			conn.setRequestProperty("Accept-Encoding", "identity");
			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.connect();
			
			
			code = conn.getResponseCode();
			if (code == HttpURLConnection.HTTP_OK) {
				URL absUrl = conn.getURL();// 获得真实Url
				Log.i(tag, "absUrl:" + absUrl.toString());
				fileSize = conn.getContentLength();
				String filename = conn.getHeaderField("Content-Disposition");
				// 通过Content-Disposition获取文件名，这点跟服务器有关，需要灵活变通
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
					publishProgress(alreadyDownloadSize);// 发出值.....
					if (alreadyDownloadSize >= fileSize) {
						// 防止丢字节及数字下标越界的情况
						break;
					}
				} while (true);

				return (params[1]);// 返回已下载文件的绝对路径

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
		Log.i(tag, "----执行完成----");// 没有执行到这一步
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
	 * 创建日期 ： 2014-7-1
	 */
	public interface CallBack {

		/**
		 * 方法名:onSuccess
		 * 
		 * 描述：
		 * 
		 * 开发人员:liuzhimin
		 * 
		 * 创建时间:2014-7-1
		 * 
		 * @param result
		 */
		public void onSuccess(String result);

		/**
		 * 方法名:onError
		 * 
		 * 描述： 请求错误
		 * 
		 * 开发人员:liuzhimin
		 * 
		 * 创建时间:2014-6-30
		 * 
		 * @param msg
		 *            请求错误信息
		 */
		public void onError(String msg);

		
		public void updateProgress(int progress);
		
		
	}

	/**
	 * 方法名:isMemoryEnough
	 * 
	 * 描述： 判断内存是否充足
	 * 
	 * 开发人员:liuzhimin
	 * 
	 * 创建时间:2014-7-17
	 * 
	 * @param fileSize
	 * @return
	 */
	private boolean isMemoryEnough(int fileSize) {
		// 得到ActivityManager
		ActivityManager am = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		// 创建ActivityManager.MemoryInfo对象
		ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
		am.getMemoryInfo(mi);
		Log.i(tag, " 磁盘可以空间  " + mi.availMem);
		return fileSize >= mi.availMem;
	}

	/**
	 * 方法名:ExistSDCard
	 * 
	 * 描述： 判断SD卡是否存在
	 * 
	 * 开发人员:liuzhimin
	 * 
	 * 创建时间:2014-9-28
	 * 
	 * @return
	 */
	private boolean ExistSDCard() {
		return (android.os.Environment.getExternalStorageState()
				.equals(android.os.Environment.MEDIA_MOUNTED));
	}

	/**
	 * 方法名:getSDFreeSize
	 * 
	 * 描述： 查看SD卡的剩余空间
	 * 
	 * 开发人员:liuzhimin
	 * 
	 * 创建时间:2014-9-28
	 * 
	 * @return
	 */
	private long getSDFreeSize() {
		// 取得SD卡文件路径
		File path = Environment.getExternalStorageDirectory();
		StatFs sf = new StatFs(path.getPath());
		// 获取单个数据块的大小(Byte)
		long blockSize = sf.getBlockSize();
		// 空闲的数据块的数量
		long freeBlocks = sf.getAvailableBlocks();
		// 返回SD卡空闲大小
		// return freeBlocks * blockSize; //单位Byte
		// return (freeBlocks * blockSize)/1024; //单位KB
		return (freeBlocks * blockSize) / 1024 / 1024; // 单位MB
	}

	/**
	 * 方法名:getSDAllSize
	 * 
	 * 描述：查看SD卡总容量
	 * 
	 * 开发人员:liuzhimin
	 * 
	 * 创建时间:2014-9-28
	 * 
	 * @return
	 */
	private long getSDAllSize() {
		// 取得SD卡文件路径
		File path = Environment.getExternalStorageDirectory();
		StatFs sf = new StatFs(path.getPath());
		// 获取单个数据块的大小(Byte)
		long blockSize = sf.getBlockSize();
		// 获取所有数据块数
		long allBlocks = sf.getBlockCount();
		// 返回SD卡大小
		// return allBlocks * blockSize; //单位Byte
		// return (allBlocks * blockSize)/1024; //单位KB
		return (allBlocks * blockSize) / 1024 / 1024; // 单位MB
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
