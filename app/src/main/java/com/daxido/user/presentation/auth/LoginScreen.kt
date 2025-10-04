package com.daxido.user.presentation.auth

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daxido.R
import com.daxido.core.theme.*
import com.daxido.user.presentation.components.PhoneNumberInput
import com.daxido.user.presentation.components.SocialLoginButton

@Composable
fun LoginScreen(
    onNavigateToSignup: () -> Unit,
    onNavigateToOtp: (String) -> Unit,
    onGoogleSignIn: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    var phoneNumber by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("+91") }
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Handle OTP sent - navigate to OTP screen
    LaunchedEffect(uiState.otpSent) {
        if (uiState.otpSent) {
            onNavigateToOtp(countryCode + phoneNumber)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                DaxidoGold,
                                DaxidoGold.copy(alpha = 0.8f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = DaxidoWhite
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Welcome Back!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Login to continue your journey",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            PhoneNumberInput(
                phoneNumber = phoneNumber,
                onPhoneNumberChange = { phoneNumber = it },
                countryCode = countryCode,
                onCountryCodeChange = { countryCode = it },
                isError = uiState.errorMessage != null,
                errorMessage = uiState.errorMessage ?: ""
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (phoneNumber.length == 10) {
                        viewModel.sendOtp(countryCode + phoneNumber, context as Activity)
                    } else {
                        // Show error for invalid phone number
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DaxidoGold
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = phoneNumber.isNotEmpty() && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = DaxidoWhite,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "Send OTP",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DaxidoWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    text = "  OR  ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Divider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SocialLoginButton(
                text = "Continue with Google",
                icon = R.drawable.ic_google,
                onClick = { 
                    viewModel.signInWithGoogle()
                    onGoogleSignIn()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SocialLoginButton(
                text = "Continue with Apple",
                icon = R.drawable.ic_apple,
                onClick = { }
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Sign Up",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DaxidoGold,
                    modifier = Modifier.clickable { onNavigateToSignup() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}