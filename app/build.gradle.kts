plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    compileSdk = 31
    buildToolsVersion = "31.0.0"

    defaultConfig {
        applicationId = "com.icebem.akt"
        minSdk = 21
        targetSdk = 30
        versionCode = 61
        versionName = "2.10.2"
        resourceConfigurations += arrayOf("zh-rCN", "en", "ja", "in")
    }
    signingConfigs {
        create("release") {
            val props = `java.util`.Properties().apply { load(file("../signing.properties").reader()) }
            storeFile = file(props.getProperty("storeFile"))
            storePassword = props.getProperty("storePassword")
            keyAlias = props.getProperty("keyAlias")
            keyPassword = props.getProperty("keyPassword")
        }
    }
    buildTypes {
        getByName("debug") {
            versionNameSuffix = "-Lune-${`java.text`.SimpleDateFormat("yyyyMMdd").format(System.currentTimeMillis())}"
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    applicationVariants.all {
        outputs.all {
            if (this is com.android.build.gradle.internal.api.ApkVariantOutputImpl)
                outputFileName = "ArkTap-v$versionName.apk"
        }
    }
    buildFeatures {
        viewBinding = false
        dataBinding = false
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.31")
    implementation("androidx.annotation:annotation:1.2.0")
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.1")
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.0.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.3.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.3.5")
    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("com.google.android.material:material:1.4.0")
}