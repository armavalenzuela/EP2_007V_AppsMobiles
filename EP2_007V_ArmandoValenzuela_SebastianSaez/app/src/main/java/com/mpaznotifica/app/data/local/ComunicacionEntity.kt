package com.mpaznotifica.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.TipoComunicacion

/** Tabla comunicaciones. Los enums se guardan como texto (enum.name). */
@Entity(tableName = "comunicaciones")
data class ComunicacionEntity(
    @PrimaryKey val id: String,
    val asunto: String,
    val cuerpo: String,
    val tipo: String,
    val alcance: String,
    val destinoReferencia: String?,
    val adjuntoPdfNombre: String?,
    val requiereFirmaFisica: Boolean,
    val autorId: String,
    val autorNombre: String,
    val fechaHora: String
)

fun Comunicacion.toEntity(): ComunicacionEntity = ComunicacionEntity(
    id = id,
    asunto = asunto,
    cuerpo = cuerpo,
    tipo = tipo.name,
    alcance = alcance.name,
    destinoReferencia = destinoReferencia,
    adjuntoPdfNombre = adjuntoPdfNombre,
    requiereFirmaFisica = requiereFirmaFisica,
    autorId = autorId,
    autorNombre = autorNombre,
    fechaHora = fechaHora
)

fun ComunicacionEntity.toModel(): Comunicacion = Comunicacion(
    id = id,
    asunto = asunto,
    cuerpo = cuerpo,
    tipo = TipoComunicacion.valueOf(tipo),
    alcance = AlcanceEnvio.valueOf(alcance),
    destinoReferencia = destinoReferencia,
    adjuntoPdfNombre = adjuntoPdfNombre,
    requiereFirmaFisica = requiereFirmaFisica,
    autorId = autorId,
    autorNombre = autorNombre,
    fechaHora = fechaHora
)
