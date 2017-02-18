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
package com.google.android.material.motion.physics.math;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Arrays;

/**
 * A general purpose one dimensional vector class. The <code>size</code> of a vector is effectively
 * final. All calculations involving multiple vectors will assume that all vectors involved are the
 * same size. All calculations that return a Vector will mutate the vector that the method was
 * called on.
 * <p>
 * The only exception is the zero-sized vector. It represents an arbitrarily sized vector with all
 * zero values.
 */
public class Vector {
  private int size;
  @Nullable
  private float[] values;

  public Vector() {
    this.size = 0;
    this.values = null;
  }

  public Vector(Vector vector) {
    if (vector.size == 0) {
      this.size = 0;
      this.values = null;
      return;
    }
    this.size = vector.size;
    this.values = new float[size];
    System.arraycopy(checkNotNull(vector.values), 0, values, 0, size);
  }

  public Vector(float... values) {
    this.size = values.length;
    this.values = new float[size];
    System.arraycopy(values, 0, this.values, 0, size);
  }

  public int getSize() {
    return size;
  }

  public float[] getValues() {
    return checkNotNull(values);
  }

  public float getValue(int index) {
    if (size == 0) {
      return 0f;
    }
    return checkNotNull(values)[index];
  }

  /**
   * Sets this vector to the given vector's values.
   */
  public Vector set(Vector other) {
    if (other.size == 0) {
      size = 0;
      values = null;
      return this;
    }
    if (size == 0) {
      size = other.size;
      values = new float[size];
    }
    checkArgument(size == other.size);
    System.arraycopy(
      checkNotNull(other.values), 0, checkNotNull(values), 0, size);
    return this;
  }

  /**
   * Sets this vector to the given values.
   */
  public Vector set(float... values) {
    if (size == 0) {
      size = values.length;
      this.values = new float[size];
    }
    checkArgument(size == values.length);
    System.arraycopy(values, 0, checkNotNull(this.values), 0, size);
    return this;
  }

  /**
   * Sets this vector to all zero values.
   */
  public Vector clear() {
    if (size > 0) {
      Arrays.fill(checkNotNull(values), 0, size, 0f);
    }
    return this;
  }

  /**
   * Returns <code>a + b</code>, the sum of the given vector and this.
   */
  public Vector add(Vector b) {
    return add(this, b);
  }

  /**
   * Sets this vector to <code>a + b</code>.
   */
  public Vector add(Vector a, Vector b) {
    if (a.size == 0) {
      set(b);
      return this;
    }
    if (b.size == 0) {
      set(a);
      return this;
    }

    if (size == 0) {
      size = a.size;
      values = new float[size];
    }
    checkArgument(size == a.size);
    checkArgument(size == b.size);
    float[] aValues = checkNotNull(a.values);
    float[] bValues = checkNotNull(b.values);
    for (int i = 0; i < a.size; i++) {
      values[i] = aValues[i] + bValues[i];
    }
    return this;
  }

  /**
   * Returns <code>a - b</code>, the subtraction of the given vector from this.
   */
  public Vector sub(Vector b) {
    return sub(this, b);
  }

  /**
   * Sets this vector to <code>a - b</code>.
   */
  public Vector sub(Vector a, Vector b) {
    if (a.size == 0) {
      scale(b, -1f);
      return this;
    }
    if (b.size == 0) {
      set(a);
      return this;
    }

    if (size == 0) {
      size = a.size;
      values = new float[size];
    }
    checkArgument(size == a.size);
    checkArgument(size == b.size);
    float[] aValues = checkNotNull(a.values);
    float[] bValues = checkNotNull(b.values);
    for (int i = 0; i < a.size; i++) {
      values[i] = aValues[i] - bValues[i];
    }
    return this;
  }

  /**
   * Returns <code>k * v</code>, the scaling of this vector by the given scalar.
   */
  public Vector scale(float k) {
    return scale(this, k);
  }

  /**
   * Sets this vector to <code>k * v</code>.
   */
  public Vector scale(Vector v, float k) {
    if (k == 1f) {
      set(v);
      return this;
    }

    if (v.size == 0) {
      size = 0;
      values = null;
      return this;
    }

    if (size == 0) {
      size = v.size;
      values = new float[size];
    }
    checkArgument(size == v.size);
    float[] vValues = checkNotNull(v.values);
    for (int i = 0; i < v.size; i++) {
      values[i] = vValues[i] * k;
    }
    return this;
  }

  /**
   * Returns <code>v / ||v||</code>, the normalization of this vector.
   * <p>
   * <p>If this is a zero vector, the normalization results in a zero vector.
   */
  public Vector normalize() {
    return normalize(this);
  }

  /**
   * Sets this vector to <code>v/||v||</code>, the normalization of this vector.
   * <p>
   * <p>If this is a zero vector, its normalization results in a zero vector.
   */
  public Vector normalize(Vector v) {
    if (v.size == 0) {
      size = 0;
      values = null;
      return this;
    }

    if (size == 0) {
      size = v.size;
      values = new float[size];
    }
    checkArgument(size == v.size);
    float magnitude = v.magnitude();
    if (magnitude == 0f) {
      clear();
      return this;
    }

    float[] vValues = checkNotNull(v.values);
    for (int i = 0; i < v.size; i++) {
      values[i] = vValues[i] / magnitude;
    }
    return this;
  }

  /**
   * Returns whether this vector is normalized within the given epsilon error.
   * <p>
   * <p>A zero vector is considered normalized.
   */
  public boolean isNormalized(float epsilon) {
    float magnitude = magnitude();
    return MathUtils.eq(magnitude, 1f, epsilon) || MathUtils.eq(magnitude, 0f, epsilon);
  }

  /**
   * Returns whether this vector is a zero vector within the given epsilon error.
   */
  public boolean isZero(float epsilon) {
    float magnitude = magnitude();
    return MathUtils.eq(magnitude, 0f, epsilon);
  }

  /**
   * Returns <code>proj<sub>a</sub>b</code>, the projection of the given vector onto this.
   * <p>
   * <p>
   * If this is a zero vector, the projection of the given vector onto this results in itself.
   */
  public Vector proj(Vector b) {
    return proj(this, b);
  }

  /**
   * Sets this vector to <code>proj<sub>a</sub>b</code>, the projection of b onto a.
   * <p>
   * <p>
   * If a is a zero vector, the projection of b onto a results in itself.
   */
  public Vector proj(Vector a, Vector b) {
    if (a.size == 0) {
      set(b);
      return this;
    }
    if (b.size == 0) {
      clear();
      return this;
    }

    if (size == 0) {
      size = a.size;
      values = new float[size];
    }
    checkArgument(size == a.size);
    checkArgument(size == b.size);
    float aa = Vectors.dot(a, a);
    if (aa == 0f) {
      set(b);
      return this;
    }

    float ab = Vectors.dot(a, b);
    float scalar = ab / aa;
    scale(a, scalar);
    return this;
  }

  /**
   * Returns <code>v1&sdot;v2</code>, the dot product of this vector and the given vector.
   */
  public float dot(Vector other) {
    if (size == 0 || other.size == 0) {
      return 0f;
    }

    checkArgument(size == other.size);
    float[] thisValues = checkNotNull(this.values);
    float[] otherValues = checkNotNull(other.values);
    float sum = 0f;
    for (int i = 0; i < size; i++) {
      sum += thisValues[i] * otherValues[i];
    }
    return sum;
  }

  /**
   * Returns <code>&theta;</code>, the unsigned angle in between this normalized vector and the
   * given normalized vector.
   * <p>
   * <p>Zero vectors are considered to have 0f angle with any other vector.
   */
  public float angle(Vector other) {
    checkState(isNormalized(MathUtils.DEFAULT_EPSILON));
    checkState(other.isNormalized(MathUtils.DEFAULT_EPSILON));

    if (isZero(MathUtils.DEFAULT_EPSILON) || other.isZero(MathUtils.DEFAULT_EPSILON)) {
      return 0f;
    }

    return MathUtils.acos(dot(other));
  }

  /**
   * Returns <code>&theta;</code>, the unsigned angle in between this vector and the given vector.
   * Both vectors will be normalized in place.
   * <p>
   * <p>Zero vectors are considered to have 0f angle with any other vector.
   */
  public float angleWithNormalization(Vector other) {
    normalize();
    other.normalize();
    return angle(other);
  }

  /**
   * Returns the unsigned distance between this vector and the given vector.
   * <p>
   * <p>This is identical to <code>||v1-v2||</code>, but without the intermediary vector
   * allocation or mutation.
   */
  public float distance(Vector other) {
    if (size == 0) {
      return other.magnitude();
    }
    if (other.size == 0) {
      return magnitude();
    }

    checkArgument(size == other.size);
    float[] thisValues = checkNotNull(this.values);
    float[] otherValues = checkNotNull(other.values);
    float sum = 0f;
    for (int i = 0; i < size; i++) {
      float diff = thisValues[i] - otherValues[i];
      sum += diff * diff;
    }
    return MathUtils.sqrt(sum);
  }

  /**
   * Returns <code>||v||</code>, the magnitude of this vector.
   */
  public float magnitude() {
    return MathUtils.sqrt(Vectors.dot(this, this));
  }

  @Override
  public String toString() {
    return String.format("%s %s", Vector.class.getSimpleName(), Arrays.toString(values));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Vector vector = (Vector) o;

    return size == 0 && vector.size == 0
      || size == 0 && vector.magnitude() == 0
      || vector.size == 0 && magnitude() == 0
      || Arrays.equals(values, vector.values);
  }

  @Override
  public int hashCode() {
    if (size == 0 || magnitude() == 0) {
      return 0;
    }

    return Arrays.hashCode(values);
  }

  /**
   * Ensures that an object reference passed as a parameter to the calling method is not null.
   *
   * @param reference an object reference
   * @return the non-null reference that was validated
   * @throws NullPointerException if {@code reference} is null
   */
  @NonNull
  private static <T> T checkNotNull(@Nullable T reference) {
    if (reference == null) {
      throw new NullPointerException();
    }
    return reference;
  }

  /**
   * Ensures the truth of an expression involving one or more parameters to the calling method.
   *
   * @param expression a boolean expression
   * @throws IllegalArgumentException if {@code expression} is false
   */
  private static void checkArgument(boolean expression) {
    if (!expression) {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Ensures the truth of an expression involving the state of the calling instance, but not
   * involving any parameters to the calling method.
   *
   * @param expression a boolean expression
   * @throws IllegalStateException if {@code expression} is false
   */
  private static void checkState(boolean expression) {
    if (!expression) {
      throw new IllegalStateException();
    }
  }
}
