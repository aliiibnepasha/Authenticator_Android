package com.husnain.authy.data.models

import com.husnain.authy.R

data class ModelTotp(
    var secretKey: String,
    var serviceName: String,
    var totp: String = "",
    var firebaseDocId: String = "",
    var logo: Int = R.drawable.img_baby_brain
)
