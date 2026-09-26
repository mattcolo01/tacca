package it.matteocolombo.tacca.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import it.matteocolombo.tacca.domain.BuiltInMethod

@Database(
    entities = [CoffeeEntity::class, MethodEntity::class, GrinderEntity::class, CalibrationEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class TaccaDatabase : RoomDatabase() {
    abstract fun coffeeDao(): CoffeeDao
    abstract fun methodDao(): MethodDao
    abstract fun grinderDao(): GrinderDao
    abstract fun calibrationDao(): CalibrationDao

    companion object {
        const val FILE_NAME = "tacca.db"

        fun build(context: Context): TaccaDatabase =
            Room.databaseBuilder(context.applicationContext, TaccaDatabase::class.java, FILE_NAME)
                .addCallback(SeedBuiltInMethods)
                .build()

        fun inMemory(context: Context): TaccaDatabase =
            Room.inMemoryDatabaseBuilder(context.applicationContext, TaccaDatabase::class.java)
                .addCallback(SeedBuiltInMethods)
                .allowMainThreadQueries()
                .build()
    }
}

/** Inserts the five built-in methods, in mockup order, when the database is created. */
internal object SeedBuiltInMethods : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        BuiltInMethod.entries.forEachIndexed { index, method ->
            db.execSQL(
                "INSERT INTO method (name, builtInKey, icon, tint, sortOrder) VALUES (NULL, ?, ?, ?, ?)",
                arrayOf<Any>(method.key, method.icon.name, method.tint.name, index),
            )
        }
    }
}
