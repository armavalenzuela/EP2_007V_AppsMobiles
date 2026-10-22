package com.mpaznotifica.app

import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.Perfil
import com.mpaznotifica.app.viewmodel.MpazNotificaViewModel
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class Rf05Test {
    private lateinit var vm: MpazNotificaViewModel

    @Before
    fun iniciarComoInspectoria() {
        vm = MpazNotificaViewModel()
        vm.entrarModoDesarrollo(Perfil.INSPECTORIA_GENERAL)
    }

    @Test
    fun inspectoriaVeLosCuatroAlcances() {
        assertEquals(
            AlcanceEnvio.entries.toSet(),
            vm.alcancesPermitidos(vm.usuarioActivo.value).toSet()
        )
    }

    @Test
    fun inspectoriaVeTodosLosCursosTalleresYApoderados() {
        val u = vm.usuarioActivo.value
        assertEquals(vm.cursos, vm.destinosPara(AlcanceEnvio.CURSO, u))
        assertEquals(vm.talleres.size, vm.destinosPara(AlcanceEnvio.TALLER, u).size)
        assertEquals(vm.apoderados.value.size, vm.destinosPara(AlcanceEnvio.APODERADO_INDIVIDUAL, u).size)
    }
}