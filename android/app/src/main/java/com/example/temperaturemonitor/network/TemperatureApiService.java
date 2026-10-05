package com.example.temperaturemonitor.network;

import com.example.temperaturemonitor.model.TemperatureMeasurement;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface TemperatureApiService {
    @GET("cities")
    Call<List<String>> getAllLocations();

    @GET("history")
    Call<List<TemperatureMeasurement>> getTemperatureHistory(@Query("location") String location);

    @DELETE("delete")
    Call<Void> deleteLocation(@Query("location") String location);
}