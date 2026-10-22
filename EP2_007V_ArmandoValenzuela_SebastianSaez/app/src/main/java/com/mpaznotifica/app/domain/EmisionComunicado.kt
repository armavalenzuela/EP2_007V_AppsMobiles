package com.mpaznotifica.app.domain

import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.EnvioDestinatario
import com.mpaznotifica.app.model.EstadoEnvio
import com.mpaznotifica.app.model.Perfil
import com.mpaznotifica.app.model.TipoComunicacion
import com.mpaznotifica.app.model.Usuario

/** Comunicado emitido y sus envíos por destinatario, listos para guardar en el repositorio. */
data class EmisionRegistrada(
    val comunicacion: Comunicacion,
    val envios: List<EnvioDestinatario>
)

/** Resultado de intentar emitir: éxito con el registro, o error con el mensaje para el usuario. */
sealed class ResultadoEmision {
    data class Ok(val registro: EmisionRegistrada) : ResultadoEmision()
    data class Error(val mensaje: String) : ResultadoEmision()
}

/**
 * RF-05 · Inspectoría General emite a cualquiera de los cuatro alcances.
 *
 * Lógica pura, sin Android. Recibe los destinatarios ya resueltos (ver ResolverDestinatarios).
 * Reglas aplicadas:
 *  - RN-06: autor y fecha se toman del usuario activo y de la fecha recibida.
 *  - RN-07: un apoderado recibe el comunicado una sola vez.
 *  - RN-05: cada envío usa el canal preferente del apoderado.
 *  - RF-07: simulación del despacho (FALLIDO si falta el dato que requiere el canal).
 *
 * El Profesor/a Jefe (RF-06) todavía no tiene reglas propias: por ahora solo puede emitir Inspectoría.
 */
object EmisionComunicado {

    const val MSG_SIN_SESION = "Debes iniciar sesión para enviar comunicados."
    const val MSG_SIN_PERMISO = "Solo Inspectoría General puede emitir este comunicado."
    const val MSG_SIN_DESTINATARIOS = "No hay destinatarios para este alcance"

    fun emitir(
        usuario: Usuario?,
        idComunicacion: String,
        asunto: String,
        cuerpo: String,
        tipo: TipoComunicacion,
        alcance: AlcanceEnvio,
        destino: String?,
        destinatarios: List<Apoderado>,
        fechaHora: String,
        adjuntoPdfNombre: String? = null,
        requiereFirmaFisica: Boolean = false
    ): ResultadoEmision {
        if (usuario == null) return ResultadoEmision.Error(MSG_SIN_SESION)
        if (usuario.perfil != Perfil.INSPECTORIA_GENERAL) return ResultadoEmision.Error(MSG_SIN_PERMISO)

        val unicos = destinatarios.distinctBy { it.id }
        if (unicos.isEmpty()) return ResultadoEmision.Error(MSG_SIN_DESTINATARIOS)

        val comunicacion = Comunicacion(
            id = idComunicacion,
            asunto = asunto,
            cuerpo = cuerpo,
            tipo = tipo,
            alcance = alcance,
            destinoReferencia = if (alcance == AlcanceEnvio.COLEGIO_COMPLETO) null else destino,
            adjuntoPdfNombre = adjuntoPdfNombre,
            requiereFirmaFisica = requiereFirmaFisica,
            autorId = usuario.id,
            autorNombre = usuario.nombre,
            fechaHora = fechaHora
        )

        val envios = unicos.map { apoderado ->
            EnvioDestinatario(
                id = "$idComunicacion-${apoderado.id}",
                comunicacionId = idComunicacion,
                apoderadoId = apoderado.id,
                canal = apoderado.canalPreferente,
                estado = simularDespacho(apoderado)
            )
        }

        return ResultadoEmision.Ok(EmisionRegistrada(comunicacion, envios))
    }

    /** RF-07: FALLIDO si el apoderado no tiene el dato que exige su canal; DESPACHADO en otro caso. */
    fun simularDespacho(apoderado: Apoderado): EstadoEnvio {
        val tieneDato = when (apoderado.canalPreferente) {
            CanalDespacho.EMAIL -> apoderado.correo.isNotBlank()
            CanalDespacho.SMS, CanalDespacho.WHATSAPP -> apoderado.telefono.isNotBlank()
        }
        return if (tieneDato) EstadoEnvio.DESPACHADO else EstadoEnvio.FALLIDO
    }
}
