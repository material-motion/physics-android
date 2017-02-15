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
package com.google.android.material.motion.physics.sample;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.widget.ViewDragHelper;
import android.support.v4.widget.ViewDragHelper.Callback;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

/**
 * A FrameLayout that uses a ViewDragHelper to allow its children to be interacted with.
 * <p>
 * <p>This class helps reduce the amount of boilerplate needed to use a ViewDragHelper.
 */
public class DragFrameLayout extends FrameLayout {

  private final ViewDragHelper helper;
  @Nullable
  private Callback callback;

  public DragFrameLayout(Context context) {
    this(context, null);
  }

  public DragFrameLayout(Context context, AttributeSet attrs) {
    super(context, attrs);

    helper =
      ViewDragHelper.create(
        this,
        new Callback() {
          @Override
          public boolean tryCaptureView(View child, int pointerId) {
            if (callback != null) {
              return callback.tryCaptureView(child, pointerId);
            }
            return true;
          }

          @Override
          public void onViewDragStateChanged(int state) {
            if (callback != null) {
              callback.onViewDragStateChanged(state);
            }
          }

          @Override
          public void onViewPositionChanged(
            View changedView, int left, int top, int dx, int dy) {
            if (callback != null) {
              callback.onViewPositionChanged(changedView, left, top, dx, dy);
            }
          }

          @Override
          public void onViewCaptured(View capturedChild, int activePointerId) {
            if (callback != null) {
              callback.onViewCaptured(capturedChild, activePointerId);
            }
          }

          @Override
          public void onViewReleased(View releasedChild, float xvel, float yvel) {
            if (callback != null) {
              callback.onViewReleased(releasedChild, xvel, yvel);
            }
          }

          @Override
          public void onEdgeTouched(int edgeFlags, int pointerId) {
            if (callback != null) {
              callback.onEdgeTouched(edgeFlags, pointerId);
            }
          }

          @Override
          public boolean onEdgeLock(int edgeFlags) {
            if (callback != null) {
              return callback.onEdgeLock(edgeFlags);
            }
            return super.onEdgeLock(edgeFlags);
          }

          @Override
          public void onEdgeDragStarted(int edgeFlags, int pointerId) {
            if (callback != null) {
              callback.onEdgeDragStarted(edgeFlags, pointerId);
            }
          }

          @Override
          public int getOrderedChildIndex(int index) {
            if (callback != null) {
              return callback.getOrderedChildIndex(index);
            }
            return super.getOrderedChildIndex(index);
          }

          @Override
          public int getViewHorizontalDragRange(View child) {
            if (callback != null) {
              return callback.getViewHorizontalDragRange(child);
            }
            return super.getViewHorizontalDragRange(child);
          }

          @Override
          public int getViewVerticalDragRange(View child) {
            if (callback != null) {
              return callback.getViewVerticalDragRange(child);
            }
            return super.getViewVerticalDragRange(child);
          }

          @Override
          public int clampViewPositionHorizontal(View child, int left, int dx) {
            if (callback != null) {
              return callback.clampViewPositionHorizontal(child, left, dx);
            }
            return super.clampViewPositionHorizontal(child, left, dx);
          }

          @Override
          public int clampViewPositionVertical(View child, int top, int dy) {
            if (callback != null) {
              return callback.clampViewPositionVertical(child, top, dy);
            }
            return super.clampViewPositionVertical(child, top, dy);
          }
        });
  }

  public void setCallback(@Nullable Callback callback) {
    this.callback = callback;
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    final int action = MotionEventCompat.getActionMasked(ev);
    if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
      helper.cancel();
      return false;
    }
    return helper.shouldInterceptTouchEvent(ev);
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    helper.processTouchEvent(ev);
    return true;
  }
}
