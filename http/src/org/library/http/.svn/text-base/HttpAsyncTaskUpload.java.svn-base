package org.library.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.library.http.CustomMultiPartEntity.ProgressListener;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

public class HttpAsyncTaskUpload extends AsyncTask<String, Integer, String> {

	private final String tag = "HttpAsyncTaskUpload";
	private Context context;
	private long totalSize;
	private CallBack callBack;

	private boolean isBase64 = false; // 是否使用base64机密
	private boolean compressImage = false; // 是否压缩上传

	/** 返回结果 */
	private String result = null;

	private Map<String, String> files;

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

	public HttpAsyncTaskUpload(Context context) {
		this.context = context;
	}

	public HttpAsyncTaskUpload(Context context, CallBack callBack,
			Map<String, String> files) {
		this.context = context;
		this.callBack = callBack;
		this.files = files;
	}

	public HttpAsyncTaskUpload(Context context, CallBack callBack,
			Map<String, String> files, boolean isBase64) {
		this.context = context;
		this.callBack = callBack;
		this.files = files;
		this.isBase64 = isBase64;
	}

	public HttpAsyncTaskUpload(Context context, CallBack callBack,
			Map<String, String> files, boolean isBase64, boolean compressImage) {
		this.context = context;
		this.callBack = callBack;
		this.files = files;
		this.isBase64 = isBase64;
		this.compressImage = compressImage;
	}

	private byte[] encode, decode;

	protected String doInBackground2(String... params) {
		String requestUrl = params[0];
		String filePath = params[1];
        String serverResponse = null;
        HttpClient httpClient = new DefaultHttpClient();
        HttpContext httpContext = new BasicHttpContext();
        HttpPost httpPost = new HttpPost(requestUrl);
 
        try {
        	CustomMultipartEntityV2 multipartContent = new CustomMultipartEntityV2(
                    new CustomMultipartEntityV2.ProgressListener() {
                        
                        public void transferred(long num) {
                            publishProgress((int) ((num / (float) totalSize) * 100));
                        }
                    });
 
            // 使用FileBody上传图片
            multipartContent.addPart("file", new FileBody(new File(filePath)));
            totalSize = multipartContent.getContentLength();
            // 上传
            httpPost.setEntity(multipartContent);
            HttpResponse response = httpClient.execute(httpPost, httpContext);
            serverResponse = EntityUtils.toString(response.getEntity());
            System.out.println(serverResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return serverResponse;
    }
	
	boolean test = false ;
	
	protected String doInBackground(String... params) {

		if(test){
			return doInBackground2(params);
		}
		
		String result = null;
		String url = params[0];// 上传地址

		HttpClient httpClient = new DefaultHttpClient();
		HttpContext httpContext = new BasicHttpContext();
		HttpPost httpPost = new HttpPost(url);

		HttpResponse response = null;
		CustomMultiPartEntity customMultiPartEntity = null;

		File file = null;
		FileBody fileBody = null;
		// StringBody values = null;
		// values = new StringBody("hello");

		ProgressListener listener = null;

		try {

			listener = new ProgressListener() {
				public void transferred(long num) {
					int progress = (int) (((float) num / (float) totalSize) * 100);
					publishProgress(progress);
				}
			};

			customMultiPartEntity = new CustomMultiPartEntity(listener);

			// We use FileBody to transfer an image
			if (files != null && files.size() > 0) {
				Iterator<String> itr = files.keySet().iterator();
				while (itr.hasNext()) {
					
					String key = itr.next();
					String path = files.get(key);
					
					file = new File(path);
					
					if (!file.exists()) {
						continue;
					}

					if (compressImage) {
						file = handleCameraResult(path);
					}

					totalSize = totalSize + file.length();

					fileBody = new FileBody(file);
					customMultiPartEntity.addPart(key, fileBody);
					
				}

				customMultiPartEntity.setFileLength(totalSize);
			}

			// customMultiPartEntity.addPart("name", values);

			// 多add几个就是了

			// totalSize = customMultiPartEntity.getContentLength();
			Log.i(tag, "getContentLength totalSize:" + totalSize);

			// Send it
			httpPost.setEntity(customMultiPartEntity);
			response = httpClient.execute(httpPost, httpContext);

			if (response.getStatusLine().getStatusCode() == 200) {
				Log.i(tag, "------响应成功-------");
				result = EntityUtils.toString(response.getEntity());
				if (isBase64) {
					decode = Base64.decode(result.getBytes("UTF-8"));
					// 将返回结果并解密
					result = new String(decode);
				}
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
			httpClient.getConnectionManager().shutdown();
		}
	}

	protected void onPreExecute() {
		this.callBack.onStart();
	}

	protected void onProgressUpdate(Integer... progress) {
		super.onProgressUpdate(progress);
		this.callBack.uploading(progress[0]);
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

	/**
	 * 方法名: handleCameraResult</br> 详述: 压缩图片</br> 开发人员：huangk</br>
	 * 创建时间：2014-4-1</br>
	 * 
	 * @param imageFilePath
	 * @return
	 */
	private File handleCameraResult(String imageFilePath) {
		File file;

		WindowManager windowManager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics dm = new DisplayMetrics();
		windowManager.getDefaultDisplay().getMetrics(dm);
		int screenWidth = dm.widthPixels;
		int screenHeight = dm.heightPixels;

		int dw = screenWidth;
		int dh = screenHeight;

		// 加载图像的尺寸而不是图像本身
		BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
		bmpFactoryOptions.inJustDecodeBounds = true;// 只得到图像尺寸
		Bitmap bitmap = BitmapFactory.decodeFile(imageFilePath,
				bmpFactoryOptions);

		int heightRatio = (int) Math.ceil(bmpFactoryOptions.outHeight
				/ (float) dh);
		int widthRatio = (int) Math.ceil(bmpFactoryOptions.outWidth
				/ (float) dw);

		if (heightRatio > 1 && widthRatio > 1) {
			if (heightRatio > widthRatio) {
				bmpFactoryOptions.inSampleSize = heightRatio;
			} else {
				bmpFactoryOptions.inSampleSize = widthRatio;
			}
		}

		// 对图像进行真正的解码
		bmpFactoryOptions.inJustDecodeBounds = false;
		bitmap = BitmapFactory.decodeFile(imageFilePath, bmpFactoryOptions);

		file = new File(imageFilePath);
		FileOutputStream fOut = null;
		try {
			fOut = new FileOutputStream(file);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 40, fOut);
			fOut.flush();
		} catch (Exception e) {
			Log.i(tag, "handleCameraResult Exception" + e.toString());
		} finally {
			if (fOut != null) {
				try {
					bitmap.recycle();
					fOut.close();
					fOut = null;
				} catch (IOException e) {
					Log.i(tag,
							"handleCameraResult close Exception" + e.toString());
				}
			}
		}
		return file;
	}

	protected void onCancelled() {
	}

	public interface CallBack {

		public void onStart();

		public void onSuccess(String result);

		public void onError(String msg);

		public void uploading(int progress);

	}

}
