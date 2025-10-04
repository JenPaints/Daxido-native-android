package com.daxido.user.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daxido.core.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationScreen(
    phoneNumber: String,
    onVerificationSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var otp by remember { mutableStateOf("") }
    var timeRemaining by remember { mutableStateOf(30) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Handle verification success
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onVerificationSuccess()
        }
    }

    LaunchedEffect(Unit) {
        while (timeRemaining > 0) {
            delay(1000)
            timeRemaining--
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Verify OTP",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "We've sent a code to $phoneNumber",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Visual OTP display boxes
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                for (i in 0..5) {
                    OtpDigitField(
                        digit = if (i < otp.length) otp[i].toString() else "",
                        isFocused = i == otp.length
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Actual OTP input field
            OutlinedTextField(
                value = otp,
                onValueChange = { newValue ->
                    if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                        otp = newValue
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Enter OTP") },
                placeholder = { Text("000000") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (timeRemaining > 0) {
                Text(
                    text = "Resend code in $timeRemaining seconds",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                TextButton(
                    onClick = {
                        viewModel.sendOtp(phoneNumber, context as android.app.Activity)
                        timeRemaining = 30
                    },
                    enabled = !uiState.isLoading
                ) {
                    Text("Resend OTP", color = DaxidoGold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (otp.length == 6) {
                        viewModel.verifyOtp(otp)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DaxidoGold
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = otp.length == 6 && !uiState.isVerifying
            ) {
                if (uiState.isVerifying) {
                    CircularProgressIndicator(
                        color = DaxidoWhite,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "Verify",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DaxidoWhite
                    )
                }
            }

            // Show error message if any
            uiState.errorMessage?.let { errorMessage ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun OtpDigitField(
    digit: String,
    isFocused: Boolean
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) DaxidoGold else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}