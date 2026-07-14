package ru.kogtie.qr.data

import kotlinx.serialization.Serializable

@Serializable
data class SpecificationsResponseDto(
    val success: Boolean,
    val items: List<ReceiptItemDto>? = null
)

@Serializable
data class ReceiptItemDto(
    val gtin: String? = null,
    val name: String,
    val quantity: Double,
    val total: Double,
    val unitPrice: Double,
    val label: String,
    val labelRate: Double,
    val taxBaseAmount: Double,
    val vatAmount: Double
)
