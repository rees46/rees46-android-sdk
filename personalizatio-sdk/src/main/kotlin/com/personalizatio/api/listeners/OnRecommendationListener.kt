package com.personalizatio.api.listeners

import com.personalizatio.entities.recommended.RecommendedEntity
import com.personalizatio.entities.recommended.RecommendedFullEntity

interface OnRecommendationListener {

    fun onGetRecommended(recommendedEntity: RecommendedEntity) {}

    fun onGetRecommended(recommendedFullEntity: RecommendedFullEntity) {}
}
