package com.husnain.authy.data.models

data class ModelUser(
    val userEmail: String,
    val userPassword: String,
    val userName: String = "",
    val userId: String = ""
) {
    constructor() : this("", "", "", "")
}