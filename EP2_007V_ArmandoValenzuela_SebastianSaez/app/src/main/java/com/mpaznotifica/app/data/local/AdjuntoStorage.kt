package com.mpaznotifica.app.data.local

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * Recurso nativo del caso: el PDF elegido con el selector del sistema (no pide permisos de
 * almacenamiento) se copia al almacenamiento interno de la app (RF-03 y RN-08).
 */
object AdjuntoStorage {

    /** Copia el PDF y devuelve el nombre del archivo interno. Lanza excepción si no es un PDF. */
    fun copiarPdf(context: Context, uri: Uri): String {
        // TODO(equipo): si el CITT define un tamaño máximo, validarlo aquí (RN-08).
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("No se pudo leer el archivo seleccionado.")

        // Un PDF válido empieza con la firma "%PDF".
        if (bytes.size < 4 || String(bytes, 0, 4, Charsets.US_ASCII) != "%PDF") {
            throw IllegalArgumentException("El archivo debe ser un PDF.")
        }

        val nombreInterno = "adjunto_${UUID.randomUUID()}.pdf"
        File(context.filesDir, nombreInterno).writeBytes(bytes)
        return nombreInterno
    }
}
