package com.example.in_gps.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.in_gps.R;
import com.example.in_gps.viewmodel.SensorDetailViewModel;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

public class SensorDetailFragment extends Fragment {

    private static final String ARG_DEVICE_ID = "device_id";

    private SensorDetailViewModel viewModel;
    private LineChart chart;
    private LineDataSet dataSetTemp1;
    private LineDataSet dataSetTemp2;
    private int pointIndex = 0;
    private TextView tvFrameTemp;
    private TextView tvAmbientTemp;

    public static SensorDetailFragment newInstance(String deviceId) {
        SensorDetailFragment fragment = new SensorDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DEVICE_ID, deviceId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sensor_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chart = view.findViewById(R.id.line_chart);
        tvFrameTemp = view.findViewById(R.id.tv_frame_temp);
        tvAmbientTemp = view.findViewById(R.id.tv_ambient_temp);

        setupChart();

        String deviceId = requireArguments().getString(ARG_DEVICE_ID);
        viewModel = new ViewModelProvider(this, new SensorDetailViewModel.Factory(deviceId))
                .get(SensorDetailViewModel.class);

        viewModel.getTemperatureData().observe(getViewLifecycleOwner(), data -> {
            tvFrameTemp.setText(data.temp1 + "°C");
            tvAmbientTemp.setText(data.temp2 + "°C");
            addPoint(data.temp1, data.temp2);
        });
    }

    private void setupChart() {
        dataSetTemp1 = new LineDataSet(new ArrayList<>(), "Temp1 (°C)");
        dataSetTemp1.setColor(Color.parseColor("#FF5722"));
        dataSetTemp1.setCircleColor(Color.parseColor("#FF5722"));
        dataSetTemp1.setLineWidth(2f);
        dataSetTemp1.setCircleRadius(4f);
        dataSetTemp1.setDrawCircleHole(false);
        dataSetTemp1.setDrawValues(true);
        dataSetTemp1.setValueTextSize(10f);
        dataSetTemp1.setValueTextColor(Color.DKGRAY);

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

        chart.getAxisLeft().setDrawGridLines(true);
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
