package com.funmanager.domain

import com.funmanager.data.Transfer
import com.funmanager.data.Wallet
import com.funmanager.data.WalletNet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class WalletsTest {
    private val cash = Wallet(1, "Tiền mặt", "cash", 0, initialBalance = 500_000)
    private val bank = Wallet(2, "Ngân hàng", "bank", 0, initialBalance = 10_000_000)
    private val card = Wallet(3, "Thẻ tín dụng", "card", 0, initialBalance = 0)
    private val day = LocalDate.of(2026, 11, 25)

    @Test
    fun balancesIncludeInitialNetAndTransfers() {
        val balances = walletBalances(
            listOf(cash, bank, card),
            listOf(WalletNet(1, -200_000), WalletNet(2, 15_000_000), WalletNet(3, -1_500_000)),
            listOf(Transfer(1, fromWalletId = 2, toWalletId = 1, amount = 2_000_000, date = day)),
            asOf = day,
        )
        assertEquals(2_300_000L, balances[1])
        assertEquals(23_000_000L, balances[2])
        assertEquals(-1_500_000L, balances[3])
    }

    @Test
    fun transfersDoNotChangeTotal() {
        val nets = listOf(WalletNet(1, 100), WalletNet(2, 200))
        val before = walletBalances(listOf(cash, bank), nets, emptyList(), day).values.sum()
        val after = walletBalances(
            listOf(cash, bank),
            nets,
            listOf(Transfer(1, 1, 2, 300_000, day), Transfer(2, 2, 1, 50_000, day)),
            day,
        ).values.sum()
        assertEquals(before, after)
    }

    @Test
    fun futureDatedEntriesAreNotInBalanceYet() {
        // Lương 15tr ghi ngày 10/12 (net tương lai), chuyển 1tr ghi ngày mai.
        val nets = listOf(WalletNet(2, net = 0, upcoming = 15_000_000))
        val transfers = listOf(Transfer(1, 2, 1, 1_000_000, day.plusDays(1)))
        val balances = walletBalances(listOf(cash, bank), nets, transfers, asOf = day)
        assertEquals(500_000L, balances[1])
        assertEquals(10_000_000L, balances[2])
        assertEquals(mapOf(2L to 15_000_000L), upcomingByWallet(nets))
        // Đến ngày mai thì khoản chuyển được tính.
        assertEquals(1_500_000L, walletBalances(listOf(cash, bank), nets, transfers, asOf = day.plusDays(1))[1])
    }

    @Test
    fun validatesTransfer() {
        assertNull(validateTransfer(1, 2, 1000))
        assertEquals(TransferError.SAME_WALLET, validateTransfer(1, 1, 1000))
        assertEquals(TransferError.SAME_WALLET, validateTransfer(null, 2, 1000))
        assertEquals(TransferError.INVALID_AMOUNT, validateTransfer(1, 2, 0))
        assertEquals(TransferError.INVALID_AMOUNT, validateTransfer(1, 2, null))
    }
}
