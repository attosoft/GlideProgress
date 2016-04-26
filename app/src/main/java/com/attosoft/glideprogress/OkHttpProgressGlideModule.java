package com.attosoft.glideprogress;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.integration.okhttp.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.GlideModule;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;


/**
 * Created by andy on 2016/4/25.
 */
public class OkHttpProgressGlideModule implements GlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
    }

    @Override
    public void registerComponents(Context context, Glide glide) {
        Log.d("attosoft","registerComponents");
        OkHttpClient client = new OkHttpClient();
        final ResponseProgressListener listener= new DispatchingProgressListener();
        Interceptor interceptor = new Interceptor(){

            @Override
            public Response intercept(Chain chain) throws IOException {
                Log.d("attosoft","intercept");
                Request request = chain.request();
                Response response = chain.proceed(request);
                ResponseBody responseBody = new OkHttpProgressResponseBody(request.url(), response.body(), listener);
                Response response1 = response.newBuilder()
                        .body(responseBody)
                        .build();
                return response1;
            }
        };
        client.networkInterceptors().add(interceptor);
//        client.networkInterceptors().add(createInterceptor(new DispatchingProgressListener()));
        glide.register(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(client));
    }

    private static Interceptor createInterceptor(final ResponseProgressListener listener) {
        return new Interceptor() {
            @Override
            public Response intercept(Interceptor.Chain chain) throws IOException {
                Request request = chain.request();
                Response response = chain.proceed(request);
                ResponseBody responseBody = new OkHttpProgressResponseBody(request.url(), response.body(), listener);
                Response response1 = response.newBuilder()
                        .body(responseBody)
                        .build();
                return response1;
//                return response.newBuilder()
//                        .body(new OkHttpProgressResponseBody(request.url(), response.body(), listener))
//                        .build();
            }
        };
    }

    public interface UIProgressListener {
        void onProgress(long bytesRead, long expectedLength);

        /**
         * Control how often the listener needs an update. 0% and 100% will always be dispatched.
         *
         * @return in percentage (0.2 = call {@link #onProgress} around every 0.2 percent of progress)
         */
        float getGranualityPercentage();
    }

    public static void forget(String url) {
        DispatchingProgressListener.forget(url);
    }

    public static void expect(String url, UIProgressListener listener) {
        DispatchingProgressListener.expect(url, listener);
    }

    public interface ResponseProgressListener {
        void update(URL url, long bytesRead, long contentLength);
    }

    public static class DispatchingProgressListener implements ResponseProgressListener {
        private static final Map<String, UIProgressListener> LISTENERS = new HashMap<>();
        private static final Map<String, Long> PROGRESSES = new HashMap<>();

        private final Handler handler;

        public DispatchingProgressListener() {
            this.handler = new Handler(Looper.getMainLooper());
        }

        static void forget(String url) {
            LISTENERS.remove(url);
            PROGRESSES.remove(url);
        }

        static void expect(String url, UIProgressListener listener) {
            LISTENERS.put(url, listener);
        }

        @Override
        public void update(URL url, final long bytesRead, final long contentLength) {
            //System.out.printf("%s: %d/%d = %.2f%%%n", url, bytesRead, contentLength, (100f * bytesRead) / contentLength);
            String key = url.toString();
            final UIProgressListener listener = LISTENERS.get(key);
            if (listener == null) {
                return;
            }
            if (contentLength <= bytesRead) {
                forget(key);
            }
            if (needsDispatch(key, bytesRead, contentLength, listener.getGranualityPercentage())) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onProgress(bytesRead, contentLength);
                    }
                });
            }
        }

        private boolean needsDispatch(String key, long current, long total, float granularity) {
            if (granularity == 0 || current == 0 || total == current) {
                return true;
            }
            float percent = 100f * current / total;
            long currentProgress = (long) (percent / granularity);
            Long lastProgress = PROGRESSES.get(key);
            if (lastProgress == null || currentProgress != lastProgress) {
                PROGRESSES.put(key, currentProgress);
                return true;
            } else {
                return false;
            }
        }
    }

    public static class OkHttpProgressResponseBody extends ResponseBody {
        private final URL url;
        private final ResponseBody responseBody;
        private final ResponseProgressListener progressListener;
        private BufferedSource bufferedSource;

        public OkHttpProgressResponseBody(URL url, ResponseBody responseBody,
                                   ResponseProgressListener progressListener) {
            this.url = url;
            this.responseBody = responseBody;
            this.progressListener = progressListener;
            Log.d("attosoft","OkHttpProgressResponseBody");
        }

        @Override
        public MediaType contentType() {
            return responseBody.contentType();
        }

        @Override
        public long contentLength() {
            return responseBody.contentLength();
        }

        @Override
        public BufferedSource source() {
            if (bufferedSource == null) {
                bufferedSource = Okio.buffer(source(responseBody.source()));
            }
            return bufferedSource;
        }

        private Source source(Source source) {
            return new ForwardingSource(source) {
                long totalBytesRead = 0L;

                @Override
                public long read(Buffer sink, long byteCount) throws IOException {
                    long bytesRead = super.read(sink, byteCount);
                    Log.d("attosoft","read  bytesRead="+bytesRead);
                    long fullLength = responseBody.contentLength();
                    if (bytesRead == -1) { // this source is exhausted
                        totalBytesRead = fullLength;
                    } else {
                        totalBytesRead += bytesRead;
                    }
                    progressListener.update(url, totalBytesRead, fullLength);
                    return bytesRead;
                }
            };
        }
    }
}
