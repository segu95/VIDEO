package com.lagrande.huetamo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

private val Red = Color(0xFFD71920)
private val Gold = Color(0xFFFFD400)
private val Ink = Color(0xFF101010)
private val White = Color(0xFFF8F8F8)
private const val ADMIN_PANEL_URL = "" // Pega aquí la URL HTTPS del panel publicado.

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RadioApp() }
    }
}

data class RadioConfig(
    val stationName: String = "La Grande de Huétamo",
    val tagline: String = "Radio La Grande de Huétamo",
    val streamUrl: String = "",
    val logoUrl: String = "",
    val backgroundUrl: String = "",
    val primaryColor: String = "#D71920",
    val chatEnabled: Boolean = true,
    val playerEnabled: Boolean = true,
    val links: List<Map<String, Any>> = emptyList()
)

data class ChatMessage(val id: String = "", val name: String = "Oyente", val message: String = "")

@Composable
fun RadioApp() {
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    var config by remember { mutableStateOf(RadioConfig()) }
    var tab by remember { mutableStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Listo para escuchar") }
    val player = remember { ExoPlayer.Builder(context).build() }

    DisposableEffect(Unit) {
        val registration: ListenerRegistration = db.collection("public").document("app")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    config = snapshot.toObject(RadioConfig::class.java) ?: RadioConfig()
                }
            }
        onDispose { registration.remove(); player.release() }
    }

    val brandColor = remember(config.primaryColor) {
        runCatching { Color(android.graphics.Color.parseColor(config.primaryColor)) }.getOrDefault(Red)
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = brandColor, background = Ink, surface = Color(0xFF191919))) {
        Scaffold(containerColor = Ink, bottomBar = {
            NavigationBar(containerColor = Color(0xFF111111), contentColor = White) {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Inicio") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.Chat, null) }, label = { Text("Chat") }, enabled = config.chatEnabled)
            }
        }) { padding ->
            if (tab == 0) HomeScreen(config, playing, status, brandColor, onPlay = {
                if (config.streamUrl.isBlank()) status = "Configura la URL de streaming desde el panel"
                else if (!playing) {
                    player.setMediaItem(MediaItem.fromUri(config.streamUrl)); player.prepare(); player.play()
                    playing = true; status = "Reproduciendo en vivo"
                } else { player.pause(); playing = false; status = "Pausado" }
            }, modifier = Modifier.padding(padding))
            else ChatScreen(db, modifier = Modifier.padding(padding))
        }
    }
}

@Composable
fun HomeScreen(c: RadioConfig, playing: Boolean, status: String, brandColor: Color, onPlay: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Ink), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(270.dp).background(Color(0xFF29211C)), contentAlignment = Alignment.Center) {
            if (c.backgroundUrl.isNotBlank()) AsyncImage(model = c.backgroundUrl, contentDescription = "Fondo de la estación", modifier = Modifier.fillMaxSize())
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (c.logoUrl.isNotBlank()) AsyncImage(model = c.logoUrl, contentDescription = "Logotipo", modifier = Modifier.size(150.dp))
                else Text("LA GRANDE\nHUÉTAMO", color = Red, fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(c.stationName, color = White, fontSize = 27.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f), color = Color.Gray)
            Text("  E N   V I V O  ", color = White, fontWeight = FontWeight.Bold)
            HorizontalDivider(Modifier.weight(1f), color = Color.Gray)
        }
        Spacer(Modifier.height(22.dp))
        Button(onClick = onPlay, enabled = c.playerEnabled, colors = ButtonDefaults.buttonColors(containerColor = brandColor), shape = RoundedCornerShape(40.dp), modifier = Modifier.fillMaxWidth(0.78f).height(66.dp)) {
            Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(12.dp)); Text(if (playing) "PAUSAR" else "ESCUCHAR", fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
        Spacer(Modifier.height(12.dp))
        Surface(color = Color(0x55333333), shape = RoundedCornerShape(28.dp)) { Text("  ▣  $status  ", color = White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) }
        Spacer(Modifier.height(28.dp))
        Text("Conéctate con nosotros", color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        c.links.filter { it["enabled"] != false }.forEach { link ->
            val label = link["label"]?.toString().orEmpty()
            val url = link["url"]?.toString().orEmpty()
            if (label.isNotBlank() && url.isNotBlank()) LinkTile(label, url)
        }
        Spacer(Modifier.height(24.dp))
        Text(c.tagline, color = Color.LightGray, modifier = Modifier.padding(bottom = 12.dp))
        if (ADMIN_PANEL_URL.isNotBlank()) LinkTile("Administración", ADMIN_PANEL_URL)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun LinkTile(label: String, url: String) {
    val context = LocalContext.current
    Surface(onClick = {
        runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
    }, color = Color(0x55222222), shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF777777)), modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 5.dp)) {
        Text(label, color = White, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(18.dp))
    }
}

@Composable
fun ChatScreen(db: FirebaseFirestore, modifier: Modifier = Modifier) {
    var name by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    DisposableEffect(Unit) {
        val reg = db.collection("chat").orderBy("createdAt").limitToLast(100).addSnapshotListener { snap, _ ->
            if (snap != null) messages = snap.documents.map { ChatMessage(it.id, it.getString("name") ?: "Oyente", it.getString("message") ?: "") }
        }
        onDispose { reg.remove() }
    }
    Column(modifier.fillMaxSize().background(Ink).padding(16.dp)) {
        Text("CHAT EN VIVO", color = White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(messages) { m ->
                Surface(color = Color(0xFF242424), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) { Text(m.name, color = Gold, fontWeight = FontWeight.Bold); Text(m.message, color = White) }
                }
            }
        }
        OutlinedTextField(name, { name = it }, label = { Text("Tu nombre") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(message, { message = it }, label = { Text("Escribe un mensaje") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            if (message.isNotBlank()) {
                db.collection("chat").add(mapOf("name" to name.ifBlank { "Oyente" }, "message" to message.trim(), "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
                message = ""
            }
        }, colors = ButtonDefaults.buttonColors(containerColor = Red), modifier = Modifier.fillMaxWidth()) { Text("ENVIAR MENSAJE") }
    }
}
