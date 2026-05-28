package pucp.edu.caritas_movile_grd.Cursos

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync

@Entity(
    tableName = "evaluacion_local",
    foreignKeys = [
        ForeignKey(
            entity = CursoCapacitacionLocal::class,
            parentColumns = ["idCursoRemoto"],
            childColumns = ["idCursoRemoto"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class EvaluacionLocal(
    @PrimaryKey val idEvaluacionRemota: Int,
    val idCursoRemoto: Int,
    val titulo: String,
    val descripcion: String,
    val puntajeMinimo: Int,
    val estadoSync: EstadoSync = EstadoSync.SINCRONIZADO
)

@Entity(
    tableName = "pregunta_local",
    foreignKeys = [
        ForeignKey(
            entity = EvaluacionLocal::class,
            parentColumns = ["idEvaluacionRemota"],
            childColumns = ["idEvaluacionRemota"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PreguntaLocal(
    @PrimaryKey val idPreguntaRemota: Int,
    val idEvaluacionRemota: Int,
    val enunciado: String,
    val opcionesJson: String, // Guardamos opciones como JSON string por simplicidad o una tabla aparte
    val respuestaCorrecta: Int // Índice de la opción correcta
)

@Entity(
    tableName = "progreso_curso_local",
    foreignKeys = [
        ForeignKey(
            entity = CursoCapacitacionLocal::class,
            parentColumns = ["idCursoRemoto"],
            childColumns = ["idCursoRemoto"]
        )
    ]
)
data class ProgresoCursoLocal(
    @PrimaryKey(autoGenerate = true) val idProgreso: Int = 0,
    val uuidUsuario: String,
    val idCursoRemoto: Int,
    val completado: Boolean = false,
    val notaObtenida: Int? = null,
    val estadoSync: EstadoSync = EstadoSync.NUEVO
)
