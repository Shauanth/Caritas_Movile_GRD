package pucp.edu.caritas_movile_grd.Cursos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademiaScreen(
    viewModel: CursoViewModel,
    onBack: () -> Unit,
    onNavigateToCurso: (Int) -> Unit
) {
    val cursos by viewModel.cursos.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Academia de Brigadistas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(cursos) { curso ->
                CursoCard(curso = curso, onClick = { onNavigateToCurso(curso.idCursoRemoto) })
            }
        }
    }
}

@Composable
fun CursoCard(
    curso: CursoCapacitacionLocal,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = curso.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = curso.facilitador, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
