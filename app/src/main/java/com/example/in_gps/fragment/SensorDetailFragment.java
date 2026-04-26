package com.example.in_gps.fragment;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.in_gps.R;
import com.example.in_gps.model.TemperatureModel;
import com.example.in_gps.viewmodel.SensorDetailViewModel;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.chip.Chip;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SensorDetailFragment extends Fragment {

    private static final String ARG_DEVICE_ID = "device_id";
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
    private static final SimpleDateFormat FMT_TIME = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private static final SimpleDateFormat FMT_DATE = new SimpleDateFormat("M/dd", Locale.getDefault());

    private SensorDetailViewModel viewModel;
    private LineChart chart;
    private LineDataSet dataSetTemp1;
    private LineDataSet dataSetTemp2;
    private int pointIndex = 0;
    private TextView tvFrameTemp;
    private TextView tvAmbientTemp;
    private Chip chipEventStatus;

    // x값(분) → 시간 레이블 매핑
    private final ArrayList<String> xLabels = new ArrayList<>();

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
        chipEventStatus = view.findViewById(R.id.chip_ai_status);

        setupChart();

        String deviceId = requireArguments().getString(ARG_DEVICE_ID);
        viewModel = new ViewModelProvider(this, new SensorDetailViewModel.Factory(deviceId))
                .get(SensorDetailViewModel.class);

        // 최신 값 → 카드 + event 칩 업데이트
        viewModel.getTemperatureData().observe(getViewLifecycleOwner(), data -> {
            tvFrameTemp.setText(data.temp1 + "°C");
            tvAmbientTemp.setText(data.temp2 + "°C");
            updateEventChip(data.event);
        });

        // 기간 데이터 → 차트 전체 재그리기
        viewModel.getPeriodData().observe(getViewLifecycleOwner(), this::reloadChart);

        // 칩 클릭 → 기간 조회
        view.findViewById(R.id.chip_1d).setOnClickListener(v -> viewModel.loadPeriod(1));
        view.findViewById(R.id.chip_1w).setOnClickListener(v -> viewModel.loadPeriod(7));
        view.findViewById(R.id.chip_1m).setOnClickListener(v -> viewModel.loadPeriod(30));
        view.findViewById(R.id.chip_1y).setOnClickListener(v -> viewModel.loadPeriod(365));

        // 초기 로드 (1일)
        viewModel.loadPeriod(1);
    }

    private void setupChart() {
        dataSetTemp1 = makeDataSet("Temp1 (°C)", Color.parseColor("#FF5722"));
        dataSetTemp2 = makeDataSet("Temp2 (°C)", Color.parseColor("#2196F3"));
        chart.setData(new LineData(dataSetTemp1, dataSetTemp2));
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
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < xLabels.size()) return xLabels.get(index);
                return "";
            }
        });

        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisRight().setEnabled(false);
        chart.invalidate();
    }

    private void reloadChart(List<TemperatureModel> items) {
        // viewport 리셋: 이전 기간 뷰의 스크롤 위치가 남아있으면 새 데이터가 범위 밖으로 밀림
        chart.clear();
        xLabels.clear();
        pointIndex = 0;

        LineDataSet newSet1 = makeDataSet("Temp1 (°C)", Color.parseColor("#FF5722"));
        LineDataSet newSet2 = makeDataSet("Temp2 (°C)", Color.parseColor("#2196F3"));

        if (items != null && !items.isEmpty()) {
            List<TemperatureModel> ordered = new ArrayList<>(items);
            java.util.Collections.reverse(ordered); // 최신순 → 오래된순

            Date firstDate = parseDate(ordered.get(0).createdAt);
            Date lastDate  = parseDate(ordered.get(ordered.size() - 1).createdAt);
            long spanDays  = (lastDate.getTime() - firstDate.getTime()) / 86_400_000L;

            for (TemperatureModel item : ordered) {
                Date date = parseDate(item.createdAt);
                newSet1.addEntry(new Entry(pointIndex, item.temp1));
                newSet2.addEntry(new Entry(pointIndex, item.temp2));
                xLabels.add(spanDays <= 2 ? FMT_TIME.format(date) : FMT_DATE.format(date));
                pointIndex++;
            }
        }

        dataSetTemp1 = newSet1;
        dataSetTemp2 = newSet2;
        chart.setData(new LineData(newSet1, newSet2));
        chart.fitScreen();
        chart.setVisibleXRangeMaximum(20);
        chart.moveViewToX(Math.max(0, pointIndex - 20));
        chart.invalidate();
    }

    private LineDataSet makeDataSet(String label, int color) {
        LineDataSet ds = new LineDataSet(new ArrayList<>(), label);
        ds.setColor(color);
        ds.setCircleColor(color);
        ds.setLineWidth(2f);
        ds.setCircleRadius(3f);
        ds.setDrawCircleHole(false);
        ds.setDrawValues(false);
        return ds;
    }

    private Date parseDate(String createdAt) {
        if (createdAt == null) return new Date();
        try {
            return DATE_FORMAT.parse(createdAt);
        } catch (ParseException e) {
            return new Date();
        }
    }

private void updateEventChip(String event) {
        if (event == null) return;
        int bgColor, textColor;
        String label;
        switch (event) {
            case "warning":
                bgColor = R.color.color_warning_bg;
                textColor = R.color.color_warning_text;
                label = "경고";
                break;
            case "critical":
                bgColor = R.color.color_critical_bg;
                textColor = R.color.color_critical_text;
                label = "위험";
                break;
            default:
                bgColor = R.color.color_normal_bg;
                textColor = R.color.color_normal_text;
                label = "정상";
                break;
        }
        chipEventStatus.setText(label);
        chipEventStatus.setChipBackgroundColor(
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), bgColor)));
        chipEventStatus.setTextColor(ContextCompat.getColor(requireContext(), textColor));
    }
}
