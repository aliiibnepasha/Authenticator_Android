package com.husnain.authy.data

import com.husnain.authy.R

data class ModelTotp(
    var secretKey: String,
    var serviceName: String,
    var totp: String = "",
    var logo: Int = R.drawable.img_baby_brain
)
