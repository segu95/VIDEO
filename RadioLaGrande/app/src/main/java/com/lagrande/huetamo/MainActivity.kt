package com.lagrande.huetamo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val RadioRed = Color(0xFFE31320)
private val RadioGold = Color(0xFFFFD21A)
private val LocalRadioPrimary = compositionLocalOf { RadioRed }
private val LocalRadioAccent = compositionLocalOf { RadioGold }

data class RadioLink(val id: Long, val title: String, val url: String, val enabled: Boolean = true)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RadioApp(this) }
    }
}

@Composable
private fun RadioApp(context: Context) {
    val prefs = remember { context.getSharedPreferences("radio_config", Context.MODE_PRIVATE) }
    var station by remember { mutableStateOf(prefs.getString("station", "La Grande de Huétamo") ?: "La Grande de Huétamo") }
    var frequency by remember { mutableStateOf(prefs.getString("frequency", "106.9 FM") ?: "106.9 FM") }
    var slogan by remember { mutableStateOf(prefs.getString("slogan", "¡La Radio de Nuestra Gente!") ?: "¡La Radio de Nuestra Gente!") }
    var stream by remember { mutableStateOf(prefs.getString("stream", "") ?: "") }
    var backgroundUri by remember { mutableStateOf(prefs.getString("background", "") ?: "") }
    var logoUri by remember { mutableStateOf(prefs.getString("logo", "") ?: "") }
    var facebook by remember { mutableStateOf(prefs.getString("facebook", "") ?: "") }
    var whatsapp by remember { mutableStateOf(prefs.getString("whatsapp", "") ?: "") }
    var website by remember { mutableStateOf(prefs.getString("website", "") ?: "") }
    var extraLinks by remember { mutableStateOf(readLinks(prefs)) }
    var playing by remember { mutableStateOf(false) }
    var admin by remember { mutableStateOf(false) }
    var showPin by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var showLinkDialog by remember { mutableStateOf(false) }

    var primaryColor by remember { mutableStateOf(parseRadioColor(prefs.getString("primary_color", "#E31320") ?: "#E31320", RadioRed)) }
    var accentColor by remember { mutableStateOf(parseRadioColor(prefs.getString("accent_color", "#FFD21A") ?: "#FFD21A", RadioGold)) }
    val scope = rememberCoroutineScope()

    fun syncRemote() {
        if (AppConfig.API_URL.isBlank() || AppConfig.API_URL.contains("TU-DOMINIO")) {
            message = "Configura el dominio del panel en AppConfig.kt"
            return
        }
        scope.launch {
            message = "Sincronizando con el panel web..."
            try {
                val json = withContext(Dispatchers.IO) { RadioApi.fetchConfig(AppConfig.API_URL) }
                if (!json.optBoolean("ok", false)) throw IllegalStateException("Respuesta API no válida")
                val cfg = json.optJSONObject("settings") ?: JSONObject()
                station = cfg.optString("station_name", station)
                frequency = cfg.optString("frequency", frequency)
                slogan = cfg.optString("subtitle", slogan)
                stream = cfg.optString("stream_url", stream)
                facebook = cfg.optString("facebook_url", facebook)
                website = cfg.optString("website_url", website)
                backgroundUri = cfg.optString("background_url", backgroundUri)
                logoUri = cfg.optString("logo_url", logoUri)
                val waNumber = cfg.optString("whatsapp_number", "")
                val waMessage = cfg.optString("whatsapp_message", "Hola, quiero comunicarme con la radio.")
                whatsapp = if (waNumber.startsWith("http")) waNumber else if (waNumber.isNotBlank()) "https://wa.me/${waNumber.filter { it.isDigit() }}?text=${Uri.encode(waMessage)}" else whatsapp
                primaryColor = parseRadioColor(cfg.optString("primary_color"), RadioRed)
                accentColor = parseRadioColor(cfg.optString("accent_color"), RadioGold)
                val serverLinks = json.optJSONArray("links") ?: JSONArray()
                extraLinks = (0 until serverLinks.length()).mapNotNull { index ->
                    val item = serverLinks.optJSONObject(index) ?: return@mapNotNull null
                    RadioLink(item.optLong("id", index.toLong()), item.optString("label"), item.optString("url"), true)
                }
                prefs.edit().putString("station", station).putString("frequency", frequency)
                    .putString("slogan", slogan).putString("stream", stream).putString("facebook", facebook)
                    .putString("whatsapp", whatsapp).putString("website", website)
                    .putString("background", backgroundUri).putString("logo", logoUri)
                    .putString("primary_color", cfg.optString("primary_color"))
                    .putString("accent_color", cfg.optString("accent_color")).apply()
                message = "Configuración sincronizada con el panel web"
            } catch (_: Exception) {
                message = "No se pudo sincronizar. Revisa dominio, HTTPS y conexión."
            }
        }
    }

    LaunchedEffect(AppConfig.API_URL) { syncRemote() }
    var editingLink by remember { mutableStateOf<RadioLink?>(null) }
    var imageTarget by remember { mutableStateOf("background") }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            })
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    fun save() {
        val linksJson = JSONArray().apply {
            extraLinks.forEach { put(JSONObject().put("id", it.id).put("title", it.title).put("url", it.url).put("enabled", it.enabled)) }
        }
        prefs.edit()
            .putString("station", station).putString("frequency", frequency).putString("slogan", slogan)
            .putString("stream", stream).putString("background", backgroundUri).putString("logo", logoUri)
            .putString("facebook", facebook).putString("whatsapp", whatsapp).putString("website", website)
            .putString("links", linksJson.toString()).apply()
        message = "Cambios guardados"
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            if (imageTarget == "logo") logoUri = it.toString() else backgroundUri = it.toString()
            save()
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = primaryColor, secondary = accentColor, background = Color(0xFF090707), surface = Color(0xFF1B1717))) {
      CompositionLocalProvider(LocalRadioPrimary provides primaryColor, LocalRadioAccent provides accentColor) {
        Box(Modifier.fillMaxSize().background(Color(0xFF090707))) {
            if (backgroundUri.isNotBlank()) {
                AsyncImage(model = Uri.parse(backgroundUri), contentDescription = "Fondo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF263E5B), Color(0xFF8B4B25), Color(0xFF080606)))))
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(.24f), Color.Black.copy(.55f), Color.Black.copy(.92f)))))
            if (admin) {
                AdminPanel(
                    station, { station = it }, frequency, { frequency = it }, slogan, { slogan = it },
                    stream, { stream = it }, facebook, { facebook = it }, whatsapp, { whatsapp = it },
                    website, { website = it }, extraLinks,
                    onPickBackground = { imageTarget = "background"; imagePicker.launch(arrayOf("image/*")) },
                    onPickLogo = { imageTarget = "logo"; imagePicker.launch(arrayOf("image/*")) },
                    onAdd = { editingLink = null; showLinkDialog = true },
                    onEdit = { editingLink = it; showLinkDialog = true },
                    onDelete = { item -> extraLinks = extraLinks.filterNot { it.id == item.id }; save() },
                    onSave = { save() }, onBack = { admin = false }, onSync = { syncRemote() }, message = message
                )
            } else {
                HomePanel(
                    station, frequency, slogan, logoUri, playing, message,
                    facebook, whatsapp, website, extraLinks.filter { it.enabled },
                    onPlay = {
                        if (stream.isBlank()) message = "Configura la URL del streaming en Administración"
                        else try {
                            if (player.isPlaying) player.pause()
                            else { player.setMediaItem(MediaItem.fromUri(stream)); player.prepare(); player.play() }
                        } catch (_: Exception) { message = "No se pudo iniciar la transmisión" }
                    },
                    onOpen = { openLink(context, it) },
                    onAdmin = { showPin = true }
                )
            }

            if (showPin) {
                AlertDialog(
                    onDismissRequest = { showPin = false; pin = "" },
                    title = { Text("Acceso de administración") },
                    text = { OutlinedTextField(pin, { pin = it }, label = { Text("PIN") }) },
                    confirmButton = {
                        TextButton(onClick = {
                            if (pin == prefs.getString("admin_pin", "1069")) admin = true else message = "PIN incorrecto"
                            showPin = false; pin = ""
                        }) { Text("Entrar") }
                    },
                    dismissButton = { TextButton(onClick = { showPin = false }) { Text("Cancelar") } }
                )
            }
            if (showLinkDialog) {
                EditLinkDialog(editingLink, onDismiss = { showLinkDialog = false }, onSave = { item ->
                    extraLinks = if (editingLink == null) extraLinks + item else extraLinks.map { if (it.id == item.id) item else it }
                    save(); showLinkDialog = false
                })
            }
        }
      }
    }
}

@Composable
private fun HomePanel(
    station: String, frequency: String, slogan: String, logo: String, playing: Boolean, message: String,
    facebook: String, whatsapp: String, website: String, links: List<RadioLink>,
    onPlay: () -> Unit, onOpen: (String) -> Unit, onAdmin: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(22.dp))
            if (logo.isNotBlank()) {
                AsyncImage(Uri.parse(logo), "Logotipo", Modifier.fillMaxWidth().height(190.dp), contentScale = ContentScale.Fit)
            } else {
                Text("LA GRANDE", color = RadioRed, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("🎙️  🎧", fontSize = 64.sp)
                Text("HUÉTAMO", color = RadioGold, fontSize = 32.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(14.dp))
            Text(station, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(frequency, color = RadioGold, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(26.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(.5f)))
                Text("  E N   V I V O  ", color = Color.White, fontWeight = FontWeight.Bold)
                Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(.5f)))
            }
            Spacer(Modifier.height(18.dp))
            Button(onClick = onPlay, modifier = Modifier.fillMaxWidth(.86f).height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RadioRed), shape = RoundedCornerShape(40.dp)) {
                Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, Modifier.size(30.dp))
                Spacer(Modifier.width(12.dp))
                Text(if (playing) "PAUSAR" else "ESCUCHAR", fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
            Spacer(Modifier.height(12.dp))
            Surface(color = Color.Black.copy(.48f), shape = RoundedCornerShape(30.dp), border = BorderStroke(1.dp, Color.White.copy(.25f))) {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Radio, null, tint = RadioGold)
                    Spacer(Modifier.width(10.dp))
                    Text(if (playing) "Transmitiendo en vivo" else "Lista para escuchar", color = Color.White)
                }
            }
            if (message.isNotBlank()) Text(message, color = RadioGold, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(25.dp))
            Text("Conéctate con nosotros", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SocialLink("Facebook", Icons.Default.Language, Modifier.weight(1f)) { onOpen(facebook) }
                SocialLink("WhatsApp", Icons.Default.Chat, Modifier.weight(1f)) { onOpen(whatsapp) }
            }
            if (website.isNotBlank()) SocialLink("La Grande de Huétamo", Icons.Default.Language, Modifier.fillMaxWidth()) { onOpen(website) }
            links.forEach { SocialLink(it.title, Icons.Default.Language, Modifier.fillMaxWidth()) { onOpen(it.url) } }
            Spacer(Modifier.height(12.dp))
            Text(slogan, color = Color.White.copy(.75f), textAlign = TextAlign.Center)
            Text("Radio La Grande de Huétamo", color = Color.White.copy(.55f), fontSize = 12.sp)
            Text("Abrir administración", color = Color.White.copy(.4f), fontSize = 11.sp,
                modifier = Modifier.clickable { onAdmin() }.padding(10.dp))
        }
        Row(Modifier.fillMaxWidth().background(Color(0xFF111111)).navigationBarsPadding().height(70.dp),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Home, null, tint = RadioRed, modifier = Modifier.size(27.dp))
                Text("Inicio", color = Color.White.copy(.75f), fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Forum, null, tint = Color.White.copy(.75f), modifier = Modifier.size(27.dp))
                Text("Chat", color = Color.White.copy(.75f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SocialLink(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.padding(vertical = 5.dp).height(56.dp).clickable { onClick() },
        color = Color.Black.copy(.45f), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color.White.copy(.38f))) {
        Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(9.dp))
            Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun AdminPanel(
    station: String, setStation: (String) -> Unit, frequency: String, setFrequency: (String) -> Unit,
    slogan: String, setSlogan: (String) -> Unit, stream: String, setStream: (String) -> Unit,
    facebook: String, setFacebook: (String) -> Unit, whatsapp: String, setWhatsapp: (String) -> Unit,
    website: String, setWebsite: (String) -> Unit, links: List<RadioLink>,
    onPickBackground: () -> Unit, onPickLogo: () -> Unit, onAdd: () -> Unit,
    onEdit: (RadioLink) -> Unit, onDelete: (RadioLink) -> Unit, onSave: () -> Unit,
    onBack: () -> Unit, onSync: () -> Unit, message: String
) {
    LazyColumn(Modifier.fillMaxSize().background(Color(0xFF101010)).statusBarsPadding().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
                Text("Panel de administración", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            }
            Text("Administra el contenido de tu emisora.", color = Color.LightGray, fontSize = 13.sp)
        }
        item { AdminField("Nombre de la emisora", station, setStation) }
        item { AdminField("Frecuencia", frequency, setFrequency) }
        item { AdminField("Eslogan", slogan, setSlogan) }
        item { AdminField("URL del streaming", stream, setStream, "https://...") }
        item { AdminField("Facebook", facebook, setFacebook, "https://...") }
        item { AdminField("WhatsApp", whatsapp, setWhatsapp, "https://wa.me/...") }
        item { AdminField("Sitio web", website, setWebsite, "https://...") }
        item {
            Text("Imágenes", color = RadioGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPickBackground, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Image, null); Text("Fondo") }
                OutlinedButton(onClick = onPickLogo, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Image, null); Text("Logotipo") }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Botones y enlaces", color = RadioGold, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = RadioRed)) { Icon(Icons.Default.Add, null); Text("Agregar") }
            }
        }
        items(links, key = { it.id }) { link ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF282222))) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(link.title, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(link.url, color = Color.LightGray, fontSize = 11.sp, maxLines = 1)
                        Text(if (link.enabled) "Visible" else "Oculto", color = RadioGold, fontSize = 11.sp)
                    }
                    IconButton(onClick = { onEdit(link) }) { Icon(Icons.Default.Edit, null, tint = RadioGold) }
                    IconButton(onClick = { onDelete(link) }) { Icon(Icons.Default.Delete, null, tint = RadioRed) }
                }
            }
        }
        item {
            OutlinedButton(onClick = onSync, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.CloudSync, null); Spacer(Modifier.width(8.dp)); Text("SINCRONIZAR CON PANEL WEB") }
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = RadioRed)) {
                Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("GUARDAR CAMBIOS")
            }
            if (message.isNotBlank()) Text(message, color = RadioGold)
            Text("PIN inicial: 1069. Cámbialo antes de distribuir la app.", color = Color.LightGray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AdminField(label: String, value: String, onChange: (String) -> Unit, hint: String = "") {
    Column {
        Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        OutlinedTextField(value, onChange, modifier = Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { if (hint.isNotBlank()) Text(hint) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = RadioGold, unfocusedBorderColor = Color.Gray))
    }
}

@Composable
private fun EditLinkDialog(initial: RadioLink?, onDismiss: () -> Unit, onSave: (RadioLink) -> Unit) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var url by remember { mutableStateOf(initial?.url ?: "") }
    var enabled by remember { mutableStateOf(initial?.enabled ?: true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Agregar enlace" else "Editar enlace") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Nombre del botón") })
                OutlinedTextField(url, { url = it }, label = { Text("URL") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(enabled, { enabled = it }); Text("Mostrar en inicio")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank() && url.isNotBlank()) onSave(RadioLink(initial?.id ?: System.currentTimeMillis(), title.trim(), url.trim(), enabled))
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

private fun parseRadioColor(value: String, fallback: Color): Color = try { Color(android.graphics.Color.parseColor(value)) } catch (_: Exception) { fallback }

private fun readLinks(prefs: android.content.SharedPreferences): List<RadioLink> = try {
    val array = JSONArray(prefs.getString("links", "[]"))
    (0 until array.length()).map { index ->
        val item = array.getJSONObject(index)
        RadioLink(item.optLong("id"), item.optString("title"), item.optString("url"), item.optBoolean("enabled", true))
    }
} catch (_: Exception) { emptyList() }

private fun openLink(context: Context, raw: String) {
    if (raw.isBlank()) return
    try {
        val url = if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "https://$raw"
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) { }
}
