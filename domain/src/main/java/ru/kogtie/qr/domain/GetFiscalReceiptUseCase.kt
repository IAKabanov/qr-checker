package ru.kogtie.qr.domain

class GetFiscalReceiptUseCase(
    private val repository: FiscalReceiptRepository
) {
    suspend operator fun invoke(receiptUrl: String): Result<FiscalReceipt> {
        if (!receiptUrl.startsWith("https://suf.purs.gov.rs/")) {
            return Result.failure(Exception("Invalid receipt URL or host"))
        }
        return repository.getReceipt(receiptUrl)
    }
}
