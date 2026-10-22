package com.mpaznotifica.app.data.repository

import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.model.Estudiante
import com.mpaznotifica.app.model.Taller
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Estudiantes y talleres, necesarios para resolver destinatarios de curso y taller. */
interface DirectorioRepository {
    fun observarEstudiantes(): Flow<List<Estudiante>>
    fun observarTalleres(): Flow<List<Taller>>
}

class MemoryDirectorioRepository(
    estudiantesIniciales: List<Estudiante> = MockData.estudiantes,
    talleresIniciales: List<Taller> = MockData.talleres
) : DirectorioRepository {

    private val _estudiantes = MutableStateFlow(estudiantesIniciales)
    private val _talleres = MutableStateFlow(talleresIniciales)

    override fun observarEstudiantes(): Flow<List<Estudiante>> = _estudiantes.asStateFlow()

    override fun observarTalleres(): Flow<List<Taller>> = _talleres.asStateFlow()
}
