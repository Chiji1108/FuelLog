package tech.chiji.drivecost.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.chiji.drivecost.data.DriveEntity
import tech.chiji.drivecost.data.FillUpEntity
import tech.chiji.drivecost.data.DriveCostDatabase
import tech.chiji.drivecost.data.DriveCostRepository

data class DriveCostUiState(
    val fillUps: List<FillUpEntity> = emptyList(),
    val drives: List<DriveEntity> = emptyList(),
    val latestFuelEconomyKmPerLiter: Double? = null,
    val latestYenPerLiter: Double? = null,
)

class DriveCostViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DriveCostRepository(DriveCostDatabase.get(application).dao())

    val uiState: StateFlow<DriveCostUiState> =
        combine(repository.fillUps, repository.drives) { fillUps, drives ->
            DriveCostUiState(
                fillUps = fillUps,
                drives = drives,
                latestFuelEconomyKmPerLiter = fillUps.fuelEconomyKmPerLiter(),
                latestYenPerLiter = fillUps.firstOrNull()?.let { it.totalYen / it.liters },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DriveCostUiState(),
        )

    fun addFillUp(odometerKm: Double, liters: Double, totalYen: Int) {
        viewModelScope.launch {
            repository.addFillUp(odometerKm, liters, totalYen)
        }
    }

    fun startDrive(startOdometerKm: Double) {
        viewModelScope.launch {
            repository.startDrive(startOdometerKm)
        }
    }

    fun completeDrive(driveId: Long, endOdometerKm: Double) {
        val state = uiState.value
        val drive = state.drives.firstOrNull { it.id == driveId } ?: return
        val fuelEconomy = state.latestFuelEconomyKmPerLiter ?: return
        val yenPerLiter = state.latestYenPerLiter ?: return
        if (endOdometerKm <= drive.startOdometerKm) return

        viewModelScope.launch {
            repository.completeDrive(
                drive = drive,
                endOdometerKm = endOdometerKm,
                fuelEconomyKmPerLiter = fuelEconomy,
                yenPerLiter = yenPerLiter,
            )
        }
    }

    fun deleteFillUp(id: Long) {
        viewModelScope.launch {
            repository.deleteFillUp(id)
        }
    }

    fun deleteDrive(id: Long) {
        viewModelScope.launch {
            repository.deleteDrive(id)
        }
    }
}

private fun List<FillUpEntity>.fuelEconomyKmPerLiter(): Double? {
    if (size < 2) return null

    val latest = this[0]
    val previous = this[1]
    val distanceKm = latest.odometerKm - previous.odometerKm
    if (distanceKm <= 0.0 || latest.liters <= 0.0) return null

    return distanceKm / latest.liters
}
