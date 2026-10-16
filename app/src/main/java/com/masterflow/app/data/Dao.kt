package com.masterflow.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterFlowDao {
    @Query("SELECT * FROM appointments ORDER BY startAt ASC")
    fun appointments(): Flow<List<Appointment>>

    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC")
    fun transactions(): Flow<List<MoneyTransaction>>

    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun clients(): Flow<List<Client>>

    @Insert suspend fun addAppointment(item: Appointment): Long
    @Insert suspend fun addTransaction(item: MoneyTransaction): Long
    @Insert suspend fun addClient(item: Client): Long

    @Query("UPDATE appointments SET completed = 1 WHERE id = :id")
    suspend fun completeAppointment(id: Long)
}
