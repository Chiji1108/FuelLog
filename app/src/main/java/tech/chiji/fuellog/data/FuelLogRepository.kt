package tech.chiji.fuellog.data

import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

class FuelLogRepository(
    private val dao: FuelLogDao,
) {
    val fillUps: Flow<List<FillUpEntity>> = dao.observeFillUps()
    val drives: Flow<List<DriveEntity>> = dao.observeDrives()

    suspend fun addFillUp(
        odometerKm: Double,
        liters: Double,
        totalYen: Int,
    ) {
        dao.insertFillUp(
            FillUpEntity(
                odometerKm = odometerKm,
                liters = liters,
                totalYen = totalYen,
            ),
        )
    }

    suspend fun startDrive(
        startOdometerKm: Double,
    ) {
        dao.insertDrive(
            DriveEntity(
                startOdometerKm = startOdometerKm,
                endOdometerKm = null,
                estimatedFuelCostYen = null,
                fuelEconomyKmPerLiter = null,
                yenPerLiter = null,
            ),
        )
    }

    suspend fun completeDrive(
        drive: DriveEntity,
        endOdometerKm: Double,
        fuelEconomyKmPerLiter: Double,
        yenPerLiter: Double,
    ) {
        val distanceKm = endOdometerKm - drive.startOdometerKm
        val estimatedFuelCostYen = (distanceKm / fuelEconomyKmPerLiter * yenPerLiter).roundToInt()
        dao.completeDrive(
            id = drive.id,
            endOdometerKm = endOdometerKm,
            estimatedFuelCostYen = estimatedFuelCostYen,
            fuelEconomyKmPerLiter = fuelEconomyKmPerLiter,
            yenPerLiter = yenPerLiter,
        )
    }

    suspend fun deleteFillUp(id: Long) {
        dao.deleteFillUp(id)
    }

    suspend fun deleteDrive(id: Long) {
        dao.deleteDrive(id)
    }
}
