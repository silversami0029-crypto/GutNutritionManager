package com.example.gutnutritionmanager.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String BASE_URL = "https://api.nal.usda.gov/fdc/v1/";
    public static Retrofit retrofit = null;

    public static final String API_KEY = "MHsqz8Z1PHUyeyhKO3qUXak458oyhZbft4dzUYBo";

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static USDAApiService getUSDAApiService() {
        return getClient().create(USDAApiService.class);
    }

    public static USDAApiService getApiService() {
        return getClient().create(USDAApiService.class);
    }
}