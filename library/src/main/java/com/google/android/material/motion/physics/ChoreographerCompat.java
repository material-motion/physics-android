/*
 * Copyright 2017-present The Material Motion Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.material.motion.physics;

import android.annotation.TargetApi;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;

/**
 * A compatibility shim for {@link android.view.Choreographer} calls, since this class was not
 * available until API 16. For older versions of Android, a Handler will be used instead.
 */
public abstract class ChoreographerCompat {
  private static final ThreadLocal<ChoreographerCompat> threadInstance =
    new ThreadLocal<ChoreographerCompat>() {
      @Override
      protected ChoreographerCompat initialValue() {
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
          return new RealChoreographer();
        } else {
          Looper looper = Looper.myLooper();
          if (looper == null) {
            throw new IllegalStateException("The current thread must have a looper!");
          }
          return new LegacyHandlerWrapper(looper);
        }
      }
    };

  /**
   * Return the instance of {@link ChoreographerCompat} for the current thread. The thread must
   * have a looper associated with it.
   */
  public static ChoreographerCompat getInstance() {
    return threadInstance.get();
  }

  /**
   * Post a frame callback to run on the next frame.
   * <p>
   * <p>The callback runs once then is automatically removed.</p>
   */
  public abstract void postFrameCallback(FrameCallback callback);

  /**
   * Post a frame callback to run on the next frame after the specified delay.
   * <p>
   * <p>The callback runs once then is automatically removed.</p>
   */
  public abstract void postFrameCallbackDelayed(FrameCallback callback, long delayMillis);

  /**
   * Remove a previously posted frame callback.
   */
  public abstract void removeFrameCallback(FrameCallback callback);


  /**
   * A callback that will occur on a future drawing frame. This is a compatible version of {@link
   * android.view.Choreographer.FrameCallback}.
   */
  public abstract static class FrameCallback {
    private Runnable runnable;
    private Choreographer.FrameCallback realCallback;

    public abstract void doFrame(long frameTimeNanos);

    @TargetApi(VERSION_CODES.JELLY_BEAN)
    Choreographer.FrameCallback getRealCallback() {
      if (realCallback == null) {
        realCallback = new Choreographer.FrameCallback() {
          @Override
          public void doFrame(long frameTimeNanos) {
            FrameCallback.this.doFrame(frameTimeNanos);
          }
        };
      }

      return realCallback;
    }

    Runnable getRunnable() {
      if (runnable == null) {
        runnable = new Runnable() {
          @Override
          public void run() {
            doFrame(System.nanoTime());
          }
        };
      }

      return runnable;
    }
  }

  /**
   * A {@link ChoreographerCompat} that just wraps a real {@link Choreographer}, for use on API
   * versions that support it.
   */
  @TargetApi(VERSION_CODES.JELLY_BEAN)
  private static class RealChoreographer extends ChoreographerCompat {
    private Choreographer choreographer;

    public RealChoreographer() {
      choreographer = Choreographer.getInstance();
    }

    @Override
    public void postFrameCallback(FrameCallback callback) {
      choreographer.postFrameCallback(callback.getRealCallback());
    }

    @Override
    public void postFrameCallbackDelayed(FrameCallback callback, long delayMillis) {
      choreographer.postFrameCallbackDelayed(callback.getRealCallback(), delayMillis);
    }

    @Override
    public void removeFrameCallback(FrameCallback callback) {
      choreographer.removeFrameCallback(callback.getRealCallback());
    }
  }

  /**
   * A {@link ChoreographerCompat} that wraps a {@link Handler} and emulates (at a basic level,
   * anyway) the behavior of a {@link Choreographer}.
   */
  private static class LegacyHandlerWrapper extends ChoreographerCompat {
    private static final long FRAME_TIME_MS = 17;
    private Handler handler;

    public LegacyHandlerWrapper(Looper looper) {
      handler = new Handler(looper);
    }

    @Override
    public void postFrameCallback(FrameCallback callback) {
      handler.postDelayed(callback.getRunnable(), 0);
    }

    @Override
    public void postFrameCallbackDelayed(FrameCallback callback, long delayMillis) {
      handler.postDelayed(callback.getRunnable(), delayMillis + FRAME_TIME_MS);
    }

    @Override
    public void removeFrameCallback(FrameCallback callback) {
      handler.removeCallbacks(callback.getRunnable());
    }
  }
}
