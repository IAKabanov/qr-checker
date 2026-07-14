package ru.kogtie.qr.domain

data class FiscalReceipt(
    val receiptText: String,
    val items: List<ReceiptItem>
)

data class ReceiptItem(
    val gtin: String?,
    val name: String,
    val quantity: Double,
    val unitPrice: Double,
    val total: Double,
    val taxLabel: String,
    val taxRate: Double,
    val taxBaseAmount: Double,
    val vatAmount: Double
)
