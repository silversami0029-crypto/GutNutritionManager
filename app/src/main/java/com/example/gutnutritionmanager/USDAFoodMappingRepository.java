package com.example.gutnutritionmanager;


import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class USDAFoodMappingRepository {
    private USDAFoodMappingDao usdaFoodMappingDao;
    private ExecutorService executorService;

    public USDAFoodMappingRepository(Application application) {
        AppDatabase database = AppDatabase.getDatabase(application);
        this.usdaFoodMappingDao = database.usdaFoodMappingDao();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void insert(USDAFoodMapping mapping) {
        executorService.execute(() -> {
            usdaFoodMappingDao.insert(mapping);
        });
    }

    public void update(USDAFoodMapping mapping) {
        executorService.execute(() -> {
            usdaFoodMappingDao.update(mapping);
        });
    }

    public String getAdviceFoodName(String usdaName) {
        try {
            return usdaFoodMappingDao.getAdviceFoodName(usdaName);
        } catch (Exception e) {
            return null;
        }
    }

    public List<USDAFoodMapping> getAllMappings() {
        try {
            return usdaFoodMappingDao.getAllMappings();
        } catch (Exception e) {
            return null;
        }
    }

    public List<USDAFoodMapping> findMatchingUSDAFoods(String pattern) {
        try {
            return usdaFoodMappingDao.findMatchingUSDAFoods("%" + pattern + "%");
        } catch (Exception e) {
            return null;
        }
    }

    public void deleteByUsdaName(String usdaName) {
        executorService.execute(() -> {
            usdaFoodMappingDao.deleteByUsdaName(usdaName);
        });
    }

    public int getMappingCount() {
        try {
            return usdaFoodMappingDao.getMappingCount();
        } catch (Exception e) {
            return 0;
        }
    }
}