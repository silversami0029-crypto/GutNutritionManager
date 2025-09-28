package com.example.gutnutritionmanager.api;


import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;


public interface USDAApiService {
    @GET("foods/search")
    Call<USDAFoodSearchResponse> searchFoods(
            @Query("api_key") String apiKey,
            @Query("query") String query,
            @Query("pageSize") int pageSize,
            @Query("pageNumber") int pageNumber
    );

    @GET("food")
    Call<USDAFood> getFood(
            @Query("api_key") String apiKey,
            @Query("fdcId") int fdcId
    );
}