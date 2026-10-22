package com.mpaznotifica.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.EnvioDestinatario
import com.mpaznotifica.app.model.EstadoEnvio

/**
 * Tabla envios. Índice único (comunicacionId, apoderadoId): RN-07, un solo envío por apoderado
 * y comunicado.
 */
@Entity(
    tableName = "envios",
    foreignKeys = [
        ForeignKey(
            entity = ComunicacionEntity::class,
            parentColumns = ["id"],
            childColumns = ["comunicacionId"]
        )
    ],
    indices = [Index(value = ["comunicacionId", "apoderadoId"], unique = true)]
)
data class EnvioEntity(
    @PrimaryKey val id: String,
    val comunicacionId: String,
    val apoderadoId: String,
    val canal: String,
    val estado: String
)

fun EnvioDestinatario.toEntity(): EnvioEntity = EnvioEntity(
    id = id,
    comunicacionId = comunicacionId,
    apoderadoId = apoderadoId,
    canal = canal.name,
    estado = estado.name
)

fun EnvioEntity.toModel(): EnvioDestinatario = EnvioDestinatario(
    id = id,
    comunicacionId = comunicacionId,
    apoderadoId = apoderadoId,
    canal = CanalDespacho.valueOf(canal),
    estado = EstadoEnvio.valueOf(estado)
)
