plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id ("kotlin-kapt")
}

android {
    namespace = "com.tangoplus.facebeauty"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tangoplus.facebeauty"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // c secretkey 저장
        externalNativeBuild {
            cmake {
                cppFlags += ""
            }
        }
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    viewBinding {
        enable = true
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

}

dependencies {
    implementation("com.davemorrissey.labs:subsampling-scale-image-view:3.10.0")
    implementation("com.github.skydoves:progressview:1.1.3")
    implementation ("androidx.core:core-ktx:1.8.0")
    implementation("com.google.mediapipe:tasks-vision:0.10.14")
    implementation("com.arthenica:ffmpeg-kit-full-gpl:6.0-2")
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("com.google.android.material:material:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.fragment:fragment-ktx:1.5.4")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.facebook.shimmer:shimmer:0.5.0")



    val camera_version = "1.4.1"
    implementation("androidx.camera:camera-core:$camera_version")
    implementation("androidx.camera:camera-camera2:$camera_version")
    implementation("androidx.camera:camera-lifecycle:$camera_version")
    implementation("androidx.camera:camera-video:$camera_version")
    implementation("androidx.camera:camera-extensions:$camera_version")
    implementation("androidx.camera:camera-view:$camera_version")

    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-effect:1.5.1")
    implementation("androidx.media3:media3-common:1.2.0")
    implementation("androidx.media3:media3-transformer:1.2.0")
    implementation("androidx.window:window:1.1.0-alpha03")

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-rxjava3:$roomVersion")
    implementation("androidx.room:room-rxjava2:$roomVersion")
    implementation("androidx.room:room-guava:$roomVersion")
    implementation("androidx.room:room-testing:$roomVersion")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")
    annotationProcessor("android.arch.persistence.room:rxjava2:1.1.1")
    //noinspection KaptUsageInsteadOfKsp
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.sqlite:sqlite:2.4.0")
    //noinspection Aligned16KB
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}