package pucp.edu.caritas_movile_grd.LocalBDConector

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import pucp.edu.caritas_movile_grd.Cursos.*
import pucp.edu.caritas_movile_grd.Kits.*
import pucp.edu.caritas_movile_grd.Incidencias.*
import pucp.edu.caritas_movile_grd.Masters.*
import pucp.edu.caritas_movile_grd.login.*
import pucp.edu.caritas_movile_grd.Evidencias.*
import pucp.edu.caritas_movile_grd.Simulacros.*
import pucp.edu.caritas_movile_grd.Observaciones.ObservacionLocal
import pucp.edu.caritas_movile_grd.Seguimientos.SeguimientoLocal

@Database(
    entities = [
        CursoCapacitacionLocal::class,
        ParticipanteLocal::class,
        MaterialCapacitacionLocal::class,
        EvaluacionLocal::class,
        PreguntaLocal::class,
        ProgresoCursoLocal::class,
        EntregaKitLocal::class,
        IncidenciaLocal::class,
        AfectadoLocal::class,
        EvidenciaLocal::class,
        ObservacionLocal::class,
        SeguimientoLocal::class,
        ParroquiaLocal::class,
        CatalogoLocal::class,
        PerfilUsuarioLocal::class,
        AsignacionTerritorio::class,
        SimulacroLocal::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cursoDao(): CursoDao
    abstract fun kitDao(): KitDao
    abstract fun evidenciaDao(): EvidenciaDao
    abstract fun incidenciaDao(): IncidenciaDao
    abstract fun loginDao(): LoginDao
    abstract fun masterDao(): MasterDao
    abstract fun syncDao(): SyncDao
    abstract fun simulacroDao(): SimulacroDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "caritas_grd_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
