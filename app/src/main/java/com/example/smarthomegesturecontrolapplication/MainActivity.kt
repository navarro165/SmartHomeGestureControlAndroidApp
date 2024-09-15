package com.example.smarthomegesturecontrolapplication

import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smarthomegesturecontrolapplication.ui.theme.SmartHomeGestureControlApplicationTheme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


val GESTURES = mapOf(
    "Turn on lights" to R.raw.h_lighton,
    "Turn off lights" to R.raw.h_lightoff,
    "Turn on fan" to R.raw.h_fanon,
    "Turn off fan" to R.raw.h_fanoff,
    "Increase fan speed" to R.raw.h_increasefanspeed,
    "Decrease fan speed" to R.raw.h_decreasefanspeed,
    "Set Thermostat to specified temperature" to R.raw.h_setthermo,
    "0" to R.raw.h_0,
    "1" to R.raw.h_1,
    "2" to R.raw.h_2,
    "3" to R.raw.h_3,
    "4" to R.raw.h_4,
    "5" to R.raw.h_5,
    "6" to R.raw.h_6,
    "7" to R.raw.h_7,
    "8" to R.raw.h_8,
    "9" to R.raw.h_9
)
val invGESTURES = GESTURES.entries.associateBy({ it.value }, { it.key })


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
                    composable(
                        route = "screen2/{videoResId}",
                        arguments = listOf(navArgument("videoResId") { type = NavType.IntType })
                    ) {
                        backStackEntry ->
                        val videoResId = backStackEntry.arguments?.getInt("videoResId") ?: 0
                        Screen2(navController, videoResId)
                    }

                    // screen 3
                    composable("screen3") { Screen3(navController) }
                }
            }
        }
    }
}


@Composable
fun Screen1(navController: NavController) {
    var selectedGesture by remember { mutableStateOf("") }
    var selectedPath by remember { mutableStateOf(0) }
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = selectedGesture,
            onValueChange = { selectedGesture = it },
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
            GESTURES.forEach { (gesture, path) ->
                DropdownMenuItem(
                    onClick = {
                        selectedGesture = gesture
                        selectedPath = path
                        expanded = false
                    },
                    text = { Text(gesture) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { navController.navigate("screen2/${selectedPath}") },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Next") }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Selected gesture: $selectedGesture", modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun Screen2(navController: NavController, videoResId: Int) {
    val videoUri = Uri.parse("android.resource://${LocalContext.current.packageName}/$videoResId")
    var replayCount by remember { mutableStateOf(0) }
    var videoView by remember { mutableStateOf<VideoView?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("Selected gesture: ${invGESTURES[videoResId]}", modifier = Modifier.fillMaxWidth())

        // video
        Spacer(modifier = Modifier.height(16.dp))
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                VideoView(context).apply {
                    videoView = this
                    setVideoURI(videoUri)
                    setOnCompletionListener {
                        if (replayCount < 2) {
                            replayCount++
                            start()
                        } else {
                            pause()
                        }
                    }
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
            Button(onClick = {
                replayCount = 0
                videoView?.start()
            }) { Text("Replay") }
            Spacer(modifier = Modifier.weight(1f))

            // practice button
            Button(onClick = { navController.navigate("screen3") }) { Text("Practice") }
        }
    }
}

@Composable
fun Screen3(navController: NavController) {
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        AndroidView(factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(context as LifecycleOwner, cameraSelector, preview)
            }, executor)
            previewView
        }, modifier = Modifier.weight(1f))

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            // Implement video capture and upload logic here
            navController.navigate("screen1")
        }) {
            Text("Upload")
        }
    }
}