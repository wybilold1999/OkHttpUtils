package app.okhttputils_sample;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.backends.okhttp.OkHttpImagePipelineConfigFactory;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.listener.RequestLoggingListener;
import com.squareup.okhttp.OkHttpClient;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import app.okhttputils_sample.OkHttpUtils.ResultCallback;
import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @Bind(R.id.img)
    SimpleDraweeView mImg;
    @Bind(R.id.progressBar)
    TextView mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initFresco();
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        downloadFile();
        mImg.setImageURI(Uri.parse("http://ac-qfktxonv.clouddn.com/BsqnpikjJ2tJjjrvHXoFddeAZOoxuPUTBqJ3jI49"));
    }

    private void downloadFile() {
        ResultCallback<File> dowloadFileCallBack = new ResultCallback<File>() {

            @Override
            public void onSuccess(File response) {
                mProgressBar.setText("下载完成");
                Log.d("test", "成功");
            }
            @Override
            public void onFailure(Exception e) {
                Log.d("test", "失败");
            }

            @Override
            public void onProgress(float progress, long total) {
                Log.d("test", String.valueOf(total) + "=" + String.valueOf(progress));
                mProgressBar.setText(String.valueOf((int)progress * 100));
            }
        };
        OkHttpUtils.downloadFile("http://ac-qfktxonv.clouddn.com/BsqnpikjJ2tJjjrvHXoFddeAZOoxuPUTBqJ3jI49", dowloadFileCallBack);
    }

    private void initFresco() {
        OkHttpClient mOkHttpClient = new OkHttpClient();
        Set<RequestListener> listeners = new HashSet<>();
        listeners.add(new RequestLoggingListener());
        ImagePipelineConfig config = OkHttpImagePipelineConfigFactory
                .newBuilder(this, mOkHttpClient)
                .setDownsampleEnabled(true)
                .setRequestListeners(listeners)
                .build();
        Fresco.initialize(this, config);
    }
}
