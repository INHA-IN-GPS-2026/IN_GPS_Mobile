package com.example.in_gps;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DeviceLogResponse {
    @SerializedName("items")
    public List<DeviceLogModel> items;
}