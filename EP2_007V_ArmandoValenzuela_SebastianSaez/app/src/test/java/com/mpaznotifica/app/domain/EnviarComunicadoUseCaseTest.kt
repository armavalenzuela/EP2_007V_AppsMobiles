package com.mpaznotifica.app.domain

import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.data.repository.MemoryApoderadoRepository
import com.mpaznotifica.app.data.repository.MemoryComunicacionRepository
import com.mpaznotifica.app.data.repository.MemoryDirectorioRepository
import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.Perfil
import com.mpaznotifica.app.model.TipoComunicacion
import com.mpaznotifica.app.model.Usuario
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** RF-05 de extremo a extremo para los cuatro alcances, con repositorios en memoria. */
class EnviarComunicadoUseCaseTest {

    private val inspectoria: Usuario = MockData.usuarios.first { it.perfil == Perfil.INSPECTORIA_GENERAL }
    private val profesor: Usuario = MockData.usuarios.first { it.perfil == Perfil.PROFESOR_JEFE }

    private fun casoDeUso(repo: MemoryComunicacionRepository) = EnviarComunicadoUseCase(
        comunicaciones = repo,
        apoderados = MemoryApoderadoRepository(),
        directorio = MemoryDirectorioRepository(),
        ahora = { "2026-10-20 12:00" },
        nuevoId = { "CT" }
    )

    private fun enviar(
        useCase: EnviarComunicadoUseCase,
        usuario: Usuario?,
        alcance: AlcanceEnvio,
        destino: String?,
        adjunto: String? = null
    ): ResultadoEmision = runBlocking {
        useCase(usuario, "Aviso", "Mensaje", TipoComunicacion.INSTITUCIONAL, alcance, destino, adjunto, false)
    }

    private fun apoderadosEnviados(repo: MemoryComunicacionRepository): Set<String> = runBlocking {
        repo.observarEnvios().first().filter { it.comunicacionId == "CT" }.map { it.apoderadoId }.toSet()
    }

    @Test
    fun colegioCompletoRegistraTresEnvios() {
        val repo = MemoryComunicacionRepository()
        val resultado = enviar(casoDeUso(repo), inspectoria, AlcanceEnvio.COLEGIO_COMPLETO, null)

        assertTrue(resultado is ResultadoEmision.Ok)
        assertEquals(setOf("AP1", "AP2", "AP3"), apoderadosEnviados(repo))
    }

    @Test
    fun cursoRegistraSoloLosApoderadosDeEseCurso() {
        val repo = MemoryComunicacionRepository()
        enviar(casoDeUso(repo), inspectoria, AlcanceEnvio.CURSO, "4° Básico A")

        assertEquals(setOf("AP1", "AP2"), apoderadosEnviados(repo))
    }

    @Test
    fun tallerRegistraSoloElApoderadoDelTaller() {
        val repo = MemoryComunicacionRepository()
        enviar(casoDeUso(repo), inspectoria, AlcanceEnvio.TALLER, "Taller de Robótica")

        assertEquals(setOf("AP1"), apoderadosEnviados(repo))
    }

    @Test
    fun apoderadoIndividualRegistraSoloEseApoderado() {
        val repo = MemoryComunicacionRepository()
        val resultado = enviar(casoDeUso(repo), inspectoria, AlcanceEnvio.APODERADO_INDIVIDUAL, "Apoderado Ficticio 3")

        assertTrue(resultado is ResultadoEmision.Ok)
        assertEquals(setOf("AP3"), apoderadosEnviados(repo))
    }

    @Test
    fun destinoSinDestinatariosNoRegistraNada() {
        val repo = MemoryComunicacionRepository()
        val resultado = enviar(casoDeUso(repo), inspectoria, AlcanceEnvio.APODERADO_INDIVIDUAL, "Nadie")

        assertEquals(ResultadoEmision.Error(EmisionComunicado.MSG_SIN_DESTINATARIOS), resultado)
        assertEquals(emptySet<String>(), apoderadosEnviados(repo))
    }

    @Test
    fun profesorNoRegistraNingunAlcance() {
        val repo = MemoryComunicacionRepository()
        val resultado = enviar(casoDeUso(repo), profesor, AlcanceEnvio.CURSO, "4° Básico A")

        assertEquals(ResultadoEmision.Error(EmisionComunicado.MSG_SIN_PERMISO), resultado)
        assertEquals(emptySet<String>(), apoderadosEnviados(repo))
    }

    @Test
    fun sinSesionNoRegistraNada() {
        val repo = MemoryComunicacionRepository()
        val resultado = enviar(casoDeUso(repo), null, AlcanceEnvio.COLEGIO_COMPLETO, null)

        assertEquals(ResultadoEmision.Error(EmisionComunicado.MSG_SIN_SESION), resultado)
    }

    @Test
    fun elAdjuntoQuedaGuardadoEnElComunicado() {
        val repo = MemoryComunicacionRepository()
        val resultado = enviar(casoDeUso(repo), inspectoria, AlcanceEnvio.COLEGIO_COMPLETO, null, "adjunto_1.pdf")

        assertEquals("adjunto_1.pdf", (resultado as ResultadoEmision.Ok).registro.comunicacion.adjuntoPdfNombre)
    }
}
