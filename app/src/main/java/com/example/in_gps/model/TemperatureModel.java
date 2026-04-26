package com.example.in_gps.model;

import com.google.gson.annotations.SerializedName;

public class TemperatureModel {
    @SerializedName("id")
    public long id;
    @SerializedName("device_id")
    public String deviceId;
    @SerializedName("temp1")
    public float temp1;
    @SerializedName("temp2")
    public float temp2;
    @SerializedName("angle_x")
    public float angleX;
    @SerializedName("angle_y")
    public float angleY;
    @SerializedName("angle_z")
    public float angleZ;
    @SerializedName("event")
    public String event;
    @SerializedName("created_at")
    public String createdAt;

    // 일별 집계 응답 전용 (aggregated=true 일 때만 값 존재)
    @SerializedName("temp1_min")
    public Float temp1Min;
    @SerializedName("temp1_max")
    public Float temp1Max;
    @SerializedName("temp2_min")
    public Float temp2Min;
    @SerializedName("temp2_max")
    public Float temp2Max;
}
