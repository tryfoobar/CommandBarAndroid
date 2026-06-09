package com.commandbarexample.mainactivity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.commandbar.android.CommandBar
import com.commandbar.android.CommandBarOptions
import com.commandbarexample.mainactivity.ui.theme.MainActivityTheme
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Replace with your Amplitude **Guides & Surveys** project API key (same key as the web snippet).
 * Leave blank to see the inline "API key not set" toast.
 */
private const val AMPLITUDE_API_KEY = ""

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AMPLITUDE_API_KEY.isNotBlank()) {
            CommandBar.boot(CommandBarOptions(apiKey = AMPLITUDE_API_KEY))
        }

        setContent {
            MainActivityTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(
                        apiKeyConfigured = AMPLITUDE_API_KEY.isNotBlank(),
                        onOpenResourceCenter = {
                            CommandBar.openResourceCenter(
                                this@MainActivity,
                                onFallbackAction = ::handleFallback,
                            )
                        },
                        onOpenAssistant = {
                            CommandBar.openAssistant(
                                this@MainActivity,
                                onFallbackAction = ::handleFallback,
                            )
                        },
                    )
                }
            }
        }
    }

    private fun handleFallback(@Suppress("UNUSED_PARAMETER") action: Map<String, Any>) {
        println("Received fallback action")
        CommandBar.closeResourceCenter()
    }
}

@Composable
private fun HomeScreen(
    apiKeyConfigured: Boolean,
    onOpenResourceCenter: () -> Unit,
    onOpenAssistant: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedGradientBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LogoImage()
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Welcome to Amplitude!",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(48.dp))
            CustomButton(text = "Open Resource Center", onClick = onOpenResourceCenter)
            Spacer(modifier = Modifier.height(12.dp))
            CustomButton(text = "Open Assistant", onClick = onOpenAssistant)
        }

        if (!apiKeyConfigured) {
            Toast(
                message = "Set AMPLITUDE_API_KEY in MainActivity.kt (Amplitude API key).",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 56.dp, start = 16.dp, end = 16.dp),
            )
        }
    }
}

/**
 * Rotating purple → deep-blue linear gradient. Mirrors the iOS example's `GradientView`
 * by cycling the gradient direction over an 8s loop.
 */
@Composable
private fun AnimatedGradientBackground() {
    val transition = rememberInfiniteTransition(label = "gradient")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "angle",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val radians = Math.toRadians(angle.toDouble())
                val cosA = cos(radians).toFloat()
                val sinA = sin(radians).toFloat()
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r = hypot(size.width, size.height) / 2f
                val start = Offset(cx - cosA * r, cy - sinA * r)
                val end = Offset(cx + cosA * r, cy + sinA * r)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF7A3FE0), Color(0xFF142168)),
                        start = start,
                        end = end,
                    ),
                )
            },
    )
}

private const val AMPLITUDE_LOGO_URL =
    "https://www.freelogovectors.net/wp-content/uploads/2023/11/amplitude_logo-freelogovectors.net_.png"

/** 100×100 Amplitude logo loaded from a remote URL via Coil. */
@Composable
private fun LogoImage() {
    Box(
        modifier = Modifier.size(100.dp),
        contentAlignment = Alignment.Center,
    ) {
        // White disc sitting behind the logo so the transparent area inside
        // the "A" reads as white instead of letting the gradient show through.
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
        AsyncImage(
            model = AMPLITUDE_LOGO_URL,
            contentDescription = "Amplitude logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(100.dp),
        )
    }
}

@Composable
private fun CustomButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Black.copy(alpha = 0.8f),
            contentColor = Color.White,
        ),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Toast(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = "⚠  $message",
            color = Color.White,
            fontSize = 14.sp,
        )
    }
}
