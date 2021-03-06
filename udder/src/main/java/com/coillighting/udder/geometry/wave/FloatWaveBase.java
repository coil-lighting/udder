package com.coillighting.udder.geometry.wave;

import com.coillighting.udder.mix.TimePoint;

/** Abstract base class for removing boilerplate from the implementation of
 *  periodic floating-point signal generators.
 */
public abstract class FloatWaveBase implements Wave<Float> {

    protected float start = 0.0f;
    protected float end = 0.0f;
    protected long period = 0;

    public FloatWaveBase(float start, float end, long period) {
        this.start = start;
        this.end = end;
        this.period = period;
    }

    public Float getValue(TimePoint time) {
        return this.getVal(time);
    }

    public float getVal(TimePoint time) {
        if(period <= 0.0f) {
            return start;
        } else {
            // Normalize time to unit length and select + or - ramp.
            long dt = time.sceneTimeMillis() % period;
            float balance = (float) dt / (float) period;
            return this.interpolate(balance);
        }
    }

    public abstract float interpolate(float x);
}
