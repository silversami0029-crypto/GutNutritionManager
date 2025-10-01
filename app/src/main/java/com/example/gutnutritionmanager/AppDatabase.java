package com.example.gutnutritionmanager;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
@Database(entities = {LogEntry.class, Food.class, FoodAdvice.class, USDAFoodMapping.class, FoodLogEntity.class, Symptom.class, Recipe.class},
        version = 20,
        exportSchema = false)

@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract LogEntryDao logEntryDao();
    public abstract FoodDao foodDao();
    public abstract FoodAdviceDao foodAdviceDao();
    public abstract USDAFoodMappingDao usdaFoodMappingDao();

    // Singleton pattern
    private static volatile AppDatabase INSTANCE;
    public static final ExecutorService databaseWriteExecutor = Executors.newSingleThreadExecutor();

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "nutrition_database_recent_new")
                            .fallbackToDestructiveMigration()
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    databaseWriteExecutor.execute(() -> {
                                        Log.d("AppDatabase", "Starting database initialization");

                                        // Add debug logging
                                        int initialCount = INSTANCE.foodDao().getFoodCount();
                                        Log.d("AppDatabase", "Initial food count: " + initialCount);

                                        // Call as instance method
                                        INSTANCE.populateInitialData(INSTANCE);

                                        int afterCount = INSTANCE.foodDao().getFoodCount();
                                        Log.d("AppDatabase", "Food count after population: " + afterCount);

                                        FoodAdviceInitializer.initializeData(INSTANCE);
                                        USDAFoodMappingInitializer.initializeData(INSTANCE);
                                        Log.d("AppDatabase", "Database initialization completed");
                                    });
                                }

                                @Override
                                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                    super.onOpen(db);
                                    Log.d("AppDatabase", "Database opened");
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    // Change this to instance method (remove 'static')
    private void populateInitialData(AppDatabase database) {
        // No more manual food entries!
        Log.d("AppDatabase", "No local foods - using USDA API exclusively");

        // Only initialize other data (advice, mappings, etc.)
        FoodAdviceInitializer.initializeData(database);
        USDAFoodMappingInitializer.initializeData(database);
    }
}