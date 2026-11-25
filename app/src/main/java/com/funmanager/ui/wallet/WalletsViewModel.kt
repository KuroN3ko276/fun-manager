package com.funmanager.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.funmanager.data.FinanceRepository
import com.funmanager.data.Transfer
import com.funmanager.data.Wallet
import com.funmanager.domain.TransferError
import com.funmanager.domain.groupDigits
import com.funmanager.domain.parseVnd
import com.funmanager.domain.validateTransfer
import com.funmanager.domain.walletBalances
import com.funmanager.ui.components.CATEGORY_COLORS
import com.funmanager.ui.components.formatAmountInput
import com.funmanager.ui.container
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs

data class WalletWithBalance(val wallet: Wallet, val balance: Long)

data class WalletEditor(
    val original: Wallet?,
    val name: String,
    val icon: String,
    val color: Long,
    val initialText: String,
    /** Số dư ban đầu là khoản nợ (ví dụ thẻ tín dụng). */
    val initialNegative: Boolean,
    val nameError: Boolean = false,
    val canDelete: Boolean = false,
)

data class WalletDeleteRequest(
    val wallet: Wallet,
    val usage: Int,
    val replacements: List<Wallet>,
    val replacementId: Long,
)

data class TransferDialog(
    val fromId: Long?,
    val toId: Long?,
    val amountText: String = "",
    val note: String = "",
    val error: TransferError? = null,
)

data class TransferItem(val transfer: Transfer, val from: Wallet?, val to: Wallet?)

data class WalletsUiState(
    val wallets: List<WalletWithBalance> = emptyList(),
    val transfers: List<TransferItem> = emptyList(),
    val editor: WalletEditor? = null,
    val deleteRequest: WalletDeleteRequest? = null,
    val transferDialog: TransferDialog? = null,
    val pendingTransferDelete: TransferItem? = null,
) {
    val total: Long get() = wallets.sumOf { it.balance }
}

private data class Ui(
    val editor: WalletEditor? = null,
    val deleteRequest: WalletDeleteRequest? = null,
    val transferDialog: TransferDialog? = null,
    val pendingTransferDelete: TransferItem? = null,
)

class WalletsViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val ui = MutableStateFlow(Ui())

    val state: StateFlow<WalletsUiState> = combine(
        ui,
        repository.wallets,
        repository.walletNets,
        repository.transfers,
    ) { ui, wallets, nets, transfers ->
        val balances = walletBalances(wallets, nets, transfers)
        val byId = wallets.associateBy { it.id }
        WalletsUiState(
            wallets = wallets.map { WalletWithBalance(it, balances[it.id] ?: 0) },
            transfers = transfers.map { TransferItem(it, byId[it.fromWalletId], byId[it.toWalletId]) },
            editor = ui.editor,
            deleteRequest = ui.deleteRequest,
            transferDialog = ui.transferDialog,
            pendingTransferDelete = ui.pendingTransferDelete,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WalletsUiState())

    // --- Sửa ví ---

    fun startCreate() = ui.update {
        it.copy(editor = WalletEditor(null, "", "bank", CATEGORY_COLORS[5], "", initialNegative = false))
    }

    fun startEdit(wallet: Wallet) = ui.update {
        it.copy(
            editor = WalletEditor(
                original = wallet,
                name = wallet.name,
                icon = wallet.icon,
                color = wallet.color,
                initialText = if (wallet.initialBalance == 0L) "" else groupDigits(abs(wallet.initialBalance).toString()),
                initialNegative = wallet.initialBalance < 0,
                canDelete = state.value.wallets.size > 1,
            ),
        )
    }

    fun onEditorChange(transform: (WalletEditor) -> WalletEditor) =
        ui.update { s -> s.copy(editor = s.editor?.let(transform)?.copy(nameError = false)) }

    fun onInitialChange(text: String) = onEditorChange { it.copy(initialText = formatAmountInput(text)) }

    fun dismissEditor() = ui.update { it.copy(editor = null) }

    fun saveEditor() {
        val e = ui.value.editor ?: return
        if (e.name.isBlank()) {
            ui.update { it.copy(editor = e.copy(nameError = true)) }
            return
        }
        val initial = (parseVnd(e.initialText) ?: 0).let { if (e.initialNegative) -it else it }
        viewModelScope.launch {
            repository.saveWallet(
                Wallet(id = e.original?.id ?: 0, name = e.name.trim(), icon = e.icon, color = e.color, initialBalance = initial),
            )
            ui.update { it.copy(editor = null) }
        }
    }

    // --- Xoá ví (gộp vào ví khác) ---

    fun requestDelete() {
        val wallet = ui.value.editor?.original ?: return
        viewModelScope.launch {
            val replacements = state.value.wallets.map { it.wallet }.filter { it.id != wallet.id }
            val usage = repository.walletUsage(wallet.id)
            ui.update {
                it.copy(
                    editor = null,
                    deleteRequest = WalletDeleteRequest(wallet, usage, replacements, replacements.first().id),
                )
            }
        }
    }

    fun onReplacementChange(id: Long) = ui.update { s -> s.copy(deleteRequest = s.deleteRequest?.copy(replacementId = id)) }

    fun dismissDelete() = ui.update { it.copy(deleteRequest = null) }

    fun confirmDelete() {
        val r = ui.value.deleteRequest ?: return
        viewModelScope.launch {
            repository.deleteWallet(r.wallet, r.replacementId)
            ui.update { it.copy(deleteRequest = null) }
        }
    }

    // --- Chuyển tiền ---

    fun startTransfer() {
        val wallets = state.value.wallets.map { it.wallet }
        ui.update { it.copy(transferDialog = TransferDialog(wallets.getOrNull(0)?.id, wallets.getOrNull(1)?.id)) }
    }

    fun onTransferChange(transform: (TransferDialog) -> TransferDialog) =
        ui.update { s -> s.copy(transferDialog = s.transferDialog?.let(transform)?.copy(error = null)) }

    fun onTransferAmount(text: String) = onTransferChange { it.copy(amountText = formatAmountInput(text)) }

    fun dismissTransfer() = ui.update { it.copy(transferDialog = null) }

    fun confirmTransfer() {
        val d = ui.value.transferDialog ?: return
        val amount = parseVnd(d.amountText)
        val error = validateTransfer(d.fromId, d.toId, amount)
        if (error != null || d.fromId == null || d.toId == null || amount == null) {
            ui.update { it.copy(transferDialog = d.copy(error = error)) }
            return
        }
        viewModelScope.launch {
            repository.addTransfer(Transfer(fromWalletId = d.fromId, toWalletId = d.toId, amount = amount, date = LocalDate.now(), note = d.note.trim()))
            ui.update { it.copy(transferDialog = null) }
        }
    }

    fun requestDeleteTransfer(item: TransferItem) = ui.update { it.copy(pendingTransferDelete = item) }

    fun dismissDeleteTransfer() = ui.update { it.copy(pendingTransferDelete = null) }

    fun confirmDeleteTransfer() {
        val item = ui.value.pendingTransferDelete ?: return
        viewModelScope.launch {
            repository.deleteTransfer(item.transfer.id)
            ui.update { it.copy(pendingTransferDelete = null) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { WalletsViewModel(container.repository) }
        }
    }
}
