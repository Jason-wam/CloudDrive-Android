package com.jason.cloud.media3.utils;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.datasource.cache.Cache;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.datasource.rtmp.RtmpDataSource;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import androidx.media3.exoplayer.source.ConcatenatingMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;

import com.jason.cloud.media3.model.Media3Item;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

@UnstableApi
public final class Media3SourceHelper {

    private static volatile Media3SourceHelper sInstance;

    private final String mUserAgent;
    private final Context mAppContext;
    private HttpDataSource.Factory mHttpDataSourceFactory;
    private Cache mCache;

    private Media3SourceHelper(Context context) {
        mAppContext = context.getApplicationContext();
        mUserAgent = Util.getUserAgent(mAppContext, mAppContext.getApplicationInfo().name);
    }

    public static Media3SourceHelper getInstance(Context context) {
        if (sInstance == null) {
            synchronized (Media3SourceHelper.class) {
                if (sInstance == null) {
                    sInstance = new Media3SourceHelper(context);
                }
            }
        }
        return sInstance;
    }

    public MediaSource getMediaSource(String uri) {
        return getMediaSource(uri, null, false);
    }

    public MediaSource getMediaSource(String uri, Map<String, String> headers) {
        return getMediaSource(uri, headers, false);
    }

    public MediaSource getMediaSource(String uri, boolean isCache) {
        return getMediaSource(uri, null, isCache);
    }

    public MediaSource getMediaSource(String uri, Map<String, String> headers, boolean isCache) {
        Uri contentUri = Uri.parse(uri);
        if ("rtmp".equals(contentUri.getScheme())) {
            return new ProgressiveMediaSource.Factory(new RtmpDataSource.Factory()).createMediaSource(MediaItem.fromUri(contentUri));
        } else if ("rtsp".equals(contentUri.getScheme())) {
            return new RtspMediaSource.Factory().createMediaSource(MediaItem.fromUri(contentUri));
        }
        int contentType = inferContentType(uri);
        DataSource.Factory factory;
        if (isCache && !uri.startsWith("http://127.0.0.1") && !uri.startsWith("http://localhost")) {
            factory = getCacheDataSourceFactory();
        } else {
            factory = getDataSourceFactory();
        }
        if (mHttpDataSourceFactory != null) {
            setHeaders(headers);
        }
        switch (contentType) {
            case C.CONTENT_TYPE_DASH:
                return new DashMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(contentUri));
            case C.CONTENT_TYPE_HLS:
                return new HlsMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(contentUri));
            default:
            case C.CONTENT_TYPE_OTHER:
                return new ProgressiveMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(contentUri));
        }
    }

    public MediaSource getMediaSource(Media3Item item) {
        return getMediaSource(item, null, item.getCacheEnabled());
    }

    public MediaSource getMediaSource(Media3Item item, boolean isCache) {
        return getMediaSource(item, item.getHeaders(), isCache);
    }

    public ConcatenatingMediaSource getMediaSource(List<Media3Item> itemList) {
        ConcatenatingMediaSource source = new ConcatenatingMediaSource();
        for (int i = 0; i < itemList.size(); i++) {
            Media3Item item = itemList.get(i);
            source.addMediaSource(getMediaSource(item, item.getHeaders(), item.getCacheEnabled()));
        }
        return source;
    }

    public ConcatenatingMediaSource getMediaSource(List<Media3Item> itemList, Map<String, String> headers, boolean isCache) {
        ConcatenatingMediaSource source = new ConcatenatingMediaSource();
        for (int i = 0; i < itemList.size(); i++) {
            Media3Item item = itemList.get(i);
            source.addMediaSource(getMediaSource(item, headers, isCache));
        }
        return source;
    }

    public ConcatenatingMediaSource getMediaSource(List<Media3Item> itemList, boolean isCache) {
        ConcatenatingMediaSource source = new ConcatenatingMediaSource();
        for (int i = 0; i < itemList.size(); i++) {
            source.addMediaSource(getMediaSource(itemList.get(i), isCache));
        }
        return source;
    }

    public MediaSource getMediaSource(Media3Item item, Map<String, String> headers, boolean isCache) {
        Uri contentUri = Uri.parse(item.getUrl());
        if ("rtmp".equals(contentUri.getScheme())) {
            return new ProgressiveMediaSource.Factory(new RtmpDataSource.Factory()).createMediaSource(createMediaItem(item));
        } else if ("rtsp".equals(contentUri.getScheme())) {
            return new RtspMediaSource.Factory().createMediaSource(createMediaItem(item));
        }
        int contentType = inferContentType(item.getUrl());
        DataSource.Factory factory;
        if (isCache && !item.getUrl().startsWith("http://127.0.0.1") && !item.getUrl().startsWith("http://localhost")) {
            factory = getCacheDataSourceFactory();
        } else {
            factory = getDataSourceFactory();
        }
        if (mHttpDataSourceFactory != null) {
            setHeaders(headers);
        }
        switch (contentType) {
            case C.CONTENT_TYPE_DASH:
                return new DashMediaSource.Factory(factory).createMediaSource(createMediaItem(item));
            case C.CONTENT_TYPE_HLS:
                return new HlsMediaSource.Factory(factory).createMediaSource(createMediaItem(item));
            default:
            case C.CONTENT_TYPE_OTHER:
                return new ProgressiveMediaSource.Factory(factory).createMediaSource(createMediaItem(item));
        }
    }

    private MediaItem createMediaItem(Media3Item item) {
        return new MediaItem.Builder().setTag(item).setUri(item.getUrl()).build();
    }

    private int inferContentType(String fileName) {
        fileName = fileName.toLowerCase();
        if (fileName.contains(".mpd")) {
            return C.CONTENT_TYPE_DASH;
        } else if (fileName.contains(".m3u8")) {
            return C.CONTENT_TYPE_HLS;
        } else {
            return C.CONTENT_TYPE_OTHER;
        }
    }

    private DataSource.Factory getCacheDataSourceFactory() {
        if (mCache == null) {
            mCache = newCache();
        }
        return new CacheDataSource.Factory()
                .setCache(mCache)
                .setUpstreamDataSourceFactory(getDataSourceFactory())
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }

    private Cache newCache() {
        //1k = 1024 b
        //1M = 1024 * 1k
        //1G = 1024 * 1M
        File cacheDir = Media3Configure.INSTANCE.getCachePoolDir();
        if (cacheDir == null) {
            cacheDir = new File(mAppContext.getExternalCacheDir(), "media3-cache");
        }
        return new SimpleCache(
                cacheDir,//缓存目录
                new LeastRecentlyUsedCacheEvictor(Media3Configure.INSTANCE.getCachePoolSize()),//缓存大小，默认512M，使用LRU算法实现
                new StandaloneDatabaseProvider(mAppContext));
    }

    /**
     * Returns a new DataSource factory.
     *
     * @return A new DataSource factory.
     */
    private DataSource.Factory getDataSourceFactory() {
        return new DefaultDataSource.Factory(mAppContext, getHttpDataSourceFactory());
    }

    /**
     * Returns a new HttpDataSource factory.
     *
     * @return A new HttpDataSource factory.
     */
    private DataSource.Factory getHttpDataSourceFactory() {
        if (mHttpDataSourceFactory == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.followRedirects(true);
            builder.followSslRedirects(true);
            OkHttpClient client = builder.build();
            mHttpDataSourceFactory = new OkHttpDataSource.Factory(new Call.Factory() {
                @NonNull
                @Override
                public Call newCall(@NonNull Request request) {
                    Log.i("DataSourceFactory", "newCall: " + request);
                    return client.newCall(request);
                }
            });
        }
        /*if (mHttpDataSourceFactory == null) {
            mHttpDataSourceFactory = new DefaultHttpDataSource.Factory()
                    .setUserAgent(mUserAgent)
                    .setAllowCrossProtocolRedirects(true);
        }*/
        return mHttpDataSourceFactory;
    }

    private void setHeaders(Map<String, String> headers) {
        if (headers != null && headers.size() > 0) {
            //如果发现用户通过header传递了UA，则强行将HttpDataSourceFactory里面的userAgent字段替换成用户的
            if (headers.containsKey("User-Agent")) {
                String value = headers.remove("User-Agent");
                if (!TextUtils.isEmpty(value)) {
                    try {
                        Field userAgentField = mHttpDataSourceFactory.getClass().getDeclaredField("userAgent");
                        userAgentField.setAccessible(true);
                        userAgentField.set(mHttpDataSourceFactory, value);
                    } catch (Exception e) {
                        //ignore
                    }
                }
            }
            mHttpDataSourceFactory.setDefaultRequestProperties(headers);
        }
    }

    public void setCache(Cache cache) {
        this.mCache = cache;
    }
}
