package com.itouxian.android.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by chenjishi on 14-3-19.
 */
public class IntentUtils {
    public static String KEY_PREVIEW_IMAGE = "preview_image";
    public static String KEY_FEED_ID = "feed_id";

    /**
     * start screen capture with no delay
     *
     * @param context
     * @param intent
     */
    public static void startPreviewActivity(Context context, Intent intent) {
        final float f = (float) (Runtime.getRuntime().totalMemory() + Debug.getNativeHeapAllocatedSize());
        if (f / (float) Runtime.getRuntime().maxMemory() > .8f) {
            context.startActivity(intent);
        } else {
            startPreviewActivity(context, intent, 0);
        }
    }

    /**
     * start screen capture after "delay" milliseconds, so the previous activity's
     * state recover to normal state, such as button click, list item click,wait
     * them to normal state so we can make a good screen capture
     *
     * @param context
     * @param intent
     * @param delay   time in milliseconds
     */
    public static void startPreviewActivity(final Context context, final Intent intent, long delay) {
        final Handler mainThread = new Handler(Looper.getMainLooper());
        final Runnable postAction = new Runnable() {
            @Override
            public void run() {
                context.startActivity(intent);
            }
        };

        /** process screen capture on background thread */
        Runnable action = new Runnable() {
            @Override
            public void run() {
                /**
                 * activity's root layout id, you can change the android.R.id.content to your root
                 * layout id
                 */
                final View contentView = ((Activity) context).findViewById(android.R.id.content);

                ByteArrayOutputStream baos = null;
                Bitmap bitmap = null;

                try {
                    bitmap = Bitmap.createBitmap(contentView.getWidth(),
                            contentView.getHeight(), Bitmap.Config.ARGB_8888);
                    contentView.draw(new Canvas(bitmap));

                    baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                    intent.putExtra(KEY_PREVIEW_IMAGE, baos.toByteArray());
                } finally {
                    try {
                        /** no need to close, actually do nothing */
                        if (null != baos) baos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (null != bitmap && !bitmap.isRecycled()) {
                        bitmap.recycle();
                        bitmap = null;
                        System.gc();
                    }
                }
                mainThread.post(postAction);
            }
        };

        if (delay > 0) {
            ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
            worker.schedule(action, delay, TimeUnit.MILLISECONDS);
        } else {
            action.run();
        }
    }
}
