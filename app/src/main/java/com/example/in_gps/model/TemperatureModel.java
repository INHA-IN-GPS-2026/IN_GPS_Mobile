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
    @SerializedName("created_at")
    public String createdAt;
}
