package com.mpaznotifica.app.data.local

import androidx.room.withTransaction
import com.mpaznotifica.app.data.repository.ComunicacionRepository
import com.mpaznotifica.app.domain.EmisionRegistrada
import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.EnvioDestinatario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Implementación con Room. */
class RoomComunicacionRepository(private val db: AppDatabase) : ComunicacionRepository {

    private val dao = db.comunicacionDao()

    override fun observarComunicados(): Flow<List<Comunicacion>> =
        dao.observarComunicados().map { filas -> filas.map { it.toModel() } }

    override fun observarEnvios(): Flow<List<EnvioDestinatario>> =
        dao.observarEnvios().map { filas -> filas.map { it.toModel() } }

    override suspend fun registrar(registro: EmisionRegistrada) {
        // Comunicado y envíos en una sola transacción: no puede quedar uno sin el otro (RT-02).
        db.withTransaction {
            dao.insertarComunicado(registro.comunicacion.toEntity())
            dao.insertarEnvios(registro.envios.map { it.toEntity() })
        }
    }
}
