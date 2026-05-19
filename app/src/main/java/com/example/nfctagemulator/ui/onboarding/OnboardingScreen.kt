package com.example.nfctagemulator.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfctagemulator.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconTint: Color,
    val accentColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val dimens = getAdaptiveDimens()
    val coroutineScope = rememberCoroutineScope()

    val pages = listOf(
        OnboardingPage(
            title = "Welcome to NFC Emulator",
            description = "Create, save and emulate NFC tags right from your phone!\n\nYour device becomes a universal NFC tag.",
            icon = Icons.Default.Nfc,
            iconTint = NeonCyan,
            accentColor = NeonCyan
        ),
        OnboardingPage(
            title = "Scan Physical Tags",
            description = "Hold your phone near any NFC tag to read it.\n\nSaved tags appear in the SAVED section.",
            icon = Icons.Default.QrCodeScanner,
            iconTint = NeonPurple,
            accentColor = NeonPurple
        ),
        OnboardingPage(
            title = "Create Custom Tags",
            description = "Create your own tags with URLs, text, phone numbers, emails or business cards (vCard).",
            icon = Icons.Default.AddCircle,
            iconTint = NeonPink,
            accentColor = NeonPink
        ),
        OnboardingPage(
            title = "Emulate Any Tag",
            description = "Tap ▶️ on any saved tag to start emulation.\n\nAnother phone can read it like a real NFC tag!",
            icon = Icons.Default.PhoneAndroid,
            iconTint = NeonGreen,
            accentColor = NeonGreen
        ),
        OnboardingPage(
            title = "Ready to Start!",
            description = "You're all set to use NFC Tag Emulator.\n\nTap 'Get Started' to begin!",
            icon = Icons.Default.CheckCircle,
            iconTint = NeonCyan,
            accentColor = NeonCyan
        )
    )

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { pages.size }
    )

    var currentPage by remember { mutableStateOf(0) }

    LaunchedEffect(pagerState.currentPage) {
        currentPage = pagerState.currentPage
    }

    // Animated background
    val infiniteTransition = rememberInfiniteTransition(label = "onboarding_bg")
    val gradientAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_angle"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BackgroundDark, SurfaceDark)
                )
            )
            .drawBehind {
                val width = size.width
                val height = size.height
                val angleRad = Math.toRadians(gradientAngle.toDouble()).toFloat()
                val centerX = width / 2
                val centerY = height / 2

                for (i in 0..2) {
                    val offsetAngle = angleRad + (i * Math.PI * 2 / 3).toFloat()
                    val x = centerX + width * 0.4f * kotlin.math.cos(offsetAngle)
                    val y = centerY + height * 0.3f * kotlin.math.sin(offsetAngle)

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                pages[currentPage].accentColor.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(x, y),
                            radius = width * 0.6f
                        ),
                        radius = width * 0.6f,
                        center = Offset(x, y)
                    )
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Skip button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.paddingLarge.dp, vertical = dimens.paddingMedium.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { onComplete() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = NeonCyan.copy(alpha = 0.7f)
                    )
                ) {
                    Text(
                        text = "SKIP",
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        fontSize = if (isLandscape) (dimens.bodyFontSize - 2).sp else dimens.bodyFontSize.sp
                    )
                }
            }

            // Horizontal Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = true,
                pageSpacing = dimens.paddingMedium.dp
            ) { pageIndex ->
                val page = pages[pageIndex]
                AnimatedContent(
                    targetState = pageIndex,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) +
                                slideInHorizontally(animationSpec = tween(300)) togetherWith
                                fadeOut(animationSpec = tween(300)) +
                                slideOutHorizontally(animationSpec = tween(300))
                    },
                    label = "page_content"
                ) { _ ->
                    OnboardingPageContent(
                        page = page,
                        isLandscape = isLandscape,
                        dimens = dimens
                    )
                }
            }

            // Bottom section: indicators + buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = dimens.paddingLarge.dp,
                        end = dimens.paddingLarge.dp,
                        bottom = dimens.paddingLarge.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page indicators
                Row(
                    modifier = Modifier.padding(bottom = dimens.paddingLarge.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pages.indices.forEach { index ->
                        val isSelected = index == currentPage
                        val indicatorWidth = if (isSelected) 24.dp else 8.dp

                        Box(
                            modifier = Modifier
                                .size(width = indicatorWidth, height = 8.dp)
                                .clip(CircleShape)
                                .background(
                                    color = if (isSelected)
                                        pages[currentPage].accentColor
                                    else
                                        pages[currentPage].accentColor.copy(alpha = 0.3f)
                                )
                                .animateContentSize()
                        )
                    }
                }

                // Next / Get Started button
                val isLastPage = currentPage == pages.size - 1

                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (isLastPage) {
                                onComplete()
                            } else {
                                pagerState.animateScrollToPage(currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isLandscape) 52.dp else 56.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(dimens.cardCornerRadius.dp),
                            clip = false
                        ),
                    shape = RoundedCornerShape(dimens.cardCornerRadius.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = pages[currentPage].accentColor,
                        contentColor = Color.Black
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isLastPage) "GET STARTED" else "NEXT",
                            fontSize = if (isLandscape) dimens.bodyFontSize.sp else (dimens.bodyFontSize + 2).sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        if (!isLastPage) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(dimens.iconSize.dp),
                                tint = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(
    page: OnboardingPage,
    isLandscape: Boolean,
    dimens: AdaptiveDimens
) {
    val infiniteTransition = rememberInfiniteTransition(label = "page_icon")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_scale"
    )
    val iconRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "icon_rotation"
    )

    val iconSize = when {
        isLandscape -> dimens.scannerSize * 1.2f
        else -> dimens.scannerSize.toFloat() * 1.1f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimens.paddingLarge.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated icon with rotating ring
        Box(
            modifier = Modifier
                .size(iconSize.dp)
                .drawBehind {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val radius = size.minDimension / 2

                    // Outer rotating ring
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                page.iconTint.copy(alpha = 0.6f),
                                page.iconTint.copy(alpha = 0.2f),
                                page.iconTint.copy(alpha = 0.6f)
                            ),
                            center = Offset(centerX, centerY)
                        ),
                        startAngle = iconRotation,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 2.5f)
                    )

                    // Inner glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                page.iconTint.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            center = Offset(centerX, centerY),
                            radius = radius * 0.7f
                        ),
                        radius = radius * 0.7f
                    )
                }
                .scale(iconScale),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier.size((iconSize / 2.2f).dp),
                tint = page.iconTint
            )
        }

        Spacer(modifier = Modifier.height(if (isLandscape) dimens.paddingLarge.dp else dimens.paddingLarge.dp * 1.5f))

        // Title
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            color = page.iconTint,
            fontFamily = FontFamily.Monospace,
            fontSize = if (isLandscape) dimens.headerFontSize.sp else (dimens.headerFontSize + 4).sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = dimens.paddingMedium.dp)
        )

        // Description
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isLandscape) dimens.paddingLarge.dp else 0.dp),
            shape = RoundedCornerShape(dimens.cardCornerRadius.dp),
            colors = CardDefaults.cardColors(
                containerColor = SurfaceGlow.copy(alpha = 0.5f)
            )
        ) {
            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = if (isLandscape) dimens.bodyFontSize.sp else (dimens.bodyFontSize + 2).sp,
                textAlign = TextAlign.Center,
                lineHeight = if (isLandscape) 22.sp else 26.sp,
                modifier = Modifier.padding(dimens.paddingLarge.dp)
            )
        }

        // Decorative dots
        if (!isLandscape && page.title != "Ready to Start!") {
            Spacer(modifier = Modifier.height(dimens.paddingLarge.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                color = page.iconTint.copy(alpha = 0.3f - (index * 0.1f))
                            )
                    )
                }
            }
        }
    }
}