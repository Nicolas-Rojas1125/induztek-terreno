package cl.duocuc.induztek.ui.components

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import androidx.core.content.ContextCompat
import cl.duocuc.induztek.nativo.ArchivosFoto
import cl.duocuc.induztek.ui.theme.Dimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberCapturaFoto(onFoto: (String) -> Unit, onAviso: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    var rutaPendiente by rememberSaveable { mutableStateOf<String?>(null) }

    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val ruta = rutaPendiente
        if (ok && ruta != null && ArchivosFoto.existe(ruta)) {
            onFoto(ruta)
        } else {
            ArchivosFoto.eliminar(ruta)
            if (!ok) onAviso("Captura cancelada")
        }
        rutaPendiente = null
    }

    val abrirCamara: () -> Unit = {
        val (archivo, uri) = ArchivosFoto.nuevoArchivo(context)
        rutaPendiente = archivo.absolutePath
        try {
            camara.launch(uri)
        } catch (_: ActivityNotFoundException) {
            ArchivosFoto.eliminar(archivo.absolutePath)
            rutaPendiente = null
            onAviso("No hay una app de cámara disponible en el dispositivo")
        }
    }

    val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) abrirCamara() else onAviso("Permiso de cámara denegado: puede continuar sin foto")
    }

    return {
        val concedido = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (concedido) abrirCamara() else permiso.launch(Manifest.permission.CAMERA)
    }
}

@Composable
fun FotoPlaca(ruta: String, modifier: Modifier = Modifier) {
    val bitmap by produceState<Bitmap?>(initialValue = null, ruta) {
        value = withContext(Dispatchers.IO) { ArchivosFoto.miniatura(ruta) }
    }
    AnimatedVisibility(visible = bitmap != null, enter = fadeIn() + scaleIn(initialScale = 0.92f)) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Foto de la placa de características",
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .fillMaxWidth()
                    .height(Dimens.alturaFoto)
                    .clip(MaterialTheme.shapes.medium)
            )
        }
    }
}
