package com.funmanager.domain

import com.funmanager.data.Transfer
import com.funmanager.data.Wallet
import com.funmanager.data.WalletNet
import java.time.LocalDate

/**
 * Số dư từng ví tính tới hết ngày [asOf] = số dư ban đầu + (thu − chi) + chuyển đến − chuyển đi.
 * Giao dịch / chuyển tiền ghi ngày sau [asOf] chưa được tính (xem [upcomingByWallet]).
 */
fun walletBalances(wallets: List<Wallet>, nets: List<WalletNet>, transfers: List<Transfer>, asOf: LocalDate): Map<Long, Long> {
    val netById = nets.associate { it.walletId to it.net }
    val transfers = transfers.filter { it.date <= asOf }
    val transferIn = transfers.groupBy { it.toWalletId }.mapValues { (_, t) -> t.sumOf { it.amount } }
    val transferOut = transfers.groupBy { it.fromWalletId }.mapValues { (_, t) -> t.sumOf { it.amount } }
    return wallets.associate { w ->
        w.id to w.initialBalance + (netById[w.id] ?: 0) + (transferIn[w.id] ?: 0) - (transferOut[w.id] ?: 0)
    }
}

/** Thu − chi đã ghi cho các ngày sau hôm nay của từng ví (bỏ qua ví bằng 0). */
fun upcomingByWallet(nets: List<WalletNet>): Map<Long, Long> =
    nets.filter { it.upcoming != 0L }.associate { it.walletId to it.upcoming }

enum class TransferError { SAME_WALLET, INVALID_AMOUNT }

fun validateTransfer(fromId: Long?, toId: Long?, amount: Long?): TransferError? = when {
    fromId == null || toId == null || fromId == toId -> TransferError.SAME_WALLET
    amount == null || amount <= 0 -> TransferError.INVALID_AMOUNT
    else -> null
}
