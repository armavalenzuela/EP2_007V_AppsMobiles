package com.mpaznotifica.app.domain

import com.mpaznotifica.app.data.repository.ApoderadoRepository
import com.mpaznotifica.app.data.repository.ComunicacionRepository
import com.mpaznotifica.app.data.repository.DirectorioRepository
import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.TipoComunicacion
import com.mpaznotifica.app.model.Usuario
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Fecha y hora actuales en formato "AAAA-MM-DD HH:mm" (ordenable como texto, RF-11). */
fun fechaHoraActual(): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

/**
 * RF-05 de extremo a extremo: resuelve los destinatarios del alcance elegido (RF-04), emite,
 * y guarda el comunicado con sus envíos. El ViewModel solo llama a esta clase.
 *
 * ahora y nuevoId se inyectan para que las pruebas sean deterministas.
 */
class EnviarComunicadoUseCase(
    private val comunicaciones: ComunicacionRepository,
    private val apoderados: ApoderadoRepository,
    private val directorio: DirectorioRepository,
    private val ahora: () -> String = ::fechaHoraActual,
    private val nuevoId: () -> String = { "C-${System.currentTimeMillis()}" }
) {
    suspend operator fun invoke(
        usuario: Usuario?,
        asunto: String,
        cuerpo: String,
        tipo: TipoComunicacion,
        alcance: AlcanceEnvio,
        destino: String?,
        adjuntoPdfNombre: String?,
        requiereFirmaFisica: Boolean
    ): ResultadoEmision {
        val destinatarios = ResolverDestinatarios.resolver(
            alcance = alcance,
            destino = destino,
            apoderados = apoderados.observarApoderados().first(),
            estudiantes = directorio.observarEstudiantes().first(),
            talleres = directorio.observarTalleres().first()
        )

        val resultado = EmisionComunicado.emitir(
            usuario = usuario,
            idComunicacion = nuevoId(),
            asunto = asunto,
            cuerpo = cuerpo,
            tipo = tipo,
            alcance = alcance,
            destino = destino,
            destinatarios = destinatarios,
            fechaHora = ahora(),
            adjuntoPdfNombre = adjuntoPdfNombre,
            requiereFirmaFisica = requiereFirmaFisica
        )

        // RN-02: solo se guarda cuando la emisión fue válida.
        if (resultado is ResultadoEmision.Ok) comunicaciones.registrar(resultado.registro)
        return resultado
    }
}
