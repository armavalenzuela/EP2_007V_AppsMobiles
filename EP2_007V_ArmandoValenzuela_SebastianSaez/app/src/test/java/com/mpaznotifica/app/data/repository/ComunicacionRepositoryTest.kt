package com.mpaznotifica.app.data.repository

import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.domain.EmisionColegioCompleto
import com.mpaznotifica.app.domain.ResultadoEmision
import com.mpaznotifica.app.model.Perfil
import com.mpaznotifica.app.model.TipoComunicacion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** RF-05 + RN-02: el repositorio guarda el comunicado y sus envíos sin borrar lo anterior. */
class ComunicacionRepositoryTest {

    @Test
    fun registrarAgregaElComunicadoYSusTresEnviosSinBorrarLosAnteriores() = runBlocking {
        val repo = MemoryComunicacionRepository()
        val comunicadosAntes = repo.observarComunicados().first().size
        val enviosAntes = repo.observarEnvios().first().size

        val inspectoria = MockData.usuarios.first { it.perfil == Perfil.INSPECTORIA_GENERAL }
        val resultado = EmisionColegioCompleto.emitir(
            usuario = inspectoria,
            idComunicacion = "CT",
            asunto = "Aviso general de prueba",
            cuerpo = "Mensaje de prueba",
            tipo = TipoComunicacion.INSTITUCIONAL,
            apoderados = MockData.apoderados,
            fechaHora = "2026-10-20 12:00"
        )
        assertTrue(resultado is ResultadoEmision.Ok)
        repo.registrar((resultado as ResultadoEmision.Ok).registro)

        val comunicados = repo.observarComunicados().first()
        val envios = repo.observarEnvios().first()

        assertEquals(comunicadosAntes + 1, comunicados.size)
        assertEquals(enviosAntes + 3, envios.size)
        assertTrue(comunicados.any { it.id == "CT" })
        assertEquals(3, envios.count { it.comunicacionId == "CT" })
    }

    @Test
    fun memoryApoderadoRepositoryEntregaLosApoderadosFicticios() = runBlocking {
        val repo = MemoryApoderadoRepository()

        assertEquals(MockData.apoderados.size, repo.observarApoderados().first().size)
    }
}
