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
package com.google.android.material.motion.physics.forces;

import com.google.android.material.motion.physics.Integrator;
import com.google.android.material.motion.physics.math.Vector;

import static com.google.android.material.motion.physics.math.Vectors.dot;

/**
 * A force that models a damped spring with a spring constant <code>k</code> and damping coefficient
 * <code>b</code>. All springs assume zero resting distance from the anchor point.
 */
public class Spring extends AnchoredForce<Spring> {

  public float k = 342f;
  public float b = 30f;

  private final Vector tmp1 = new Vector();
  private final Vector tmp2 = new Vector();

  /**
   * Creates a spring with the given spring constant and damping coefficient.
   */
  public static Spring create(float k, float b) {
    Spring spring = new Spring();
    spring.k = k;
    spring.b = b;

    return spring;
  }

  /**
   * Creates a critically damped spring with the given spring constant.
   * <p>
   * The damping coefficient is chosen so that for all initial states: <ul> <li>The spring has at
   * most one overshoot.</li> <li>The spring comes to rest in the minimum possible duration.</li>
   * </ul>
   */
  public static Spring createCriticallyDamped(float k) {
    Spring spring = new Spring();
    spring.k = k;
    spring.b = (float) Math.sqrt(4 * Integrator.MASS * k);

    return spring;
  }

  /**
   * Creates a viscous frictional force that opposes the object's velocity.
   */
  public static Spring createFriction(float mu) {
    Spring spring = new Spring();
    spring.k = 0f;
    spring.b = mu;

    return spring;
  }

  @Override
  public Vector acceleration(Vector x, Vector v, double t) {
    Vector displacement = displacement(x, tmp2);

    // Vector tension = -k * displacement;
    Vector tension = tmp1;
    tension.scale(displacement, -k);

    // Vector damping = -b * v;
    Vector damping = tmp2;
    damping.scale(v, -b);

    // Vector force = tension + damping;
    Vector force = tension.add(damping);
    return force.scale(1f / Integrator.MASS);
  }

  @Override
  public float potentialEnergy(Vector x, double t) {
    Vector displacement = displacement(x, tmp2);
    return 0.5f * k * dot(displacement, displacement);
  }
}
