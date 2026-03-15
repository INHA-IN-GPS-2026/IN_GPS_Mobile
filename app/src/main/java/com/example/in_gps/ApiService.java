package com.example.in_gps;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @GET("devices")
    Call<DeviceModelResponse>getDevices();

    @GET("devices/{device_id}/logs")
    Call<DeviceLogResponse> getDeviceLogs(
            @Path("device_id") String deviceId,
            @Query("limit") int limit
    );
}
