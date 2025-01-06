package com.husnain.authy.utls

sealed class DataState<T> {
    data class Success<T>(val data: T? = null) : DataState<T>()
    data class Error<T>(val message: String, val data: T? = null) : DataState<T>()
    class Loading<T> : DataState<T>()
}
