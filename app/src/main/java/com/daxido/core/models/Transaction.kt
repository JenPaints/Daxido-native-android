package com.daxido.core.models

data class Transaction(
    val id: String,
    val type: TransactionType,
    val amount: Double,
    val description: String,
    val date: Long,
    val status: TransactionStatus,
    val referenceId: String? = null
)
