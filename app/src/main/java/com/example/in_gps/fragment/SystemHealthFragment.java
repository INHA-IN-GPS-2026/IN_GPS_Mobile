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
import com.example.in_gps.viewmodel.SystemHealthViewModel;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;
import java.util.List;

public class SystemHealthFragment extends Fragment {

    private SystemHealthViewModel viewModel;
    private PieChart pieChart;
    private TextView tvCountNormal;
    private TextView tvCountWarning;
    private TextView tvCountCritical;
    private TextView tvTotalDevices;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_system_health, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pieChart = view.findViewById(R.id.pie_chart);
        tvCountNormal = view.findViewById(R.id.tv_count_normal);
        tvCountWarning = view.findViewById(R.id.tv_count_warning);
        tvCountCritical = view.findViewById(R.id.tv_count_critical);
        tvTotalDevices = view.findViewById(R.id.tv_total_devices);

        setupPieChart();

        viewModel = new ViewModelProvider(this).get(SystemHealthViewModel.class);
        viewModel.getDeviceStats().observe(getViewLifecycleOwner(), stats -> {
            tvCountNormal.setText(String.valueOf(stats.normal));
            tvCountWarning.setText(String.valueOf(stats.warning));
            tvCountCritical.setText(String.valueOf(stats.critical));
            tvTotalDevices.setText(String.valueOf(stats.total));
            updatePieChart(stats);
        });
    }

    private void setupPieChart() {
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(45f);
        pieChart.setDrawEntryLabels(false);
        pieChart.getLegend().setEnabled(true);
        pieChart.setUsePercentValues(false);
    }

    private void updatePieChart(SystemHealthViewModel.DeviceStats stats) {
        List<PieEntry> entries = new ArrayList<>();
        if (stats.normal > 0) entries.add(new PieEntry(stats.normal, "정상"));
        if (stats.warning > 0) entries.add(new PieEntry(stats.warning, "경고"));
        if (stats.critical > 0) entries.add(new PieEntry(stats.critical, "위험"));

        if (entries.isEmpty()) {
            pieChart.clear();
            pieChart.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(
                Color.parseColor("#34A853"),
                Color.parseColor("#FBBC04"),
                Color.parseColor("#EA4335")
        );
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        pieChart.setData(new PieData(dataSet));
        pieChart.invalidate();
    }
}
