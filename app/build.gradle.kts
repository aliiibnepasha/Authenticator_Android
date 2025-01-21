plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
    id ("com.google.gms.google-services")
    id("com.google.protobuf")
}

android {
    namespace = "com.husnain.authy"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.theswiftvision.authenticator"
        minSdk = 26
        //noinspection EditedTargetSdkVersion,OldTargetApi
        targetSdk = 35
        versionCode = 4
        versionName = "1.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
//            signingConfig = signingConfigs.getByName("debug")
        }
    }
    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

kapt {
    correctErrorTypes = true
}

//noinspection UseTomlInstead
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.androidx.hilt.common)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //Camera
    implementation(libs.androidx.camera.lifecycle)
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("com.google.guava:guava:33.0.0-jre")


    //Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")

    //navigation components
    val navVersion = "2.8.5"
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")

    //lifeCycle
    val lifecycleVersion = "2.8.7"
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-common-java8:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")

    //hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-compiler:2.51.1")

    //Responsiveness
    implementation("com.intuit.ssp:ssp-android:1.1.1")
    implementation("com.intuit.sdp:sdp-android:1.1.1")

    //Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    //Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("androidx.credentials:credentials:1.3.0") //Google credential manager
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")


    //Room database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    //Gson
    implementation("com.google.code.gson:gson:2.10.1")

    //Dots indicator
    implementation("com.tbuonomo:dotsindicator:5.1.0")

    //Qr code scanner
//    implementation("io.github.g00fy2.quickie:quickie-bundled:1.10.0")

    //biometric
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    //Totp
    implementation("dev.turingcomplete:kotlin-onetimepassword:2.4.1")

    //Localization
    implementation("com.akexorcist:localization:1.2.11")

    //Qr
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    //Admob
    implementation("com.google.android.gms:play-services-ads:23.6.0")

    //shimmer
    implementation("com.facebook.shimmer:shimmer:0.5.0")

    //billing
    val billing_version = "7.1.1"
    implementation("com.android.billingclient:billing-ktx:$billing_version")

    //worker
    val work_version = "2.10.0"
    implementation("androidx.work:work-runtime-ktx:$work_version")
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    //Ml kit qr scanner
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    //To read google authenticator data
    implementation("com.google.protobuf:protobuf-javalite:3.21.12")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.21.12"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite") // Generates lightweight classes
                }
            }
        }
    }
}