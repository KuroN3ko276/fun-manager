package com.funmanager.domain

import com.funmanager.data.Transfer
import com.funmanager.data.Wallet
import com.funmanager.data.WalletNet

/** Số dư từng ví = số dư ban đầu + (thu − chi) + chuyển đến − chuyển đi. */
fun walletBalances(wallets: List<Wallet>, nets: List<WalletNet>, transfers: List<Transfer>): Map<Long, Long> {
    val netById = nets.associate { it.walletId to it.net }
    val transferIn = transfers.groupBy { it.toWalletId }.mapValues { (_, t) -> t.sumOf { it.amount } }
    val transferOut = transfers.groupBy { it.fromWalletId }.mapValues { (_, t) -> t.sumOf { it.amount } }
    return wallets.associate { w ->
        w.id to w.initialBalance + (netById[w.id] ?: 0) + (transferIn[w.id] ?: 0) - (transferOut[w.id] ?: 0)
    }
}

enum class TransferError { SAME_WALLET, INVALID_AMOUNT }

fun validateTransfer(fromId: Long?, toId: Long?, amount: Long?): TransferError? = when {
    fromId == null || toId == null || fromId == toId -> TransferError.SAME_WALLET
    amount == null || amount <= 0 -> TransferError.INVALID_AMOUNT
    else -> null
}
