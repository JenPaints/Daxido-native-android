package com.daxido.driver.presentation.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.daxido.core.theme.*

data class OnboardingStep(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val isCompleted: Boolean,
    val isRequired: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverOnboardingScreen(
    onNavigateToDocuments: () -> Unit,
    onNavigateToVehicleDetails: () -> Unit,
    onNavigateToBankDetails: () -> Unit,
    onNavigateToTraining: () -> Unit,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    var agreedToTerms by remember { mutableStateOf(false) }
    var profilePhotoUri by remember { mutableStateOf<String?>(null) }

    // Personal Details Form States
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }
    var emergencyContact by remember { mutableStateOf("") }

    val onboardingSteps = remember {
        listOf(
            OnboardingStep(
                "Personal Details",
                "Basic information and profile setup",
                Icons.Default.Person,
                isCompleted = false,
                isRequired = true
            ),
            OnboardingStep(
                "Document Upload",
                "KYC and driving license verification",
                Icons.Default.Description,
                isCompleted = false,
                isRequired = true
            ),
            OnboardingStep(
                "Vehicle Details",
                "Vehicle registration and documents",
                Icons.Default.DirectionsCar,
                isCompleted = false,
                isRequired = true
            ),
            OnboardingStep(
                "Bank Details",
                "For earnings and withdrawals",
                Icons.Default.AccountBalance,
                isCompleted = false,
                isRequired = true
            ),
            OnboardingStep(
                "Training",
                "Complete mandatory training modules",
                Icons.Default.School,
                isCompleted = false,
                isRequired = true
            ),
            OnboardingStep(
                "Background Check",
                "Verification and approval",
                Icons.Default.VerifiedUser,
                isCompleted = false,
                isRequired = true
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Driver Registration",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { }) {
                        Text("Help")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Progress Indicator
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Step ${currentStep + 1} of ${onboardingSteps.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${((currentStep + 1) * 100 / onboardingSteps.size)}% Complete",
                        style = MaterialTheme.typography.bodySmall,
                        color = DaxidoGold,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = (currentStep + 1).toFloat() / onboardingSteps.size,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = DaxidoGold,
                    trackColor = DaxidoLightGray
                )
            }

            // Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp)
            ) {
                when (currentStep) {
                    0 -> {
                        // Personal Details Step
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = DaxidoPaleGold.copy(alpha = 0.1f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Welcome to Daxido Driver",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "Start earning with India's premium ride-hailing platform",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))

                            // Profile Photo Upload
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier.size(120.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (profilePhotoUri != null) {
                                        AsyncImage(
                                            model = profilePhotoUri,
                                            contentDescription = "Profile",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .border(3.dp, DaxidoGold, CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(DaxidoLightGray)
                                                .border(3.dp, DaxidoGold, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CameraAlt,
                                                contentDescription = null,
                                                tint = DaxidoMediumBrown,
                                                modifier = Modifier.size(40.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { /* Upload photo */ },
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(DaxidoGold)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = DaxidoWhite,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    "Upload Profile Photo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))

                            // Personal Details Form
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Full Name *") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email Address *") },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text("Phone Number *") },
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = null)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Address *") },
                                leadingIcon = {
                                    Icon(Icons.Default.Home, contentDescription = null)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = city,
                                    onValueChange = { city = it },
                                    label = { Text("City *") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = pincode,
                                    onValueChange = { pincode = it },
                                    label = { Text("Pincode *") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = emergencyContact,
                                onValueChange = { emergencyContact = it },
                                label = { Text("Emergency Contact *") },
                                leadingIcon = {
                                    Icon(Icons.Default.ContactPhone, contentDescription = null)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    1 -> {
                        // Document Upload Step
                        item {
                            DocumentUploadSection(
                                onNavigateToDocuments = onNavigateToDocuments
                            )
                        }
                    }

                    2 -> {
                        // Vehicle Details Step
                        item {
                            VehicleDetailsSection(
                                onNavigateToVehicleDetails = onNavigateToVehicleDetails
                            )
                        }
                    }

                    3 -> {
                        // Bank Details Step
                        item {
                            BankDetailsSection(
                                onNavigateToBankDetails = onNavigateToBankDetails
                            )
                        }
                    }

                    4 -> {
                        // Training Step
                        item {
                            TrainingSection(
                                onNavigateToTraining = onNavigateToTraining
                            )
                        }
                    }

                    5 -> {
                        // Background Check Step
                        item {
                            BackgroundCheckSection(
                                agreedToTerms = agreedToTerms,
                                onAgreedToTermsChange = { agreedToTerms = it }
                            )
                        }
                    }
                }
            }

            // Bottom Navigation
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Previous")
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStep < onboardingSteps.size - 1) {
                                currentStep++
                            } else {
                                onComplete()
                            }
                        },
                        modifier = if (currentStep == 0) Modifier.fillMaxWidth() else Modifier.weight(1f),
                        enabled = when(currentStep) {
                            0 -> fullName.isNotEmpty() && email.isNotEmpty() && phoneNumber.isNotEmpty()
                            5 -> agreedToTerms
                            else -> true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DaxidoGold
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (currentStep == onboardingSteps.size - 1) "Complete Registration"
                            else "Next"
                        )
                        if (currentStep < onboardingSteps.size - 1) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentUploadSection(
    onNavigateToDocuments: () -> Unit
) {
    Column {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = DaxidoGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Required Documents",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val requiredDocs = listOf(
                    "Driving License" to Icons.Default.Badge,
                    "Aadhar Card" to Icons.Default.CreditCard,
                    "PAN Card" to Icons.Default.AccountBox,
                    "Profile Photo" to Icons.Default.Person,
                    "Address Proof" to Icons.Default.Home
                )

                requiredDocs.forEach { (doc, icon) ->
                    DocumentUploadItem(
                        name = doc,
                        icon = icon,
                        isUploaded = false
                    )
                    if (doc != requiredDocs.last().first) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNavigateToDocuments,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = DaxidoGold
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Upload,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Upload Documents")
        }
    }
}

@Composable
fun VehicleDetailsSection(
    onNavigateToVehicleDetails: () -> Unit
) {
    Column {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = DaxidoGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Vehicle Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val vehicleDocs = listOf(
                    "Vehicle Registration (RC)" to Icons.Default.Article,
                    "Insurance" to Icons.Default.Security,
                    "Pollution Certificate" to Icons.Default.Eco,
                    "Permit" to Icons.Default.VerifiedUser,
                    "Vehicle Photos" to Icons.Default.CameraAlt
                )

                vehicleDocs.forEach { (doc, icon) ->
                    DocumentUploadItem(
                        name = doc,
                        icon = icon,
                        isUploaded = false
                    )
                    if (doc != vehicleDocs.last().first) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNavigateToVehicleDetails,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = DaxidoGold
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Vehicle Details")
        }
    }
}

@Composable
fun BankDetailsSection(
    onNavigateToBankDetails: () -> Unit
) {
    Column {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = StatusOnline.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = StatusOnline,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Bank Account Setup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Add your bank details for seamless earnings withdrawal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    "Required Information",
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))

                listOf(
                    "Bank Account Number",
                    "IFSC Code",
                    "Account Holder Name",
                    "Bank Passbook/Statement"
                ).forEach { requirement ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            " $requirement",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNavigateToBankDetails,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = StatusOnline
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Setup Bank Account")
        }
    }
}

@Composable
fun TrainingSection(
    onNavigateToTraining: () -> Unit
) {
    Column {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Mandatory Training",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val trainingModules = listOf(
                    "Safe Driving Practices" to "30 min",
                    "Customer Service Excellence" to "25 min",
                    "App Navigation & Features" to "20 min",
                    "Earnings & Incentives" to "15 min",
                    "Emergency Procedures" to "20 min"
                )

                trainingModules.forEach { (module, duration) ->
                    TrainingModuleItem(
                        name = module,
                        duration = duration,
                        isCompleted = false
                    )
                    if (module != trainingModules.last().first) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = 0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF2196F3),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    "0 of 5 modules completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNavigateToTraining,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start Training")
        }
    }
}

@Composable
fun BackgroundCheckSection(
    agreedToTerms: Boolean,
    onAgreedToTermsChange: (Boolean) -> Unit
) {
    Column {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = StatusOnline,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Final Verification",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Your application will be reviewed within 24-48 hours",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    "What happens next?",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                val steps = listOf(
                    "Document verification" to "We'll verify all uploaded documents",
                    "Background check" to "Criminal and traffic violation records",
                    "Vehicle inspection" to "Physical verification of your vehicle",
                    "Account activation" to "Start accepting rides and earning"
                )

                steps.forEachIndexed { index, (title, description) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(DaxidoGold),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${index + 1}",
                                fontSize = 12.sp,
                                color = DaxidoWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        ) {
                            Text(
                                title,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (title != steps.last().first) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Terms Agreement
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = DaxidoPaleGold.copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAgreedToTermsChange(!agreedToTerms) }
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = agreedToTerms,
                    onCheckedChange = onAgreedToTermsChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = DaxidoGold
                    )
                )
                Column(
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        "I agree to the Terms & Conditions",
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "By agreeing, you confirm that all information provided is accurate and you agree to Daxido's driver partner terms.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentUploadItem(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isUploaded: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isUploaded) StatusOnline else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                " $name",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Icon(
            imageVector = if (isUploaded) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isUploaded) StatusOnline else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun TrainingModuleItem(
    name: String,
    duration: String,
    isCompleted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.PlayCircle,
                contentDescription = null,
                tint = if (isCompleted) StatusOnline else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Column(
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    duration,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (!isCompleted) {
            Text(
                "Pending",
                style = MaterialTheme.typography.labelSmall,
                color = DaxidoWarning
            )
        }
    }
}