package com.daxido.driver.presentation.documents

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daxido.core.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class Document(
    val id: String,
    val name: String,
    val type: DocumentType,
    val status: DocumentStatus,
    val expiryDate: LocalDate?,
    val uploadedDate: LocalDate,
    val verificationNote: String?,
    val documentNumber: String?
)

enum class DocumentType {
    DRIVING_LICENSE,
    VEHICLE_REGISTRATION,
    INSURANCE,
    POLLUTION_CERTIFICATE,
    PERMIT,
    AADHAR_CARD,
    PAN_CARD,
    BANK_STATEMENT,
    VEHICLE_FITNESS,
    PROFILE_PHOTO
}

enum class DocumentStatus {
    VERIFIED,
    PENDING,
    REJECTED,
    EXPIRED,
    NOT_UPLOADED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentManagerScreen(
    onBack: () -> Unit,
    onUploadDocument: (DocumentType) -> Unit,
    onViewDocument: (Document) -> Unit,
    onUpdateDocument: (Document) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("all") }
    var showUploadDialog by remember { mutableStateOf(false) }
    var selectedDocumentType by remember { mutableStateOf<DocumentType?>(null) }

    val documents = remember {
        listOf(
            Document(
                "DOC001",
                "Driving License",
                DocumentType.DRIVING_LICENSE,
                DocumentStatus.VERIFIED,
                LocalDate.of(2025, 6, 15),
                LocalDate.now().minusMonths(2),
                null,
                "KA09-2024-0012345"
            ),
            Document(
                "DOC002",
                "Vehicle Registration",
                DocumentType.VEHICLE_REGISTRATION,
                DocumentStatus.VERIFIED,
                LocalDate.of(2035, 3, 10),
                LocalDate.now().minusMonths(1),
                null,
                "KA01AB1234"
            ),
            Document(
                "DOC003",
                "Insurance",
                DocumentType.INSURANCE,
                DocumentStatus.EXPIRED,
                LocalDate.now().minusDays(5),
                LocalDate.now().minusYears(1),
                "Please upload renewed insurance",
                "INS2023456789"
            ),
            Document(
                "DOC004",
                "Pollution Certificate",
                DocumentType.POLLUTION_CERTIFICATE,
                DocumentStatus.PENDING,
                LocalDate.of(2024, 12, 31),
                LocalDate.now().minusDays(2),
                "Under review",
                null
            ),
            Document(
                "DOC005",
                "Aadhar Card",
                DocumentType.AADHAR_CARD,
                DocumentStatus.VERIFIED,
                null,
                LocalDate.now().minusMonths(6),
                null,
                "****-****-4567"
            )
        )
    }

    val requiredDocuments = listOf(
        DocumentType.DRIVING_LICENSE,
        DocumentType.VEHICLE_REGISTRATION,
        DocumentType.INSURANCE,
        DocumentType.POLLUTION_CERTIFICATE,
        DocumentType.AADHAR_CARD,
        DocumentType.PAN_CARD,
        DocumentType.PROFILE_PHOTO
    )

    val uploadedCount = documents.count { it.status != DocumentStatus.NOT_UPLOADED }
    val verifiedCount = documents.count { it.status == DocumentStatus.VERIFIED }
    val pendingCount = documents.count { it.status == DocumentStatus.PENDING }
    val expiredCount = documents.count { it.status == DocumentStatus.EXPIRED }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Documents",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.HelpOutline, "Help")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showUploadDialog = true },
                containerColor = DaxidoGold,
                contentColor = DaxidoWhite
            ) {
                Icon(Icons.Default.Upload, "Upload")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload Document")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Verification Status Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            expiredCount > 0 -> Color.Red.copy(alpha = 0.1f)
                            pendingCount > 0 -> DaxidoWarning.copy(alpha = 0.1f)
                            verifiedCount == requiredDocuments.size -> StatusOnline.copy(alpha = 0.1f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = when {
                                        expiredCount > 0 -> "Action Required"
                                        pendingCount > 0 -> "Verification Pending"
                                        verifiedCount == requiredDocuments.size -> "All Verified"
                                        else -> "Documents Incomplete"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$verifiedCount of ${requiredDocuments.size} documents verified",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(
                                modifier = Modifier.size(60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = verifiedCount.toFloat() / requiredDocuments.size,
                                    modifier = Modifier.fillMaxSize(),
                                    strokeWidth = 6.dp,
                                    color = when {
                                        expiredCount > 0 -> Color.Red
                                        pendingCount > 0 -> DaxidoWarning
                                        else -> StatusOnline
                                    },
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Text(
                                    text = "${(verifiedCount * 100 / requiredDocuments.size)}%",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        if (expiredCount > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.Red.copy(alpha = 0.1f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color.Red,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = " $expiredCount document(s) expired. Update immediately!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Red,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Document Stats
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        count = verifiedCount,
                        label = "Verified",
                        color = StatusOnline,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        count = pendingCount,
                        label = "Pending",
                        color = DaxidoWarning,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        count = expiredCount,
                        label = "Expired",
                        color = Color.Red,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Filter Tabs
            item {
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == "all",
                            onClick = { selectedCategory = "all" },
                            label = { Text("All") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedCategory == "required",
                            onClick = { selectedCategory = "required" },
                            label = { Text("Required") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedCategory == "expired",
                            onClick = { selectedCategory = "expired" },
                            label = { Text("Expired") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedCategory == "pending",
                            onClick = { selectedCategory = "pending" },
                            label = { Text("Pending") }
                        )
                    }
                }
            }

            // Documents List
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Your Documents",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Document Cards
            items(documents.filter { doc ->
                when(selectedCategory) {
                    "expired" -> doc.status == DocumentStatus.EXPIRED
                    "pending" -> doc.status == DocumentStatus.PENDING
                    "required" -> doc.type in listOf(
                        DocumentType.DRIVING_LICENSE,
                        DocumentType.VEHICLE_REGISTRATION,
                        DocumentType.INSURANCE
                    )
                    else -> true
                }
            }.size) { index ->
                val document = documents.filter { doc ->
                    when(selectedCategory) {
                        "expired" -> doc.status == DocumentStatus.EXPIRED
                        "pending" -> doc.status == DocumentStatus.PENDING
                        "required" -> doc.type in listOf(
                            DocumentType.DRIVING_LICENSE,
                            DocumentType.VEHICLE_REGISTRATION,
                            DocumentType.INSURANCE
                        )
                        else -> true
                    }
                }[index]

                DocumentCard(
                    document = document,
                    onView = { onViewDocument(document) },
                    onUpdate = { onUpdateDocument(document) }
                )
            }

            // Missing Documents Section
            val missingDocuments = requiredDocuments.filter { type ->
                documents.none { it.type == type }
            }

            if (missingDocuments.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Required Documents",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                items(missingDocuments.size) { index ->
                    val docType = missingDocuments[index]
                    Card(
                        onClick = {
                            selectedDocumentType = docType
                            onUploadDocument(docType)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DaxidoLightGray)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(
                                    modifier = Modifier.padding(start = 12.dp)
                                ) {
                                    Text(
                                        text = getDocumentTypeName(docType),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Not uploaded",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            TextButton(onClick = { onUploadDocument(docType) }) {
                                Icon(
                                    imageVector = Icons.Default.Upload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Upload")
                            }
                        }
                    }
                }
            }
        }
    }

    // Upload Dialog
    if (showUploadDialog) {
        AlertDialog(
            onDismissRequest = { showUploadDialog = false },
            title = {
                Text("Upload Document", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Select document type to upload:")
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(requiredDocuments.size) { index ->
                            val docType = requiredDocuments[index]
                            val isUploaded = documents.any { it.type == docType }

                            Card(
                                onClick = {
                                    if (!isUploaded) {
                                        selectedDocumentType = docType
                                        showUploadDialog = false
                                        onUploadDocument(docType)
                                    }
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUploaded)
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = getDocumentTypeName(docType),
                                        color = if (isUploaded)
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isUploaded) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = StatusOnline,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUploadDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentCard(
    document: Document,
    onView: () -> Unit,
    onUpdate: () -> Unit
) {
    Card(
        onClick = onView,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        border = when(document.status) {
            DocumentStatus.EXPIRED -> BorderStroke(1.dp, Color.Red)
            DocumentStatus.REJECTED -> BorderStroke(1.dp, Color.Red)
            else -> null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                when(document.status) {
                                    DocumentStatus.VERIFIED -> StatusOnline.copy(alpha = 0.1f)
                                    DocumentStatus.PENDING -> DaxidoWarning.copy(alpha = 0.1f)
                                    DocumentStatus.EXPIRED -> Color.Red.copy(alpha = 0.1f)
                                    DocumentStatus.REJECTED -> Color.Red.copy(alpha = 0.1f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getDocumentIcon(document.type),
                            contentDescription = null,
                            tint = when(document.status) {
                                DocumentStatus.VERIFIED -> StatusOnline
                                DocumentStatus.PENDING -> DaxidoWarning
                                DocumentStatus.EXPIRED -> Color.Red
                                DocumentStatus.REJECTED -> Color.Red
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text(
                            text = document.name,
                            fontWeight = FontWeight.Medium
                        )
                        if (document.documentNumber != null) {
                            Text(
                                text = document.documentNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Uploaded ${document.uploadedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when(document.status) {
                        DocumentStatus.VERIFIED -> StatusOnline.copy(alpha = 0.1f)
                        DocumentStatus.PENDING -> DaxidoWarning.copy(alpha = 0.1f)
                        DocumentStatus.EXPIRED -> Color.Red.copy(alpha = 0.1f)
                        DocumentStatus.REJECTED -> Color.Red.copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = document.status.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when(document.status) {
                            DocumentStatus.VERIFIED -> StatusOnline
                            DocumentStatus.PENDING -> DaxidoWarning
                            DocumentStatus.EXPIRED -> Color.Red
                            DocumentStatus.REJECTED -> Color.Red
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Expiry Info
            if (document.expiryDate != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(
                    LocalDate.now(),
                    document.expiryDate
                )

                if (daysUntilExpiry <= 30) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (daysUntilExpiry < 0)
                                Color.Red.copy(alpha = 0.1f)
                            else DaxidoWarning.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (daysUntilExpiry < 0) Color.Red else DaxidoWarning,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (daysUntilExpiry < 0)
                                    " Expired ${-daysUntilExpiry} days ago"
                                else " Expires in $daysUntilExpiry days",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (daysUntilExpiry < 0) Color.Red else DaxidoWarning,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Verification Note
            if (document.verificationNote != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = document.verificationNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            // Action Button
            if (document.status == DocumentStatus.EXPIRED || document.status == DocumentStatus.REJECTED) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onUpdate,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (document.status == DocumentStatus.EXPIRED)
                            DaxidoWarning else Color.Red
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Update Document")
                }
            }
        }
    }
}

@Composable
fun StatCard(
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun getDocumentIcon(type: DocumentType): androidx.compose.ui.graphics.vector.ImageVector {
    return when(type) {
        DocumentType.DRIVING_LICENSE -> Icons.Default.Badge
        DocumentType.VEHICLE_REGISTRATION -> Icons.Default.DirectionsCar
        DocumentType.INSURANCE -> Icons.Default.Security
        DocumentType.POLLUTION_CERTIFICATE -> Icons.Default.Eco
        DocumentType.PERMIT -> Icons.Default.Article
        DocumentType.AADHAR_CARD -> Icons.Default.CreditCard
        DocumentType.PAN_CARD -> Icons.Default.AccountBox
        DocumentType.BANK_STATEMENT -> Icons.Default.AccountBalance
        DocumentType.VEHICLE_FITNESS -> Icons.Default.VerifiedUser
        DocumentType.PROFILE_PHOTO -> Icons.Default.Person
    }
}

fun getDocumentTypeName(type: DocumentType): String {
    return when(type) {
        DocumentType.DRIVING_LICENSE -> "Driving License"
        DocumentType.VEHICLE_REGISTRATION -> "Vehicle Registration"
        DocumentType.INSURANCE -> "Insurance"
        DocumentType.POLLUTION_CERTIFICATE -> "Pollution Certificate"
        DocumentType.PERMIT -> "Permit"
        DocumentType.AADHAR_CARD -> "Aadhar Card"
        DocumentType.PAN_CARD -> "PAN Card"
        DocumentType.BANK_STATEMENT -> "Bank Statement"
        DocumentType.VEHICLE_FITNESS -> "Vehicle Fitness"
        DocumentType.PROFILE_PHOTO -> "Profile Photo"
    }
}