package ru.kogtie.qr.data

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface FiscalReceiptApi {

    @GET
    suspend fun getReceiptPage(
        @Url url: String
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("specifications")
    @Headers(
        "Accept: application/json",
        "X-Requested-With: XMLHttpRequest"
    )
    suspend fun getSpecifications(
        @Field("invoiceNumber") invoiceNumber: String,
        @Field("token") token: String
    ): Response<SpecificationsResponseDto>
}
