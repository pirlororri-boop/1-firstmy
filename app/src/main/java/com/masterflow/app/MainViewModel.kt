package com.masterflow.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.masterflow.app.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).dao()

    val appointments = dao.appointments().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val transactions = dao.transactions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val clients = dao.clients().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTransaction(type: String, category: String, amount: Double, client: String, note: String) {
        if (amount <= 0) return
        viewModelScope.launch {
            dao.addTransaction(MoneyTransaction(type = type, category = category, amountKopecks = (amount * 100).toLong(), occurredAt = System.currentTimeMillis(), clientName = client.trim(), note = note.trim()))
        }
    }

    fun addAppointment(name: String, phone: String, service: String, startAt: Long, duration: Int, price: Double) {
        if (name.isBlank() || phone.isBlank() || service.isBlank()) return
        viewModelScope.launch {
            dao.addAppointment(Appointment(clientName = name.trim(), phone = phone.trim(), service = service.trim(), startAt = startAt, durationMinutes = duration.coerceAtLeast(15), priceKopecks = (price.coerceAtLeast(0.0) * 100).toLong()))
            dao.addClient(Client(name = name.trim(), phone = phone.trim()))
        }
    }

    fun completeAppointment(item: Appointment) {
        viewModelScope.launch {
            dao.completeAppointment(item.id)
            if (item.priceKopecks > 0) {
                dao.addTransaction(MoneyTransaction(type = "INCOME", category = "Услуга", amountKopecks = item.priceKopecks, occurredAt = System.currentTimeMillis(), clientName = item.clientName, note = item.service))
            }
        }
    }
}
