package com.personalization.stories

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.personalization.SDK
import com.personalization.api.OnApiCallbackListener
import com.personalization.sdk.domain.models.RecommendedBy
import com.personalization.sdk.domain.usecases.network.SendNetworkMethodUseCase
import com.personalization.sdk.domain.usecases.recommendation.SetRecommendedByUseCase
import com.personalization.stories.models.Story
import com.personalization.stories.views.StoriesView
import java.lang.ref.WeakReference
import javax.inject.Inject
import org.json.JSONException
import org.json.JSONObject

/**
 * Stateless stories service: it fetches and parses blocks and tracks events, but does not own any
 * view. Each [StoriesView] loads its own block through [loadStories] and renders itself, so several
 * blocks on one screen no longer contend for a single held reference.
 *
 * The only reference kept is a weak pointer to the most recently attached view, used solely as the
 * presentation surface for the view-less [SDK.showStories] entry point — never for loading data.
 */
class StoriesManager @Inject constructor(
    val setRecommendedByUseCase: SetRecommendedByUseCase,
    val sendNetworkMethodUseCase: SendNetworkMethodUseCase
) {

    private var lastAttachedView: WeakReference<StoriesView>? = null

    /** The block the SDK last requested — the fallback for tracking calls that omit the code. */
    @Volatile
    private var lastRequestedCode: String? = null

    /** Records the surface [SDK.showStories] presents from. Not used for loading. */
    internal fun rememberAttachedView(storiesView: StoriesView) {
        lastAttachedView = WeakReference(storiesView)
    }

    /** Fetches and parses the block for [code], delivering the stories to [onLoaded]. Stateless. */
    internal fun loadStories(code: String, onLoaded: (List<Story>) -> Unit) {
        requestStories(
            code = code,
            listener = object : OnApiCallbackListener() {
                override fun onSuccess(response: JSONObject?) {
                    response ?: return
                    try {
                        onLoaded(getStories(response))
                    } catch (e: JSONException) {
                        Log.e(SDK.TAG, e.message, e)
                    }
                }
            }
        )
    }

    internal fun showStories(looper: Looper, code: String) {
        val view = lastAttachedView?.get()
        if (view == null) {
            Log.w(SDK.TAG, "showStories($code): no StoriesView is attached to present from")
            return
        }
        requestStories(
            code = code,
            listener = object : OnApiCallbackListener() {
                override fun onSuccess(response: JSONObject?) {
                    response?.let {
                        Log.d("stories", response.toString())
                        try {
                            val stories = getStories(response)

                            if (stories.isEmpty()) return

                            resetStoriesStartPositions(stories)

                            showStories(looper, view, stories)
                        } catch (e: JSONException) {
                            Log.e(SDK.TAG, e.message, e)
                        }
                    }
                }
            }
        )
    }

    internal fun requestStories(code: String, listener: OnApiCallbackListener) {
        lastRequestedCode = code
        sendNetworkMethodUseCase.getAsync(
            method = String.format(REQUEST_STORIES_METHOD, code),
            params = JSONObject(),
            listener = listener
        )
    }

    /**
     * Triggers a story event
     * Also remember the last click in stories in order to add it when calling the product view event
     *
     * @param event Event
     * @param code Stories block code
     * @param storyId Story ID
     * @param slideId Slide ID
     */
    internal fun trackStory(event: String, code: String, storyId: Int, slideId: String) {
        trackStory(
            event = event,
            code = code,
            storyId = storyId.toString(),
            slideId = slideId,
            listener = null
        )
    }

    /**
     * Story event with the story id as a string — the shape the `tracking` namespace speaks.
     * A numeric id still goes on the wire as a number, so the request is unchanged.
     *
     * [code] falls back to the block the SDK last requested; without either the event is dropped,
     * since the backend attributes it to a block.
     */
    internal fun trackStory(
        event: String,
        code: String?,
        storyId: String,
        slideId: String,
        listener: OnApiCallbackListener? = null
    ) {
        val effectiveCode = code ?: lastRequestedCode
        if (effectiveCode == null) {
            Log.w(SDK.TAG, "trackStory($event): no stories code given and no block loaded yet")
            return
        }
        try {
            val params = JSONObject()
            params.put(EVENT_PARAMS_NAME, event)
            params.put(STORY_ID_PARAMS_NAME, storyId.toIntOrNull() ?: storyId)
            params.put(SLIDE_ID_PARAMS_NAME, slideId)
            params.put(CODE_PARAMS_NAME, effectiveCode)

            setRecommendedByUseCase(RecommendedBy(RecommendedBy.TYPE.STORIES, effectiveCode))

            sendNetworkMethodUseCase.postAsync(TRACK_STORIES_METHOD, params, listener)
        } catch (e: JSONException) {
            Log.e(SDK.TAG, e.message, e)
        }
    }

    private fun getStories(json: JSONObject): List<Story> {
        val stories = ArrayList<Story>()

        val jsonStories = json.getJSONArray("stories")

        for (i in 0 until jsonStories.length()) {
            stories.add(Story(jsonStories.getJSONObject(i)))
        }

        return stories
    }

    private fun resetStoriesStartPositions(stories: List<Story>) {
        for (story in stories) {
            story.startPosition = 0
        }
    }

    private fun showStories(
        looper: Looper,
        storiesView: StoriesView,
        stories: List<Story>,
        startPosition: Int = 0
    ) {
        val handler = Handler(looper)
        handler.post {
            storiesView.showStories(
                stories = stories,
                startPosition = startPosition
            )
        }
    }

    companion object {
        const val TRACK_STORIES_METHOD = "track/stories"
        const val REQUEST_STORIES_METHOD = "stories/%s"

        const val EVENT_PARAMS_NAME = "event"
        const val STORY_ID_PARAMS_NAME = "story_id"
        const val SLIDE_ID_PARAMS_NAME = "slide_id"
        const val CODE_PARAMS_NAME = "code"
    }
}
