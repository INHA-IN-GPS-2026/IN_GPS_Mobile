package com.example.in_gps;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

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
    private LineDataSet dataSet;
    private int pointIndex = 0;
    private java.util.List<DeviceLogModel> logItems = new ArrayList<>();
    private int currentItemIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Button get_button = findViewById(R.id.button_check);
        TextView test_Text = findViewById(R.id.test_Text);
        String deviceId = "DEV_2001";

        // 차트 초기화
        chart = findViewById(R.id.Verifying_graph);
        setupChart();

        // 버튼 누를 때마다 최신 데이터를 가져와서 다음 항목 추가
        get_button.setOnClickListener(v -> {
            RetrofitClient.getInstance().getApiService()
                    .getDeviceLogs(deviceId, 100)
                    .enqueue(new Callback<DeviceLogResponse>() {
                        @Override
                        public void onResponse(Call<DeviceLogResponse> call, Response<DeviceLogResponse> response) {
                            if (response.isSuccessful() && response.body() != null
                                    && !response.body().items.isEmpty()) {
                                java.util.List<DeviceLogModel> fetched = response.body().items;
                                java.util.Collections.reverse(fetched);
                                // 새로 추가된 항목만 뒤에 붙이기
                                if (fetched.size() > logItems.size()) {
                                    logItems = fetched;
                                }

                                runOnUiThread(() -> {
                                    if (currentItemIndex >= logItems.size()) {
                                        test_Text.setText("모든 데이터 표시 완료 (" + logItems.size() + "개)");
                                        return;
                                    }
                                    double coreTemp = logItems.get(currentItemIndex).tempCoreC;
                                    test_Text.setText("Core Temp: " + coreTemp + "°C  (" + (currentItemIndex + 1) + "/" + logItems.size() + ")");
                                    addPoint((float) coreTemp);
                                    currentItemIndex++;
                                });
                            }
                        }

                        @Override
                        public void onFailure(Call<DeviceLogResponse> call, Throwable t) {
                            Log.e("ErrorCheck", t.getMessage());
                        }
                    });
        });
    }

    private void setupChart() {
        dataSet = new LineDataSet(new ArrayList<>(), "Core Temp (°C)");
        dataSet.setColor(Color.parseColor("#FF5722"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(Color.parseColor("#FF5722"));
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(false);
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.DKGRAY);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        // 배경 설정
        chart.setBackgroundColor(Color.WHITE);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(true);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);

        // X축
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(true);

        // Y축
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        chart.getAxisRight().setEnabled(false);

        chart.invalidate();
    }

    private void addPoint(float temp) {
        dataSet.addEntry(new Entry(pointIndex, temp));
        pointIndex++;

        // X축 범위를 데이터에 맞게 자동 조정
        chart.getData().notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.setVisibleXRangeMaximum(10);
        chart.moveViewToX(dataSet.getEntryCount());
        chart.invalidate();
    }
}
