package ru.kogtie.qr.data

import ru.kogtie.qr.domain.FiscalReceipt
import ru.kogtie.qr.domain.FiscalReceiptRepository
import ru.kogtie.qr.domain.ReceiptItem

class FiscalReceiptRepositoryImpl(
    private val api: FiscalReceiptApi,
    private val parser: ReceiptHtmlParser
) : FiscalReceiptRepository {

    override suspend fun getReceipt(receiptUrl: String): Result<FiscalReceipt> {
        return try {
            val htmlResponse = api.getReceiptPage(receiptUrl)
            if (!htmlResponse.isSuccessful) {
                return Result.failure(Exception("HTTP error: ${htmlResponse.code()}"))
            }
            val html = htmlResponse.body()?.string() ?: return Result.failure(Exception("Empty HTML response"))

            val parsed = parser.parse(html)
            
            val specResponse = api.getSpecifications(
                invoiceNumber = parsed.credentials.invoiceNumber,
                token = parsed.credentials.token
            )

            if (!specResponse.isSuccessful) {
                return Result.failure(Exception("Specifications request failed: ${specResponse.code()}"))
            }

            val specDto = specResponse.body() ?: return Result.failure(Exception("Empty specifications response"))
            
            if (!specDto.success) {
                return Result.failure(Exception("Specifications success=false"))
            }

            val items = specDto.items?.map { it.toDomain() } ?: emptyList()
            
            Result.success(
                FiscalReceipt(
                    receiptText = parsed.receiptText,
                    items = items
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun ReceiptItemDto.toDomain() = ReceiptItem(
        gtin = gtin,
        name = name,
        quantity = quantity,
        unitPrice = unitPrice,
        total = total,
        taxLabel = label,
        taxRate = labelRate,
        taxBaseAmount = taxBaseAmount,
        vatAmount = vatAmount
    )
}
