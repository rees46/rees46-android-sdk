package com.personalizatio.api.managers

import com.personalizatio.api.OnApiCallbackListener
import com.personalizatio.stories.views.StoriesView

interface StoriesManager {

    /**
     * Initialize stories view
     *
     * @param storiesView StoriesView
     */
    fun initialize(storiesView: StoriesView)

    /**
     * Show story by id
     *
     * @param storyId Story ID
     */
    fun showStory(storyId: Int)

    /**
     * Request stories
     *
     * @param storyId Story ID
     * @param listener Callback
     */
    fun requestStories(code: String, listener: OnApiCallbackListener)

    /**
     * Triggers a story event
     * Also remember the last click in stories in order to add it when calling the product view event
     *
     * @param event Event
     * @param code Stories block code
     * @param storyId Story ID
     * @param slideId Slide ID
     */
    fun trackStory(event: String, code: String, storyId: Int, slideId: String)
}
