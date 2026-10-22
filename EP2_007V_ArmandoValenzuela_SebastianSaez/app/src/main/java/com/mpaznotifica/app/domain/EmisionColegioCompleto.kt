package com.mpaznotifica.app.domain

import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.EstadoEnvio
import com.mpaznotifica.app.model.TipoComunicacion
import com.mpaznotifica.app.model.Usuario

/**
 * Atajo para emitir a colegio completo (todos los apoderados). La regla real está en
 * EmisionComunicado; aquí solo se fija el alcance.
 */
object EmisionColegioCompleto {

    const val MSG_SIN_SESION = EmisionComunicado.MSG_SIN_SESION
    const val MSG_SIN_PERMISO = EmisionComunicado.MSG_SIN_PERMISO
    const val MSG_SIN_DESTINATARIOS = EmisionComunicado.MSG_SIN_DESTINATARIOS

    fun emitir(
        usuario: Usuario?,
        idComunicacion: String,
        asunto: String,
        cuerpo: String,
        tipo: TipoComunicacion,
        apoderados: List<Apoderado>,
        fechaHora: String,
        adjuntoPdfNombre: String? = null,
        requiereFirmaFisica: Boolean = false
    ): ResultadoEmision = EmisionComunicado.emitir(
        usuario = usuario,
        idComunicacion = idComunicacion,
        asunto = asunto,
        cuerpo = cuerpo,
        tipo = tipo,
        alcance = AlcanceEnvio.COLEGIO_COMPLETO,
        destino = null,
        destinatarios = apoderados,
        fechaHora = fechaHora,
        adjuntoPdfNombre = adjuntoPdfNombre,
        requiereFirmaFisica = requiereFirmaFisica
    )

    fun simularDespacho(apoderado: Apoderado): EstadoEnvio = EmisionComunicado.simularDespacho(apoderado)
}
