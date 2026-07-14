package ru.kogtie.qr.domain

interface FiscalReceiptRepository {
    suspend fun getReceipt(receiptUrl: String): Result<FiscalReceipt>
}
