package com.andreibelous.yogalessons.view.waves;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.Random;

public class BlobDrawable {

    private static final float MAX_SPEED = 8.2f;
    private static final float MIN_SPEED = 0.8f;

    public static float SCALE_BIG_MIN = 0.878f;
    public static float SCALE_SMALL_MIN = 0.926f;

    public float minRadius;
    public float maxRadius;

    private final Path path = new Path();
    public Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final float[] radius;
    private final float[] angle;
    private final float[] radiusNext;
    private final float[] angleNext;
    private final float[] progress;
    private final float[] speed;


    private final float[] pointStart = new float[4];
    private final float[] pointEnd = new float[4];

    private final Random random = new Random();

    private final float N;
    private final float L;
    private final float cubicBezierK = 1f;

    private final Matrix m = new Matrix();

    public BlobDrawable(int n) {
        N = n;
        L = (float) ((4.0 / 3.0) * Math.tan(Math.PI / (2 * N)));
        radius = new float[n];
        angle = new float[n];

        radiusNext = new float[n];
        angleNext = new float[n];
        progress = new float[n];
        speed = new float[n];

        for (int i = 0; i < N; i++) {
            generateBlob(radius, angle, i);
            generateBlob(radiusNext, angleNext, i);
            progress[i] = 0;
        }
    }

    private void generateBlob(float[] radius, float[] angle, int i) {
        float angleDif = 360f / N * 0.05f;
        float radDif = maxRadius - minRadius;
        radius[i] = minRadius + Math.abs(((random.nextInt() % 100f) / 100f)) * radDif;
        angle[i] = 360f / N * i + ((random.nextInt() % 100f) / 100f) * angleDif;
        speed[i] = (float) (0.017 + 0.003 * (Math.abs(random.nextInt() % 100f) / 100f));
    }

    public void update(float amplitude, float speedScale) {
        for (int i = 0; i < N; i++) {
            progress[i] += (speed[i] * MIN_SPEED) + amplitude * speed[i] * MAX_SPEED * speedScale;
            if (progress[i] >= 1f) {
                progress[i] = 0;
                radius[i] = radiusNext[i];
                angle[i] = angleNext[i];
                generateBlob(radiusNext, angleNext, i);
            }
        }
    }

    public void draw(float cX, float cY, Canvas canvas, Paint paint) {
        path.reset();

        for (int i = 0; i < N; i++) {
            float progress = this.progress[i];
            int nextIndex = i + 1 < N ? i + 1 : 0;
            float progressNext = this.progress[nextIndex];
            float r1 = radius[i] * (1f - progress) + radiusNext[i] * progress;
            float r2 = radius[nextIndex] * (1f - progressNext) + radiusNext[nextIndex] * progressNext;
            float angle1 = angle[i] * (1f - progress) + angleNext[i] * progress;
            float angle2 = angle[nextIndex] * (1f - progressNext) + angleNext[nextIndex] * progressNext;

            float l = L * (Math.min(r1, r2) + (Math.max(r1, r2) - Math.min(r1, r2)) / 2f) * cubicBezierK;
            m.reset();
            m.setRotate(angle1, cX, cY);

            pointStart[0] = cX;
            pointStart[1] = cY - r1;
            pointStart[2] = cX + l;
            pointStart[3] = cY - r1;

            m.mapPoints(pointStart);

            pointEnd[0] = cX;
            pointEnd[1] = cY - r2;
            pointEnd[2] = cX - l;
            pointEnd[3] = cY - r2;

            m.reset();
            m.setRotate(angle2, cX, cY);

            m.mapPoints(pointEnd);

            if (i == 0) {
                path.moveTo(pointStart[0], pointStart[1]);
            }

            path.cubicTo(
                    pointStart[2], pointStart[3],
                    pointEnd[2], pointEnd[3],
                    pointEnd[0], pointEnd[1]
            );
        }

        canvas.save();
        canvas.drawPath(path, paint);
        canvas.restore();
    }

    public void generateBlob() {
        for (int i = 0; i < N; i++) {
            generateBlob(radius, angle, i);
            generateBlob(radiusNext, angleNext, i);
            progress[i] = 0;
        }
    }


    private float animateToAmplitude;
    public float amplitude;
    private float animateAmplitudeDiff;

    private final static float ANIMATION_SPEED_WAVE_HUGE = 0.65f;
    private final static float ANIMATION_SPEED_WAVE_SMALL = 0.45f;
    private final static float animationSpeed = 1f - ANIMATION_SPEED_WAVE_HUGE;
    private final static float animationSpeedTiny = 1f - ANIMATION_SPEED_WAVE_SMALL;

    public void setValue(float value, boolean isBig) {
        animateToAmplitude = value;
        if (isBig) {
            if (animateToAmplitude > amplitude) {
                animateAmplitudeDiff = (animateToAmplitude - amplitude) / (100f + 300f * animationSpeed);
            } else {
                animateAmplitudeDiff = (animateToAmplitude - amplitude) / (100f + 500f * animationSpeed);
            }
        } else {
            if (animateToAmplitude > amplitude) {
                animateAmplitudeDiff = (animateToAmplitude - amplitude) / (100f + 400f * animationSpeedTiny);
            } else {
                animateAmplitudeDiff = (animateToAmplitude - amplitude) / (100f + 500f * animationSpeedTiny);
            }
        }
    }

    public void updateAmplitude(long dt) {
        if (animateToAmplitude != amplitude) {
            amplitude += animateAmplitudeDiff * dt;
            if (animateAmplitudeDiff > 0) {
                if (amplitude > animateToAmplitude) {
                    amplitude = animateToAmplitude;
                }
            } else {
                if (amplitude < animateToAmplitude) {
                    amplitude = animateToAmplitude;
                }
            }
        }
    }
}
