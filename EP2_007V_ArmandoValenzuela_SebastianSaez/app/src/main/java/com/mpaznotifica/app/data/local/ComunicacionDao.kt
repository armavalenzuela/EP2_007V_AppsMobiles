package com.mpaznotifica.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ComunicacionDao {

    @Query("SELECT * FROM comunicaciones ORDER BY fechaHora DESC")
    fun observarComunicados(): Flow<List<ComunicacionEntity>>

    @Query("SELECT * FROM envios")
    fun observarEnvios(): Flow<List<EnvioEntity>>

    // RN-02: comunicados y envíos son inmutables. Por eso no hay @Update ni @Delete.
    @Insert
    suspend fun insertarComunicado(comunicacion: ComunicacionEntity)

    @Insert
    suspend fun insertarEnvios(envios: List<EnvioEntity>)
}
