package com.personalizatio.features.search

import com.personalizatio.AbstractParams.ParamInterface

internal enum class SearchParameter(override val value: String) : ParamInterface {
    SEARCH_TYPE("type"),
    SEARCH_QUERY("search_query")
}