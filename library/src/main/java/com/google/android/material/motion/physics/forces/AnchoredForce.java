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

import com.google.android.material.motion.physics.Force;
import com.google.android.material.motion.physics.math.Vector;

/**
 * A force with an intrinsic anchor point. The direction and magnitude that this force acts upon an
 * object is usually a function of the displacement between that object and the anchor point.
 *
 * @param <T> Subclass type. This is used to enable method chaining.
 */
public abstract class AnchoredForce<T extends AnchoredForce<?>> implements Force {
  private final Vector anchorPoint = new Vector();
  private final T self;

  @SuppressWarnings("unchecked")
  protected AnchoredForce() {
    self = (T) this;
  }

  public Vector getAnchorPoint() {
    return new Vector(anchorPoint);
  }

  /**
   * Sets the anchor point of the force. All displacements are calculated relative to this point.
   */
  public T setAnchorPoint(Vector anchorPoint) {
    this.anchorPoint.set(anchorPoint);
    return self;
  }

  protected final Vector displacement(Vector x, Vector out) {
    out.sub(x, anchorPoint);
    return out;
  }
}
