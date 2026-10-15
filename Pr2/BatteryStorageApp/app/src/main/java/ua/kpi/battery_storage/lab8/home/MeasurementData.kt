package ua.kpi.battery_storage.lab8.home

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Вимірювання енергетичних параметрів накопичувача (одна таблиця журналу). */
@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stateOfCharge: Int,
    val powerKw: Int,
    val temperatureC: Double,
    val note: String,
    val recordedAt: Long,
)

/** DAO з повним набором CRUD-операцій. */
@Dao
interface MeasurementDao {
    @Insert
    suspend fun insert(measurement: MeasurementEntity): Long

    @Update
    suspend fun update(measurement: MeasurementEntity)

    @Delete
    suspend fun delete(measurement: MeasurementEntity)

    @Query("SELECT * FROM measurements ORDER BY recordedAt DESC")
    fun observeAll(): Flow<List<MeasurementEntity>>
}

@Database(entities = [MeasurementEntity::class], version = 1, exportSchema = false)
abstract class MeasurementDatabase : RoomDatabase() {
    abstract fun measurementDao(): MeasurementDao

    companion object {
        @Volatile
        private var instance: MeasurementDatabase? = null

        fun getInstance(context: Context): MeasurementDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, MeasurementDatabase::class.java, "measurements.db")
                .build()
                .also { instance = it }
        }
    }
}

/** Repository — проміжний шар між ViewModel і DAO. */
class MeasurementRepository(private val dao: MeasurementDao) {
    val measurements: Flow<List<MeasurementEntity>> = dao.observeAll()

    suspend fun save(measurement: MeasurementEntity) {
        if (measurement.id == 0L) dao.insert(measurement) else dao.update(measurement)
    }

    suspend fun delete(measurement: MeasurementEntity) = dao.delete(measurement)
}
