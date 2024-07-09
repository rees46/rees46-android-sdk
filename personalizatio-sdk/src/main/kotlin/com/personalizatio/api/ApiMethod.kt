package com.personalizatio.api

@Deprecated(
    "This class will be removed in future versions. Used function from sdk.networkManager",
    level = DeprecationLevel.WARNING
)
sealed class ApiMethod(val type: String, val method: String) {

    class POST(method: String): ApiMethod(type = "POST", method = method)

    class GET(method: String): ApiMethod(type = "GET", method = method)
}
