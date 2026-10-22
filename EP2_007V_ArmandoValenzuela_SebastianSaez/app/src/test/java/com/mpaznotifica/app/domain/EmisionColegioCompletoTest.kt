package com.mpaznotifica.app.domain

import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.EstadoEnvio
import com.mpaznotifica.app.model.Perfil
import com.mpaznotifica.app.model.TipoComunicacion
import com.mpaznotifica.app.model.Usuario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** RF-05: Inspectoría General emite a colegio completo (y RN-06, RN-07, RN-05, RF-07). */
class EmisionColegioCompletoTest {

    private val inspectoria: Usuario = MockData.usuarios.first { it.perfil == Perfil.INSPECTORIA_GENERAL }
    private val profesor: Usuario = MockData.usuarios.first { it.perfil == Perfil.PROFESOR_JEFE }

    private fun emitir(
        usuario: Usuario?,
        apoderados: List<Apoderado> = MockData.apoderados
    ): ResultadoEmision = EmisionColegioCompleto.emitir(
        usuario = usuario,
        idComunicacion = "CT",
        asunto = "Aviso general de prueba",
        cuerpo = "Mensaje de prueba",
        tipo = TipoComunicacion.INSTITUCIONAL,
        apoderados = apoderados,
        fechaHora = "2026-10-20 12:00"
    )

    private fun registroOk(resultado: ResultadoEmision): EmisionRegistrada {
        assertTrue("Se esperaba un registro exitoso, se obtuvo: $resultado", resultado is ResultadoEmision.Ok)
        return (resultado as ResultadoEmision.Ok).registro
    }

    @Test
    fun inspectoriaRegistraColegioCompletoConLosTresApoderados() {
        val registro = registroOk(emitir(inspectoria))

        assertEquals(3, registro.envios.size)
        assertEquals(setOf("AP1", "AP2", "AP3"), registro.envios.map { it.apoderadoId }.toSet())
    }

    @Test
    fun cadaEnvioUsaElCanalPreferenteDelApoderado() {
        val registro = registroOk(emitir(inspectoria))
        val canalPorApoderado = registro.envios.associate { it.apoderadoId to it.canal }

        assertEquals(CanalDespacho.EMAIL, canalPorApoderado["AP1"])
        assertEquals(CanalDespacho.WHATSAPP, canalPorApoderado["AP2"])
        assertEquals(CanalDespacho.SMS, canalPorApoderado["AP3"])
    }

    @Test
    fun elComunicadoRegistraAutorFechaYAlcanceAutomaticamente() {
        val comunicacion = registroOk(emitir(inspectoria)).comunicacion

        assertEquals(AlcanceEnvio.COLEGIO_COMPLETO, comunicacion.alcance)
        assertEquals(inspectoria.id, comunicacion.autorId)
        assertEquals(inspectoria.nombre, comunicacion.autorNombre)
        assertEquals("2026-10-20 12:00", comunicacion.fechaHora)
        assertNull(comunicacion.destinoReferencia)
    }

    @Test
    fun sinSesionNoSeRegistra() {
        assertEquals(
            ResultadoEmision.Error(EmisionColegioCompleto.MSG_SIN_SESION),
            emitir(null)
        )
    }

    @Test
    fun profesorJefeNoPuedeEmitirAColegioCompleto() {
        assertEquals(
            ResultadoEmision.Error(EmisionColegioCompleto.MSG_SIN_PERMISO),
            emitir(profesor)
        )
    }

    @Test
    fun sinApoderadosNoSeRegistra() {
        assertEquals(
            ResultadoEmision.Error(EmisionColegioCompleto.MSG_SIN_DESTINATARIOS),
            emitir(inspectoria, emptyList())
        )
    }

    @Test
    fun apoderadoRepetidoRecibeUnSoloEnvio() {
        // Mismo apoderado dos veces (por ejemplo, con dos pupilos): RN-07
        val repetidos = MockData.apoderados + MockData.apoderados.first()
        val registro = registroOk(emitir(inspectoria, repetidos))

        assertEquals(3, registro.envios.size)
    }

    @Test
    fun envioPorCorreoSinCorreoQuedaFallido() {
        val sinCorreo = Apoderado("AP9", "Apoderado Ficticio 9", "", "+56900000009", CanalDespacho.EMAIL)

        assertEquals(EstadoEnvio.FALLIDO, EmisionColegioCompleto.simularDespacho(sinCorreo))
    }

    @Test
    fun envioPorSmsSinTelefonoQuedaFallido() {
        val sinTelefono = Apoderado("AP9", "Apoderado Ficticio 9", "apoderado9@ejemplo.cl", "", CanalDespacho.SMS)

        assertEquals(EstadoEnvio.FALLIDO, EmisionColegioCompleto.simularDespacho(sinTelefono))
    }

    @Test
    fun apoderadoConDatoDelCanalQuedaDespachado() {
        val completo = Apoderado("AP9", "Apoderado Ficticio 9", "apoderado9@ejemplo.cl", "+56900000009", CanalDespacho.WHATSAPP)

        assertEquals(EstadoEnvio.DESPACHADO, EmisionColegioCompleto.simularDespacho(completo))
    }
}
