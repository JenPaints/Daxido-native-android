package com.daxido.user.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daxido.core.models.VehicleType
import com.daxido.core.theme.*

data class VehicleOption(
    val type: VehicleType,
    val name: String,
    val icon: ImageVector,
    val basePrice: Double,
    val pricePerKm: Double,
    val estimatedTime: Int,
    val capacity: String,
    val color: Color
)

@Composable
fun VehicleTypeSelector(
    selectedType: VehicleType,
    onTypeSelected: (VehicleType) -> Unit,
    estimatedFare: Double,
    estimatedTime: Int,
    modifier: Modifier = Modifier
) {
    val vehicleOptions = listOf(
        VehicleOption(
            type = VehicleType.BIKE,
            name = "Bike",
            icon = Icons.Default.TwoWheeler,
            basePrice = 25.0,
            pricePerKm = 8.0,
            estimatedTime = 12,
            capacity = "1",
            color = BikeColor
        ),
        VehicleOption(
            type = VehicleType.AUTO,
            name = "Auto",
            icon = Icons.Default.ElectricRickshaw,
            basePrice = 40.0,
            pricePerKm = 12.0,
            estimatedTime = 18,
            capacity = "3",
            color = AutoColor
        ),
        VehicleOption(
            type = VehicleType.CAR,
            name = "Car",
            icon = Icons.Default.DirectionsCar,
            basePrice = 60.0,
            pricePerKm = 15.0,
            estimatedTime = 15,
            capacity = "4",
            color = CarColor
        ),
        VehicleOption(
            type = VehicleType.PREMIUM,
            name = "Premium",
            icon = Icons.Default.CarRental,
            basePrice = 100.0,
            pricePerKm = 25.0,
            estimatedTime = 15,
            capacity = "4",
            color = PremiumColor
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Choose your ride",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(vehicleOptions) { option ->
                VehicleCard(
                    option = option,
                    isSelected = selectedType == option.type,
                    onClick = { onTypeSelected(option.type) }
                )
            }
        }

        selectedType?.let { type ->
            val selectedOption = vehicleOptions.find { it.type == type }
            selectedOption?.let { option ->
                Divider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Estimated Fare",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₹${option.basePrice + (option.pricePerKm * 5)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = DaxidoGold
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "ETA",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${option.estimatedTime} min",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleCard(
    option: VehicleOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) DaxidoGold else Color.Transparent
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) DaxidoPaleGold else DaxidoLightGray
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 0.dp
    )

    Card(
        modifier = Modifier
            .width(100.dp)
            .clickable { onClick() }
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = option.name,
                tint = option.color,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = option.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )

            Text(
                text = "₹${option.basePrice}+",
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) DaxidoGold else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = option.capacity,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}