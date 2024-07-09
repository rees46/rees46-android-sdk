package com.personalizatio.api.params

class ProductItemParams(id: String) {
    enum class PARAMETER(val value: String) {
        ID("id"),
        AMOUNT("amount"),
        PRICE("price"),
        FASHION_SIZE("fashion_size")
    }

    val parameters: HashMap<String, String> = HashMap()

    init {
        parameters[PARAMETER.ID.value] = id
    }

    fun set(column: PARAMETER, value: String): ProductItemParams {
        parameters[column.value] = value
        return this
    }

    fun set(column: PARAMETER, value: Int): ProductItemParams {
        return set(column, value.toString())
    }

    fun set(column: PARAMETER, value: Double): ProductItemParams {
        return set(column, value.toString())
    }

    fun set(column: PARAMETER, value: Boolean): ProductItemParams {
        return set(column, if (value) "1" else "0")
    }
}
