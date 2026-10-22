package com.mpaznotifica.app.ui.screen.comunicados

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mpaznotifica.app.data.local.AdjuntoStorage
import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.ErroresComunicado
import com.mpaznotifica.app.model.TipoComunicacion
import com.mpaznotifica.app.ui.components.BarraSuperior
import com.mpaznotifica.app.ui.components.SelectorDesplegable
import com.mpaznotifica.app.viewmodel.MpazNotificaViewModel
import com.mpaznotifica.app.viewmodel.ResultadoEnvio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Módulo "Redacción y envío": asunto, cuerpo, tipo, alcance y destino, adjunto PDF opcional y
 * marca de autorización con firma física. Valida cada campo, pide confirmación antes de enviar,
 * muestra progreso y una alerta visual con el resultado.
 */
@Composable
fun RedactarComunicadoScreen(navController: NavHostController, viewModel: MpazNotificaViewModel) {
    val usuario by viewModel.usuarioActivo.collectAsState()
    val resultadoEnvio by viewModel.resultadoEnvio.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var asunto by remember { mutableStateOf("") }
    var cuerpo by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf<TipoComunicacion?>(null) }
    var alcance by remember { mutableStateOf<AlcanceEnvio?>(null) }
    var destino by remember { mutableStateOf<String?>(null) }
    var adjuntoMostrado by remember { mutableStateOf<String?>(null) }
    var adjuntoInterno by remember { mutableStateOf<String?>(null) }
    var errorAdjunto by remember { mutableStateOf<String?>(null) }
    var requiereFirma by remember { mutableStateOf(false) }
    var errores by remember { mutableStateOf(ErroresComunicado()) }
    var confirmando by remember { mutableStateOf(false) }

    // Al salir de la pantalla se limpia el resultado, para que la alerta no reaparezca después.
    DisposableEffect(Unit) {
        onDispose { viewModel.limpiarResultadoEnvio() }
    }

    // Recurso nativo: selector del sistema, solo PDF. El archivo se copia al almacenamiento interno.
    val selectorPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            errorAdjunto = null
            scope.launch {
                try {
                    adjuntoInterno = withContext(Dispatchers.IO) { AdjuntoStorage.copiarPdf(context, uri) }
                    adjuntoMostrado = uri.lastPathSegment ?: "documento.pdf"
                } catch (e: Exception) {
                    errorAdjunto = e.message ?: "No se pudo adjuntar el archivo."
                }
            }
        }
    }

    Scaffold(topBar = { BarraSuperior("Redactar comunicado", navController) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = asunto,
                onValueChange = { asunto = it },
                label = { Text("Asunto") },
                singleLine = true,
                isError = errores.asunto != null,
                supportingText = if (errores.asunto != null) {
                    { Text(errores.asunto.orEmpty()) }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = cuerpo,
                onValueChange = { cuerpo = it },
                label = { Text("Cuerpo del mensaje") },
                minLines = 4,
                isError = errores.cuerpo != null,
                supportingText = if (errores.cuerpo != null) {
                    { Text(errores.cuerpo.orEmpty()) }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth()
            )

            SelectorDesplegable(
                etiqueta = "Tipo de comunicación",
                opciones = viewModel.tiposPermitidos(usuario),
                seleccion = tipo,
                textoDe = { it.etiqueta },
                onSeleccion = { tipo = it },
                error = errores.tipo
            )
            SelectorDesplegable(
                etiqueta = "Alcance (destinatarios)",
                opciones = viewModel.alcancesPermitidos(usuario),
                seleccion = alcance,
                textoDe = { it.etiqueta },
                onSeleccion = {
                    alcance = it
                    destino = null
                },
                error = errores.alcance
            )

            // El campo Destino aparece y desaparece con animación según el alcance elegido.
            AnimatedVisibility(
                visible = alcance != null && alcance != AlcanceEnvio.COLEGIO_COMPLETO,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                SelectorDesplegable(
                    etiqueta = "Destino",
                    opciones = viewModel.destinosPara(alcance, usuario),
                    seleccion = destino,
                    textoDe = { it },
                    onSeleccion = { destino = it },
                    error = errores.destino
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { selectorPdf.launch(arrayOf("application/pdf")) }) {
                    Text(if (adjuntoInterno == null) "Adjuntar PDF (opcional)" else "Cambiar PDF")
                }
                if (adjuntoInterno != null) {
                    TextButton(onClick = {
                        adjuntoInterno = null
                        adjuntoMostrado = null
                    }) { Text("Quitar") }
                }
            }
            adjuntoMostrado?.let { Text("Adjunto: $it", style = MaterialTheme.typography.bodySmall) }
            errorAdjunto?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = requiereFirma, onCheckedChange = { requiereFirma = it })
                Text("Es una autorización que requiere firma física del apoderado")
            }
            AnimatedVisibility(visible = requiereFirma) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Rol solo informativo: la app notifica y avisa que la autorización debe firmarse en papel. " +
                            "No se capturan firmas en la app.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                enabled = resultadoEnvio !is ResultadoEnvio.Enviando,
                onClick = {
                    errores = viewModel.validarComunicado(asunto, cuerpo, tipo, alcance, destino)
                    if (!errores.hayErrores && tipo != null && alcance != null) confirmando = true
                }
            ) {
                if (resultadoEnvio is ResultadoEnvio.Enviando) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Enviar")
                }
            }
        }
    }

    // Confirmación antes de enviar (el comunicado queda registrado de forma inmutable).
    if (confirmando) {
        AlertDialog(
            onDismissRequest = { confirmando = false },
            title = { Text("¿Enviar comunicado?") },
            text = { Text("Una vez enviado no se puede editar. Quedará registrado en el historial.") },
            confirmButton = {
                TextButton(onClick = {
                    val tipoElegido = tipo
                    val alcanceElegido = alcance
                    if (tipoElegido != null && alcanceElegido != null) {
                        viewModel.enviarComunicado(
                            asunto = asunto.trim(),
                            cuerpo = cuerpo.trim(),
                            tipo = tipoElegido,
                            alcance = alcanceElegido,
                            destino = destino,
                            adjuntoPdfNombre = adjuntoInterno,
                            requiereFirmaFisica = requiereFirma
                        )
                    }
                    confirmando = false
                }) { Text("Enviar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmando = false }) { Text("Cancelar") }
            }
        )
    }

    // Alerta visual con el resultado del envío (RNF del caso).
    when (val estado = resultadoEnvio) {
        is ResultadoEnvio.Enviado -> AlertDialog(
            onDismissRequest = { },
            title = { Text("Comunicado enviado") },
            text = { Text(textoEnvio(estado)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.limpiarResultadoEnvio()
                    navController.popBackStack()
                }) { Text("Aceptar") }
            }
        )
        is ResultadoEnvio.Error -> AlertDialog(
            onDismissRequest = { viewModel.limpiarResultadoEnvio() },
            title = { Text("No se pudo enviar") },
            text = { Text(estado.mensaje) },
            confirmButton = {
                TextButton(onClick = { viewModel.limpiarResultadoEnvio() }) { Text("Entendido") }
            }
        )
        else -> Unit
    }
}

private fun textoEnvio(estado: ResultadoEnvio.Enviado): String {
    val base = if (estado.total == 1) {
        "Enviado a 1 apoderado."
    } else {
        "Enviado a ${estado.total} apoderados."
    }
    return if (estado.fallidos == 0) {
        base
    } else {
        "$base ${estado.fallidos} envío(s) fallido(s): contáctalos por otra vía."
    }
}
