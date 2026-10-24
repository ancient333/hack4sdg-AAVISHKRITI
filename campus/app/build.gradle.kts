import java.security.MessageDigest

plugins { id("com.android.application"); kotlin("android") }
android {
 namespace = "org.campuslens"
 compileSdk = 35
 defaultConfig {
  applicationId = "org.campuslens"
  minSdk = 31
  targetSdk = 35
  versionCode = 4
  versionName = "0.4.0"
  testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
 }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 androidResources { noCompress += "tflite" }
 buildFeatures { buildConfig = true }
 lint { abortOnError = true }
}
dependencies {
 implementation(project(":core"))
 implementation("androidx.activity:activity-ktx:1.9.3")
 implementation("androidx.core:core-ktx:1.15.0")
 implementation("androidx.webkit:webkit:1.12.1")
 implementation("androidx.camera:camera-core:1.4.2")
 implementation("androidx.camera:camera-camera2:1.4.2")
 implementation("androidx.camera:camera-lifecycle:1.4.2")
 implementation("androidx.camera:camera-view:1.4.2")
 implementation("com.google.mediapipe:tasks-vision:0.10.32")
 implementation("com.google.mlkit:text-recognition:16.0.1")
 androidTestImplementation("androidx.test:runner:1.6.2")
 androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
// Fail before packaging if the bundled detector is missing or corrupted.
val verifyModel by tasks.registering {
 val model = layout.projectDirectory.file("src/main/assets/models/efficientdet_lite0_int8.tflite")
 inputs.file(model)
 doLast {
  val f = model.asFile
  check(f.isFile && f.length() > 1024) { "Bundled detector missing; see docs/MODELS.md" }
  val bytes = f.readBytes()
  check(String(bytes.copyOfRange(4, 8), Charsets.US_ASCII) == "TFL3") { "Invalid TFLite model" }
  val manifest = layout.projectDirectory.file("src/main/assets/models/model_manifest.json").asFile.readText()
  val expected = Regex(""""sha256"\s*:\s*"([a-f0-9]{64})"""").find(manifest)?.groupValues?.get(1)
  val actual = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
  check(actual == expected) { "Model checksum mismatch" }
 }
}
tasks.named("preBuild").configure { dependsOn(verifyModel) }
