package com.daxido.user.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.daxido.core.theme.DaxidoGold
import com.daxido.core.theme.DaxidoMediumBrown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneNumberInput(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    countryCode: String,
    onCountryCodeChange: (String) -> Unit,
    isError: Boolean = false,
    errorMessage: String = ""
) {
    var showCountryCodeMenu by remember { mutableStateOf(false) }
    val countryCodes = listOf("+91", "+1", "+44", "+61", "+971", "+65", "+86")

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(0.3f)
                    .height(56.dp)
                    .border(
                        width = 1.dp,
                        color = if (isError) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                    )
                    .clickable { showCountryCodeMenu = true },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = countryCode,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showCountryCodeMenu,
                    onDismissRequest = { showCountryCodeMenu = false }
                ) {
                    countryCodes.forEach { code ->
                        DropdownMenuItem(
                            text = { Text(code) },
                            onClick = {
                                onCountryCodeChange(code)
                                showCountryCodeMenu = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { if (it.length <= 10 && it.all { char -> char.isDigit() }) onPhoneNumberChange(it) },
                modifier = Modifier.weight(0.7f),
                placeholder = {
                    Text("Phone Number")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = if (isError) MaterialTheme.colorScheme.error
                               else DaxidoMediumBrown
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                ),
                singleLine = true,
                shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DaxidoGold,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    errorBorderColor = MaterialTheme.colorScheme.error
                ),
                isError = isError
            )
        }

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}