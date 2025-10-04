package com.daxido.user.presentation.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import com.daxido.R
import com.daxido.core.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val animationRes: Int
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onNavigateToAuth: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            title = "Book Your Ride",
            description = "Choose from bikes, autos, cars, or premium vehicles. Get instant fare estimates and ETAs.",
            animationRes = R.raw.booking_animation
        ),
        OnboardingPage(
            title = "Track in Real-Time",
            description = "Follow your driver's location live. Share your trip with loved ones for safety.",
            animationRes = R.raw.tracking_animation
        ),
        OnboardingPage(
            title = "Safe & Secure",
            description = "Verified drivers, SOS button, and 24/7 support. Your safety is our priority.",
            animationRes = R.raw.safety_animation
        ),
        OnboardingPage(
            title = "Multiple Payment Options",
            description = "Pay with cash, cards, UPI, or wallet. Enjoy hassle-free transactions.",
            animationRes = R.raw.payment_animation
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                OnboardingPageContent(pages[page])
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.forEachIndexed { index, _ ->
                    PageIndicator(
                        isSelected = pagerState.currentPage == index,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    ) {
                        Text("Previous", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    TextButton(
                        onClick = onNavigateToAuth
                    ) {
                        Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onNavigateToAuth()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DaxidoGold
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = if (pagerState.currentPage < pages.size - 1) "Next" else "Get Started",
                        color = DaxidoWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            val composition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(page.animationRes)
            )
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
fun PageIndicator(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(if (isSelected) 32.dp else 8.dp, 8.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) DaxidoGold else DaxidoMediumBrown.copy(alpha = 0.3f)
            )
    )
}