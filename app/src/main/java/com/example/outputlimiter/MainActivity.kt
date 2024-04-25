package com.example.outputlimiter

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.outputlimiter.ui.theme.OutputLimiterTheme
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val audioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    private var volumeLocked by mutableStateOf(false)
    private var lockedVolume: Int = 0

    // Handler and Runnable for volume adjustment
    private val handler = Handler(Looper.getMainLooper())
    private val volumeCheckRunnable = object : Runnable {
        override fun run() {
            if (volumeLocked) {
                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (currentVolume > lockedVolume) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, lockedVolume, 0)
                } else if (currentVolume < lockedVolume) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, lockedVolume, 0)
                }
            }
            // Schedule the next check after 5 seconds (adjust as needed)
            handler.postDelayed(this, 250)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start the volume check runnable
        handler.post(volumeCheckRunnable)

        //Splash Screen
        Thread.sleep(3000)
        installSplashScreen()

        setContent {
            OutputLimiterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // App Name Title Header with Background Color
                        Surface(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                                .background(Color.Blue), // Change background color here
                            contentColor = Color.Black
                        ) {
                            Text(
                                text = "Output Limiter",
                                style = MaterialTheme.typography.headlineLarge,
                                modifier = Modifier.padding(bottom = 32.dp)
                            )

                            // Pass volumeLocked state and its setter to VolumeControlBox
                            VolumeControlBox(
                                volumeLocked = volumeLocked
                            ) {
                                toggleVolumeLock()
                            }
                        }

                        // Check for WRITE_SETTINGS permission
                        if (!Settings.System.canWrite(LocalContext.current)) {
                            // Permission is already granted
                            //val currentBrightness = getCurrentBrightness(LocalContext.current)
                            // Set the initial position of the brightness slider
                            // BrightnessControl(context = LocalContext.current, initialBrightness = currentBrightness)

                            // Permission not granted, show PermissionAlertDialog
                            PermissionAlertDialog(
                                onExitRequest = {
                                    Process.killProcess(Process.myPid())
                                },
                                onConfirmation = {
                                    // Request WRITE_SETTINGS permission
                                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                                    intent.data = Uri.parse("package:$packageName")
                                    startActivity(intent)
                                },
                                dialogText = "In order to toggle Brightness, you must enable the permission on your device\n\nAuto Brightness will also be disabled",
                                dialogTitle = "Permission Request",
                                icon = Icons.Default.Warning
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the runnable callbacks to prevent memory leaks
        handler.removeCallbacks(volumeCheckRunnable)
    }
    private fun toggleVolumeLock() {
        volumeLocked = !volumeLocked
        if (volumeLocked) {
            // Store the current volume level as the locked volume
            lockedVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        }
    }
}

@Composable
fun VolumeControlBox(
    volumeLocked: Boolean,
    toggleVolumeLock: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp) // Add padding to the Box for spacing
    ) {
        VolumeControl(
            modifier = Modifier,
            volumeLocked = volumeLocked
        ) {
            toggleVolumeLock()
        }

        BrightnessControl(
            modifier = Modifier,
            context = LocalContext.current
        )
    }
}


@Composable
fun VolumeControl(
    modifier: Modifier = Modifier,
    volumeLocked: Boolean,
    toggleVolumeLock: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    //val volumeLocked = mainActivity.getVolumeLockedState()
    var sliderPosition by remember { mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) }

    Column(
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "Volume Control:")
        Spacer(modifier = Modifier.height(8.dp))

        Row {
            Text(text = "Lock Volume")
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = volumeLocked,
                onCheckedChange = {
                    // Call toggleVolumeLock directly since it's a method of MainActivity
                    toggleVolumeLock()
                }
            )
        }
        
        Slider(
            value = sliderPosition.toFloat(),
            onValueChange = {
                if (!volumeLocked) {
                    sliderPosition = it.roundToInt()
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, sliderPosition, AudioManager.FLAG_SHOW_UI)
                }
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = if (volumeLocked) Color.Gray else MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = if (volumeLocked) Color.Gray else MaterialTheme.colorScheme.secondaryContainer,
            ),
            steps = 15,
            valueRange = 0f..audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat(),
            enabled = !volumeLocked // Disable the slider when volume is locked
        )
        Text(text = sliderPosition.toString())

        Spacer(modifier = Modifier.height(16.dp))


    }
}


@Composable
fun BrightnessControl(modifier: Modifier = Modifier, context: Context, initialBrightness: Int = 0) {
    var sliderPosition by remember { mutableIntStateOf(initialBrightness) }
    Column(
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.height(16.dp)) // Add this line for spacing above

        Text(text = "Brightness Control:")

        Column {
            Spacer(modifier = Modifier.height(16.dp)) // Add this line for spacing below "Volume Control"

            Slider(
                value = sliderPosition.toFloat(),
                onValueChange = {
                    sliderPosition = it.roundToInt()
                    updateBrightness(context, sliderPosition)
                },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.secondary,
                    activeTrackColor = MaterialTheme.colorScheme.secondary,
                    inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                steps = 9,
                valueRange = 100f..1600f
            )
            Text(text = sliderPosition.toString())
        }
    }
}

private fun updateBrightness(context: Context, brightnessLevel: Int) {
    try {
        if (Settings.System.canWrite(context)) {
            val brightnessValue = (brightnessLevel.toFloat() / 100.0f) * 255.0f
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                brightnessValue.roundToInt()
            )
        } else {
            // Request WRITE_SETTINGS permission
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:" + context.packageName)
            context.startActivity(intent)
        }
    } catch (e: Settings.SettingNotFoundException) {
        // Handle exception as needed
        e.printStackTrace()
    }

    val autoBrightnessEnabled =
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE) ==
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC

    Log.d("Brightness", "Auto Brightness Enabled: $autoBrightnessEnabled")

    if (autoBrightnessEnabled) {
        // Disable auto-brightness
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )
        Log.d("Brightness", "Auto Brightness Override to False")
    }
}



@Preview(showBackground = true)
@Composable
fun VolumeControlBoxPreview() {
    LocalContext.current // Assuming LocalContext is available in this scope
    OutputLimiterTheme {
        //VolumeControlBox(context, MainActivity())
        BrightnessControl(context = LocalContext.current)
    }
}

@Composable
fun PermissionAlertDialog(
    onExitRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String,
    icon: ImageVector,
) {
    AlertDialog(
        icon = {
            Icon(icon, contentDescription = "Example Icon")
        },
        title = {
            Text(text = dialogTitle)
        },
        text = {
            Text(text = dialogText)
        },
        onDismissRequest = {
            onExitRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onExitRequest()
                }
            ) {
                Text("Exit")
            }
        }
    )
}