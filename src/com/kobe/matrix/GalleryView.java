package com.kobe.matrix;

import java.util.Arrays;
import java.util.Comparator;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.text.style.TtsSpan.OrdinalBuilder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewGroup.OnHierarchyChangeListener;
import android.view.animation.Transformation;
import android.widget.EdgeEffect;
import android.widget.OverScroller;

public class GalleryView extends ViewGroup implements OnHierarchyChangeListener {

	private static final int INVALID_POINTER = -1;
	private int mWidth, mHeight;
	private int mUnselectedGap;
	private int mDensity;
	private int maxRotation;
	private Matrix mMatrix = new Matrix();
	private Camera transformationCamera;
	private int mSelectedPos;
	private boolean mIsBeingDragged;
	private float mLastMotionX, mLastMotionY;
	private int mTouchSlop;
	private int mActivePointerId;
	private VelocityTracker mVelocityTracker;
	private int mMinimumVelocity;
	private int mMaximumVelocity;

	private int mOverscrollDistance;
	private int mOverflingDistance;

	private EdgeEffect mEdgeGlowTop;
	private EdgeEffect mEdgeGlowBottom;
	private Integer[] drawOrder;

	private int duration = 500;

	private boolean toRight;

	public GalleryView(Context context, AttributeSet attrs, int defStyleAttr,
			int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		initValues();
	}

	public GalleryView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initValues();
	}

	public GalleryView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initValues();
	}

	public GalleryView(Context context) {
		super(context);
		initValues();
	}

	private void initValues() {
		mDensity = (int) getResources().getDisplayMetrics().density;
		mUnselectedGap = 50 * mDensity;
		maxRotation = 20;
		transformationCamera = new Camera();
		setStaticTransformationsEnabled(false);
		setChildrenDrawingOrderEnabled(true);
		mSelectedPos = 3;
		final ViewConfiguration configuration = ViewConfiguration
				.get(getContext());
		mTouchSlop = configuration.getScaledTouchSlop();
		mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
		mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
		mOverscrollDistance = configuration.getScaledOverscrollDistance();
		mOverflingDistance = configuration.getScaledOverflingDistance();

	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mWidth = w;
		mHeight = h;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		measureChildren(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {

		int childCount = getChildCount();
		int mPaddingLeft = getPaddingLeft();
		int mPaddingRight = getPaddingRight();
		int mPaddingBottom = getPaddingBottom();
		int mPaddingTop = getPaddingTop();
		int startLeft = 0;
		int childrenWidth = 0;
		if (childCount > 0) {
			childrenWidth = getChildAt(0).getWidth() + mUnselectedGap
					* (childCount - 1);
		}
		startLeft = (mWidth - mPaddingLeft - mPaddingRight - childrenWidth) >> 1;
		View child;
		for (int i = 0; i < childCount; i++) {
			child = getChildAt(i);
			int bottom = Math.min(mHeight - mPaddingTop - mPaddingBottom,
					child.getMeasuredHeight());
			int right = Math
					.min(mWidth - mPaddingRight, mPaddingLeft + startLeft
							+ mUnselectedGap * i + child.getMeasuredWidth());
			child.layout(mPaddingLeft + startLeft + mUnselectedGap * i,
					getPaddingTop(), right, bottom);
		}
	}


	@Override
	protected int getChildDrawingOrder(int childCount, int i) {
		if (drawOrder == null)
			calculateOrder();
		return drawOrder[i];
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		final int action = ev.getAction();
		if ((action == MotionEvent.ACTION_MOVE) && (mIsBeingDragged)) {
			return true;
		}

		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_MOVE: {
			/*
			 * mIsBeingDragged == false, otherwise the shortcut would have
			 * caught it. Check whether the user has moved far enough from his
			 * original down touch.
			 */

			/*
			 * Locally do absolute value. mLastMotionY is set to the y value of
			 * the down event.
			 */
			final int activePointerId = mActivePointerId;
			if (activePointerId == INVALID_POINTER) {
				// If we don't have a valid id, the touch down wasn't on
				// content.
				break;
			}

			final int pointerIndex = ev.findPointerIndex(activePointerId);
			final float x = ev.getX(pointerIndex);
			final int xDiff = (int) Math.abs(x - mLastMotionX);
			if (xDiff > mTouchSlop) {
				mIsBeingDragged = true;
				mLastMotionX = x;
				initVelocityTrackerIfNotExists();
				mVelocityTracker.addMovement(ev);
			}
			break;
		}

		case MotionEvent.ACTION_DOWN: {
			final float x = ev.getX();
			if (!inChild((int) ev.getX(), (int) ev.getY())) {
				mIsBeingDragged = false;
				recycleVelocityTracker();
				break;
			}

			/*
			 * Remember location of down touch. ACTION_DOWN always refers to
			 * pointer index 0.
			 */
			mLastMotionX = x;
			mActivePointerId = ev.getPointerId(0);

			initOrResetVelocityTracker();
			mVelocityTracker.addMovement(ev);
			/*
			 * If being flinged and user touches the screen, initiate drag;
			 * otherwise don't. mScroller.isFinished should be false when being
			 * flinged.
			 */
			break;
		}

		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			/* Release the drag */
			mIsBeingDragged = false;
			mActivePointerId = INVALID_POINTER;
			recycleVelocityTracker();
			break;
		case MotionEvent.ACTION_POINTER_UP:
			onSecondaryPointerUp(ev);
			break;
		}

		/*
		 * The only time we want to intercept motion events is if we are in the
		 * drag mode.
		 */
		return mIsBeingDragged;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		initVelocityTrackerIfNotExists();
		mVelocityTracker.addMovement(ev);

		final int action = ev.getAction();

		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN: {
			mIsBeingDragged = getChildCount() != 0;
			if (!mIsBeingDragged) {
				return false;
			}

			/*
			 * If being flinged and user touches, stop the fling. isFinished
			 * will be false if being flinged.
			 */

			// Remember where the motion event started
			mLastMotionX = ev.getX();
			mActivePointerId = ev.getPointerId(0);
			break;
		}
		case MotionEvent.ACTION_MOVE:
			if (mIsBeingDragged) {
				// Scroll to follow the motion event
				final int activePointerIndex = ev
						.findPointerIndex(mActivePointerId);
				final float x = ev.getX(activePointerIndex);
				final int deltaX = (int) (mLastMotionX - x);
				// mLastMotionX = x;

				Log.d("event", "move:" + deltaX);
			}
			break;
		case MotionEvent.ACTION_UP:
			if (mIsBeingDragged) {
				// final VelocityTracker velocityTracker = mVelocityTracker;
				// velocityTracker.computeCurrentVelocity(1000,
				// mMaximumVelocity);
				// int initialVelocity = (int) velocityTracker
				// .getYVelocity(mActivePointerId);
				//
				// if (getChildCount() > 0) {
				// if ((Math.abs(initialVelocity) > mMinimumVelocity)) {
				// fling(-initialVelocity);
				// } else {
				// if (mScroller.springBack(getScrollX(), getScrollY(), 0,
				// 0, 0, getScrollRange())) {
				// invalidate();
				// }
				// }
				// }
				// mActivePointerId = INVALID_POINTER;
				// endDrag();
				final int activePointerIndex = ev
						.findPointerIndex(mActivePointerId);
				final float x = ev.getX(activePointerIndex);
				if (x != mLastMotionX) {
					if (x > mLastMotionX)
						setSelectionPos(mSelectedPos - 1);
					else
						setSelectionPos(mSelectedPos + 1);
					invalidate();
				}
			}
			break;
		case MotionEvent.ACTION_CANCEL:
			if (mIsBeingDragged && getChildCount() > 0) {
				mActivePointerId = INVALID_POINTER;
				endDrag();
			}
			break;
		case MotionEvent.ACTION_POINTER_DOWN: {
			final int index = ev.getActionIndex();
			final float y = ev.getY(index);
			mLastMotionY = y;
			mActivePointerId = ev.getPointerId(index);
			break;
		}
		case MotionEvent.ACTION_POINTER_UP:
			onSecondaryPointerUp(ev);
			mLastMotionY = ev.getY(ev.findPointerIndex(mActivePointerId));
			break;
		}
		return true;
	}

	private boolean inChild(int x, int y) {
		if (getChildCount() > 0) {
			final int scrollY = getScrollY();
			final View child = getChildAt(0);
			return !(y < child.getTop() - scrollY
					|| y >= child.getBottom() - scrollY || x < child.getLeft() || x >= child
					.getRight());
		}
		return false;
	}

	private void initOrResetVelocityTracker() {
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		} else {
			mVelocityTracker.clear();
		}
	}

	private void initVelocityTrackerIfNotExists() {
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
	}

	private void recycleVelocityTracker() {
		if (mVelocityTracker != null) {
			mVelocityTracker.recycle();
			mVelocityTracker = null;
		}
	}

	private int getScrollRange() {
		int scrollRange = 0;
		if (getChildCount() > 0) {
			View child = getChildAt(0);
			scrollRange = Math.max(0, child.getHeight()
					- (getHeight() - getPaddingBottom() - getPaddingTop()));
		}
		return scrollRange;
	}

	private void onSecondaryPointerUp(MotionEvent ev) {
		final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
		final int pointerId = ev.getPointerId(pointerIndex);
		if (pointerId == mActivePointerId) {
			final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
			mLastMotionY = ev.getY(newPointerIndex);
			mActivePointerId = ev.getPointerId(newPointerIndex);
			if (mVelocityTracker != null) {
				mVelocityTracker.clear();
			}
		}
	}

	private void endDrag() {
		mIsBeingDragged = false;

		recycleVelocityTracker();

		if (mEdgeGlowTop != null) {
			mEdgeGlowTop.onRelease();
			mEdgeGlowBottom.onRelease();
		}

	}

	@Override
	public void setOverScrollMode(int mode) {
		if (mode != OVER_SCROLL_NEVER) {
			if (mEdgeGlowTop == null) {
				Context context = getContext();
				mEdgeGlowTop = new EdgeEffect(context);
				mEdgeGlowBottom = new EdgeEffect(context);
			}
		} else {
			mEdgeGlowTop = null;
			mEdgeGlowBottom = null;
		}
		super.setOverScrollMode(mode);
	}

	public void fling(int velocityY) {
		if (getChildCount() > 0) {
			int height = getHeight() - getPaddingBottom() - getPaddingTop();
			int bottom = getChildAt(0).getHeight();


			final boolean movingDown = velocityY > 0;

			invalidate();
		}
	}

	@Override
	protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
		boolean ret;
		final float offset = calculateOffsetOfCenter(child);
		getTransformationMatrix(child, offset);
		// child.setAlpha(1 - offset);
		canvas.save();
		canvas.concat(mMatrix);
		ret = super.drawChild(canvas, child, drawingTime);
		canvas.restore();
		return ret;
	}

	void getTransformationMatrix(View child, float offset) {
		final int childWidth = child.getWidth();
		final int childHeight = child.getHeight();
		mMatrix.reset();
		int childIndex = indexOfChild(child);

		int rotationAngle = 0;
		if (curAnimated) {
			if (childIndex == mSelectedPos) {
				rotationAngle = curRotation;
				rotatedByCamera(rotationAngle);
				if (toRight) {
					final float translateX = childWidth;
					final float translateY = childHeight / 2.0f;
					mMatrix.preTranslate(-translateX, -translateY);
					mMatrix.postTranslate(translateX, translateY);
				} else {
					final float translateY = childHeight / 2.0f;
					mMatrix.preTranslate(0, -translateY);
					mMatrix.postTranslate(0, translateY);
				}
				mMatrix.preTranslate(-(child.getLeft() - getPaddingLeft()), 0);
				mMatrix.postTranslate((child.getLeft() - getPaddingLeft()), 0);
				return;
			}
			// if (childIndex == prevSelection) {
			// rotatedByCamera((toRight ? 1 : -1) * maxRotation);
			// if (toRight)
			// transformRightSibling(child);
			// else
			// transformLeftSibling(child);
			// return;
			// }
		}
		if (prevAnimated) {
			if (childIndex == prevSelection) {
				rotationAngle = prevRotation;
				rotatedByCamera(rotationAngle);
				// toRight
				if (!toRight) {
					final float translateX = childWidth;
					final float translateY = childHeight / 2.0f;
					mMatrix.preTranslate(-translateX, -translateY);
					mMatrix.postTranslate(translateX, translateY);
				} else {
					final float translateY = childHeight / 2.0f;
					mMatrix.preTranslate(0, -translateY);
					mMatrix.postTranslate(0, translateY);
				}
				mMatrix.preTranslate(-(child.getLeft() - getPaddingLeft()), 0);
				mMatrix.postTranslate((child.getLeft() - getPaddingLeft()), 0);
				return;
			}
			if (childIndex == mSelectedPos) {
				rotatedByCamera((toRight ? -1 : 1) * maxRotation);
				if (toRight)
					transformRightSibling(child);
				else
					transformLeftSibling(child);

				return;
			}
		}

		if (childIndex == mSelectedPos) {
			return;
		}

		boolean isRight = childIndex > mSelectedPos;
		if (this.maxRotation != 0) {
			rotationAngle = (int) ((isRight ? -1 : 1) * this.maxRotation);
		}
		rotatedByCamera(rotationAngle);
		if (isRight) {
			transformRightSibling(child);
		} else {
			transformLeftSibling(child);
		}

	}

	private void transformRightSibling(View child) {

		final int childWidth = child.getWidth();
		final int childHeight = child.getHeight();

		final float translateX = childWidth;
		final float translateY = childHeight / 2.0f;
		mMatrix.preTranslate(-translateX, -translateY);
		mMatrix.postTranslate(translateX, translateY);
		mMatrix.preTranslate(-(child.getLeft() - getPaddingLeft()), 0);
		mMatrix.postTranslate((child.getLeft() - getPaddingLeft()), 0);
	}

	private void transformLeftSibling(View child) {
		final float translateY = child.getHeight() / 2.0f;
		mMatrix.preTranslate(0, -translateY);
		mMatrix.postTranslate(0, translateY);
		mMatrix.preTranslate(-(child.getLeft() - getPaddingLeft()), 0);
		mMatrix.postTranslate((child.getLeft() - getPaddingLeft()), 0);
	}

	private void rotatedByCamera(int rotation) {
		transformationCamera.save();
		transformationCamera.rotateY(rotation);
		transformationCamera.getMatrix(mMatrix);
		transformationCamera.restore();
	}

	// 获取父控件中心点 X 的位置
	protected int getCenterOfCoverflow() {
		return ((getWidth() - getPaddingLeft() - getPaddingRight()) >> 1)
				+ getPaddingLeft();
	}

	// 获取 child 中心点 X 的位置
	protected int getCenterOfView(View view) {
		return view.getLeft() + (view.getWidth() >> 1);
	}

	// 计算 child 偏离 父控件中心的 offset 值， -1 <= offset <= 1
	protected float calculateOffsetOfCenter(View view) {
		final int pCenter = getCenterOfCoverflow();
		final int cCenter = getCenterOfView(view);

		float offset = (cCenter - pCenter) / (pCenter * 1.0f);
		offset = Math.min(offset, 1.0f);
		offset = Math.max(offset, -1.0f);

		return offset;
	}

	private void calculateOrder() {
		int childCount = getChildCount();
		drawOrder = new Integer[childCount];
		for (int i = 0; i < drawOrder.length; i++) {
			drawOrder[i] = i;
		}

		int temp = 0;
		for (int i = mSelectedPos + 1; i < drawOrder.length; i++) {
			temp = drawOrder[i];
			for (int j = i - 1; j >= 0; j--) {
				drawOrder[j + 1] = drawOrder[j];
			}
			drawOrder[0] = temp;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < drawOrder.length; i++) {
			if (i == drawOrder.length - 1)
				sb.append(drawOrder[i]);
			else
				sb.append(drawOrder[i] + ",");

		}
		sb.append("]");
		Log.d("order", "sp:" + mSelectedPos + "," + sb.toString());
	}

	@Override
	public void onChildViewAdded(View parent, View child) {
		calculateOrder();
	}

	@Override
	public void onChildViewRemoved(View parent, View child) {
		calculateOrder();
	}

	private int curRotation = 0;
	private int prevRotation = 0;
	private boolean curAnimated;
	private boolean prevAnimated;
	private int prevSelection = 0;

	private void setSelectionPos(int selectionPos) {
		if (selectionPos == mSelectedPos)
			return;
		if (selectionPos >= getChildCount() || selectionPos < 0)
			return;
		prevSelection = mSelectedPos;
		mSelectedPos = selectionPos;

		playAnim();
	}

	private void playAnim() {
		toRight = mSelectedPos > prevSelection;

		ValueAnimator prevAnim = ValueAnimator.ofInt(0, (toRight ? 1 : -1)
				* maxRotation);
		final ValueAnimator curAnim = ValueAnimator.ofInt((toRight ? -1 : 1)
				* maxRotation, 0);
		prevAnim.setDuration(duration);
		curAnim.setDuration(duration);
		prevAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				prevRotation = (Integer) animation.getAnimatedValue();
				invalidate();
			}
		});
		prevAnim.addListener(new AnimatorListenerAdapter() {

			@Override
			public void onAnimationEnd(Animator animation) {
				calculateOrder();
				curAnim.start();
				prevAnimated = false;
			}

			@Override
			public void onAnimationStart(Animator animation) {
				prevAnimated = true;
			}

		});
		curAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				curRotation = (Integer) animation.getAnimatedValue();
				invalidate();
			}
		});
		curAnim.addListener(new AnimatorListenerAdapter() {

			@Override
			public void onAnimationEnd(Animator animation) {
				curAnimated = false;
			}

			@Override
			public void onAnimationStart(Animator animation) {
				curAnimated = true;
			}

		});
		prevAnim.start();
	}
}
