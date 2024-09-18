package com.example.smarthomegesturecontrolapplication

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.smarthomegesturecontrolapplication.ui.theme.SmartHomeGestureControlApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import java.io.File


// constants
val userLastName: String = "NavarroGonzalez"
val serverUploadURL: String = "http://10.0.2.2:5000/upload"  // fog server (this maps to localhost on emulator's host computer)

// will keep track of gesture selection
var selectedGestureName: String = ""
var selectedGestureID: Int = 0

val gesturesNameToIdMap = mapOf(
    "Turn on lights" to R.raw.lighton,
    "Turn off lights" to R.raw.lightoff,
    "Turn on fan" to R.raw.fanon,
    "Turn off fan" to R.raw.fanoff,
    "Increase fan speed" to R.raw.fanup,
    "Decrease fan speed" to R.raw.fandown,
    "Set Thermostat to specified temperature" to R.raw.setthermo,
    "0" to R.raw.num0,
    "1" to R.raw.num1,
    "2" to R.raw.num2,
    "3" to R.raw.num3,
    "4" to R.raw.num4,
    "5" to R.raw.num5,
    "6" to R.raw.num6,
    "7" to R.raw.num7,
    "8" to R.raw.num8,
    "9" to R.raw.num9
)

val gesturesIdToLabelMap = mapOf(
    R.raw.lighton to "LightOn",
    R.raw.lightoff to "LightOff",
    R.raw.fanon to "FanOn",
    R.raw.fanoff to "FanOff",
    R.raw.fanup to "FanUp",
    R.raw.fandown to "FanDown",
    R.raw.setthermo to "SetThermo",
    R.raw.num0 to "Num0",
    R.raw.num1 to "Num1",
    R.raw.num2 to "Num2",
    R.raw.num3 to "Num3",
    R.raw.num4 to "Num4",
    R.raw.num5 to "Num5",
    R.raw.num6 to "Num6",
    R.raw.num7 to "Num7",
    R.raw.num8 to "Num8",
    R.raw.num9 to "Num9"
)



class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartHomeGestureControlApplicationTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "screen1") {
                    // screen 1
                    composable(route = "screen1") { Screen1(navController) }

                    // screen 2
                    composable("screen2") { Screen2(navController) }


                    // screen 3
                    composable("screen3") { Screen3(navController) }
                }
            }
        }
    }
}


@Composable
fun Screen1(navController: NavController) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = selectedGestureName,
            onValueChange = { },
            label = { Text("Selected gesture") },
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Filled.ArrowDropDown, "Dropdown menu")
                }
            },
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            gesturesNameToIdMap.forEach { (gesture, path) ->
                DropdownMenuItem(
                    onClick = {
                        selectedGestureName = gesture
                        selectedGestureID = path
                        expanded = false
                    },
                    text = { Text(gesture) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { navController.navigate("screen2") },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Next") }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Selected gesture: $selectedGestureName", modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun Screen2(navController: NavController) {
    val videoUri = Uri.parse("android.resource://${LocalContext.current.packageName}/$selectedGestureID")
    var videoView by remember { mutableStateOf<VideoView?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("Selected gesture: $selectedGestureName", modifier = Modifier.fillMaxWidth())

        // video
        Spacer(modifier = Modifier.height(16.dp))
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                VideoView(context).apply {
                    videoView = this
                    setVideoURI(videoUri)
                    setOnCompletionListener { pause() }
                    requestFocus()
                    start()
                }
            }
        )

        // buttons row
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            // back button
            Button(onClick = { navController.popBackStack() }) { Text("Back") }
            Spacer(modifier = Modifier.weight(1f))

            // replay button
            Button(onClick = { videoView?.start() }) { Text("Replay") }
            Spacer(modifier = Modifier.weight(1f))

            // practice button
            Button(onClick = { navController.navigate("screen3") }) { Text("Practice") }
        }
    }
}

fun uploadVideo(videoFile: File, serverUploadURL: String): Response {
    val client = OkHttpClient()
    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart(
            "file", videoFile.name,
            videoFile.asRequestBody("video/mp4".toMediaTypeOrNull())
        )
        .build()
    val request = Request.Builder()
        .url(serverUploadURL)
        .post(requestBody)
        .build()

    return client.newCall(request).execute()
}

@Composable
fun Screen3(navController: NavController) {
    val context = LocalContext.current
    var recording: Recording? = remember { null }
    val recorder = remember { Recorder.Builder().build() }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }
    var isRecording by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(5) }
    var showMessage by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var pathToLastVideo by remember { mutableStateOf("") }
    var practiceNumber by remember { mutableIntStateOf(1) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> showCamera = granted }
    )
    LaunchedEffect(key1 = true) { launcher.launch(Manifest.permission.CAMERA) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("Gesture to record: $selectedGestureName", modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))

        if (!showCamera) {
            Text("Camera permission required")
        } else {
            AndroidView(
                modifier = Modifier.weight(1f),
                factory = { context ->
                    val previewView = PreviewView(context)
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

                    cameraProviderFuture.addListener(
                        {
                            val cameraProvider = cameraProviderFuture.get()
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                context as ComponentActivity,
                                cameraSelector,
                                preview,
                                videoCapture
                            )
                        },
                        ContextCompat.getMainExecutor(context)
                    )
                    previewView
                }
            )

            if (isRecording) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Recording: $countdown seconds remaining",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (showMessage) {
                Text(
                    text = message,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }

            // buttons row
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                // back button
                Button(onClick = { navController.popBackStack() }) { Text("Back") }

                // home button
                Button(onClick = { navController.navigate("screen1") }) { Text("Home") }

                // record button
                Button(
                    onClick = {
                        if (!isRecording) {
                            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val outputFile = File(downloadsDir, "${gesturesIdToLabelMap[selectedGestureID]}_PRACTICE_${practiceNumber}_$userLastName.mp4")
                            val outputOptions = FileOutputOptions.Builder(outputFile).build()

                            recording = videoCapture.output
                                .prepareRecording(context, outputOptions)
                                .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                                    when (recordEvent) {
                                        is VideoRecordEvent.Start -> {
                                            isRecording = true
                                            showMessage = false
                                            CoroutineScope(Dispatchers.Main).launch {
                                                while (countdown > 0) {
                                                    delay(1000)
                                                    countdown--
                                                }
                                                recording?.stop()
                                            }
                                        }

                                        is VideoRecordEvent.Finalize -> {
                                            isRecording = false
                                            countdown = 5
                                            pathToLastVideo = outputFile.absolutePath
                                            if (!recordEvent.hasError()) {
                                                message = "Video capture succeeded: $pathToLastVideo"
                                            } else {
                                                message = "Video capture ends with error: ${recordEvent.error}"
                                                recording?.close()
                                                recording = null
                                            }
                                            showMessage = true
                                        }
                                    }
                                }

                            practiceNumber += 1

                        } else {
                            recording?.stop()
                            countdown = 5
                        }
                    },
                ) { Text(if (isRecording) "Stop Recording" else "Record") }

                // upload button
                Button(onClick = {
                    val file = File(pathToLastVideo)
                    if (file.exists()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val response = uploadVideo(file, serverUploadURL)
                            message = "Upload response: ${response.body?.string()}"
                            showMessage = true
                        }
                    } else {
                        message = "Can't find video file=$pathToLastVideo"
                        showMessage = true
                    }

                }) { Text("Upload") }
            }
        }
    }
}
