package com.personalizatio.stories;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.media3.common.C;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.personalizatio.R;
import com.personalizatio.SDK;

import java.util.HashMap;

final class StoryView extends ConstraintLayout implements StoriesProgressView.StoriesListener, Player.Listener {

	private final StoriesView stories_view;
	private Story story;

	long pressTime = 0L;
	private final static long LIMIT = 500L;
	private Runnable completeListener;
	private Runnable prevStoryListener;

	public final StoriesProgressView storiesProgressView;
	private final OnTouchListener onTouchListener;
	public final ViewPager2 mViewPager;
	private boolean storiesStarted = false;
	private boolean prevFocusState = true;
	private boolean locked = false;
	private final HashMap<Integer, PagerHolder> holders = new HashMap<>();
	private final ToggleButton mute;

	//Heading
	private final StoryDialog.OnProgressState state_listener;

	@SuppressLint("ClickableViewAccessibility")
	public StoryView(StoriesView stories_view, StoryDialog.OnProgressState state_listener) {
		super(stories_view.getContext());
		this.stories_view = stories_view;
		this.state_listener = state_listener;

		inflate(getContext(), R.layout.story_view, this);
		setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		onTouchListener = (v, event) -> {
			if( storiesStarted ) {
				switch( event.getAction() ) {
					case MotionEvent.ACTION_DOWN:
						pressTime = System.currentTimeMillis();
						pause();
//							setHeadingVisibility(GONE);
						return false;
					case MotionEvent.ACTION_UP:
						long now = System.currentTimeMillis();
						if( LIMIT < now - pressTime ) {
							resume();
						}
//							setHeadingVisibility(VISIBLE);
						return LIMIT < now - pressTime;
				}
			}
			return false;
		};
		storiesProgressView = findViewById(R.id.storiesProgressView);
		storiesProgressView.setColor(Color.parseColor(stories_view.settings.background_progress));
		mViewPager = findViewById(R.id.storiesViewPager);
		storiesProgressView.setStoriesListener(this);
		mViewPager.setClipToPadding(false);
		mViewPager.setClipChildren(false);
		mViewPager.setOffscreenPageLimit(1);
		mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
			@Override
			public void onPageSelected(int position) {
				if( stories_view.player.player.isPlaying() || stories_view.player.player.isLoading() ) {
					stories_view.player.player.pause();
				}
				//При изменении слайда останавливаем видео на скрытых
				for( int i = 0; i < story.slides.size(); i++ ) {
					PagerHolder holder = getHolder(i);
					if( holder != null ) {
						if( i != position ) {
							holder.release();
						}
					}
				}
				if( storiesStarted ) {
					SDK.track_story("view", stories_view.code, story.id, story.slides.get(position).id);
					playVideo();
				}
			}
		});

		stories_view.player.player.setVolume(stories_view.isMute() ? 0f : 1f);
		stories_view.mute_listener = () -> {
			stories_view.player.player.setVolume(1f);
		};

		//Управление звуком
		mute = findViewById(R.id.mute);
		mute.setOnClickListener(v -> {
			stories_view.muteVideo(mute.isChecked());
			stories_view.player.player.setVolume(mute.isChecked() ? 0f : 1f);
		});
		mute.setChecked(stories_view.isMute());
	}

	public void setStory(Story story, Runnable completeListener, Runnable prevStoryListener) {
		this.story = story;
		this.completeListener = completeListener;
		this.prevStoryListener = prevStoryListener;

		storiesProgressView.setStoriesCount(story.slides.size());
		mViewPager.setAdapter(new ViewPagerAdapter());
		//Хак, чтобы не срабатывал onPageSelected при открытии первой кампании
		mViewPager.setCurrentItem(story.start_position == 0 ? story.slides.size() : 0, false);
		//Устанавливаем позицию
		mViewPager.setCurrentItem(story.start_position, false);
	}

	@UnstableApi
	private void playVideo() {
		if( storiesStarted ) {
			PagerHolder holder = getHolder(mViewPager.getCurrentItem());
			if( holder == null ) {
				return;
			}
			Story.Slide slide = story.slides.get(mViewPager.getCurrentItem());
			if( slide.type.equals("video") ) {
				slide.prepared = false;
				//Подготавливаем плеер
				stories_view.player.player.addListener(this);
				holder.story_item.video.setVisibility(GONE);
				holder.story_item.image.setAlpha(1f);
				holder.story_item.video.setPlayer(stories_view.player.player);
				stories_view.player.prepare(slide.background);
				storiesProgressView.pause();
				mute.setChecked(stories_view.isMute());
			}
		}
	}

	@Override
	public void onPlaybackStateChanged(@Player.State int playbackState) {
		switch( playbackState ) {
			case Player.STATE_BUFFERING:
				storiesProgressView.pause();
				break;
			case Player.STATE_READY:
				stories_view.player.player.play();
				break;
		}
	}

	@Override
	public void onIsPlayingChanged(boolean isPlaying) {
		if( isPlaying ) {
			PagerHolder holder = getHolder(mViewPager.getCurrentItem());
			holder.story_item.video.setVisibility(VISIBLE);
			holder.story_item.image.animate().alpha(0).setDuration(300);
			story.slides.get(mViewPager.getCurrentItem()).prepared = true;
			resume();
		}
	}

	@Override
	public void onTracksChanged(@NonNull Tracks tracks) {
		if( stories_view.player.player.getContentDuration() > 0 ) {
			story.slides.get(mViewPager.getCurrentItem()).duration = stories_view.player.player.getContentDuration();
			updateDuration(mViewPager.getCurrentItem());
		}
		mute.setVisibility(tracks.isTypeSupported(C.TRACK_TYPE_AUDIO) ? VISIBLE : GONE);
	}

	@Override
	public void onPlayerError(PlaybackException error) {
		Log.e(SDK.TAG, "player error: " + error.getMessage() + ", story: " + story.id);
		PagerHolder holder = getCurrentHolder();
		holder.story_item.reload_layout.setVisibility(VISIBLE);

		holder.story_item.reload.setOnClickListener((View) -> {
			Story.Slide slide = story.slides.get(mViewPager.getCurrentItem());
			if( slide.type.equals("video") ) {
				holder.story_item.reload_layout.setVisibility(GONE);
				playVideo();
			} else {
				holder.story_item.update(slide, mViewPager.getCurrentItem(), stories_view.code, story.id);
			}
		});

		//Добавляем авто-обновление
		holder.story_item.reload.postDelayed(() -> holder.story_item.reload.callOnClick(), 15000L);
	}

	@Override
	public void onVolumeChanged(float volume) {
		mute.setChecked(volume == 0);
	}

	class PagerHolder extends RecyclerView.ViewHolder {
		public StoryItemView story_item;

		public PagerHolder(@NonNull View view) {
			super(view);
			story_item = (StoryItemView) view;
			story_item.setStoriesView(stories_view);
			story_item.setOnPageListener(new StoryItemView.OnPageListener() {
				@Override
				public void onPrev() {
					previousSlide();
				}

				@Override
				public void onNext() {
					nextSlide();
				}

				@Override
				public void onPrepared(int position) {
					if( story.slides.get(position).type.equals("image") && storiesStarted ) {
						try {
							storiesProgressView.resume();
						} catch (IndexOutOfBoundsException ignored) {
						}
					}
				}

				@Override
				public void onLocked(boolean lock) {
					locked = lock;
					state_listener.onState(!lock);
					if( lock ) {
						pause();
					} else {
						resume();
					}
				}
			});
		}

		public void bind(Story.Slide slide, int position) {
			holders.put(position, this);
			mute.setVisibility(GONE);
			story_item.update(slide, position, stories_view.code, story.id);
			story_item.setOnTouchListener(onTouchListener);

			//Устанавливаем загрузку видео, если биндим текущий элемент
			if( position == mViewPager.getCurrentItem() ) {
				playVideo();
			}
		}

		public void release() {
			story_item.release();
		}
	}

	class ViewPagerAdapter extends RecyclerView.Adapter<PagerHolder> {

		@NonNull
		@Override
		public PagerHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new PagerHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.story_item, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull PagerHolder holder, int position) {
			holder.bind(story.slides.get(position), position);
		}

		@Override
		public int getItemCount() {
			return story.slides.size();
		}
	}

	@Override
	public void onWindowFocusChanged(boolean state) {
		super.onWindowFocusChanged(state);
		//Когда окно сворачивается, нужно остановить видео и прогресс
		if( storiesProgressView != null && storiesStarted && prevFocusState != state ) {
			prevFocusState = state;
			if( state ) {
				resume();
			} else {
				pause();
			}
		}
	}

	public void updateDurations() {
		long[] durations = new long[story.slides.size()];
		for( int i = 0; i < story.slides.size(); i++ ) {
			durations[i] = story.slides.get(i).duration;
		}
		storiesProgressView.setStoriesCountWithDurations(durations);
	}

	private PagerHolder getCurrentHolder() {
		return getHolder(mViewPager.getCurrentItem());
	}

	private PagerHolder getHolder(int position) {
		return holders.get(position);
	}

	public void pause() {
		storiesProgressView.pause();
		stories_view.player.player.pause();
	}

	public void resume() {
		if( !locked && story.slides.get(mViewPager.getCurrentItem()).prepared ) {
			storiesProgressView.resume();
			if( story.slides.get(mViewPager.getCurrentItem()).type.equals("video") && !stories_view.player.player.isLoading() && !stories_view.player.player.isPlaying() ) {
				stories_view.player.player.play();
			}
		}
	}

	@Override
	public void onNext() {
		if( story.start_position + 1 >= story.slides.size() ) {
			onComplete();
			return;
		}
		mViewPager.setCurrentItem(++story.start_position, false);
		storiesProgressView.startStories(story.start_position);
	}

	@Override
	public void onStart(int position) {
		//Для текущего слайда
		if( position == mViewPager.getCurrentItem() ) {
			Story.Slide slide = story.slides.get(position);
			//Если видео еще подгружается, приостанавливаем таймер анимации
			if( !slide.prepared ) {
				storiesProgressView.pause();
			}
		}
	}

	@Override
	public void onPrev() {
		if( story.start_position <= 0 ) {
			prevStoryListener.run();
			return;
		}
		mViewPager.setCurrentItem(--story.start_position, false);
		storiesProgressView.startStories(story.start_position);
	}

	@Override
	public void onComplete() {
		story.viewed = true;
		story.start_position = 0;
		updateDurations();
		completeListener.run();
	}

	public void updateDuration(int position) {
		storiesProgressView.updateStoryDuration(position, story.slides.get(position).duration);
	}

	public void startStories() {
		if( !storiesStarted ) {
			storiesStarted = true;
			playVideo();
			SDK.track_story("view", stories_view.code, story.id, story.slides.get(story.start_position).id);
		}
		if( !locked ) {
			updateDurations();
			storiesProgressView.startStories(story.start_position);
			mViewPager.setCurrentItem(story.start_position, false);
		}
	}

	public void stopStories() {
		storiesStarted = false;
		//Если кампания сториса не активна на экране, удаляем листенеры
		stories_view.player.player.removeListener(this);
		pause();
		storiesProgressView.destroy();
		PagerHolder holder = getCurrentHolder();
		if( holder != null ) {
			holder.release();
		}
	}

	private void previousSlide() {
		if( storiesStarted && !locked ) {
			updateDurations();
			onPrev();
		}
	}

	public void nextSlide() {
		if( storiesStarted && !locked ) {
			onNext();
		}
	}

	public void release() {
		stories_view.player.player.removeListener(this);
		stories_view.player.player.stop();
		for( int i = 0; i < story.slides.size(); i++ ) {
			PagerHolder holder = getHolder(i);
			if( holder != null ) {
				holder.release();
			}
		}
	}
}
