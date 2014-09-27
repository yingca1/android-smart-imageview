
package me.caiying.asiv;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;

import me.caiying.asiv.CompressedBackedLruCache.CompressedBackedBitmap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.ByteArrayBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class SmartImageManager {
    private static final int MAX_TASKS = 4;
    private static final BitmapFactory.Options options = new BitmapFactory.Options();
    private static SmartImageManager sInstance;
    private HashMap<String, LoadBitmapTask> mAllTasks = new HashMap<String, LoadBitmapTask>();
    private HttpClient mHttpClient;
    private CompressedBackedLruCache<String> mLoadedBitmaps;
    private HashSet<LoadBitmapTask> mRunningTasks = new HashSet<LoadBitmapTask>();
    private LinkedList<LoadBitmapTask> mTaskQueue = new LinkedList<LoadBitmapTask>();
    private Context mContext;

    static {
        options.inPurgeable = true;
        options.inInputShareable = false;
    }

    public SmartImageManager(Context context) {
        this.mHttpClient = new DefaultHttpClient();
        this.mContext = context;

        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        mLoadedBitmaps = new CompressedBackedLruCache<String>((int) memoryInfo.threshold, 500, 60);

    }

    public static SmartImageManager getInstance(Context context) {
        if (sInstance == null)
            sInstance = new SmartImageManager(context);
        return sInstance;
    }

    private void queueTask(boolean prior, LoadBitmapTask paramLoadBitmapTask) {
        if (prior)
            this.mTaskQueue.addFirst(paramLoadBitmapTask);
        else
            this.mTaskQueue.addLast(paramLoadBitmapTask);
    }

    public void loadBitmap(String imageUrl, BitmapCallback bitmapCallback, boolean reportProgress, boolean prior) {
        CompressedBackedBitmap compressedBackedBitmap = (CompressedBackedBitmap) this.mLoadedBitmaps.get(imageUrl);
        if (compressedBackedBitmap == null) {
            if (this.mAllTasks.containsKey(imageUrl)) {
                LoadBitmapTask task = (LoadBitmapTask) this.mAllTasks.get(imageUrl);
                if (bitmapCallback != null) {
                    task.addCallback(bitmapCallback);
                    if (reportProgress)
                        task.setReportProgress(reportProgress);
                }
                if (!this.mRunningTasks.contains(task)) {
                    this.mTaskQueue.remove(task);
                    queueTask(prior, task);
                }
            } else {
                LoadBitmapTask task = new LoadBitmapTask(imageUrl, reportProgress);
                if (bitmapCallback != null) {
                    task.addCallback(bitmapCallback);
                    this.mAllTasks.put(imageUrl, task);
                    queueTask(prior, task);
                }
            }
            updateTasks();
        } else {
            if (bitmapCallback != null)
                bitmapCallback.setBitmap(imageUrl, compressedBackedBitmap.getBitmap());
        }
    }

    public void preLoadBitmaps(String[] imageUrls) {
        int i = imageUrls.length;
        for (int j = 0; j < i; j++)
            loadBitmap(imageUrls[j], null, false, false);
    }

    public void updateTasks() {
        while ((this.mRunningTasks.size() < MAX_TASKS) && (!this.mTaskQueue.isEmpty())) {
            LoadBitmapTask loadBitmapTask = (LoadBitmapTask) this.mTaskQueue.removeFirst();
            this.mRunningTasks.add(loadBitmapTask);
            loadBitmapTask.execute();
        }
    }

    public void warm(String imageUrl) {
        CompressedBackedBitmap compressedBackedBitmap = (CompressedBackedBitmap) this.mLoadedBitmaps
                .get(imageUrl);
        if (compressedBackedBitmap != null)
            compressedBackedBitmap.getBitmap().getPixel(0, 0);
    }

    public static abstract interface BitmapCallback {
        public abstract void bindToTask(SmartImageManager.LoadBitmapTask loadBitmapTask);

        public abstract void reportError();

        public abstract void reportProgress(String imageUrl, int progress);

        public abstract void setBitmap(String imageUrl, Bitmap bitmap);
    }

    public class LoadBitmapTask extends AsyncTask<Void, Integer, Bitmap> {
        private List<SmartImageManager.BitmapCallback> mBitmapCallbacks = new ArrayList<SmartImageManager.BitmapCallback>();
        private int mProgress = 0;
        private boolean mReportProgress;
        private String mUrl;

        public LoadBitmapTask(String imageUrl, boolean reportProgress) {
            this.mUrl = imageUrl;
            this.mReportProgress = reportProgress;
        }

        private void reportProgressToCallbacks(Integer progress) {
            Iterator<SmartImageManager.BitmapCallback> iterator = this.mBitmapCallbacks.iterator();
            while (iterator.hasNext()) {
                SmartImageManager.BitmapCallback bitmapCallback = (SmartImageManager.BitmapCallback) iterator
                        .next();
                if (bitmapCallback == null)
                    continue;
                bitmapCallback.reportProgress(this.mUrl, progress.intValue());
            }
        }

        public void addCallback(SmartImageManager.BitmapCallback bitmapCallback) {
            bitmapCallback.bindToTask(this);
            bitmapCallback.reportProgress(this.mUrl, this.mProgress);
            this.mBitmapCallbacks.add(bitmapCallback);
        }

        public String getUrl() {
            return this.mUrl;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            HttpGet get = new HttpGet(mUrl);
            HttpResponse resp = null;
            try {
                resp = mHttpClient.execute(get);
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            HttpEntity entity = null;
            try {
                entity = resp.getEntity();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Bitmap bitmap = null;
            try {
                if (entity != null) {
                    byte[] bytes = toByteArray(entity);
                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (bitmap != null)
                mLoadedBitmaps.put(mUrl, new CompressedBackedBitmap(bitmap, getBitmapsize(bitmap)));
            return bitmap;
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
        public int getBitmapsize(Bitmap bitmap) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                return bitmap.getByteCount();
            }
            return bitmap.getRowBytes() * bitmap.getHeight();
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            Iterator<SmartImageManager.BitmapCallback> iterator = this.mBitmapCallbacks.iterator();
            while (iterator.hasNext()) {
                SmartImageManager.BitmapCallback bitmapCallback = (SmartImageManager.BitmapCallback) iterator.next();
                if (bitmapCallback == null)
                    continue;
                if (bitmap != null) {
                    bitmapCallback.setBitmap(this.mUrl, bitmap);
                    continue;
                }
                bitmapCallback.reportError();
            }
            SmartImageManager.this.mAllTasks.remove(this.mUrl);
            SmartImageManager.this.mRunningTasks.remove(this);
            SmartImageManager.this.updateTasks();
            super.onPostExecute(bitmap);
        }

        @Override
        protected void onPreExecute() {
            reportProgressToCallbacks(Integer.valueOf(0));
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... progresses) {
            this.mProgress = progresses[0].intValue();
            reportProgressToCallbacks(Integer.valueOf(this.mProgress));
            super.onProgressUpdate(progresses);
        }

        public byte[] readBytes(InputStream inputStream) throws IOException {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] arrayOfByte = new byte[4096];
            while (true) {
                int i = inputStream.read(arrayOfByte);
                if (i == -1)
                    break;
                byteArrayOutputStream.write(arrayOfByte, 0, i);
            }
            return byteArrayOutputStream.toByteArray();
        }

        public void removeCallback(SmartImageManager.BitmapCallback bitmapCallback) {
            this.mBitmapCallbacks.remove(bitmapCallback);
        }

        public void setReportProgress(boolean reportProgress) {
            this.mReportProgress = reportProgress;
        }

        public byte[] toByteArray(final HttpEntity entity) throws IOException {
            if (entity == null) {
                throw new IllegalArgumentException("HTTP entity may not be null");
            }
            InputStream instream = entity.getContent();
            if (instream == null) {
                return null;
            }
            try {
                if (entity.getContentLength() > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException(
                            "HTTP entity too large to be buffered in memory");
                }
                int i = (int) entity.getContentLength();
                if (i < 0) {
                    i = 4096;
                }
                ByteArrayBuffer buffer = new ByteArrayBuffer(i);
                byte[] tmp = new byte[4096];
                int l;
                int cur = 0;
                while ((l = instream.read(tmp)) != -1) {
                    cur = cur + l;
                    buffer.append(tmp, 0, l);
                    publishProgress((int) (100.0 * (Float.valueOf(buffer.length()) / i)));
                }
                return buffer.toByteArray();
            } finally {
                instream.close();
            }
        }
    }
}
