package com.attosoft.glideprogress;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.integration.okhttp.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private ImageView mImageView;
    private Button mButton;
    private FrameLayout mFrameLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        mImageView = (ImageView) findViewById(R.id.image);
//        mButton = (Button) findViewById(R.id.buttonPanel);
        mFrameLayout = (FrameLayout)findViewById(R.id.fragment_container);
//        mButton.setOnClickListener(this);


        OkHttpClient client = new OkHttpClient();
        final OkHttpProgressGlideModule.ResponseProgressListener listener= new OkHttpProgressGlideModule.DispatchingProgressListener();
        Interceptor interceptor = new Interceptor(){

            @Override
            public Response intercept(Chain chain) throws IOException {
                Log.d("attosoft","intercept");
                Request request = chain.request();
                Response response = chain.proceed(request);
                ResponseBody responseBody = new OkHttpProgressGlideModule.OkHttpProgressResponseBody(request.url(), response.body(), listener);
                Response response1 = response.newBuilder()
                        .body(responseBody)
                        .build();
                return response1;
            }
        };
        client.networkInterceptors().add(interceptor);
//        client.networkInterceptors().add(createInterceptor(new DispatchingProgressListener()));
        Glide.get(this).register(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(client));

        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment fragment = new TestFragment();
        fragmentTransaction.add(R.id.fragment_container, fragment, "" + fragment.getClass());
//        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    public void onClick(View v) {
        if (v == mButton) {
            Toast.makeText(this, "click", Toast.LENGTH_SHORT).show();
            Glide.with(this)
                    .load("http://img.godeyes.cn/2008/0701/mg2_s.jpg")
                    .into(mImageView);
        }
    }


}

//
//        import android.os.Bundle;
//        import android.support.v7.app.AppCompatActivity;
//        import android.util.Log;
//        import android.widget.ImageView;
//        import android.widget.ProgressBar;
//
//        import com.bumptech.glide.Glide;
//        import com.bumptech.glide.integration.okhttp.OkHttpUrlLoader;
//        import com.bumptech.glide.load.engine.DiskCacheStrategy;
//        import com.bumptech.glide.load.model.GlideUrl;
//        import com.squareup.okhttp.Interceptor;
//        import com.squareup.okhttp.MediaType;
//        import com.squareup.okhttp.OkHttpClient;
//        import com.squareup.okhttp.Response;
//        import com.squareup.okhttp.ResponseBody;
//
//        import java.io.IOException;
//        import java.io.InputStream;
//
//        import okio.Buffer;
//        import okio.BufferedSource;
//        import okio.ForwardingSource;
//        import okio.Okio;
//        import okio.Source;
//
///*
//   For more information see:
//   https://github.com/square/okhttp/blob/master/samples/guide/src/main/java/com/squareup/okhttp/recipes/Progress.java
//*/
//
//public class MainActivity extends AppCompatActivity {
//    private static final String LOG_TAG = "MainActivity";
//
//    private final static String DOWNLOAD_URL = "https://i.imgur.com/mYBXl6X.jpg";
//
//    private OkHttpClient mOkHttpClient;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        final ImageView imageView = (ImageView) findViewById(R.id.image_view);
//
//        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
//
//        mOkHttpClient = new OkHttpClient();
//
//        final ProgressListener progressListener = new ProgressListener() {
//            @Override
//            public void update(long bytesRead, long contentLength, boolean done) {
//                int progress = (int) ((100 * bytesRead) / contentLength);
//
//                // Enable if you want to see the progress with logcat
//                // Log.v(LOG_TAG, "Progress: " + progress + "%");
//                progressBar.setProgress(progress);
//                Log.d("attosoft","progress="+progress);
//                if (done) {
//                    Log.i(LOG_TAG, "Done loading");
//                }
//            }
//        };
//
//        mOkHttpClient.networkInterceptors().add(new Interceptor() {
//            @Override
//            public Response intercept(Chain chain) throws IOException {
//                Log.d("attosoft","intercept");
//                Response originalResponse = chain.proceed(chain.request());
//                return originalResponse.newBuilder()
//                        .body(new ProgressResponseBody(originalResponse.body(), progressListener))
//                        .build();
//            }
//        });
//
//        Glide.get(this)
//                .register(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(mOkHttpClient));
//        Glide.with(this)
//                .load(DOWNLOAD_URL)
//                // Disabling cache to see download progress with every app load
//                // You may want to enable caching again in production
//                .diskCacheStrategy(DiskCacheStrategy.NONE)
//                .into(imageView);
//    }
//
//    private static class ProgressResponseBody extends ResponseBody {
//
//        private final ResponseBody responseBody;
//        private final ProgressListener progressListener;
//        private BufferedSource bufferedSource;
//
//        public ProgressResponseBody(ResponseBody responseBody, ProgressListener progressListener) {
//            this.responseBody = responseBody;
//            this.progressListener = progressListener;
//        }
//
//        @Override
//        public MediaType contentType() {
//            return responseBody.contentType();
//        }
//
//        @Override
//        public long contentLength(){
//            return responseBody.contentLength();
//        }
//
//        @Override
//        public BufferedSource source() {
//            if (bufferedSource == null) {
//                bufferedSource = Okio.buffer(source(responseBody.source()));
//            }
//            return bufferedSource;
//        }
//
//        private Source source(Source source) {
//            return new ForwardingSource(source) {
//                long totalBytesRead = 0L;
//
//                @Override
//                public long read(Buffer sink, long byteCount) throws IOException {
//                    long bytesRead = super.read(sink, byteCount);
//                    Log.d("attosoft","read  bytesRead="+bytesRead);
//                    // read() returns the number of bytes read, or -1 if this source is exhausted.
//                    totalBytesRead += bytesRead != -1 ? bytesRead : 0;
//                    progressListener.update(totalBytesRead, responseBody.contentLength(), bytesRead == -1);
//                    return bytesRead;
//                }
//            };
//        }
//    }
//
//    interface ProgressListener {
//        void update(long bytesRead, long contentLength, boolean done);
//    }
//
//}



