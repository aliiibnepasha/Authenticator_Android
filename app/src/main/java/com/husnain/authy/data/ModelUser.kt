package com.husnain.authy.data

data class ModelUser(
    val userEmail: String,
    val userPassword: String,
    val userName: String = "",
    val userId: String = ""
) {
    constructor() : this("", "", "", "")
}