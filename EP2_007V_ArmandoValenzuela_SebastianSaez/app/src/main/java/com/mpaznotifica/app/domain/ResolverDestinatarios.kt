package com.mpaznotifica.app.domain

import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.Estudiante
import com.mpaznotifica.app.model.Taller

/**
 * RF-04 · Convierte un alcance y su destino en la lista de apoderados destinatarios.
 *
 *  - COLEGIO_COMPLETO: todos los apoderados.
 *  - CURSO: apoderados con al menos un pupilo en ese curso.
 *  - TALLER: apoderados con al menos un pupilo inscrito en ese taller.
 *  - APODERADO_INDIVIDUAL: solo el apoderado cuyo nombre coincide con el destino.
 *
 * Cada apoderado aparece una sola vez (RN-07). Pura: no depende de Android.
 */
object ResolverDestinatarios {

    fun resolver(
        alcance: AlcanceEnvio,
        destino: String?,
        apoderados: List<Apoderado>,
        estudiantes: List<Estudiante>,
        talleres: List<Taller>
    ): List<Apoderado> {
        val resueltos = when (alcance) {
            AlcanceEnvio.COLEGIO_COMPLETO -> apoderados

            AlcanceEnvio.CURSO -> {
                val pupilos = estudiantes.filter { it.curso == destino }.map { it.id }.toSet()
                apoderados.filter { apoderado -> apoderado.pupilosIds.any { it in pupilos } }
            }

            AlcanceEnvio.TALLER -> {
                val tallerId = talleres.firstOrNull { it.nombre == destino }?.id
                val pupilos = if (tallerId == null) {
                    emptySet()
                } else {
                    estudiantes.filter { tallerId in it.talleresIds }.map { it.id }.toSet()
                }
                apoderados.filter { apoderado -> apoderado.pupilosIds.any { it in pupilos } }
            }

            AlcanceEnvio.APODERADO_INDIVIDUAL -> apoderados.filter { it.nombreCompleto == destino }
        }
        return resueltos.distinctBy { it.id }
    }
}
