package com.mpaznotifica.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.data.repository.ApoderadoRepository
import com.mpaznotifica.app.data.repository.ComunicacionRepository
import com.mpaznotifica.app.data.repository.DirectorioRepository
import com.mpaznotifica.app.data.repository.MemoryDirectorioRepository
import com.mpaznotifica.app.data.repository.MemoryApoderadoRepository
import com.mpaznotifica.app.data.repository.MemoryComunicacionRepository
import com.mpaznotifica.app.domain.EnviarComunicadoUseCase
import com.mpaznotifica.app.domain.ResultadoEmision
import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.EnvioDestinatario
import com.mpaznotifica.app.model.EstadoEnvio
import com.mpaznotifica.app.model.ErroresComunicado
import com.mpaznotifica.app.model.Perfil
import com.mpaznotifica.app.model.TipoComunicacion
import com.mpaznotifica.app.model.Usuario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Estado del envío que muestra la pantalla de redacción. */
sealed class ResultadoEnvio {
    data object Inactivo : ResultadoEnvio()
    data object Enviando : ResultadoEnvio()
    data class Enviado(val total: Int, val fallidos: Int) : ResultadoEnvio()
    data class Error(val mensaje: String) : ResultadoEnvio()
}

/**
 * ViewModel único, compartido por todas las pantallas (se crea una sola vez en AppNavigation).
 *
 * Estado de los módulos:
 *  - RF-05 (Inspectoría a los cuatro alcances): implementado con repositorios y EnviarComunicadoUseCase.
 *  - Resto de módulos: TODO(equipo), sin cambios respecto al esqueleto original.
 *
 * Los repositorios en memoria son la implementación por defecto; cuando Room esté conectado, se
 * pasa RoomComunicacionRepository desde el punto de creación del ViewModel.
 */
class MpazNotificaViewModel(
    private val comunicaciones: ComunicacionRepository = MemoryComunicacionRepository(),
    private val apoderadosRepo: ApoderadoRepository = MemoryApoderadoRepository(),
    private val directorioRepo: DirectorioRepository = MemoryDirectorioRepository()
) : ViewModel() {

    private val emitirComunicado = EnviarComunicadoUseCase(comunicaciones, apoderadosRepo, directorioRepo)

    // Alcance propio para seguir los repositorios. No usa Dispatchers.Main en la construcción,
    // así el ViewModel también puede crearse en pruebas JVM. Los StateFlow son seguros entre hilos.
    private val sincronizacion = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // =====================================================================
    // Módulo "Autenticación y perfiles"
    // =====================================================================

    private val _usuarioActivo = MutableStateFlow<Usuario?>(null)
    val usuarioActivo: StateFlow<Usuario?> = _usuarioActivo.asStateFlow()

    private val _errorLogin = MutableStateFlow<String?>(null)
    val errorLogin: StateFlow<String?> = _errorLogin.asStateFlow()

    /**
     * TODO(equipo): inicio de sesión diferenciado para Inspectoría General y Profesor/a Jefe.
     * Validar campos vacíos (patrón Guía 11) y luego comparar contra MockData.usuarios (o el
     * backend que confirme el CITT). Si es correcto: dejar _usuarioActivo y devolver true.
     * Si no: dejar _errorLogin con un mensaje y devolver false.
     */
    fun login(usuario: String, clave: String): Boolean {
        _errorLogin.value = "Login sin implementar — ver TODO(equipo) en MpazNotificaViewModel.login()."
        return false
    }

    /**
     * SOLO PARA DESARROLLO: entra con el usuario de prueba del perfil indicado, sin validar
     * credenciales. TODO(equipo): eliminar esta función y los botones "Modo desarrollo".
     */
    fun entrarModoDesarrollo(perfil: Perfil) {
        _usuarioActivo.value = MockData.usuarios.first { it.perfil == perfil }
        _errorLogin.value = null
    }

    fun logout() {
        _usuarioActivo.value = null
        _errorLogin.value = null
        _comunicadoSeleccionadoId.value = null
    }

    // =====================================================================
    // Datos de referencia (no es lógica de negocio)
    // =====================================================================

    val cursos: List<String> = MockData.cursos
    val talleres = MockData.talleres
    val emisores: List<Usuario> = MockData.usuarios

    private val _apoderados = MutableStateFlow(MockData.apoderados)
    val apoderados: StateFlow<List<Apoderado>> = _apoderados.asStateFlow()

    fun apoderadoPorId(id: String): Apoderado? = _apoderados.value.find { it.id == id }

    fun nombresPupilos(apoderado: Apoderado): List<String> =
        MockData.estudiantes.filter { it.id in apoderado.pupilosIds }.map { "${it.nombreReferencial} (${it.curso})" }

    // =====================================================================
    // Módulo "Redacción y envío"
    // =====================================================================

    /**
     * TODO(equipo): devolver solo los alcances que el perfil puede usar (RF-02, RF-06).
     * Hoy devuelve los cuatro alcances para cualquier perfil.
     */
    fun alcancesPermitidos(usuario: Usuario?): List<AlcanceEnvio> = when (usuario?.perfil) {
        null -> emptyList()

        // RF-05: Inspectoría General emite a los cuatro alcances
        Perfil.INSPECTORIA_GENERAL -> AlcanceEnvio.entries.toList()

        // TODO(equipo) RF-06 / RN-01: restringir al Profesor/a Jefe
        Perfil.PROFESOR_JEFE -> AlcanceEnvio.entries.toList()
    }

    /**
     * TODO(equipo): devolver solo los tipos de comunicación que el perfil puede emitir (RF-02).
     * Hoy devuelve todos los tipos.
     */
    fun tiposPermitidos(usuario: Usuario?): List<TipoComunicacion> = TipoComunicacion.entries.toList()

    /**
     * TODO(equipo): opciones de destino según el alcance, respetando el perfil (RF-04, RF-06).
     * RF-05: Inspectoría General ve todos los cursos, talleres y apoderados.
     */
    fun destinosPara(alcance: AlcanceEnvio?, usuario: Usuario?): List<String> {
        if (usuario == null) return emptyList()
        return when (alcance) {
            AlcanceEnvio.CURSO -> cursos
            AlcanceEnvio.TALLER -> talleres.map { it.nombre }
            AlcanceEnvio.APODERADO_INDIVIDUAL -> _apoderados.value.map { it.nombreCompleto }
            AlcanceEnvio.COLEGIO_COMPLETO, null -> emptyList()
        }
    }

    /**
     * Valida el formulario y devuelve un mensaje por campo (formularios con validaciones).
     * Un alcance de colegio completo no exige destino.
     */
    fun validarComunicado(
        asunto: String,
        cuerpo: String,
        tipo: TipoComunicacion?,
        alcance: AlcanceEnvio?,
        destino: String?
    ): ErroresComunicado {
        val usuario = _usuarioActivo.value
        val asuntoLimpio = asunto.trim()
        val alcancePermitido = alcance != null && alcance in alcancesPermitidos(usuario)

        return ErroresComunicado(
            asunto = when {
                asuntoLimpio.isEmpty() -> "Ingresa un asunto."
                asuntoLimpio.length > 100 -> "El asunto no puede superar 100 caracteres."
                else -> null
            },
            cuerpo = if (cuerpo.isBlank()) "Escribe el contenido del comunicado." else null,
            tipo = when {
                tipo == null -> "Elige un tipo de comunicación."
                tipo !in tiposPermitidos(usuario) -> "Este tipo de comunicado no está disponible para tu perfil."
                else -> null
            },
            alcance = when {
                alcance == null -> "Elige un alcance."
                !alcancePermitido -> "Este alcance no está disponible para tu perfil."
                else -> null
            },
            destino = when {
                alcance == null || alcance == AlcanceEnvio.COLEGIO_COMPLETO -> null
                destino.isNullOrBlank() -> "Elige un destino."
                else -> null
            }
        )
    }

    private val _comunicados = MutableStateFlow(MockData.comunicados)
    val comunicados: StateFlow<List<Comunicacion>> = _comunicados.asStateFlow()

    private val _envios = MutableStateFlow(MockData.envios)

    private val _resultadoEnvio = MutableStateFlow<ResultadoEnvio>(ResultadoEnvio.Inactivo)
    val resultadoEnvio: StateFlow<ResultadoEnvio> = _resultadoEnvio.asStateFlow()

    init {
        // Las listas de la app siguen a los repositorios: lo que se registra aparece en el historial.
        sincronizacion.launch { comunicaciones.observarComunicados().collect { _comunicados.value = it } }
        sincronizacion.launch { comunicaciones.observarEnvios().collect { _envios.value = it } }
        sincronizacion.launch { apoderadosRepo.observarApoderados().collect { _apoderados.value = it } }
    }

    override fun onCleared() {
        sincronizacion.cancel()
        super.onCleared()
    }

    /** Vuelve el estado de envío a inactivo (al cerrar la alerta o salir de la pantalla). */
    fun limpiarResultadoEnvio() {
        _resultadoEnvio.value = ResultadoEnvio.Inactivo
    }

    /**
     * Envía el comunicado a cualquiera de los cuatro alcances (RF-05). La resolución de
     * destinatarios, la regla de perfil y el registro están en EnviarComunicadoUseCase.
     */
    fun enviarComunicado(
        asunto: String,
        cuerpo: String,
        tipo: TipoComunicacion,
        alcance: AlcanceEnvio,
        destino: String?,
        adjuntoPdfNombre: String?,
        requiereFirmaFisica: Boolean
    ) {
        _resultadoEnvio.value = ResultadoEnvio.Enviando
        viewModelScope.launch {
            val resultado = emitirComunicado(
                usuario = _usuarioActivo.value,
                asunto = asunto,
                cuerpo = cuerpo,
                tipo = tipo,
                alcance = alcance,
                destino = destino,
                adjuntoPdfNombre = adjuntoPdfNombre,
                requiereFirmaFisica = requiereFirmaFisica
            )
            _resultadoEnvio.value = when (resultado) {
                is ResultadoEmision.Ok -> ResultadoEnvio.Enviado(
                    total = resultado.registro.envios.size,
                    fallidos = resultado.registro.envios.count { it.estado == EstadoEnvio.FALLIDO }
                )
                is ResultadoEmision.Error -> ResultadoEnvio.Error(resultado.mensaje)
            }
        }
    }

    // =====================================================================
    // Módulo "Historial y trazabilidad"
    // =====================================================================

    private val _comunicadoSeleccionadoId = MutableStateFlow<String?>(null)
    val comunicadoSeleccionadoId: StateFlow<String?> = _comunicadoSeleccionadoId.asStateFlow()

    fun seleccionarComunicado(id: String?) {
        _comunicadoSeleccionadoId.value = id
    }

    fun comunicadoPorId(id: String?): Comunicacion? = _comunicados.value.find { it.id == id }

    fun enviosDe(comunicacionId: String): List<EnvioDestinatario> =
        _envios.value.filter { it.comunicacionId == comunicacionId }

    /**
     * TODO(equipo): filtrar la bitácora por tipo, emisor y rango de fechas (RF-09, RF-10).
     * Hoy devuelve la lista completa sin filtrar.
     */
    fun historialFiltrado(
        tipo: TipoComunicacion?,
        emisorId: String?,
        fechaDesde: String?,
        fechaHasta: String?
    ): List<Comunicacion> = _comunicados.value

    // =====================================================================
    // Canal preferente del apoderado (supuesto pendiente con el CITT)
    // =====================================================================

    /** TODO(equipo): guardar el canal preferente del apoderado (RF-13). Hoy no hace nada. */
    fun actualizarCanalPreferente(apoderadoId: String, canal: CanalDespacho) {
        // Sin implementar todavía.
    }
}
