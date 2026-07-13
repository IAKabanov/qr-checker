package ru.kogtie.qr.reader.qr

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class QrScannerViewModel : ViewModel() {

    private val _scannedLink = MutableStateFlow<String?>(null)
    val scannedLink: StateFlow<String?> = _scannedLink.asStateFlow()

    fun onQrScanned(value: String) {
        _scannedLink.value = value
    }
}