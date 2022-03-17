package com.zchd.sdk.floatviewdemo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.lang.ref.WeakReference;


/**
 * Copyright (C), 2020-2022, 中传互动（湖北）信息技术有限公司
 * Author: HeChao
 * Date: 2022/3/15 16:37
 * Description:
 */
public class FloatView extends FrameLayout implements GestureDetector.OnGestureListener {

    private final String TAG = FloatView.class.getSimpleName();
    private WindowManager windowManager;
    private WindowManager.LayoutParams windowManagerParams;
    private GestureDetector gestureDetector;
    private final int[] initialCoordinate = new int[2];
    private int screenWidth, screenHeight;
    private int verticalBound = 0;
    private boolean isDragging = false;
    private long fingerUpTime;
    private AttachEdgeMode attachEdgeMode = AttachEdgeMode.ALL;
    private static FloatView floatView;
    private WeakReference<TextView> badgeView;

    public static void attach(Activity activity) {
        attach(activity, AttachEdgeMode.ALL);
    }

    public static void attach(Activity activity, AttachEdgeMode mode) {
        if (floatView == null) {
            synchronized (FloatView.class) {
                if (floatView == null) {
                    floatView = new FloatView(activity);
                    floatView.attachEdgeMode = mode;
                }
            }
        }
    }

    public FloatView(Context context) {
        this(context, null);
    }

    public FloatView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setBackgroundResource(R.mipmap.ic_dragview);
        int notchHeight = NotchUtil.getNotchHeight(context);
        int[] screenSize = getScreenSize();
        if (context instanceof Activity && ((Activity) context).getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            screenWidth = screenSize[0] + notchHeight;
            screenHeight = screenSize[1];
        } else {
            screenWidth = screenSize[0];
            screenHeight = screenSize[1] + notchHeight;
        }
        post(new Runnable() {
            @Override
            public void run() {
                verticalBound = screenHeight / 2 - getWidth() / 2;
            }
        });
        initWindowManager(context);
        gestureDetector = new GestureDetector(context, this);
        postDelayed(new Runnable() {
            @Override
            public void run() {
                hideAnimation(Direction.HORIZONTAL, 0, -getWidth() / 2);
            }
        }, 1000);

        addBadge(context);
    }

    private void initWindowManager(Context context) {
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManagerParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.RGBA_8888);
        windowManagerParams.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
        windowManager.addView(this, windowManagerParams);
    }

    public static FloatView getInstance() {
        return floatView;
    }

    public void detach() {
        if (windowManager != null) {
            windowManager.removeView(this);
        }
        floatView = null;
        badgeView = null;
    }

    public void show() {
        if (floatView != null && floatView.getVisibility() == View.GONE) {
            floatView.setVisibility(VISIBLE);
            animationScale(floatView, 0, 1, new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    floatView.setVisibility(VISIBLE);
                }
            });
        }
    }

    public void hide() {
        if (floatView != null && floatView.getVisibility() == View.VISIBLE) {
            animationScale(floatView, 1, 0, new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    floatView.setVisibility(GONE);
                }
            });
        }
    }

    private void addBadge(Context context) {
        badgeView = new WeakReference<>(new TextView(context));
//        badgeView.setText("1");
        badgeView.get().setTextColor(ContextCompat.getColor(context, R.color.white));
//        badgeView.setTextSize(13);
        badgeView.get().setBackgroundResource(R.drawable.shape_red_dot);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp2px(10), dp2px(10));
        params.gravity = Gravity.END | Gravity.TOP;
        params.setMargins(dp2px(5), dp2px(15), dp2px(5), dp2px(15));
        addView(badgeView.get(), params);
        badgeView.get().setVisibility(GONE);
    }

    public void showBadge() {
        if (badgeView != null && badgeView.get().getVisibility() == View.GONE) {
            badgeView.get().setVisibility(VISIBLE);
            animationScale(badgeView.get(), 0, 1, new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    badgeView.get().setVisibility(VISIBLE);
                }
            });
        }
    }

    public void hideBadge() {
        if (badgeView != null && badgeView.get().getVisibility() == View.VISIBLE) {
            animationScale(badgeView.get(), 1, 0, new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    badgeView.get().setVisibility(GONE);
                }
            });
        }
    }

    private void updateBadgeCoordinate(Direction direction) {
        if (badgeView.get() == null)
            return;
        FrameLayout.LayoutParams params = (LayoutParams) badgeView.get().getLayoutParams();
        if (direction == Direction.HORIZONTAL) {
            params.setMargins(dp2px(5), dp2px(15), dp2px(5), dp2px(15));
            if (windowManagerParams.x <= 0) {
                params.gravity = Gravity.END | Gravity.TOP;
            } else {
                params.gravity = Gravity.START | Gravity.TOP;
            }
        } else {
            if (windowManagerParams.y <= 0) {
                params.setMargins(0, 0, 0, 0);
                params.gravity = Gravity.CENTER | Gravity.BOTTOM;
            } else {
                params.setMargins(0, 0, 0, 0);
                params.gravity = Gravity.CENTER | Gravity.TOP;
            }
        }
        badgeView.get().setLayoutParams(params);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (System.currentTimeMillis() - fingerUpTime < 350) {
                    Log.i(TAG, "快速点击");
                    return false;
                }
                fingerUpTime = System.currentTimeMillis();
                float width = getWidth(), height = getHeight(), left = event.getRawX(), top = event.getRawY(), right, bottom;
                right = screenWidth - left - width / 2;
                bottom = screenHeight - top - height / 2;

                float verticalOffset = Math.min(top, bottom);
                float horizontalOffset = Math.min(left, right);

                Direction direction;
                int[] target;
                if (attachEdgeMode == AttachEdgeMode.VERTICAL) {
                    direction = Direction.VERTICAL;
                    target = calculationVerticalTarget(top);
                } else if (attachEdgeMode == AttachEdgeMode.HORIZONTAL) {
                    direction = Direction.HORIZONTAL;
                    target = calculationHorizontalTarget(left);
                } else {
                    //垂直吸边
                    if (verticalOffset < horizontalOffset) {
                        direction = Direction.VERTICAL;
                        target = calculationVerticalTarget(top);
                    } else {//水平吸边
                        direction = Direction.HORIZONTAL;
                        target = calculationHorizontalTarget(left);
                    }
                }
                attachEdge(direction, target[0], target[1]);
        }
        return gestureDetector.onTouchEvent(event);
    }

    private int[] calculationVerticalTarget(Float top) {
        int[] target = new int[2];
        if (top > screenHeight / 2F) {
            target[0] = windowManagerParams.y;
            target[1] = verticalBound;
        } else {
            target[0] = windowManagerParams.y;
            target[1] = -verticalBound;
        }
        return target;
    }

    private int[] calculationHorizontalTarget(Float left) {
        int[] target = new int[2];
        if (left > screenWidth / 2F) {
            target[0] = windowManagerParams.x;
            target[1] = (int) (screenWidth - getWidth());
        } else {
            target[0] = windowManagerParams.x;
        }
        if (target[1] < 0) {
            target[1] = 0;
        }
        if (target[1] > screenWidth) {
            target[1] = (int) (screenWidth - getWidth());
        }
        return target;
    }

    private void attachEdge(final Direction direction, float fromTarget, float toTarget) {
        animationTranslation(direction, fromTarget, toTarget, 350, new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int value = (int) (float) valueAnimator.getAnimatedValue();
                if (direction == Direction.HORIZONTAL) {
                    int y = windowManagerParams.y;
                    //修正左上角，右上角边界问题
                    if (y < -verticalBound) {
                        y = -verticalBound;
                    } else if (y > verticalBound)
                        y = verticalBound;
                    updateWindowManagerPosition(value, y);
                } else {
                    int x = windowManagerParams.x;
                    //修正左下角，右下角边界问题
                    if (x < 0) {
                        x = 0;
                    } else if (x > (screenWidth - getWidth())) {
                        x = screenWidth - getWidth();
                    }
                    updateWindowManagerPosition(x, value);
                }

                //隐藏动画
                if (value == toTarget) {
                    updateBadgeCoordinate(direction);
                    getHandler().removeCallbacksAndMessages(null);
                    isDragging = false;
                    postDelayed(new HideRunnable(direction, value), 1000);
                }
            }
        });
    }

    private class HideRunnable implements Runnable {
        private final Direction direction;
        private final int value;

        public HideRunnable(Direction direction, int value) {
            this.direction = direction;
            this.value = value;
        }

        @Override
        public void run() {
            if (isDragging)
                return;
            int fromTarget = 0, toTarget = 0;
            if (direction == Direction.HORIZONTAL) {
                if (value > screenWidth / 2) {
                    fromTarget = windowManagerParams.x;
                    toTarget = value + getWidth() / 2;
                } else {
                    toTarget = value - getWidth() / 2;
                }
            } else {
                fromTarget = value;
                if (value < 0) {
                    toTarget = value - getHeight() / 2;
                } else {
                    toTarget = value + getHeight() / 2;
                }
            }
            hideAnimation(direction, fromTarget, toTarget);
        }
    }

    private void hideAnimation(Direction direction, int fromTarget, int toTarget) {
        animationTranslation(direction, fromTarget, toTarget, 250, new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int value = (int) (float) valueAnimator.getAnimatedValue();
                if (direction == Direction.HORIZONTAL) {
                    updateWindowManagerPosition(value, windowManagerParams.y);
                } else {
                    updateWindowManagerPosition(windowManagerParams.x, value);
                }
            }
        });
    }

    private void animationTranslation(final Direction direction, float fromTarget, float toTarget, int duration, ValueAnimator.AnimatorUpdateListener listener) {
        String translationDirection = direction == Direction.VERTICAL ? "translationY" : "translationX";
        @SuppressLint("ObjectAnimatorBinding")
        ObjectAnimator translation = ObjectAnimator.ofFloat(windowManager, translationDirection, fromTarget, toTarget);
        translation.setDuration(duration);
        translation.addUpdateListener(listener);
        translation.start();
    }

    private void animationScale(View target, int from, int to, AnimatorListenerAdapter listenerAdapter) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(target, "scaleX", from, to);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(target, "scaleY", from, to);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(200);
        animatorSet.addListener(listenerAdapter);
        animatorSet.start();
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        initialCoordinate[0] = windowManagerParams.x;
        initialCoordinate[1] = windowManagerParams.y;
        return false;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {
        Log.i(TAG, "onShowPress");
    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        Log.i(TAG, "onSingleTapUp");
        performClick();
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        isDragging = true;
        int x = (int) (motionEvent1.getRawX() - motionEvent.getRawX()) + initialCoordinate[0];
        int y = (int) (motionEvent1.getRawY() - motionEvent.getRawY()) + initialCoordinate[1];
        updateWindowManagerPosition(x, y);
        return true;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    private void updateWindowManagerPosition(int x, int y) {
        windowManagerParams.x = x;
        windowManagerParams.y = y;
        windowManager.updateViewLayout(this, windowManagerParams);
    }

    private int[] getScreenSize() {
        int[] size = new int[2];
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        size[0] = metrics.widthPixels;
        size[1] = metrics.heightPixels;
        return size;
    }

    private int dp2px(int dp) {
        return (int) (Resources.getSystem().getDisplayMetrics().density * dp + 0.5);
    }

    enum AttachEdgeMode {
        ALL(-1),
        VERTICAL(0),
        HORIZONTAL(1);

        AttachEdgeMode(int direction) {
        }
    }

    enum Direction {
        VERTICAL(0),
        HORIZONTAL(1);

        Direction(int direction) {
        }
    }
}
