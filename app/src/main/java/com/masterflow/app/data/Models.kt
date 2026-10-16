package com.masterflow.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val notes: String = ""
)

@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientName: String,
    val phone: String,
    val service: String,
    val startAt: Long,
    val durationMinutes: Int,
    val priceKopecks: Long,
    val completed: Boolean = false
)

@Entity(tableName = "transactions")
data class MoneyTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val category: String,
    val amountKopecks: Long,
    val occurredAt: Long,
    val clientName: String = "",
    val note: String = ""
)
