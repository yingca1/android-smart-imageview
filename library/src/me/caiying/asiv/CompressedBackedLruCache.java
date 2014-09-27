package me.caiying.asiv;

import android.graphics.Bitmap;

import me.caiying.asiv.CompressedBackedLruCache.CompressedBackedBitmap;
import static me.caiying.asiv.Logger.*;
import java.util.Map;

public class CompressedBackedLruCache<T> extends LruCache<T, CompressedBackedBitmap> {
    private final int mMaxCount;
    private final int mMinTrimCount;

    public CompressedBackedLruCache(int max_size, int max_entries, int min_trim_count) {
        super(max_size);
        this.mMaxCount = max_entries;
        this.mMinTrimCount = min_trim_count;
    }

    private void trimToCount(int count) {
        int i = 0;
        while ((this.map.size() > count) && (i < this.mMinTrimCount) && !this.map.isEmpty()) {
            Map.Entry<T, CompressedBackedBitmap> entry = (Map.Entry<T, CompressedBackedBitmap>) this.map.entrySet().iterator().next();
            CompressedBackedBitmap compressedBackedBitmap = (CompressedBackedBitmap) entry.getValue();
            this.map.remove(entry.getKey());
            entryRemoved(true, (T) entry.getKey(),compressedBackedBitmap, null);
            i++;
        }
        Logger.d(DEBUG_MEMO_CACHE_TAG, "count -> " + map.size() + " : size -> " + size());
    }

    public CompressedBackedBitmap put(T type, CompressedBackedBitmap compressedBackedBitmap) {
        CompressedBackedBitmap bitmap = (CompressedBackedBitmap) super.put(type, compressedBackedBitmap);
        trimToCount(this.mMaxCount);
        return bitmap;
    }
 
    @Override
    protected int sizeOf(T type, CompressedBackedBitmap compressedBackedBitmap) {
        return compressedBackedBitmap.getCompressedImageSize();
    }

    public static class CompressedBackedBitmap {
        private int compressedImageSize;
        private Bitmap mBitmap;

        public CompressedBackedBitmap(Bitmap bitmap, int size) {
            this.mBitmap = bitmap;
            this.compressedImageSize = size;
        }

        public Bitmap getBitmap() {
            return this.mBitmap;
        }

        public int getCompressedImageSize() {
            return this.compressedImageSize;
        }
    }
}
