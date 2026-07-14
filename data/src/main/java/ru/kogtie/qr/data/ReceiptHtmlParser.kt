package ru.kogtie.qr.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

data class ReceiptRequestCredentials(
    val invoiceNumber: String,
    val token: String
)

data class ParsedReceiptHtml(
    val receiptText: String,
    val credentials: ReceiptRequestCredentials
)

class ReceiptHtmlParser {

    fun parse(html: String): ParsedReceiptHtml {
        val doc = Jsoup.parse(html)
        
        val receiptText = extractReceiptText(doc)
        val credentials = extractCredentials(html)
        
        return ParsedReceiptHtml(receiptText, credentials)
    }

    private fun extractReceiptText(doc: Document): String {
        val preElement = doc.select("#collapse3 pre").firstOrNull()
            ?: doc.select("pre:contains(ФИСКАЛНИ РАЧУН)").firstOrNull()
            ?: doc.select("pre:contains(FISKALNI RAČUN)").firstOrNull()
            ?: throw Exception("Receipt text not found")

        // Create a copy to not modify the original doc
        val clonedPre = preElement.clone()
        
        // Remove img tags
        clonedPre.select("img").remove()

        // Replace <br> tags with \n before getting text
        // Jsoup's .text() collapses whitespace, so we need a custom approach
        return getCleanText(clonedPre)
    }

    private fun getCleanText(element: Element): String {
        val sb = StringBuilder()
        
        fun traverse(node: org.jsoup.nodes.Node) {
            when (node) {
                is TextNode -> {
                    sb.append(node.wholeText)
                }
                is Element -> {
                    if (node.tagName() == "br") {
                        sb.append("\n")
                    } else {
                        for (child in node.childNodes()) {
                            traverse(child)
                        }
                    }
                }
            }
        }
        
        for (child in element.childNodes()) {
            traverse(child)
        }

        return sb.toString()
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace("\u00A0", " ") // non-breaking space
            .trim { it == '\n' || it.isWhitespace() && it != ' ' && it != '\n' } 
            // The requirement says: "remove only leading/trailing empty lines. do not trim every line"
            // So I should trim leading/trailing newlines.
            .let { trimEmptyLines(it) }
    }

    private fun trimEmptyLines(text: String): String {
        val lines = text.split("\n")
        var start = 0
        while (start < lines.size && lines[start].isBlank()) {
            start++
        }
        var end = lines.size - 1
        while (end >= start && lines[end].isBlank()) {
            end--
        }
        if (start > end) return ""
        return lines.subList(start, end + 1).joinToString("\n")
    }

    private fun extractCredentials(html: String): ReceiptRequestCredentials {
        val invoiceNumberRegex = """viewModel\.InvoiceNumber\('([^']+)'\)""".toRegex()
        val tokenRegex = """viewModel\.Token\('([^']+)'\)""".toRegex()

        val invoiceNumber = invoiceNumberRegex.find(html)?.groupValues?.get(1)
            ?: throw Exception("Invoice number not found")
        val token = tokenRegex.find(html)?.groupValues?.get(1)
            ?: throw Exception("Token not found")

        return ReceiptRequestCredentials(invoiceNumber, token)
    }
}
