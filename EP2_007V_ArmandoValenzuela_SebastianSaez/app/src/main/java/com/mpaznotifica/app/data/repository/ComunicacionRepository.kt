package com.mpaznotifica.app.data.repository

import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.domain.EmisionRegistrada
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.EnvioDestinatario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Acceso a comunicados y envíos. El ViewModel depende de esta interfaz, no de la implementación. */
interface ComunicacionRepository {
    fun observarComunicados(): Flow<List<Comunicacion>>
    fun observarEnvios(): Flow<List<EnvioDestinatario>>
    suspend fun registrar(registro: EmisionRegistrada)
}

interface ApoderadoRepository {
    fun observarApoderados(): Flow<List<Apoderado>>
}

/** Implementación en memoria, usada en pruebas y mientras Room no está conectado. */
class MemoryComunicacionRepository(
    comunicadosIniciales: List<Comunicacion> = MockData.comunicados,
    enviosIniciales: List<EnvioDestinatario> = MockData.envios
) : ComunicacionRepository {

    private val _comunicados = MutableStateFlow(comunicadosIniciales)
    private val _envios = MutableStateFlow(enviosIniciales)

    override fun observarComunicados(): Flow<List<Comunicacion>> = _comunicados.asStateFlow()

    override fun observarEnvios(): Flow<List<EnvioDestinatario>> = _envios.asStateFlow()

    override suspend fun registrar(registro: EmisionRegistrada) {
        // RN-02: solo se agrega. Nunca se modifica ni se borra lo ya registrado.
        _comunicados.value = _comunicados.value + registro.comunicacion
        _envios.value = _envios.value + registro.envios
    }
}

class MemoryApoderadoRepository(
    apoderadosIniciales: List<Apoderado> = MockData.apoderados
) : ApoderadoRepository {

    private val _apoderados = MutableStateFlow(apoderadosIniciales)

    override fun observarApoderados(): Flow<List<Apoderado>> = _apoderados.asStateFlow()
}
