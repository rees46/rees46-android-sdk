package com.personalizatio.stories;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.personalizatio.Api;
import com.personalizatio.OnLinkClickListener;
import com.personalizatio.R;
import com.personalizatio.SDK;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public final class StoriesView extends ConstraintLayout implements StoriesAdapter.ClickListener {

	private StoriesAdapter adapter;
	private final ArrayList<Story> list = new ArrayList<>();
	private ContentObserver observer;
	public final Settings settings = new Settings();
	String code;
	Player player;
	@Nullable
	OnLinkClickListener click_listener;
	boolean mute = true;
	Runnable mute_listener;

	public StoriesView(Context context, String code) {
		super(context);
		this.code = code;
		initialize();
	}

	public StoriesView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		parseAttrs(attrs);
	}

	public StoriesView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		parseAttrs(attrs);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public StoriesView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		parseAttrs(attrs);
	}

	/**
	 * Вызывать, когда объект сторисов удален с экрана и больше не нужен
	 */
	public void release() {
		player.release();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		unregisterObserver();
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		initialize();
	}

	private void parseAttrs(AttributeSet attrs) {
		TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.StoriesView);
		code = typedArray.getString(R.styleable.StoriesView_code);
	}

	//Инициализация
	private void initialize() {
		View view = inflate(getContext(), R.layout.stories, this);
		RecyclerView stories = view.findViewById(R.id.stories);

		adapter = new StoriesAdapter(this, list, this);
		stories.setAdapter(adapter);

		Handler handler = new Handler(Looper.getMainLooper()) {
			@Override
			public void handleMessage(Message msg) {
				registerObserver();
				adapter.notifyDataSetChanged();
			}
		};

		//Плеер для просмотра видео
		player = new Player(getContext());

		settings.failed_load_text = getResources().getString(R.string.failed_load_text);

		//Запрашиваем сторисы
		SDK.stories(code, new Api.OnApiCallbackListener() {
			@Override
			public void onSuccess(JSONObject response) {
				Log.d("stories", response.toString());
				try {
					JSONArray json_stories = response.getJSONArray("stories");
					for( int i = 0; i < json_stories.length(); i++ ) {
						list.add(new Story(json_stories.getJSONObject(i)));
					}
					handler.sendEmptyMessage(1);
				} catch(JSONException e) {
					Log.e(SDK.TAG, e.getMessage(), e);
				}
			}
		});
	}

	@Override
	public void onStoryClick(int index) {
		Story story = list.get(index);

		//Сбрасываем позицию
		if( story.start_position >= story.slides.size() || story.start_position < 0 ) {
			story.start_position = 0;
		}

		StoryDialog dialog = new StoryDialog(this, list, index, () -> {
			adapter.notifyDataSetChanged();
		});
		dialog.show();
	}

	/**
	 * Устанавливает слушатель клика по элементам
	 * @param listener OnLinkClickListener
	 */
	public void setOnLinkClickListener(@Nullable OnLinkClickListener listener) {
		this.click_listener = listener;
	}

	public void muteVideo(boolean mute) {
		this.mute = mute;
	}

	public boolean isMute() {
		return mute;
	}

	public void registerObserver() {
		observer = new ContentObserver(new Handler()) {
			@Override
			public void onChange(boolean selfChange) {
				super.onChange(selfChange);
				AudioManager manager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
				if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
					mute = manager.isStreamMute(AudioManager.STREAM_MUSIC);
					if( mute_listener != null ) {
						mute_listener.run();
					}
				}
			}
		};
		getContext().getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, observer);
	}

	public void unregisterObserver() {
		if( observer != null ) {
			getContext().getContentResolver().unregisterContentObserver(observer);
		}
	}
}
