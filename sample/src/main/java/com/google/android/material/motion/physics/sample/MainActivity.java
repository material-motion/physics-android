/*
 * Copyright 2016-present The Material Motion Authors. All Rights Reserved.
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

import android.os.Bundle;
import android.support.v4.widget.ViewDragHelper.Callback;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnLayoutChangeListener;

import com.google.android.material.motion.physics.Integrator;
import com.google.android.material.motion.physics.Integrator.SimpleListener;
import com.google.android.material.motion.physics.forces.AnchoredForce;
import com.google.android.material.motion.physics.forces.Spring;
import com.google.android.material.motion.physics.integrators.Rk4Integrator;
import com.google.android.material.motion.physics.math.Vector;
import com.google.android.material.motion.physics.math.Vectors;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that displays a basic demo of dynamic motion in Odeon.
 */
public class MainActivity extends AppCompatActivity {

  private final List<Demo> demos = new ArrayList<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main_activity);

    getWindow().getDecorView().addOnLayoutChangeListener(layoutChangeListener);

    final View dynamicEndValueTarget = findViewById(R.id.target);
    final DragFrameLayout dynamicEndValueContainer =
      (DragFrameLayout) dynamicEndValueTarget.getParent();
    View dynamicEndValue1 = findViewById(R.id.anchor1);
    View dynamicEndValue2 = findViewById(R.id.anchor2);
    AnchoredForce<?> dynamicEndValueSpring1 = Spring.createCriticallyDamped(75f);
    AnchoredForce<?> dynamicEndValueSpring2 = Spring.createCriticallyDamped(75f);
    final Integrator dynamicEndValueIntegrator = new Rk4Integrator(this);
    final Demo dynamicEndValueDemo =
      Demo.of(dynamicEndValueContainer, dynamicEndValueIntegrator, dynamicEndValueTarget)
        .with(dynamicEndValue1, dynamicEndValue2)
        .with(dynamicEndValueSpring1, dynamicEndValueSpring2);

    dynamicEndValueIntegrator.addListener(new TrackingListener(dynamicEndValueDemo));
    dynamicEndValueContainer.setCallback(
      new TrackingCallback(dynamicEndValueDemo) {
        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
          super.onViewReleased(releasedChild, xvel, yvel);

          if (releasedChild == dynamicEndValueDemo.target) {
            activateAnchoredForceForTargetVelocity(xvel, yvel, dynamicEndValueDemo);
          }
        }
      });
    activateNextForce(dynamicEndValueDemo);
    demos.add(dynamicEndValueDemo);
  }

  private void activateNextForce(Demo demo) {
    if (!demo.integrator.hasForces()) {
      activateForce(0, demo);
    } else if (demo.forces.length > 0) {
      int currentIndex = -1;
      for (int i = 0; i < demo.forces.length; i++) {
        if (demo.integrator.hasForce(demo.forces[i])) {
          currentIndex = i;
          break;
        }
      }
      activateForce((currentIndex + 1) % demo.forces.length, demo);
    }
  }

  private void activateForce(int index, Demo demo) {
    for (int i = 0; i < demo.forces.length; i++) {
      AnchoredForce<?> force = demo.forces[i];
      if (i == index) {
        demo.integrator.addForce(force);
      } else {
        demo.integrator.removeForce(force);
      }
    }
    demo.integrator.start();
  }

  private void activateAnchoredForceForTargetVelocity(float xvel, float yvel, Demo demo) {
    Vector targetLocation = new Vector(getCenterX(demo.target), getCenterY(demo.target));
    Vector[] anchorLocations = new Vector[demo.forces.length];
    for (int i = 0; i < demo.forces.length; i++) {
      AnchoredForce<?> force = demo.forces[i];
      anchorLocations[i] = force.getAnchorPoint();
    }

    Vector flingDirection = new Vector(xvel, yvel).normalize();
    Vector[] anchorDirections = new Vector[demo.forces.length];
    for (int i = 0; i < demo.forces.length; i++) {
      anchorDirections[i] = new Vector().sub(anchorLocations[i], targetLocation).normalize();
    }

    float minAngle = Float.MAX_VALUE;
    int minIndex = -1;
    for (int i = 0; i < anchorDirections.length; i++) {
      float angle = Vectors.angle(flingDirection, anchorDirections[i]);
      if (angle < minAngle) {
        minAngle = angle;
        minIndex = i;
      }
    }

    activateForce(minIndex, demo);
  }

  @Override
  public void onResume() {
    super.onResume();
    for (Demo demo : demos) {
      demo.integrator.start();
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    for (Demo demo : demos) {
      demo.integrator.stop();
    }
  }

  private void setCenter(View view, float x, float y) {
    view.offsetLeftAndRight((int) (x - getCenterX(view)));
    view.offsetTopAndBottom((int) (y - getCenterY(view)));
  }

  private float getCenterX(View view) {
    return view.getLeft() + view.getWidth() / 2f;
  }

  private float getCenterY(View view) {
    return view.getTop() + view.getHeight() / 2f;
  }

  private final OnLayoutChangeListener layoutChangeListener =
    new OnLayoutChangeListener() {
      @Override
      public void onLayoutChange(
        View v,
        int left,
        int top,
        int right,
        int bottom,
        int oldLeft,
        int oldTop,
        int oldRight,
        int oldBottom) {
        // Re-initialize the data of any animators that depend on laid out positioning
        setPhysicsValues();
      }
    };

  private void setPhysicsValues() {
    for (Demo demo : demos) {
      for (int i = 0; i < demo.anchors.length; i++) {
        View anchor = demo.anchors[i];
        AnchoredForce<?> force = demo.forces[i];
        force.setAnchorPoint(new Vector(getCenterX(anchor), getCenterY(anchor)));
      }
      demo.integrator.setState(
        new Vector(getCenterX(demo.target), getCenterY(demo.target)), new Vector());
      demo.integrator.start();
    }
  }

  /**
   * An encapsulation of a physics demo. It consists of the container for all UI components, a
   * target view, multiple forces and their associated anchor views.
   */
  private static class Demo {
    public final DragFrameLayout container;
    public final Integrator integrator;
    public final View target;
    public View[] anchors;
    public AnchoredForce<?>[] forces;

    public static Demo of(
      DragFrameLayout container, Integrator dynamicEndValueIntegrator, View targets) {
      return new Demo(container, dynamicEndValueIntegrator, targets);
    }

    private Demo(DragFrameLayout container, Integrator integrator, View target) {
      this.container = container;
      this.integrator = integrator;
      this.target = target;
    }

    /**
     * Sets the anchor views on this demo. Each anchor view in {@link #anchors} at index
     * <code>i</code> must correspond to the {@link AnchoredForce} in {@link #forces} at index
     * <code>i</code>.
     */
    public Demo with(View... anchors) {
      this.anchors = anchors;
      return this;
    }

    /**
     * Sets the forces on this demo. Each anchor view in {@link #anchors} at index
     * <code>i</code> must correspond to the {@link AnchoredForce} in {@link #forces} at index
     * <code>i</code>.
     * <p>
     * <p>Forces without an anchor view should just be directly added to the {@link
     * Integrator}.
     */
    public Demo with(AnchoredForce<?>... forces) {
      this.forces = forces;
      return this;
    }
  }

  /**
   * An {@link Integrator.Listener} that syncs the integrator's state with the target view's
   * position. When the integrator settles, it activates the next force.
   */
  private class TrackingListener extends SimpleListener {

    private final Demo demo;

    public TrackingListener(Demo demo) {
      this.demo = demo;
    }

    @Override
    public void onUpdate(Vector x, Vector v) {
      setCenter(demo.target, x.getValue(0), x.getValue(1));
    }

    @Override
    public void onSettle() {
      activateNextForce(demo);
    }
  }

  /**
   * A {@link Callback} that allows both the target and anchor views to be captured. It syncs the
   * anchor view's position with the corresponding force's anchor point. When the target view is
   * flung, it calculates which force to activate.
   */
  private class TrackingCallback extends Callback {

    private final Demo demo;

    public TrackingCallback(Demo demo) {
      this.demo = demo;
    }

    @Override
    public boolean tryCaptureView(View child, int pointerId) {
      if (child == demo.target) {
        return true;
      }
      for (View anchor : demo.anchors) {
        if (child == anchor) {
          return true;
        }
      }
      return false;
    }

    @Override
    public void onViewCaptured(View capturedChild, int activePointerId) {
      demo.container.getParent().requestDisallowInterceptTouchEvent(true);
      if (capturedChild == demo.target) {
        demo.integrator.stop();
      }
    }

    @Override
    public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
      for (int i = 0; i < demo.anchors.length; i++) {
        View anchor = demo.anchors[i];
        if (changedView == anchor) {
          AnchoredForce<?> force = demo.forces[i];
          force.setAnchorPoint(new Vector(getCenterX(anchor), getCenterY(anchor)));
          break;
        }
      }
    }

    @Override
    public void onViewReleased(View releasedChild, float xvel, float yvel) {
      if (releasedChild == demo.target) {
        demo.integrator.setState(
          new Vector(getCenterX(releasedChild), getCenterY(releasedChild)),
          new Vector(xvel, yvel));
        demo.integrator.start();
      }
    }

    @Override
    public int clampViewPositionHorizontal(View child, int left, int dx) {
      return left;
    }

    @Override
    public int clampViewPositionVertical(View child, int top, int dy) {
      return top;
    }
  }
}
