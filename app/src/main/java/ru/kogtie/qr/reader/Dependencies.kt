package ru.kogtie.qr.reader

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import ru.kogtie.qr.data.FiscalReceiptApi
import ru.kogtie.qr.data.FiscalReceiptRepositoryImpl
import ru.kogtie.qr.data.ReceiptHtmlParser
import ru.kogtie.qr.domain.FiscalReceiptRepository
import ru.kogtie.qr.domain.GetFiscalReceiptUseCase

object Dependencies {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://suf.purs.gov.rs/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val api = retrofit.create(FiscalReceiptApi::class.java)
    
    private val parser = ReceiptHtmlParser()
    
    val repository: FiscalReceiptRepository = FiscalReceiptRepositoryImpl(api, parser)
    
    val getFiscalReceiptUseCase = GetFiscalReceiptUseCase(repository)
}
