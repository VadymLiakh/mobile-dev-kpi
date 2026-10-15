package ua.kpi.battery_storage.lab8.basic

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

/** Entity: таблиця soc_records, один енергетичний параметр - рівень заряду. */
@Entity(tableName = "soc_records")
data class SocRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stateOfCharge: Int,
    val recordedAt: Long,
)

/** DAO з базовими методами вставки та отримання даних. */
@Dao
interface SocRecordDao {
    @Insert
    suspend fun insert(record: SocRecord): Long

    /** Flow повторно видає список щоразу, коли таблиця змінюється. */
    @Query("SELECT * FROM soc_records ORDER BY recordedAt DESC")
    fun observeAll(): Flow<List<SocRecord>>
}

/** База даних базового рівня з однією таблицею. */
@Database(entities = [SocRecord::class], version = 1, exportSchema = false)
abstract class BasicDatabase : RoomDatabase() {
    abstract fun socRecordDao(): SocRecordDao

    companion object {
        @Volatile
        private var instance: BasicDatabase? = null

        /** Єдиний екземпляр бази на весь застосунок (створення бази - дорога операція). */
        fun getInstance(context: Context): BasicDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, BasicDatabase::class.java, "basic_battery.db")
                .build()
                .also { instance = it }
        }
    }
}

/** Repository приховує від ViewModel, звідки беруться дані. */
class SocRepository(private val dao: SocRecordDao) {
    val records: Flow<List<SocRecord>> = dao.observeAll()

    suspend fun add(stateOfCharge: Int) {
        dao.insert(SocRecord(stateOfCharge = stateOfCharge, recordedAt = System.currentTimeMillis()))
    }
}
