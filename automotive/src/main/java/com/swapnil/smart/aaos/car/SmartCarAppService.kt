package com.swapnil.smart.aaos.car
import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.swapnil.smart.aaos.viewmodel.CarViewModelStore

class SmartCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }
    override fun onCreateSession(): Session {
        return SmartSession()
    }
    override fun onDestroy() {
        super.onDestroy()
        CarViewModelStore.clear() // ✅ clean up ViewModels
    }

}