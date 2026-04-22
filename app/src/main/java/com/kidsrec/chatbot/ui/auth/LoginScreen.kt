package com.kidsrec.chatbot.ui.auth

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsrec.chatbot.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val SkyTop = Color(0xFFAEDFFF)
private val SkyMid = Color(0xFFDDF2FF)
private val SkyBottom = Color(0xFFFFEBDD)
private val SkyPink = Color(0xFFFBEAF6)

private val TitleBlue = Color(0xFF4D94E6)
private val TitleBlueDark = Color(0xFF367FD4)
private val BodyBlue = Color(0xFF6D82AE)
private val RegisterBlue = Color(0xFF3E86D8)

private val CardFill = Color(0xEFFFFFFF)
private val CardBorder = Color(0xF6FFFFFF)
private val CardShadowGlow = Color(0x40FFFFFF)

private val InputFill = Color(0xF9FFFFFF)
private val InputBorder = Color(0xFFD8E5F5)
private val InputFocused = Color(0xFFA8CDF8)
private val InputText = Color(0xFF61749B)
private val InputPlaceholder = Color(0xFF8FA0B8)

private val LoginGreenTop = Color(0xFF7EE96C)
private val LoginGreenBottom = Color(0xFF58C95D)

private val StarYellow = Color(0xFFFFE27A)
private val MoonYellow = Color(0xFFF9E28B)
private val BubbleBlue = Color(0x6689D0FF)
private val CloudWhite = Color(0xCFFFFFFF)

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(SkyTop, SkyMid, SkyBottom, SkyPink)
                )
            )
            .systemBarsPadding()
    ) {
        DreamySkyBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            DreamyHeroSection()

            Spacer(modifier = Modifier.height(18.dp))

            DreamyLoginCard {
                Text(
                    text = "Welcome back!",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TitleBlue
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Login to continue your reading adventure\nwith Little Dino.",
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = BodyBlue
                )

                Spacer(modifier = Modifier.height(22.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Email") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = RegisterBlue
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(28.dp),
                    colors = dreamyTextFieldColors()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = RegisterBlue
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Default.Visibility
                                } else {
                                    Icons.Default.VisibilityOff
                                },
                                contentDescription = if (passwordVisible) {
                                    "Hide password"
                                } else {
                                    "Show password"
                                },
                                tint = RegisterBlue.copy(alpha = 0.85f)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    shape = RoundedCornerShape(28.dp),
                    colors = dreamyTextFieldColors()
                )

                if (authState is AuthState.Error) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = (authState as AuthState.Error).message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { viewModel.signIn(email.trim(), password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(28.dp),
                    enabled = authState !is AuthState.Loading &&
                            email.isNotBlank() &&
                            password.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = if (authState is AuthState.Loading) {
                                        listOf(
                                            LoginGreenTop.copy(alpha = 0.6f),
                                            LoginGreenBottom.copy(alpha = 0.6f)
                                        )
                                    } else {
                                        listOf(LoginGreenTop, LoginGreenBottom)
                                    }
                                ),
                                shape = RoundedCornerShape(28.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(28.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.6.dp
                            )
                        } else {
                            Text(
                                text = "Login",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = buildAnnotatedString {
                        append("Don’t have an account? ")
                        withStyle(
                            SpanStyle(
                                color = RegisterBlue,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append("Register")
                        }
                    },
                    fontSize = 14.sp,
                    color = BodyBlue,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }
        }
    }
}

@Composable
private fun DreamyHeroSection() {
    val transition = rememberInfiniteTransition(label = "hero")

    val floatY by transition.animateFloat(
        initialValue = -4f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x55FFF0A9),
                                Color(0x44FFFFFF),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Box(
                modifier = Modifier
                    .size(190.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.20f),
                        shape = CircleShape
                    )
            )

            Image(
                painter = painterResource(id = R.drawable.little_dino),
                contentDescription = "Little Dino mascot",
                modifier = Modifier
                    .size(185.dp)
                    .offset(y = floatY.dp),
                contentScale = ContentScale.Fit
            )
        }

        Text(
            text = "Little Dino",
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TitleBlue,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.White,
                    blurRadius = 12f
                )
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "A safe and fun AI buddy for\nbooks and videos.",
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = BodyBlue
        )
    }
}

@Composable
private fun DreamyLoginCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(34.dp),
        color = CardFill,
        shadowElevation = 18.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            CardShadowGlow,
                            Color.Transparent
                        )
                    )
                )
                .border(
                    width = 1.3.dp,
                    color = CardBorder,
                    shape = RoundedCornerShape(34.dp)
                )
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
private fun DreamySkyBackground() {
    val transition = rememberInfiniteTransition(label = "sky")

    val starShift by transition.animateFloat(
        initialValue = -6f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starShift"
    )

    val bubbleShift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bubbleShift"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val s = min(w, h)

        drawSoftCloud(Offset(w * 0.08f, h * 0.14f), 110f)
        drawSoftCloud(Offset(w * 0.90f, h * 0.16f), 95f)
        drawSoftCloud(Offset(w * 0.12f, h * 0.83f), 140f)
        drawSoftCloud(Offset(w * 0.86f, h * 0.80f), 120f)
        drawSoftCloud(Offset(w * 0.47f, h * 0.90f), 150f)

        drawDreamyStar(center = Offset(w * 0.15f + starShift, h * 0.08f), radius = s * 0.03f)
        drawDreamyStar(center = Offset(w * 0.34f, h * 0.22f + starShift), radius = s * 0.018f)
        drawDreamyStar(center = Offset(w * 0.72f, h * 0.14f), radius = s * 0.02f)
        drawDreamyStar(center = Offset(w * 0.78f, h * 0.32f), radius = s * 0.016f)
        drawDreamyStar(center = Offset(w * 0.21f, h * 0.73f), radius = s * 0.02f)
        drawDreamyStar(center = Offset(w * 0.87f, h * 0.92f), radius = s * 0.03f)

        drawTwinkle(center = Offset(w * 0.42f, h * 0.10f), size = s * 0.016f)
        drawTwinkle(center = Offset(w * 0.65f, h * 0.22f), size = s * 0.02f)
        drawTwinkle(center = Offset(w * 0.74f, h * 0.66f), size = s * 0.015f)
        drawTwinkle(center = Offset(w * 0.28f, h * 0.62f), size = s * 0.014f)

        drawMoon(
            topLeft = Offset(w * 0.77f, h * 0.07f),
            size = s * 0.12f
        )

        drawBubble(center = Offset(w * 0.92f, h * 0.24f + bubbleShift), radius = s * 0.045f)
        drawBubble(center = Offset(w * 0.93f, h * 0.76f - bubbleShift), radius = s * 0.05f)

        drawCircle(
            color = Color.White.copy(alpha = 0.20f),
            radius = s * 0.24f,
            center = Offset(w * 0.95f, h * 0.86f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.14f),
            radius = s * 0.18f,
            center = Offset(w * 0.02f, h * 0.88f)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSoftCloud(
    center: Offset,
    baseWidth: Float
) {
    val height = baseWidth * 0.42f

    drawRoundRect(
        color = CloudWhite,
        topLeft = Offset(center.x - baseWidth / 2f, center.y - height / 2f),
        size = androidx.compose.ui.geometry.Size(baseWidth, height),
        cornerRadius = CornerRadius(height / 2f, height / 2f)
    )
    drawCircle(
        color = CloudWhite,
        radius = height * 0.52f,
        center = Offset(center.x - baseWidth * 0.18f, center.y - height * 0.18f)
    )
    drawCircle(
        color = CloudWhite,
        radius = height * 0.65f,
        center = Offset(center.x + baseWidth * 0.02f, center.y - height * 0.28f)
    )
    drawCircle(
        color = CloudWhite,
        radius = height * 0.45f,
        center = Offset(center.x + baseWidth * 0.25f, center.y - height * 0.12f)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDreamyStar(
    center: Offset,
    radius: Float
) {
    val path = Path()
    val innerRadius = radius * 0.45f
    for (i in 0 until 10) {
        val angle = (PI / 5.0 * i) - PI / 2.0
        val r = if (i % 2 == 0) radius else innerRadius
        val x = center.x + (cos(angle) * r).toFloat()
        val y = center.y + (sin(angle) * r).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()

    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(StarYellow, Color(0xFFFFC85A))
        )
    )
    drawPath(
        path = path,
        color = Color.White.copy(alpha = 0.35f),
        style = Stroke(width = radius * 0.12f)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTwinkle(
    center: Offset,
    size: Float
) {
    drawLine(
        color = Color.White.copy(alpha = 0.85f),
        start = Offset(center.x, center.y - size),
        end = Offset(center.x, center.y + size),
        strokeWidth = size * 0.18f
    )
    drawLine(
        color = Color.White.copy(alpha = 0.85f),
        start = Offset(center.x - size, center.y),
        end = Offset(center.x + size, center.y),
        strokeWidth = size * 0.18f
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMoon(
    topLeft: Offset,
    size: Float
) {
    drawArc(
        color = MoonYellow,
        startAngle = 70f,
        sweepAngle = 220f,
        useCenter = true,
        topLeft = topLeft,
        size = androidx.compose.ui.geometry.Size(size, size)
    )
    drawCircle(
        color = Color(0x55FFD98A),
        radius = size * 0.08f,
        center = Offset(topLeft.x + size * 0.35f, topLeft.y + size * 0.32f)
    )
    drawCircle(
        color = Color(0x45FFD98A),
        radius = size * 0.05f,
        center = Offset(topLeft.x + size * 0.48f, topLeft.y + size * 0.58f)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBubble(
    center: Offset,
    radius: Float
) {
    drawCircle(
        color = BubbleBlue,
        radius = radius,
        center = center,
        style = Stroke(width = radius * 0.08f)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.28f),
        radius = radius * 0.18f,
        center = Offset(center.x - radius * 0.28f, center.y - radius * 0.30f)
    )
}

@Composable
private fun dreamyTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = InputFill,
    unfocusedContainerColor = InputFill,
    disabledContainerColor = InputFill,
    focusedBorderColor = InputFocused,
    unfocusedBorderColor = InputBorder,
    disabledBorderColor = InputBorder.copy(alpha = 0.6f),
    focusedTextColor = InputText,
    unfocusedTextColor = InputText,
    disabledTextColor = InputText.copy(alpha = 0.5f),
    cursorColor = RegisterBlue,
    focusedLabelColor = RegisterBlue,
    unfocusedLabelColor = InputPlaceholder,
    focusedLeadingIconColor = RegisterBlue,
    unfocusedLeadingIconColor = RegisterBlue.copy(alpha = 0.82f),
    focusedTrailingIconColor = RegisterBlue,
    unfocusedTrailingIconColor = RegisterBlue.copy(alpha = 0.82f)
)