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

import com.google.android.material.motion.physics.math.Vector;

/**
 * A dynamic Force that acts on an object. Usually models a force in real life.
 */
public interface Force {

  /**
   * Calculates the acceleration this Force applies on an object.
   *
   * @param x The object's position.
   * @param v The object's velocity.
   * @param t The time elapsed since this Force was first active.
   */
  Vector acceleration(Vector x, Vector v, double t);

  /**
   * Calculates the potential energy of this Force on an object. The potential energy of a force
   * which is a function of an object's position is the integral of the force function with
   * respect to position.
   * <p>
   * Return {@link Integrator#SOME_ENERGY} if the potential energy of this force is difficult or
   * unrealistic to calculate, but is a non-trivial amount.
   * <p>
   * Return {@link Integrator#NO_ENERGY} if this force does not have potential energy or only a
   * trivial amount. For example, if the force is not a function of the object's position.
   *
   * @param x The object's position.
   * @param t The time elapsed since this Force was first active.
   */
  float potentialEnergy(Vector x, double t);
}
