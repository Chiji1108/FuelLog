package tech.chiji.drivecost.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "fill_ups")
data class FillUpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val odometerKm: Double,
    val liters: Double,
    val totalYen: Int,
    val recordedAtMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "drives")
data class DriveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startOdometerKm: Double,
    val endOdometerKm: Double?,
    val estimatedFuelCostYen: Int?,
    val fuelEconomyKmPerLiter: Double?,
    val yenPerLiter: Double?,
    val recordedAtMillis: Long = System.currentTimeMillis(),
    val completedAtMillis: Long? = null,
)

@Dao
interface DriveCostDao {
    @Query("SELECT * FROM fill_ups ORDER BY odometerKm DESC, recordedAtMillis DESC")
    fun observeFillUps(): Flow<List<FillUpEntity>>

    @Query("SELECT * FROM drives ORDER BY recordedAtMillis DESC")
    fun observeDrives(): Flow<List<DriveEntity>>

    @Insert
    suspend fun insertFillUp(fillUp: FillUpEntity)

    @Insert
    suspend fun insertDrive(drive: DriveEntity)

    @Query("DELETE FROM fill_ups WHERE id = :id")
    suspend fun deleteFillUp(id: Long)

    @Query("DELETE FROM drives WHERE id = :id")
    suspend fun deleteDrive(id: Long)

    @Query(
        """
        UPDATE drives
        SET endOdometerKm = :endOdometerKm,
            estimatedFuelCostYen = :estimatedFuelCostYen,
            fuelEconomyKmPerLiter = :fuelEconomyKmPerLiter,
            yenPerLiter = :yenPerLiter,
            completedAtMillis = :completedAtMillis
        WHERE id = :id
        """,
    )
    suspend fun completeDrive(
        id: Long,
        endOdometerKm: Double,
        estimatedFuelCostYen: Int?,
        fuelEconomyKmPerLiter: Double?,
        yenPerLiter: Double?,
        completedAtMillis: Long = System.currentTimeMillis(),
    )
}

@Database(
    entities = [FillUpEntity::class, DriveEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class DriveCostDatabase : RoomDatabase() {
    abstract fun dao(): DriveCostDao

    companion object {
        @Volatile
        private var instance: DriveCostDatabase? = null

        fun get(context: Context): DriveCostDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DriveCostDatabase::class.java,
                    "drive-cost.db",
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
