
package me.caiying.asiv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewAnimator;

import java.io.InputStream;

public class SmartImageView extends ViewAnimator{
    public static final int INDEX_IMAGE = 0;
    public static final int INDEX_PROGRESS_BAR = 1;
    public static final int INDEX_TEXT_VIEW = 2;
    public static final int PROGRESS_BAR_HEIGHT_DIP = 10;
    private CoreImageView mCoreImageView;
    private ProgressBar mProgressBar;
    private TextView mTextView;

    public SmartImageView(Context paramContext) {
        super(paramContext);
        init();
    }

    public SmartImageView(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
        init();
    }

    private CoreImageView getCoreImageView() {
        if (this.mCoreImageView == null) {
            this.mCoreImageView = new CoreImageView(getContext());
            this.mCoreImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            this.mCoreImageView.setProgressListener(new CoreImageView.OnProgressListener() {
                        public void onProgress(int paramInt) {
                            if (SmartImageView.this.getDisplayedChild() != INDEX_PROGRESS_BAR)
                                SmartImageView.this.setDisplayedChild(INDEX_PROGRESS_BAR);
                            SmartImageView.this.getProgressBar().setProgress(paramInt);
                        }
                    });
            this.mCoreImageView.setReportProgress(true);
            this.mCoreImageView.setOnLoadListener(new CoreImageView.OnLoadListener() {
                        public void onLoad(Bitmap paramBitmap) {
                            if (paramBitmap != null)
                                SmartImageView.this.setDisplayedChild(INDEX_IMAGE);
                            else
                                SmartImageView.this.setDisplayedChild(INDEX_TEXT_VIEW);
                        }
                    });
        }
        return this.mCoreImageView;
    }

    private ProgressBar getProgressBar() {
        if (this.mProgressBar == null) {
            this.mProgressBar = (ProgressBar)LayoutInflater.from(getContext()).inflate(R.layout.progressbar_imageloading, null);
            this.mProgressBar.setIndeterminate(false);
            this.mProgressBar.setMax(100);
        }
        return this.mProgressBar;
    }

    private TextView getTextView() {
        if (this.mTextView == null) {
            this.mTextView = new TextView(getContext());
            this.mTextView.setText("Tap to reload");
            this.mTextView.setGravity(17);
            this.mTextView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View paramView) {
                    SmartImageView.this.getCoreImageView().setUrl(SmartImageView.this.getCoreImageView().getUrl());
                }
            });
        }
        return this.mTextView;
    }

    protected void init() {
        removeAllViews();
        addView(getCoreImageView(), INDEX_IMAGE, new FrameLayout.LayoutParams(-1, -1, 17));
        addView(getProgressBar(), INDEX_PROGRESS_BAR, new FrameLayout.LayoutParams(-1, -2, 17));
        addView(getTextView(), INDEX_TEXT_VIEW, new FrameLayout.LayoutParams(-1, -2, 17));
        this.setBackgroundColor(Color.WHITE);
    }

    public void loadBitmap(InputStream paramInputStream) {
        getCoreImageView().loadBitmap(paramInputStream);
    }

    public void removeLoadCallback() {
        getCoreImageView().removeLoadCallback();
    }

    public void setUrl(String paramString) {
        getCoreImageView().setUrl(paramString);
    }
    
    public static class CoreImageView extends ImageView {
        private SmartImageManager.BitmapCallback callback = new SmartImageManager.BitmapCallback() {
            public void bindToTask(SmartImageManager.LoadBitmapTask paramLoadBitmapTask) {
                if (paramLoadBitmapTask.getUrl().equals(CoreImageView.this.mUrl))
                    CoreImageView.this.mLoadTask = paramLoadBitmapTask;
                
            }

            public void reportError() {
                CoreImageView.this.mLoadTask = null;
                CoreImageView.this.mReportProgress = true;
                if (CoreImageView.this.onLoadListener != null)
                    CoreImageView.this.onLoadListener.onLoad(null);
            }

            public void reportProgress(String paramString, int paramInt) {
                if ((!CoreImageView.this.mLoaded) && (CoreImageView.this.mUrl.equals(paramString)) && (CoreImageView.this.mProgressListener != null))
                    CoreImageView.this.mProgressListener.onProgress(paramInt);
            }

            public void setBitmap(String paramString, Bitmap paramBitmap) {
                if (CoreImageView.this.mUrl.equals(paramString)) {
                    CoreImageView.this.mReportProgress = true;
                    CoreImageView.this.setImageBitmap(paramBitmap);
                    CoreImageView.this.mLoadTask = null;
                    if (CoreImageView.this.onLoadListener != null)
                        CoreImageView.this.onLoadListener.onLoad(paramBitmap);
                }
            }
        };
        
        private SmartImageManager.LoadBitmapTask mLoadTask;
        private boolean mLoaded = false;
        private Drawable mOriginalDrawable;
        private OnProgressListener mProgressListener = new OnProgressListener() {
            public void onProgress(int paramInt) {
                if (CoreImageView.this.mOriginalDrawable != null)
                    CoreImageView.this.setImageDrawable(CoreImageView.this.mOriginalDrawable);
            }
        };
        private boolean mReportProgress = false;
        private String mUrl;
        private OnLoadListener onLoadListener;

        public CoreImageView(Context context) {
            super(context);
            init();
        }

        public CoreImageView(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        public CoreImageView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            init();
        }

        private void fetchBitmap(String paramString) {
            this.mLoaded = false;
            this.mUrl = paramString;
            SmartImageManager.getInstance(getContext()).loadBitmap(paramString, this.callback, mReportProgress, true);
        }

        private void init() {
            this.mOriginalDrawable = getDrawable();
        }

        public String getUrl() {
            return this.mUrl;
        }

        void loadBitmap(InputStream paramInputStream) {
            this.mLoaded = true;
            Bitmap localBitmap = BitmapFactory.decodeStream(paramInputStream);
            if (localBitmap != null)
                setImageBitmap(localBitmap);
        }

        protected void onDetachedFromWindow() {
            removeLoadCallback();
            super.onDetachedFromWindow();
        }

        public void removeLoadCallback() {
            if (this.mLoadTask != null) {
                this.mLoadTask.removeCallback(this.callback);
                this.mLoadTask = null;
            }
        }

        public void setOnLoadListener(OnLoadListener paramOnLoadListener) {
            this.onLoadListener = paramOnLoadListener;
        }

        public void setProgressListener(OnProgressListener paramOnProgressListener) {
            this.mProgressListener = paramOnProgressListener;
        }

        public void setReportProgress(boolean paramBoolean) {
            mReportProgress = paramBoolean;
        }

        public void setUrl(String paramString) {
            setImageDrawable(getResources().getDrawable(R.drawable.ic_launcher));
            removeLoadCallback();
            fetchBitmap(paramString);
        }

        static abstract interface OnLoadListener {
            public abstract void onLoad(Bitmap bitmap);
        }

        static abstract interface OnProgressListener {
            public abstract void onProgress(int progress);
        }
    }
}
