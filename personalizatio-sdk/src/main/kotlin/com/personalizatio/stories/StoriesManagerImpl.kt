package com.personalizatio.stories

import android.util.Log
import com.personalizatio.Params.RecommendedBy
import com.personalizatio.SDK
import com.personalizatio.api.OnApiCallbackListener
import com.personalizatio.api.managers.StoriesManager
import com.personalizatio.stories.models.Story
import com.personalizatio.stories.views.StoriesView
import org.json.JSONException
import org.json.JSONObject

internal class StoriesManagerImpl(val sdk: SDK) : StoriesManager {

    private lateinit var storiesView: StoriesView

    override fun initialize(storiesView: StoriesView) {
        this.storiesView = storiesView

        updateStories()
    }

    override fun showStory(storyId: Int) {
        val show = storiesView.showStory(storyId)

        if (show) return

        sdk.getAsync(String.format(REQUEST_STORY_METHOD, storyId), JSONObject(), object : OnApiCallbackListener() {
            override fun onSuccess(response: JSONObject?) {
                Log.d("story", response.toString())
                response?.let {
                    val story = Story(response)
                    storiesView.showStory(story)
                }
            }
        })
    }

    override fun requestStories(code: String, listener: OnApiCallbackListener) {
        sdk.getAsync(String.format(REQUEST_STORIES_METHOD, code), JSONObject(), listener)
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
    override fun trackStory(event: String, code: String, storyId: Int, slideId: String) {
        try {
            val params = JSONObject()
            params.put(EVENT_PARAMS_NAME, event)
            params.put(STORY_ID_PARAMS_NAME, storyId)
            params.put(SLIDE_ID_PARAMS_NAME, slideId)
            params.put(CODE_PARAMS_NAME, code)

            sdk.lastRecommendedBy = RecommendedBy(RecommendedBy.TYPE.STORIES, code)

            sdk.sendAsync(TRACK_STORIES_METHOD, params, null)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun updateStories() {
        requestStories(storiesView.code, object : OnApiCallbackListener() {
            override fun onSuccess(response: JSONObject?) {
                response?.let {
                    Log.d("stories", response.toString())
                    try {
                        val stories = getStories(response)
                        storiesView.updateStories(stories)
                    } catch (e: JSONException) {
                        Log.e(SDK.TAG, e.message, e)
                    }
                }
            }
        })
    }

    private fun getStories(json: JSONObject): List<Story> {
        val stories = ArrayList<Story>()

        val jsonStories = json.getJSONArray("stories")

        for (i in 0 until jsonStories.length()) {
            stories.add(Story(jsonStories.getJSONObject(i)))
        }

        return stories
    }

    companion object {
        private const val TRACK_STORIES_METHOD = "track/stories"
        private const val REQUEST_STORIES_METHOD = "stories/%s"
        private const val REQUEST_STORY_METHOD = "story/%s"

        private const val EVENT_PARAMS_NAME = "event"
        private const val STORY_ID_PARAMS_NAME = "story_id"
        private const val SLIDE_ID_PARAMS_NAME = "slide_id"
        private const val CODE_PARAMS_NAME = "code"
    }
}
