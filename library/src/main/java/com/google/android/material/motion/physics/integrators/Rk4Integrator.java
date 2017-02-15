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
package com.google.android.material.motion.physics.integrators;

import android.content.Context;

import com.google.android.material.motion.physics.Integrator;
import com.google.android.material.motion.physics.math.Vector;

/**
 * An integrator based on the Runge-Kutta method.
 */
public class Rk4Integrator extends Integrator {
  private static final float FRAME = 1f / 60f;
  /**
   * Experimentally determined. Lowest value with acceptable accuracy.
   */
  private static final float ITERATIONS_PER_FRAME = 2;
  private static final float DT = (1 / ITERATIONS_PER_FRAME) * FRAME;

  private final State tmpState = new State();
  private final Derivative tmpDerivative1 = new Derivative();
  private final Derivative tmpDerivative2 = new Derivative();
  private final Derivative tmpDerivative3 = new Derivative();
  private final Derivative tmpDerivative4 = new Derivative();
  private final Vector tmpVector1 = new Vector();
  private final Vector tmpVector2 = new Vector();

  private double accumulator;

  public Rk4Integrator(Context context) {
    super(context);
  }

  @Override
  protected State onFrame(
    double deltaTime, State current, State previous, float animatorDurationScale) {
    deltaTime = deltaTime / animatorDurationScale;
    float dt = DT / animatorDurationScale;

    accumulator += deltaTime;

    while (accumulator >= dt) {
      accumulator -= dt;
      previous.set(current);
      integrate(current, dt);
    }

    return interpolate(previous, current, (float) (accumulator / dt), tmpState);
  }

  private void integrate(State state, float dt) {
    Derivative a = evaluate(state, tmpDerivative1);
    Derivative b = evaluate(state, dt * 0.5f, a, tmpDerivative2);
    Derivative c = evaluate(state, dt * 0.5f, b, tmpDerivative3);
    Derivative d = evaluate(state, dt, c, tmpDerivative4);

    // Vector dxdt = 1.0f / 6.0f * (a.dx + 2.0f * (b.dx + c.dx) + d.dx);
    Vector dxdt = tmpVector1;
    dxdt.add(b.dx, c.dx);
    dxdt.scale(2.0f);
    dxdt.add(a.dx);
    dxdt.add(d.dx);
    dxdt.scale(1.0f / 6.0f);

    // Vector dvdt = 1.0f / 6.0f * (a.dv + 2.0f * (b.dv + c.dv) + d.dv);
    Vector dvdt = tmpVector2;
    dvdt.add(b.dv, c.dv);
    dvdt.scale(2.0f);
    dvdt.add(a.dv);
    dvdt.add(d.dv);
    dvdt.scale(1.0f / 6.0f);

    // state.x = state.x + dxdt * dt;
    dxdt.scale(dt);
    state.x.add(dxdt);

    // state.v = state.v + dvdt * dt;
    dvdt.scale(dt);
    state.v.add(dvdt);

    // state.t = state.t + dt;
    state.t += dt;
  }

  private Derivative evaluate(State initial, Derivative out) {
    out.dx.set(initial.v);
    out.dv.set(acceleration(initial, tmpVector1));
    return out;
  }

  private Derivative evaluate(State initial, float dt, Derivative d, Derivative out) {
    State state = tmpState;

    // state.x = initial.x + d.dx * dt;
    Vector x = state.x;
    x.scale(d.dx, dt);
    x.add(initial.x);

    // state.v = initial.v + d.dv * dt;
    Vector v = state.v;
    v.scale(d.dv, dt);
    v.add(initial.v);

    state.t = initial.t + dt;

    out.dx.set(state.v);
    out.dv.set(acceleration(state, tmpVector1));
    return out;
  }

  private State interpolate(State previous, State current, float alpha, State out) {
    // out.x = current.x * alpha + previous.x * (1 - alpha);
    Vector x = out.x;
    x.scale(current.x, alpha);
    tmpVector1.scale(previous.x, 1 - alpha);
    x.add(tmpVector1);

    // out.v = current.v * alpha + previous.v * (1 - alpha);
    Vector v = out.v;
    v.scale(current.v, alpha);
    tmpVector1.scale(previous.v, 1 - alpha);
    v.add(tmpVector1);

    out.t = current.t * alpha + previous.t * (1 - alpha);

    return out;
  }
}
