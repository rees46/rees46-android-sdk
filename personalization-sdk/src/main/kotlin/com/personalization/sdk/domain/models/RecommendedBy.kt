package com.personalization.sdk.domain.models

data class RecommendedBy(
    val type: TYPE,
    var code: String? = null
) {
    enum class TYPE(val value: String) {
        RECOMMENDATION("dynamic"),
        TRIGGER("chain"),
        BULK("bulk"),
        TRANSACTIONAL("transactional"),
        INSTANT_SEARCH("instant_search"),
        FULL_SEARCH("full_search"),
        STORIES("stories"),

        /**
         * Traffic arriving from a web push digest. Internal domain model only — deliberately not added
         * to the public `Params.RecommendedBy.TYPE`, which hosts can `when` over.
         */
        WEB_PUSH_DIGEST("web_push_digest"),
    }
}
