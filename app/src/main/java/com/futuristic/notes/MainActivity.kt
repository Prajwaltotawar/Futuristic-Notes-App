package com.futuristic.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.*
import java.text.SimpleDateFormat
import java.util.*

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuturisticApp() {
    var notes by remember { mutableStateOf(listOf(
        Note(title="Welcome to FUTURISTIC NOTES ✨", content="Google Keep + 1000 Features\n\n✓ Pin & Archive\n✓ Trash\n✓ Search\n✓ 6 Neon Colors\n✓ Tags\n✓ Checklist\n✓ Voice\n✓ Drawing\n✓ Reminder\n✓ Share", color=0xFF2D1B69, isPinned=true),
        Note(title="My Checklist 📝", content="☐ Design UI\n☐ Add voice notes\n☑ Build APK\n☐ Launch app", color=0xFF0F4C75),
        Note(title="Idea: Future is Now 🚀", content="This app uses Glassmorphism, Neon Gradients, Dark Space Theme. Everything is futuristic.", color=0xFF1B262C, tags="#idea #future")
    )) }
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") } // All, Pinned, Archive, Trash
    var isGrid by remember { mutableStateOf(true) }
    var showAdd by remember { mutableStateOf(false) }
    var editNote by remember { mutableStateOf<Note?>(null) }

    val filtered = notes.filter {
        val matchesSearch = it.title.contains(search, true) || it.content.contains(search, true) || it.tags.contains(search, true)
        val matchesFilter = when(filter){
            "Pinned" -> it.isPinned && !it.isTrashed
            "Archive" -> it.isArchived
            "Trash" -> it.isTrashed
            else -> !it.isTrashed && !it.isArchived
        }
        matchesSearch && matchesFilter
    }

    MaterialTheme(colorScheme = darkColorScheme(primary=Color(0xFF00E5FF), background=Color(0xFF070A14), surface=Color(0xFF1A1A2E))) {
        Scaffold(
            containerColor = Color(0xFF070A14),
            topBar = {
                TopAppBar(title={Text("FUTURISTIC NOTES", fontWeight=FontWeight.Black, letterSpacing=2.sp, fontSize=17.sp)},
                    actions={
                        IconButton(onClick={isGrid=!isGrid}){ Icon(if(isGrid) Icons.Default.ViewAgenda else Icons.Default.GridView, null, tint=Color(0xFF00E5FF)) }
                    }, colors=TopAppBarDefaults.topAppBarColors(containerColor=Color.Transparent))
            },
            floatingActionButton = {
                FloatingActionButton(onClick={showAdd=true}, containerColor=Color(0xFF00E5FF), contentColor=Color.Black){ Icon(Icons.Default.Add, null) }
            }
        ) { pad ->
            Column(Modifier.padding(pad).fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF070A14), Color(0xFF16213E)))).padding(16.dp)) {
                OutlinedTextField(value=search, onValueChange={search=it}, placeholder={Text("Search notes, tags...")},
                    leadingIcon={Icon(Icons.Default.Search,null,tint=Color(0xFF00E5FF))},
                    modifier=Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
                    colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Color(0xFF00E5FF), unfocusedBorderColor=Color.White.copy(0.2f)),
                    shape=RoundedCornerShape(16.dp), singleLine=true
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    listOf("All","Pinned","Archive","Trash").forEach{ tab ->
                        FilterChip(selected=filter==tab, onClick={filter=tab}, label={Text(tab, fontSize=12.sp)},
                            colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Color(0xFF00E5FF), selectedLabelColor=Color.Black))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween){
                    StatCard("${notes.count{!
