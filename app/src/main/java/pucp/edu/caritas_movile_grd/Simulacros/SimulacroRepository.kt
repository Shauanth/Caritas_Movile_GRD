package pucp.edu.caritas_movile_grd.Simulacros

import kotlinx.coroutines.flow.Flow

class SimulacroRepository(private val dao: SimulacroDao) {
    val allSimulacros: Flow<List<SimulacroLocal>> = dao.getAllSimulacros()

    fun getByEstado(estado: String) = dao.getSimulacrosByEstado(estado)

    suspend fun guardarSimulacro(s: SimulacroLocal) = dao.insertSimulacro(s)
    suspend fun actualizarSimulacro(s: SimulacroLocal) = dao.updateSimulacro(s)
}
