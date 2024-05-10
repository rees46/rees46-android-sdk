package com.personalizatio.stories;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;
import android.widget.FrameLayout;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.personalizatio.R;
import com.personalizatio.SDK;

final class PausableProgressBar extends FrameLayout {

	private static final int DEFAULT_PROGRESS_DURATION = 2000;

	public final View frontProgressView;
	public final View maxProgressView;

	public PausableScaleAnimation animation;
	private long duration = DEFAULT_PROGRESS_DURATION;
	private Callback callback;
	private boolean paused = false;

	interface Callback {
		void onStartProgress();

		void onFinishProgress();
	}

	public PausableProgressBar(Context context) {
		this(context, null);
	}

	public PausableProgressBar(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PausableProgressBar(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		LayoutInflater.from(context).inflate(R.layout.pausable_progress, this);
		frontProgressView = findViewById(R.id.front_progress);
		maxProgressView = findViewById(R.id.max_progress); // work around
	}

	public void setColor(int color) {
		frontProgressView.setBackgroundColor(color);
		maxProgressView.setBackgroundColor(color);
	}

	public void setDuration(long duration) {
		this.duration = duration;
		if( animation != null ) {
			animation.setDuration(duration);
		}
	}

	public void setCallback(@NonNull Callback callback) {
		this.callback = callback;
	}

	void setMax() {
		finishProgress(true);
	}

	void setMin() {
		finishProgress(false);
	}

	void setMinWithoutCallback() {
		maxProgressView.setBackgroundResource(R.color.progress_secondary);

		maxProgressView.setVisibility(VISIBLE);
		if( animation != null ) {
			animation.setAnimationListener(null);
			animation.cancel();
		}
	}

	void setMaxWithoutCallback() {
		//		maxProgressView.setBackgroundResource(R.color.progress_max_active);
		maxProgressView.setVisibility(VISIBLE);
		if( animation != null ) {
			animation.setAnimationListener(null);
			animation.cancel();
		}
	}

	private void finishProgress(boolean isMax) {
		if(isMax) maxProgressView.setBackgroundResource(R.color.progress_max_active);
		maxProgressView.setVisibility(isMax ? VISIBLE : GONE);
		if( animation != null ) {
			animation.setAnimationListener(null);
			animation.cancel();
			if( callback != null ) {
				callback.onFinishProgress();
			}
		}
	}

	public void startProgress() {
		maxProgressView.setVisibility(GONE);

		animation = new PausableScaleAnimation(0, 1, 1, 1, Animation.ABSOLUTE, 0, Animation.RELATIVE_TO_SELF, 0);
		animation.setDuration(duration);
		animation.setInterpolator(new LinearInterpolator());
		animation.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
				frontProgressView.setVisibility(View.VISIBLE);
				if( callback != null ) callback.onStartProgress();
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				if( callback != null && !paused ) {
					Log.d(SDK.TAG, "onAnimationEnd");
					callback.onFinishProgress();
				}
			}
		});
		animation.setFillAfter(true);
		frontProgressView.startAnimation(animation);
	}

	public void pauseProgress() {
		if( animation != null ) {
			paused = true;
			animation.pause();
		}
	}

	public void resumeProgress() {
		if( animation != null ) {
			paused = false;
			animation.resume();
		}
	}

	void clear() {
		if( animation != null ) {
			animation.setAnimationListener(null);
			animation.cancel();
			animation = null;
		}
	}

	public static class PausableScaleAnimation extends ScaleAnimation {

		private long mElapsedAtPause = 0;
		private boolean mPaused = false;

		PausableScaleAnimation(float fromX, float toX, float fromY,
													 float toY, int pivotXType, float pivotXValue, int pivotYType,
													 float pivotYValue) {
			super(fromX, toX, fromY, toY, pivotXType, pivotXValue, pivotYType,
					pivotYValue);
		}

		@Override
		public boolean getTransformation(long currentTime, Transformation outTransformation, float scale) {
			if( mPaused && mElapsedAtPause == 0 ) {
				mElapsedAtPause = currentTime - getStartTime();
			}
			if(mPaused) {
				setStartTime(currentTime - mElapsedAtPause);
			}
			return super.getTransformation(currentTime, outTransformation, scale);
		}

		/***
		 * pause animation
		 */
		void pause() {
			if(mPaused) return;
			mElapsedAtPause = 0;
			mPaused = true;
		}

		/***
		 * resume animation
		 */
		void resume() {
			mPaused = false;
		}
	}
}