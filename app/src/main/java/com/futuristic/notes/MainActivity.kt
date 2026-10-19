package com.futuristic.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.*

@Entity
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val color: Long = 0xFF1E1E1E,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val tags: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface NoteDao {
    @Query("SELECT * FROM Note ORDER BY isPinned DESC, timestamp DESC")
    suspend fun getAll(): List<Note>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(note: Note)
    @Delete suspend fun delete(note: Note)
}

@Database(entities = [Note::class], version = 1, exportSchema = false)
abstract class AppDB : RoomDatabase() { abstract fun dao(): NoteDao }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FuturisticApp() }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(0.08f)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF00E5FF))
        Text(label, fontSize = 11.sp, color = Color.White.copy(0.7f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuturisticApp() {
    var notes by remember { mutableStateOf(listOf(
        Note(1, "Welcome to FUTURISTIC NOTES ✨", "Pin, archive, trash, search, edit.", 0xFF2D1B69, isPinned = true),
        Note(2, "My Checklist 📝", "☐ Design UI\n☐ Add voice notes\n☑ Build APK\n☐ Launch app", 0xFF0F4C75),
        Note(3, "Idea: Future is Now 🚀", "Neon gradients, dark space theme.", 0xFF1B262C, tags = "#idea #future")
    )) }
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    var isGrid by remember { mutableStateOf(true) }
    var showAdd by remember { mutableStateOf(false) }
    var editNote by remember { mutableStateOf<Note?>(null) }

    fun upd(n: Note, f: (Note) -> Note) { notes = notes.map { if (it.id == n.id) f(it) else it } }

    val filtered = notes.filter {
        val s = it.title.contains(search, true) || it.content.contains(search, true) || it.tags.contains(search, true)
        val f = when (filter) {
            "Pinned" -> it.isPinned && !it.isTrashed
            "Archive" -> it.isArchived && !it.isTrashed
            "Trash" -> it.isTrashed
            else -> !it.isTrashed && !it.isArchived
        }
        s && f
    }.sortedByDescending { it.isPinned }

    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF00E5FF), background = Color(0xFF070A14), surface = Color(0xFF1A1A2E))) {
        Scaffold(
            containerColor = Color(0xFF070A14),
            topBar = {
                TopAppBar(
                    title = { Text("FUTURISTIC NOTES", fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 17.sp) },
                    actions = {
                        IconButton(onClick = { isGrid = !isGrid }) {
                            Icon(if (isGrid) Icons.Default.ViewAgenda else Icons.Default.GridView, null, tint = Color(0xFF00E5FF))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAdd = true }, containerColor = Color(0xFF00E5FF), contentColor = Color.Black) {
                    Icon(Icons.Default.Add, null)
                }
            }
        ) { pad ->
            Column(
                Modifier.padding(pad).fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF070A14), Color(0xFF16213E))))
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = search, onValueChange = { search = it },
                    placeholder = { Text("Search notes, tags...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF00E5FF)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00E5FF), unfocusedBorderColor = Color.White.copy(0.2f)),
                    shape = RoundedCornerShape(16.dp), singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Pinned", "Archive", "Trash").forEach { tab ->
                        FilterChip(
                            selected = filter == tab, onClick = { filter = tab },
                            label = { Text(tab, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF00E5FF), selectedLabelColor = Color.Black)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Notes", notes.count { !it.isTrashed }.toString(), Modifier.weight(1f))
                    StatCard("Pinned", notes.count { it.isPinned && !it.isTrashed }.toString(), Modifier.weight(1f))
                    StatCard("Trash", notes.count { it.isTrashed }.toString(), Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (isGrid) 2 else 1),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { n ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { editNote = n },
                            colors = CardDefaults.cardColors(containerColor = Color(n.color)),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(n.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(6.dp))
                                Text(n.content, fontSize = 13.sp, maxLines = 6, overflow = TextOverflow.Ellipsis, color = Color.White.copy(0.85f))
                                if (n.tags.isNotBlank()) Text(n.tags, fontSize = 11.sp, color = Color(0xFF00E5FF))
                                Row {
                                    IconButton(onClick = { upd(n) { it.copy(isPinned = !it.isPinned) } }) {
                                        Icon(Icons.Default.Star, null, tint = if (n.isPinned) Color(0xFFFFD600) else Color.White.copy(0.5f))
                                    }
                                    IconButton(onClick = { upd(n) { it.copy(isArchived = !it.isArchived) } }) {
                                        Icon(Icons.Default.Archive, null, tint = Color.White.copy(0.7f))
                                    }
                                    IconButton(onClick = { upd(n) { it.copy(isTrashed = !it.isTrashed) } }) {
                                        Icon(if (n.isTrashed) Icons.Default.Restore else Icons.Default.Delete, null, tint = Color.White.copy(0.7f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAdd || editNote != null) {
            val editing = editNote
            var title by remember(editing) { mutableStateOf(editing?.title ?: "") }
            var content by remember(editing) { mutableStateOf(editing?.content ?: "") }
            val close = { showAdd = false; editNote = null }
            AlertDialog(
                onDismissRequest = close,
                title = { Text(if (editing == null) "New note" else "Edit note") },
                text = {
                    Column {
                        OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(content, { content = it }, label = { Text("Content") }, minLines = 3)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (editing == null) {
                            val nid = (notes.maxOfOrNull { it.id } ?: 0) + 1
                            notes = listOf(Note(id = nid, title = title, content = content, color = 0xFF0F4C75)) + notes
                        } else {
                            notes = notes.map { if (it.id == editing.id) it.copy(title = title, content = content) else it }
                        }
                        close()
                    }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = close) { Text("Cancel") } }
            )
        }
    }
}
