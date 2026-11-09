package com.vevs.xiaomi

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { Screen() } }
    }
}

@Composable
fun Screen() {
    Column(
        Modifier.fillMaxSize().background(Color.Black).systemBarsPadding().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (Config.CAMERA_RTSP.isNotBlank()) CameraView(Modifier.fillMaxWidth().weight(1f))
        Row(Modifier.fillMaxWidth().weight(1.6f, fill = true), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BulbColumn(Config.BULB_1, Modifier.weight(1f))
        }
    }
}

@Composable
fun BulbColumn(light: Light, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    fun send(on: Boolean) {
        status = "⏳"
        scope.launch {
            status = withContext(Dispatchers.IO) {
                runCatching { light.setPower(on) }
                    .fold({ if (on) "✅ ON" else "✅ OFF" }, { "❌ ${it.message}" })
            }
        }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(light.name, color = Color.White, fontSize = 20.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        BigButton("✓  ON", Color(0xFF2E9E3F), Modifier.weight(1f)) { send(true) }
        BigButton("✗  OFF", Color(0xFFC62828), Modifier.weight(1f)) { send(false) }
        Text(status, color = Color.LightGray, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 20.dp))
    }
}

@Composable
fun BigButton(label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White)
    ) { Text(label, fontSize = 32.sp) }
}

@OptIn(UnstableApi::class)
@Composable
fun CameraView(modifier: Modifier) {
    val ctx = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    val player = remember {
        ExoPlayer.Builder(ctx).build().apply {
            val src = RtspMediaSource.Factory().setForceUseRtpTcp(true)
                .createMediaSource(MediaItem.fromUri(Config.CAMERA_RTSP))
            setMediaSource(src)
            addListener(object : Player.Listener {
                override fun onPlayerError(e: PlaybackException) { error = "📷 ${e.errorCodeName}" }
                override fun onRenderedFirstFrame() { error = null }
            })
            prepare(); playWhenReady = true
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }
    Box(modifier.background(Color(0xFF111111), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
        AndroidView(factory = { PlayerView(it).apply { this.player = player; useController = false } },
            modifier = Modifier.fillMaxSize())
        error?.let { Text(it, color = Color.White, fontSize = 16.sp) }
    }
}
