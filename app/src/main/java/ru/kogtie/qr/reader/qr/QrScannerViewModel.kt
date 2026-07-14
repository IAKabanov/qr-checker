package ru.kogtie.qr.reader.qr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.kogtie.qr.domain.FiscalReceipt
import ru.kogtie.qr.domain.GetFiscalReceiptUseCase
import ru.kogtie.qr.reader.Dependencies

sealed class QrScannerUiState {
    object Idle : QrScannerUiState()
    object Loading : QrScannerUiState()
    data class Success(val receipt: FiscalReceipt) : QrScannerUiState()
    data class Error(val message: String) : QrScannerUiState()
}

class QrScannerViewModel(
    private val getFiscalReceiptUseCase: GetFiscalReceiptUseCase = Dependencies.getFiscalReceiptUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<QrScannerUiState>(QrScannerUiState.Idle)
    val uiState: StateFlow<QrScannerUiState> = _uiState.asStateFlow()

    private val _scannedLink = MutableStateFlow<String?>(null)
    val scannedLink: StateFlow<String?> = _scannedLink.asStateFlow()

    fun onQrScanned(value: String) {
        if (_scannedLink.value == value) return
        _scannedLink.value = value
        fetchReceipt(value)
    }

    private fun fetchReceipt(url: String) {
        viewModelScope.launch {
            _uiState.value = QrScannerUiState.Loading
            getFiscalReceiptUseCase(url)
                .onSuccess { receipt ->
                    _uiState.value = QrScannerUiState.Success(receipt)
                }
                .onFailure { error ->
                    _uiState.value = QrScannerUiState.Error(error.message ?: "Unknown error")
                }
        }
    }
    
    fun reset() {
        _scannedLink.value = null
        _uiState.value = QrScannerUiState.Idle
    }
}
