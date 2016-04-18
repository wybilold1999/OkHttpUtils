package app.okhttputils_sample;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.internal.$Gson$Types;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import app.okhttputils_sample.CountingRequestBody.Listener;
import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class OkHttpUtils {

    private static final String TAG = "OkHttpUtils";

    /**
     * 目标文件存储的文件夹路径
     */
    private String destFileDir = Environment.getExternalStorageDirectory().getAbsolutePath();
    /**
     * 目标文件存储的文件名
     */
    private String destFileName;

    private static final String mImgType = "image/jpeg";

    private static OkHttpUtils mInstance;
    private OkHttpClient mOkHttpClient;
    private Handler mDelivery;

    private OkHttpUtils() {
        int cacheSize = 10 * 1024 * 1024; // 10 MiB
        File cacheDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Cache");//缓存目录
        mOkHttpClient = new OkHttpClient();
        mOkHttpClient.newBuilder()
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .cookieJar(mOkHttpClient.cookieJar())
                .cache(new Cache(cacheDirectory, cacheSize))
                .build();
        mDelivery = new Handler(Looper.getMainLooper());
    }

    private synchronized static OkHttpUtils getmInstance() {
        if (mInstance == null) {
            synchronized (OkHttpUtils.class) {
                if (mInstance == null) {
                    mInstance = new OkHttpUtils();
                }
            }
        }
        return mInstance;
    }

    private void getRequest(String url, final ResultCallback callback) {
        final Request request = new Request.Builder().url(url).build();
        deliveryResult(callback, request);
    }

    private void postRequest(String url, final ResultCallback callback, List<Param> params) {
        Request request = buildPostRequest(url, params);
        deliveryResult(callback, request);
    }

    private void postFileRequest(String serverUrl, List<String> url, final ResultCallback callback, List<Param> params) {
        Request request = buildFilePostRequest(callback, serverUrl, url, params);
        deliveryResult(callback, request);
    }

    private void deliveryResult(final ResultCallback callback, Request request) {

        mOkHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                sendFailCallback(callback, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    if (callback.mType == String.class) {
                        String str = response.body().string();
                        sendSuccessCallBack(callback, str);
                    }
                    if (callback.mType == File.class) {
                        saveFile(callback, response);
                    }
                }
            }
        });
    }

    /**
     * 下载文件
     *
     * @param callback
     * @param response
     * @return
     * @throws IOException
     */
    public File saveFile(final ResultCallback callback, Response response) throws IOException {
        InputStream is = null;
        byte[] buf = new byte[2048];
        int len = 0;
        FileOutputStream fos = null;
        try {
            is = response.body().byteStream();
            final long total = response.body().contentLength();
            long sum = 0;

            File dir = new File(destFileDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            final File file = new File(dir, getPhotoFileName());
            fos = new FileOutputStream(file);
            while ((len = is.read(buf)) != -1) {
                sum += len;
                fos.write(buf, 0, len);
                final long finalSum = sum;
                mDelivery.post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            if (finalSum * 1.0f / total != 1.0) {
                                callback.onProgress(finalSum * 1.0f / total, total);
                            } else {
                                callback.onSuccess(file);
                            }
                        }
                    }
                });
                if (finalSum * 1.0f / total == 1.0) {
                    break;
                }
            }
            fos.flush();
            return file;

        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException e) {
            }
            try {
                if (fos != null) fos.close();
            } catch (IOException e) {
            }

        }
    }

    private void sendFailCallback(final ResultCallback callback, final Exception e) {
        mDelivery.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onFailure(e);
                }
            }
        });
    }

    private void sendSuccessCallBack(final ResultCallback callback, final Object obj) {
        mDelivery.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onSuccess(obj);
                }
            }
        });
    }

    private Request buildPostRequest(String url, List<Param> params) {
        FormBody.Builder builder = new FormBody.Builder();
        for (Param param : params) {
            builder.add(param.key, param.value);
        }
        RequestBody requestBody = builder.build();
        return new Request.Builder().url(url).post(requestBody).build();
    }

    private Request buildFilePostRequest(final ResultCallback callback, String serverUrl, List<String> urls, List<Param> params) {
        MultipartBody.Builder builder = new MultipartBody.Builder();
        builder.setType(MultipartBody.FORM);
        if (params != null && params.size() > 0) {
            for (Param param : params) {
                builder.addFormDataPart(param.key, param.value);
            }
        }
        if (urls != null && urls.size() > 0) {
            for (String url : urls) {
                File file = new File(url);
                builder.addFormDataPart("image", file.getName(),
                        RequestBody.create(MediaType.parse(mImgType), file));
            }
        }
        RequestBody requestBody = builder.build();

        RequestBody progressBody = new CountingRequestBody(requestBody, new Listener() {
            @Override
            public void onRequestProgress(final long bytesWritten, final long contentLength) {
                mDelivery.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onProgress(bytesWritten * 1.0f / contentLength, contentLength);
                    }
                });
            }
        });

        Request request = new Request.Builder()
                .url(serverUrl)
                .post(progressBody)
                .build();
        return request;
    }


    /**********************对外接口************************/

    /**
     * get请求
     *
     * @param url      请求url
     * @param callback 请求回调
     */
    public static void get(String url, ResultCallback callback) {
        getmInstance().getRequest(url, callback);
    }

    /**
     * post请求
     *
     * @param url      请求url
     * @param callback 请求回调
     * @param params   请求参数
     */
    public static void post(String url, final ResultCallback callback, List<Param> params) {
        getmInstance().postRequest(url, callback, params);
    }

    /**
     * 下载文件
     *
     * @param url
     * @param callback
     */
    public static void downloadFile(String url, ResultCallback callback) {
        getmInstance().getRequest(url, callback);
    }

    /**
     * 上传文件
     *
     * @param url      服务器的url
     * @param fileUrls 文件路径
     * @param callback 请求回调
     * @param params   请求参数
     */
    public static void multiFileUpload(String url, List<String> fileUrls, final ResultCallback callback, List<Param> params) {
        getmInstance().postFileRequest(url, fileUrls, callback, params);
    }

    /**
     * http请求回调类,回调方法在UI线程中执行
     *
     * @param <T>
     */
    public static abstract class ResultCallback<T> {

        Type mType;

        public ResultCallback() {
            mType = getSuperclassTypeParameter(getClass());
        }

        static Type getSuperclassTypeParameter(Class<?> subclass) {
            Type superclass = subclass.getGenericSuperclass();
            if (superclass instanceof Class) {
                throw new RuntimeException("Missing type parameter.");
            }
            ParameterizedType parameterized = (ParameterizedType) superclass;
            return $Gson$Types.canonicalize(parameterized.getActualTypeArguments()[0]);
        }

        /**
         * 请求成功回调
         *
         * @param response
         */
        public abstract void onSuccess(T response);

        /**
         * 请求失败回调
         *
         * @param e
         */
        public abstract void onFailure(Exception e);

        /**
         * 下载文件的进度
         *
         * @param progress
         * @param total
         */
        public abstract void onProgress(float progress, long total);
    }

    /**
     * post请求参数类
     */
    public static class Param {

        String key;
        String value;

        public Param() {
        }

        public Param(String key, String value) {
            this.key = key;
            this.value = value;
        }

    }

    /**
     * 返回图片文件名
     *
     * @return
     */
    private String getPhotoFileName() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "'IMG'_yyyyMMdd_HHmmss");
        return dateFormat.format(date) + ".jpg";
    }


}
