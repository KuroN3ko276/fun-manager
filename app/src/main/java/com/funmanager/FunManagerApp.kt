package com.funmanager

import android.app.Application

class FunManagerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Hẹn lại lời nhắc mỗi lần app khởi động, phòng khi báo thức bị hệ thống xoá (cập nhật app, buộc dừng…).
        container.reminders.schedule()
    }
}
