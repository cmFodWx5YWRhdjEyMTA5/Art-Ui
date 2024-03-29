package com.art.uilibrary.nui.multiphotopicker.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;

import com.art.uilibrary.R;
import com.art.uilibrary.utils.UiImageUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.HashMap;

public class ImageDisplayer {
    private static final int THUMB_WIDTH = 256;
    private static final int THUMB_HEIGHT = 256;
    private static ImageDisplayer instance;
    private final String TAG = getClass().getSimpleName();
    private final Handler h = new Handler();
    private final int mScreenWidth;
    private final int mScreenHeight;
    private final HashMap<String, SoftReference<Bitmap>> imageCache = new HashMap<>();

    private ImageDisplayer(Context context) {
        Context context1;
        context1 = context.getApplicationContext() != null ? context.getApplicationContext() : context;

        DisplayMetrics dm = new DisplayMetrics();
        dm = context1.getResources().getDisplayMetrics();
        mScreenWidth = dm.widthPixels;
        mScreenHeight = dm.heightPixels;
    }

    public static ImageDisplayer getInstance(Context context) {
        if (instance == null) {
            synchronized (ImageDisplayer.class) {
                instance = new ImageDisplayer(context);
            }
        }

        return instance;
    }

    private void put(String key, Bitmap bmp) {
        if (!TextUtils.isEmpty(key) && bmp != null) {
            imageCache.put(key, new SoftReference<>(bmp));
        }
    }

    public void displayBmp(final ImageView iv, final String thumbPath, final String sourcePath) {
        displayBmp(iv, thumbPath, sourcePath, true, 0, 0);
    }

    public void displayBmp(final ImageView iv, final String thumbPath, final String sourcePath, final boolean showThumb) {
        displayBmp(iv, thumbPath, sourcePath, showThumb, 0, 0);
    }

    public void displayBmp(final ImageView iv, final String thumbPath, final String sourcePath, final boolean showThumb, final int width, final int height) {
        if (TextUtils.isEmpty(thumbPath) && TextUtils.isEmpty(sourcePath)) {
            Log.e(TAG, "no paths pass in");
            return;
        }

        if (iv.getTag() != null && iv.getTag().equals(sourcePath)) {
            return;
        }

        // showDefault(iv);

        final String path;
        if (!TextUtils.isEmpty(thumbPath) && showThumb) {
            path = thumbPath;
        } else if (!TextUtils.isEmpty(sourcePath)) {
            path = sourcePath;
        } else {
            return;
        }

        iv.setTag(path);

        if (imageCache.containsKey(showThumb ? path + THUMB_WIDTH + THUMB_HEIGHT : path)) {
            SoftReference<Bitmap> reference = imageCache.get(showThumb ? path + THUMB_WIDTH + THUMB_HEIGHT : path);
            // 可以用LruCahche会好些
            Bitmap imgInCache = reference.get();
            if (imgInCache != null) {
                refreshView(iv, imgInCache, path);
                return;
            }
        }
        iv.setImageBitmap(null);

        // 不在缓存则加载图片
        new Thread() {
            Bitmap img;

            public void run() {

                try {
                    if (path != null && path.equals(thumbPath)) {
                        img = BitmapFactory.decodeFile(path);
                    }
                    if (img == null) {
                        img = compressImg(sourcePath, showThumb);
                    }

                    if (img != null) {
                        if (width != 0 && height != 0) {
                            img = UiImageUtils.getResize(img, width, height);
                        }
                        put(showThumb ? path + THUMB_WIDTH + THUMB_HEIGHT : path, img);
                    }
                } catch (Exception e) {

                } catch (OutOfMemoryError e) {
                    // TODO: handle exception
                }
                h.post(new Runnable() {
                    @Override
                    public void run() {
                        refreshView(iv, img, path);
                    }
                });
            }
        }.start();

    }

    private void refreshView(ImageView imageView, Bitmap bitmap, String path) {
        if (imageView != null && bitmap != null) {
            if (path != null) {
                imageView.setImageBitmap(bitmap);
                imageView.setTag(path);
            }
        }
    }

    private void showDefault(ImageView iv) {
        iv.setBackgroundResource(R.mipmap.nui_photo_default);
    }

    private Bitmap compressImg(String path, boolean showThumb) throws IOException {
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(new File(path)));
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(in, null, opt);
        in.close();
        int i = 0;
        Bitmap bitmap = null;
        if (showThumb) {
            while (true) {
                if ((opt.outWidth >> i <= THUMB_WIDTH) && (opt.outHeight >> i <= THUMB_HEIGHT)) {
                    in = new BufferedInputStream(new FileInputStream(new File(path)));
                    opt.inSampleSize = (int) Math.pow(2.0D, i);
                    opt.inJustDecodeBounds = false;
                    bitmap = BitmapFactory.decodeStream(in, null, opt);
                    break;
                }
                i += 1;
            }
        } else {
            while (true) {
                if ((opt.outWidth >> i <= mScreenWidth) && (opt.outHeight >> i <= mScreenHeight)) {
                    in = new BufferedInputStream(new FileInputStream(new File(path)));
                    opt.inSampleSize = (int) Math.pow(2.0D, i);
                    opt.inJustDecodeBounds = false;
                    bitmap = BitmapFactory.decodeStream(in, null, opt);
                    break;
                }
                i += 1;
            }
        }
        return bitmap;
    }

    public interface ImageCallback {
        public void imageLoad(ImageView imageView, Bitmap bitmap, Object... params);
    }
}
