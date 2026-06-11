package pucp.edu.caritas_movile_grd.Kits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistorialEntregasScreen(
    entregas: List<EntregaKitLocal>
) {
    if (entregas.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Sin entregas registradas",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "Las entregas confirmadas aparecerán aquí",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "${entregas.size} entrega(s) registrada(s)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(entregas, key = { it.uuidEntrega }) { entrega ->
                EntregaKitCard(entrega = entrega)
            }
        }
    }
}

@Composable
private fun EntregaKitCard(entrega: EntregaKitLocal) {
    val (badgeColor, badgeText) = when (entrega.estadoSync) {
        EstadoSync.SINCRONIZADO    -> Color(0xFF2E7D32) to "Sincronizado"
        EstadoSync.NUEVO           -> Color(0xFFE65100) to "Pendiente"
        EstadoSync.EDITADO         -> Color(0xFF1565C0) to "Modificado"
        EstadoSync.PENDIENTE_SUBIDA -> Color(0xFF6A1B9A) to "Subiendo"
    }

    val fechaFormateada = remember(entrega.fechaEntrega) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "PE"))
            .format(Date(entrega.fechaEntrega))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entrega.kitEntregado,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Cantidad: ${entrega.cantidad}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Afectado: ${entrega.uuidAfectado}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = fechaFormateada,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Box(
                modifier = Modifier
                    .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeColor
                )
            }
        }
    }
}
