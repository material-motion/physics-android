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

import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.provider.Settings.Global;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.SimpleArrayMap;

import com.google.android.material.motion.physics.ChoreographerCompat.FrameCallback;
import com.google.android.material.motion.physics.math.Vector;

import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.google.android.material.motion.physics.math.Vectors.dot;

/**
 * An integrator that runs an ongoing physics simulation of an object with multiple forced being
 * applied to it.
 */
public abstract class Integrator {

  /**
   * A Listener that receives notifications from a physics simulation, including on every
   * animation frame.
   */
  public interface Listener {

    /**
     * Notifies the start of the physics simulation.
     */
    void onStart();

    /**
     * Notifies the occurrence of another frame of the physics simulation. This is called after
     * the current frame's values have been calculated.
     *
     * @param x The position of the object.
     * @param v The velocity of the object.
     */
    void onUpdate(Vector x, Vector v);

    /**
     * Notifies the physics simulation coming to a settled state. This is called after the total
     * energy of the system is below the energy threshold.
     */
    void onSettle();

    /**
     * Notifies the end of the physics simulation. This is called upon reaching a settled state,
     * or if {@link #stop()} is called externally.
     */
    void onStop();
  }

  /**
   * This class provides empty implementations of the methods from {@link Listener}. Any
   * custom listener that cares only about a subset of the methods of this listener can simply
   * subclass this adapter class instead of implementing the interface directly.
   */
  public abstract static class SimpleListener implements Listener {

    @Override
    public void onStart() {
    }

    @Override
    public void onUpdate(Vector x, Vector v) {
    }

    @Override
    public void onSettle() {
    }

    @Override
    public void onStop() {
    }
  }

  public static final float MASS = 1f;
  /**
   * The minimum amount of energy required to continue the physics simulation.
   * <p>
   * This value can be returned from {@link Force#potentialEnergy(Vector, double)} to signify that
   * the force acting on the given object constitutes some non-trivial amount of potential
   * energy.
   */
  public static final float SOME_ENERGY = 1f;
  /**
   * This value can be returned from {@link Force#potentialEnergy(Vector, double)} to signify that
   * the force acting on the given object constitutes zero or a trivial amount of potential
   * energy.
   */
  public static final float NO_ENERGY = 0f;

  private static final float FRAME = 1f / 60f;
  private static final float MAX_FRAMES_TO_SIMULATE = 4;
  private static final float MAX_DELTA = MAX_FRAMES_TO_SIMULATE * FRAME;

  private final ChoreographerCompat choreographer = ChoreographerCompat.getInstance();
  private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
  /**
   * A mapping from a Force to the initial time at which that force was first active in this
   * system.
   */
  private final SimpleArrayMap<Force, Double> forces = new SimpleArrayMap<>();

  private final State current = new State();
  private final State previous = new State();
  private final State interpolated = new State();
  private final float animatorDurationScale;

  private boolean isActive;
  private boolean isScheduled;
  private double lastTime = -1;

  public Integrator(Context context) {
    if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
      animatorDurationScale =
        Global.getFloat(context.getContentResolver(), Global.ANIMATOR_DURATION_SCALE, 1.0f);
    } else {
      animatorDurationScale = 1f;
    }
  }

  public final void start() {
    boolean wasActive = isActive;

    isActive = true;
    schedule();

    if (!wasActive) {
      lastTime = -1;
      for (int i = 0, count = forces.size(); i < count; i++) {
        forces.setValueAt(i, lastTime);
      }

      for (Listener listener : listeners) {
        listener.onStart();
      }
    }
  }

  public final void stop() {
    boolean wasActive = isActive;

    isActive = false;
    unschedule();

    if (wasActive) {
      for (Listener listener : listeners) {
        listener.onStop();
      }
    }
  }

  public final Integrator setState(Vector x, Vector v) {
    current.x.set(x);
    current.v.set(v);

    previous.x.set(x);
    previous.v.set(v);

    interpolated.x.set(x);
    interpolated.v.set(v);
    return this;
  }

  public final State getState() {
    return new State().set(interpolated);
  }

  public final Integrator addListener(Listener listener) {
    if (!listeners.contains(listener)) {
      listeners.add(listener);
    }
    return this;
  }

  public final Integrator removeListener(Listener listener) {
    listeners.remove(listener);
    return this;
  }

  public final Integrator addForce(Force force) {
    if (!hasForce(force)) {
      forces.put(force, lastTime);
    }
    return this;
  }

  public final Integrator removeForce(Force force) {
    forces.remove(force);
    return this;
  }

  public final boolean hasForce(Force force) {
    return forces.containsKey(force);
  }

  public final boolean hasForces() {
    return !forces.isEmpty();
  }

  public final Integrator clearForces() {
    forces.clear();
    return this;
  }

  private void schedule() {
    boolean wasScheduled = isScheduled;

    isScheduled = true;

    if (!wasScheduled) {
      choreographer.postFrameCallback(frameCallback);
    }
  }

  private void unschedule() {
    boolean wasScheduled = isScheduled;

    isScheduled = false;

    if (wasScheduled) {
      choreographer.removeFrameCallback(frameCallback);
    }
  }

  private void doFrame(double frameTime) {
    boolean hasEnergy =
      hasKineticEnergyGreaterThan(current, SOME_ENERGY)
        || hasPotentialEnergyGreaterThan(current, SOME_ENERGY);
    if (!hasEnergy) {
      for (Listener listener : listeners) {
        listener.onSettle();
      }

      // Ensure listener did not #start.
      if (!isScheduled) {
        stop();
      }
      return;
    }

    schedule();

    if (lastTime == -1) {
      lastTime = frameTime;
    }

    double deltaTime = frameTime - lastTime;
    lastTime = frameTime;

    if (deltaTime > MAX_DELTA) {
      deltaTime = MAX_DELTA;
    }

    for (int i = 0, count = forces.size(); i < count; i++) {
      if (forces.valueAt(i) == -1) {
        forces.setValueAt(i, lastTime);
      }
    }

    State state = onFrame(deltaTime);

    for (Listener listener : listeners) {
      listener.onUpdate(state.x, state.v);
    }
  }

  @VisibleForTesting
  final State onFrame(double deltaTime) {
    State state = onFrame(deltaTime, current, previous, animatorDurationScale);
    interpolated.set(state);
    return interpolated;
  }

  /**
   * Advance the physics simulation by one frame. The implementation should update
   * <code>current</code> and <code>previous</code>, then return an interpolated State.
   */
  protected abstract State onFrame(
    double deltaTime, State current, State previous, float animatorDurationScale);

  protected final Vector acceleration(State state, Vector out) {
    out.clear();
    for (int i = 0, count = forces.size(); i < count; i++) {
      Force force = forces.keyAt(i);
      double initialTime = forces.valueAt(i);
      out.add(force.acceleration(state.x, state.v, state.t - initialTime));
    }
    return out;
  }

  @VisibleForTesting
  final boolean hasKineticEnergyGreaterThan(State state, float energy) {
    return 0.5f * MASS * dot(state.v, state.v) > energy;
  }

  @VisibleForTesting
  final boolean hasPotentialEnergyGreaterThan(State state, float energy) {
    for (int i = 0, count = forces.size(); i < count; i++) {
      Force force = forces.keyAt(i);
      double initialTime = forces.valueAt(i);
      float potentialEnergy = force.potentialEnergy(state.x, state.t - initialTime);
      if (potentialEnergy > energy) {
        return true;
      }
    }
    return false;
  }

  private final FrameCallback frameCallback =
    new FrameCallback() {
      private static final double NANOS_PER_SECOND = 1000000000.0;

      @Override
      public void doFrame(long frameTimeNanos) {
        isScheduled = false;
        Integrator.this.doFrame(frameTimeNanos / NANOS_PER_SECOND);
      }
    };

  /**
   * A data holder for a <code>position</code> and <code>velocity</code> at time <code>t</code>.
   */
  public static class State {
    public final Vector x = new Vector();
    public final Vector v = new Vector();
    public double t;

    public State set(State other) {
      this.x.set(other.x);
      this.v.set(other.v);
      this.t = other.t;
      return this;
    }

    @Override
    public String toString() {
      return String.format(Locale.US, "%s\nx=%s\nv=%s", State.class.getSimpleName(), x, v);
    }
  }

  /**
   * A data holder for rate of change. This is used to calculate intermediary {@link State
   * States}.
   */
  public static class Derivative {
    public final Vector dx = new Vector();
    public final Vector dv = new Vector();

    @Override
    public String toString() {
      return String.format(Locale.US, "%s\ndx=%s\ndv=%s", State.class.getSimpleName(), dx, dv);
    }
  }
}
