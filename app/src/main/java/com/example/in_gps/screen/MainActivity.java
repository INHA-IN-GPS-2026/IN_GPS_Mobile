package com.example.in_gps.screen;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.in_gps.R;
import com.example.in_gps.api.RetrofitClient;
import com.example.in_gps.model.TemperatureModel;
import com.example.in_gps.model.TemperatureResponse;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private LineChart chart;
    private LineDataSet dataSetTemp1;
    private LineDataSet dataSetTemp2;
    private int pointIndex = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final long INTERVAL_MS = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        TextView test_Text = findViewById(R.id.test_Text);
        String deviceId = "esp_32";

        Button btnCube = findViewById(R.id.button_cube);
        btnCube.setOnClickListener(v ->
                startActivity(new Intent(this, CubeActivity.class)));

        chart = findViewById(R.id.Verifying_graph);
        setupChart();

        handler.post(new Runnable() {
            @Override
            public void run() {
                RetrofitClient.getInstance().getApiService()
                        .getTemperature(deviceId, 1)
                        .enqueue(new Callback<TemperatureResponse>() {
                            @Override
                            public void onResponse(Call<TemperatureResponse> call, Response<TemperatureResponse> response) {
                                if (response.isSuccessful() && response.body() != null
                                        && !response.body().items.isEmpty()) {
                                    TemperatureModel data = response.body().items.get(0);
                                    test_Text.setText(
                                            "Temp1: " + data.temp1 + "°C  |  Temp2: " + data.temp2 + "°C");
                                    addPoint(data.temp1, data.temp2);
                                }
                            }

                            @Override
                            public void onFailure(Call<TemperatureResponse> call, Throwable t) {
                                Log.e("ErrorCheck", t.getMessage());
                            }
                        });
                handler.postDelayed(this, INTERVAL_MS);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    private void setupChart() {
        // Temp1 — 주황/빨강
        dataSetTemp1 = new LineDataSet(new ArrayList<>(), "Temp1 (°C)");
        dataSetTemp1.setColor(Color.parseColor("#FF5722"));
        dataSetTemp1.setCircleColor(Color.parseColor("#FF5722"));
        dataSetTemp1.setLineWidth(2f);
        dataSetTemp1.setCircleRadius(4f);
        dataSetTemp1.setDrawCircleHole(false);
        dataSetTemp1.setDrawValues(true);
        dataSetTemp1.setValueTextSize(10f);
        dataSetTemp1.setValueTextColor(Color.DKGRAY);

        // Temp2 — 파랑
        dataSetTemp2 = new LineDataSet(new ArrayList<>(), "Temp2 (°C)");
        dataSetTemp2.setColor(Color.parseColor("#2196F3"));
        dataSetTemp2.setCircleColor(Color.parseColor("#2196F3"));
        dataSetTemp2.setLineWidth(2f);
        dataSetTemp2.setCircleRadius(4f);
        dataSetTemp2.setDrawCircleHole(false);
        dataSetTemp2.setDrawValues(true);
        dataSetTemp2.setValueTextSize(10f);
        dataSetTemp2.setValueTextColor(Color.DKGRAY);

        LineData lineData = new LineData(dataSetTemp1, dataSetTemp2);
        chart.setData(lineData);

        chart.setBackgroundColor(Color.WHITE);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(true);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(true);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        chart.getAxisRight().setEnabled(false);

        chart.invalidate();
    }

    private void addPoint(float temp1, float temp2) {
        dataSetTemp1.addEntry(new Entry(pointIndex, temp1));
        dataSetTemp2.addEntry(new Entry(pointIndex, temp2));
        pointIndex++;

        chart.getData().notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.setVisibleXRangeMaximum(10);
        chart.moveViewToX(pointIndex);
        chart.invalidate();
    }
}
