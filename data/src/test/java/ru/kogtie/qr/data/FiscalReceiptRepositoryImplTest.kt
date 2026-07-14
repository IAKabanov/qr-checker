package ru.kogtie.qr.data

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType

class FiscalReceiptRepositoryImplTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: FiscalReceiptRepositoryImpl
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        val api = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FiscalReceiptApi::class.java)
        
        repository = FiscalReceiptRepositoryImpl(api, ReceiptHtmlParser())
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getReceipt should return success when api calls are successful`() = runBlocking {
        // 1. Mock HTML response
        val html = """
            <html><body>
            <div id="collapse3"><pre>RECEIPT CONTENT</pre></div>
            <script>
                viewModel.InvoiceNumber('INV123');
                viewModel.Token('TOK456');
            </script>
            </body></html>
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(html))

        // 2. Mock Specifications response
        val specJson = """
            {
              "success": true,
              "items": [
                {
                  "name": "Item 1",
                  "quantity": 1,
                  "total": 100,
                  "unitPrice": 100,
                  "label": "E",
                  "labelRate": 10,
                  "taxBaseAmount": 90,
                  "vatAmount": 10
                }
              ]
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(specJson))

        val url = mockWebServer.url("/v/?vl=test").toString()
        val result = repository.getReceipt(url)

        assertTrue(result.toString(), result.isSuccess)
        val receipt = result.getOrNull()!!
        assertEquals("RECEIPT CONTENT", receipt.receiptText)
        assertEquals(1, receipt.items.size)
        assertEquals("Item 1", receipt.items[0].name)
        
        // Verify POST request
        mockWebServer.takeRequest() // GET
        val postRequest = mockWebServer.takeRequest()
        assertEquals("/specifications", postRequest.path)
        assertEquals("POST", postRequest.method)
        val body = postRequest.body.readUtf8()
        assertTrue(body.contains("invoiceNumber=INV123"))
        assertTrue(body.contains("token=TOK456"))
    }

    @Test
    fun `getReceipt should return failure when specifications success is false`() = runBlocking {
        val html = """
            <html><body>
            <div id="collapse3"><pre>RECEIPT CONTENT</pre></div>
            <script>
                viewModel.InvoiceNumber('INV123');
                viewModel.Token('TOK456');
            </script>
            </body></html>
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(html))
        mockWebServer.enqueue(MockResponse().setBody("""{"success": false}"""))

        val url = mockWebServer.url("/v/?vl=test").toString()
        val result = repository.getReceipt(url)

        assertTrue(result.isFailure)
        assertEquals("Specifications success=false", result.exceptionOrNull()?.message)
    }
}
