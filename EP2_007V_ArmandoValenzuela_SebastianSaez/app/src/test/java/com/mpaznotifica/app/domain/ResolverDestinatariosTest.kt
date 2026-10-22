package com.mpaznotifica.app.domain

import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.CanalDespacho
import org.junit.Assert.assertEquals
import org.junit.Test

/** RF-04: los cuatro alcances resuelven a los apoderados correctos (datos de prueba del caso). */
class ResolverDestinatariosTest {

    private fun resolver(alcance: AlcanceEnvio, destino: String?, apoderados: List<Apoderado> = MockData.apoderados) =
        ResolverDestinatarios.resolver(alcance, destino, apoderados, MockData.estudiantes, MockData.talleres)
            .map { it.id }

    @Test
    fun colegioCompletoResuelveTodosLosApoderados() {
        assertEquals(listOf("AP1", "AP2", "AP3"), resolver(AlcanceEnvio.COLEGIO_COMPLETO, null))
    }

    @Test
    fun cursoResuelveLosApoderadosDeSusPupilos() {
        assertEquals(listOf("AP1", "AP2"), resolver(AlcanceEnvio.CURSO, "4° Básico A"))
    }

    @Test
    fun tallerRoboticaResuelveSoloAlApoderadoDeSuPupilo() {
        assertEquals(listOf("AP1"), resolver(AlcanceEnvio.TALLER, "Taller de Robótica"))
    }

    @Test
    fun tallerFutbolResuelveSoloAlApoderadoDeSuPupilo() {
        assertEquals(listOf("AP3"), resolver(AlcanceEnvio.TALLER, "Taller de Fútbol"))
    }

    @Test
    fun apoderadoIndividualResuelveSoloEseApoderado() {
        assertEquals(listOf("AP2"), resolver(AlcanceEnvio.APODERADO_INDIVIDUAL, "Apoderado Ficticio 2"))
    }

    @Test
    fun destinoInexistenteNoResuelveNadie() {
        assertEquals(emptyList<String>(), resolver(AlcanceEnvio.CURSO, "Curso que no existe"))
        assertEquals(emptyList<String>(), resolver(AlcanceEnvio.APODERADO_INDIVIDUAL, "Nadie"))
    }

    @Test
    fun apoderadoConDosPupilosEnElCursoAparecesUnaVez() {
        val conDosPupilos = Apoderado(
            "AP9", "Apoderado Ficticio 9", "apoderado9@ejemplo.cl", "+56900000009",
            CanalDespacho.EMAIL, listOf("E1", "E2")
        )
        assertEquals(listOf("AP9"), resolver(AlcanceEnvio.CURSO, "4° Básico A", listOf(conDosPupilos)))
    }
}
