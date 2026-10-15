package ua.kpi.battery_storage.lab8.medium

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Батарейний модуль (батьківська таблиця). Назва модуля унікальна. */
@Entity(tableName = "modules", indices = [Index(value = ["name"], unique = true)])
data class BatteryModuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val capacityKwh: Double,
)

/**
 * Показання модуля (дочірня таблиця). Зовнішній ключ moduleId з CASCADE:
 * при видаленні модуля автоматично видаляються всі його показання.
 */
@Entity(
    tableName = "module_readings",
    foreignKeys = [
        ForeignKey(
            entity = BatteryModuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["moduleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("moduleId")],
)
data class ModuleReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val moduleId: Long,
    val stateOfCharge: Int,
    val temperatureC: Double,
    val recordedAt: Long,
    /** Звідки отримано показання: «local» (введено вручну) або «remote» (з сервера). */
    val source: String,
)

/** Зв'язок «один-до-багатьох»: модуль разом з усіма його показаннями. */
data class ModuleWithReadings(
    @Embedded val module: BatteryModuleEntity,
    @Relation(parentColumn = "id", entityColumn = "moduleId")
    val readings: List<ModuleReadingEntity>,
) {
    val latest: ModuleReadingEntity? get() = readings.maxByOrNull { it.recordedAt }
}

/** Розширений DAO: пошук, вставка, оновлення, видалення. */
@Dao
interface ModuleDao {
    /** @Transaction потрібна, бо Room виконує два запити (модулі + показання). */
    @Transaction
    @Query("SELECT * FROM modules WHERE name LIKE '%' || :query || '%' ORDER BY name")
    fun searchModules(query: String): Flow<List<ModuleWithReadings>>

    @Query("SELECT * FROM modules")
    suspend fun getModules(): List<BatteryModuleEntity>

    @Insert
    suspend fun insertModule(module: BatteryModuleEntity): Long

    @Insert
    suspend fun insertModules(modules: List<BatteryModuleEntity>)

    @Update
    suspend fun updateModule(module: BatteryModuleEntity)

    @Delete
    suspend fun deleteModule(module: BatteryModuleEntity)

    @Insert
    suspend fun insertReadings(readings: List<ModuleReadingEntity>)
}

@Database(
    entities = [BatteryModuleEntity::class, ModuleReadingEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MediumDatabase : RoomDatabase() {
    abstract fun moduleDao(): ModuleDao

    companion object {
        @Volatile
        private var instance: MediumDatabase? = null

        fun getInstance(context: Context): MediumDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, MediumDatabase::class.java, "medium_battery.db")
                .build()
                .also { instance = it }
        }
    }
}
