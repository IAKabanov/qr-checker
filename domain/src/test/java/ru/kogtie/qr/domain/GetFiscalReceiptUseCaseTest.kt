package ru.kogtie.qr.domain

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetFiscalReceiptUseCaseTest {

    private val repository = object : FiscalReceiptRepository {
        override suspend fun getReceipt(receiptUrl: String): Result<FiscalReceipt> {
            return Result.success(FiscalReceipt("test", emptyList()))
        }
    }

    private val useCase = GetFiscalReceiptUseCase(repository)

    @Test
    fun `invoke should return success for valid host`() = runBlocking {
        val result = useCase("https://suf.purs.gov.rs/v/?vl=abc")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `invoke should return failure for invalid host`() = runBlocking {
        val result = useCase("https://google.com")
        assertTrue(result.isFailure)
        assertEquals("Invalid receipt URL or host", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke should return failure for http`() = runBlocking {
        val result = useCase("http://suf.purs.gov.rs/v/?vl=abc")
        assertTrue(result.isFailure)
    }
}
