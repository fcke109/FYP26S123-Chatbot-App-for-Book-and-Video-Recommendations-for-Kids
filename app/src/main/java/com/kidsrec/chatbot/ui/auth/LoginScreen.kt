package com.kidsrec.chatbot.ui.auth

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsrec.chatbot.R

private val SkyTop = Color(0xFFDFF2FF)
private val SkyMid = Color(0xFFF2FAFF)
private val SkyBottom = Color(0xFFFFF8F2)

private val PrimaryBlue = Color(0xFF2F76D2)
private val PrimaryBlueDark = Color(0xFF205A9D)
private val SoftText = Color(0xFF687684)
private val LoginGreen = Color(0xFF67C96B)

private val CardWhite = Color(0xF7FFFFFF)
private val CardBorder = Color(0xFFFFFFFF)

private val CloudFront = Color(0xF7FFFFFF)
private val CloudBack = Color(0xCCFFFFFF)
private val CloudShadow = Color(0x22AFCFE8)

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onAdminLogin: () -> Unit,
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
                    colors = listOf(SkyTop, SkyMid, SkyBottom)
                )
            )
            .systemBarsPadding()
    ) {
        ProminentCloudBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LoginHeroSection()

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                color = CardWhite,
                shadowElevation = 14.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.5.dp,
                            color = CardBorder,
                            shape = RoundedCornerShape(30.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Welcome back",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryBlue
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Login to continue your reading adventure with Little Dino.",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = SoftText
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                tint = PrimaryBlue
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(18.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = PrimaryBlue
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
                                    }
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(18.dp)
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
                        onClick = {
                            viewModel.signIn(email.trim(), password)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        enabled = authState !is AuthState.Loading &&
                                email.isNotBlank() &&
                                password.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LoginGreen
                        )
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = "Login",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(onClick = onNavigateToRegister) {
                        Text(
                            text = "Don't have an account? Register",
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryBlueDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginHeroSection() {
    val transition = rememberInfiniteTransition(label = "login_hero")

    val rotation by transition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    val floatY by transition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(220.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.45f),
                shadowElevation = 0.dp
            ) {}

            Image(
                painter = painterResource(id = R.drawable.little_dino),
                contentDescription = "Little Dino Mascot",
                modifier = Modifier
                    .size(190.dp)
                    .offset(y = floatY.dp)
                    .rotate(rotation),
                contentScale = ContentScale.Fit
            )
        }

        Text(
            text = "Little Dino",
            fontSize = 38.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PrimaryBlue,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.White,
                    blurRadius = 10f
                )
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Safe and fun books and videos for kids",
            fontSize = 15.sp,
            color = SoftText
        )
    }
}

@Composable
private fun ProminentCloudBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        CloudCluster(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-30).dp, y = 30.dp),
            scale = 1.0f
        )

        CloudCluster(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 28.dp, y = 90.dp),
            scale = 1.15f
        )

        CloudCluster(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-36).dp, y = (-10).dp),
            scale = 1.1f
        )

        CloudCluster(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 34.dp, y = 40.dp),
            scale = 1.2f
        )

        CloudCluster(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-26).dp, y = (-70).dp),
            scale = 1.3f
        )

        CloudCluster(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 36.dp, y = (-120).dp),
            scale = 1.2f
        )

        BigBottomClouds(
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun CloudCluster(
    modifier: Modifier = Modifier,
    scale: Float = 1f
) {
    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .size(width = (150 * scale).dp, height = (60 * scale).dp)
                .offset(y = (10 * scale).dp),
            shape = RoundedCornerShape(50),
            color = CloudBack,
            shadowElevation = 4.dp
        ) {}

        Surface(
            modifier = Modifier
                .size(width = (170 * scale).dp, height = (68 * scale).dp),
            shape = RoundedCornerShape(50),
            color = CloudFront,
            shadowElevation = 6.dp
        ) {}

        Surface(
            modifier = Modifier
                .size((68 * scale).dp)
                .offset(x = (14 * scale).dp, y = (-18 * scale).dp),
            shape = CircleShape,
            color = CloudFront,
            shadowElevation = 6.dp
        ) {}

        Surface(
            modifier = Modifier
                .size((82 * scale).dp)
                .offset(x = (56 * scale).dp, y = (-26 * scale).dp),
            shape = CircleShape,
            color = CloudFront,
            shadowElevation = 6.dp
        ) {}

        Surface(
            modifier = Modifier
                .size((60 * scale).dp)
                .offset(x = (110 * scale).dp, y = (-10 * scale).dp),
            shape = CircleShape,
            color = CloudFront,
            shadowElevation = 6.dp
        ) {}
    }
}

@Composable
private fun BigBottomClouds(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.BottomCenter),
            color = CloudFront,
            shadowElevation = 8.dp
        ) {}

        Surface(
            modifier = Modifier
                .size(180.dp)
                .offset(x = (-20).dp, y = 25.dp)
                .align(Alignment.BottomStart),
            shape = CircleShape,
            color = CloudFront,
            shadowElevation = 8.dp
        ) {}

        Surface(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.BottomCenter)
                .offset(y = 5.dp),
            shape = CircleShape,
            color = CloudFront,
            shadowElevation = 8.dp
        ) {}

        Surface(
            modifier = Modifier
                .size(170.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 20.dp, y = 22.dp),
            shape = CircleShape,
            color = CloudFront,
            shadowElevation = 8.dp
        ) {}

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .align(Alignment.BottomCenter),
            color = CloudShadow
        ) {}
    }
}