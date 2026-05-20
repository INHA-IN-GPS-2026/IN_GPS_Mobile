package com.example.in_gps.fragment;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.utils.Transformer;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SensorDetailFragment extends Fragment {

    private static final String ARG_DEVICE_ID = "device_id";

    private static final SimpleDateFormat FMT_DATE  = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat FMT_MONTH = new SimpleDateFormat("yyyy-MM",    Locale.US);

    private static final int COLOR_FRAME   = Color.parseColor("#FF5722");
    private static final int COLOR_AMBIENT = Color.parseColor("#2196F3");
    private static final int COLOR_AXIS    = Color.parseColor("#9E9E9E");
    private static final int COLOR_NOW     = Color.parseColor("#E53935");
    private static final int COLOR_WARN    = Color.parseColor("#FF6D00");
    private static final int COLOR_DISC    = Color.parseColor("#757575");

    // 범위 인디케이터: 회색 점선 + 양 끝의 짧은 가로선(캡)
    private static final int COLOR_RANGE_DASH = Color.parseColor("#9E9E9E");

    private static final String[] DAY_OF_WEEK = {"일", "월", "화", "수", "목", "금", "토"};
    private static final String[] MONTH_NAMES = {
        "1월","2월","3월","4월","5월","6월","7월","8월","9월","10월","11월","12월"
    };

    private SensorDetailViewModel viewModel;
    private LineChart chart;
    private RangeOverlay rangeOverlay;
    private TextView tvFrameTemp, tvAmbientTemp, tvChartPeriodLabel;
    private Chip chipEventStatus;

    private final ArrayList<String> xLabels = new ArrayList<>();
    private int currentDays = 1;

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

        chart              = view.findViewById(R.id.line_chart);
        tvFrameTemp        = view.findViewById(R.id.tv_frame_temp);
        tvAmbientTemp      = view.findViewById(R.id.tv_ambient_temp);
        chipEventStatus    = view.findViewById(R.id.chip_ai_status);
        tvChartPeriodLabel = view.findViewById(R.id.tv_chart_period_label);

        setupChart();
        setupRangeOverlay();

        String deviceId = requireArguments().getString(ARG_DEVICE_ID);
        viewModel = new ViewModelProvider(this, new SensorDetailViewModel.Factory(deviceId))
                .get(SensorDetailViewModel.class);

        viewModel.getTemperatureData().observe(getViewLifecycleOwner(), data -> {
            tvFrameTemp.setText(String.format(Locale.getDefault(), "%.1f°C", data.temp1));
            tvAmbientTemp.setText(String.format(Locale.getDefault(), "%.1f°C", data.temp2));
            updateEventChip(data.event);
        });

        viewModel.getPeriodData().observe(getViewLifecycleOwner(), this::reloadChart);

        view.findViewById(R.id.chip_1d).setOnClickListener(v -> loadPeriod(1));
        view.findViewById(R.id.chip_1w).setOnClickListener(v -> loadPeriod(7));
        view.findViewById(R.id.chip_1m).setOnClickListener(v -> loadPeriod(30));
        view.findViewById(R.id.chip_1y).setOnClickListener(v -> loadPeriod(365));

        loadPeriod(1);
    }

    private void loadPeriod(int days) {
        currentDays = days;
        viewModel.loadPeriod(days);
    }

    // ── 차트 초기 설정 ──────────────────────────────────────────────

    private void setupChart() {
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setTextColor(COLOR_AXIS);
        chart.getLegend().setTextSize(11f);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setDrawBorders(false);
        chart.setExtraBottomOffset(8f);
        chart.setNoDataText("데이터를 불러오는 중...");
        chart.getLegend().setEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setTextColor(COLOR_AXIS);
        xAxis.setTextSize(10f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int i = Math.round(value);
                return (i >= 0 && i < xLabels.size()) ? xLabels.get(i) : "";
            }
        });

        YAxis yAxis = chart.getAxisLeft();
        yAxis.setDrawGridLines(false);
        yAxis.setDrawAxisLine(false);
        yAxis.setTextColor(COLOR_AXIS);
        yAxis.setTextSize(10f);
        yAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + "°";
            }
        });

        chart.getAxisRight().setEnabled(false);
    }

    // ── 차트 전체 재구성 ────────────────────────────────────────────

    private void reloadChart(List<TemperatureModel> items) {
        chart.clear();
        xLabels.clear();

        LineDataSet set1    = makeLineDataSet("", COLOR_FRAME);
        LineDataSet set1Max = makeLineDataSet("", COLOR_FRAME);
        LineDataSet set1Min = makeLineDataSet("", COLOR_FRAME);
        LineDataSet set2    = makeLineDataSet("", COLOR_AMBIENT);
        LineDataSet set2Max = makeLineDataSet("", COLOR_AMBIENT);
        LineDataSet set2Min = makeLineDataSet("", COLOR_AMBIENT);
        LineDataSet setWarn = makeEventDataSet("경고",     COLOR_WARN);
        LineDataSet setDisc = makeEventDataSet("연결 끊김", COLOR_DISC);

        int totalSlots;
        if (currentDays == 1) {
            totalSlots = 24;
            buildHourlySlots(items, set1, set1Max, set1Min, set2, set2Max, set2Min, setWarn, setDisc);
        } else if (currentDays == 7) {
            totalSlots = 7;
            buildDailySlots(items, set1, set1Max, set1Min, set2, set2Max, set2Min, setWarn, setDisc);
        } else if (currentDays <= 31) {
            totalSlots = 5;
            buildWeeklySlots(items, set1, set1Max, set1Min, set2, set2Max, set2Min, setWarn, setDisc);
        } else {
            totalSlots = 12;
            buildMonthlySlots(items, set1, set1Max, set1Min, set2, set2Max, set2Min, setWarn, setDisc);
        }

        XAxis xAxis = chart.getXAxis();
        xAxis.removeAllLimitLines();
        if (currentDays == 1) {
            xAxis.setLabelCount(8, true);
        } else if (currentDays == 7) {
            xAxis.setLabelCount(7, true);
        } else if (currentDays <= 31) {
            xAxis.setLabelCount(5, true);
        } else {
            xAxis.setLabelCount(12, true);
        }

        addNowIndicator(xAxis, totalSlots);

        if (tvChartPeriodLabel != null) {
            String label;
            switch (currentDays) {
                case 1:   label = "오늘 24시간 · 시간별 평균/최고/최저"; break;
                case 7:   label = "최근 7일 · 일별 평균/최고/최저";      break;
                case 30:  label = "최근 30일 · 주별 평균/최고/최저";     break;
                default:  label = "최근 12개월 · 월별 평균/최고/최저";   break;
            }
            tvChartPeriodLabel.setText(label);
        }

        chart.getXAxis().setAxisMinimum(-0.5f);
        chart.getXAxis().setAxisMaximum(totalSlots - 0.5f);

        applyYAxisRange(set1Max, set1Min, set2Max, set2Min);

        int splineSteps = (currentDays == 1) ? 4 : 10;

        LineDataSet smoothAvg1 = toSmoothedSet(set1, "코어 온도", COLOR_FRAME,   splineSteps);
        LineDataSet smoothAvg2 = toSmoothedSet(set2, "표면 온도", COLOR_AMBIENT, splineSteps);

        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(smoothAvg1);
        dataSets.add(smoothAvg2);
        if (setWarn.getEntryCount() > 0) dataSets.add(setWarn);
        if (setDisc.getEntryCount() > 0) dataSets.add(setDisc);

        chart.setData(new LineData(dataSets));
        chart.invalidate();

        if (rangeOverlay != null) {
            rangeOverlay.update(set1Max, set1Min, set2Max, set2Min);
        }
    }

    // ── 현재 시점 LimitLine ─────────────────────────────────────────

    private void addNowIndicator(XAxis xAxis, int totalSlots) {
        float nowX;
        String label;

        if (currentDays == 1) {
            Calendar now = Calendar.getInstance();
            nowX  = now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE) / 60f;
            label = String.format(Locale.getDefault(), "%02d:%02d",
                    now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
        } else {
            nowX  = totalSlots - 1;
            label = "오늘";
        }

        LimitLine nowLine = new LimitLine(nowX, label);
        nowLine.setLineColor(COLOR_NOW);
        nowLine.setLineWidth(1.5f);
        nowLine.enableDashedLine(8f, 6f, 0f);
        nowLine.setLabelPosition(LimitLine.LimitLabelPosition.LEFT_TOP);
        nowLine.setTextColor(COLOR_NOW);
        nowLine.setTextSize(9f);

        xAxis.addLimitLine(nowLine);
        xAxis.setDrawLimitLinesBehindData(false);
    }

    // ── 슬롯 빌더 ──────────────────────────────────────────────────

    /** 1일 뷰: 시간별 avg/max/min (raw 데이터) */
    private void buildHourlySlots(List<TemperatureModel> items,
                                  LineDataSet set1, LineDataSet set1Max, LineDataSet set1Min,
                                  LineDataSet set2, LineDataSet set2Max, LineDataSet set2Min,
                                  LineDataSet setWarn, LineDataSet setDisc) {
        float[]   sum1 = new float[24], sum2 = new float[24];
        float[]   max1 = new float[24], max2 = new float[24];
        float[]   min1 = new float[24], min2 = new float[24];
        int[]     cnt  = new int[24];
        boolean[] hasData    = new boolean[24];
        String[]  worstEvent = new String[24];

        if (items != null) {
            for (TemperatureModel item : items) {
                if (item.createdAt == null || item.createdAt.length() < 13) continue;
                int h;
                try { h = Integer.parseInt(item.createdAt.substring(11, 13)); }
                catch (NumberFormatException e) { continue; }
                if (h < 0 || h >= 24) continue;
                sum1[h] += item.temp1;
                sum2[h] += item.temp2;
                if (!hasData[h] || item.temp1 > max1[h]) max1[h] = item.temp1;
                if (!hasData[h] || item.temp1 < min1[h]) min1[h] = item.temp1;
                if (!hasData[h] || item.temp2 > max2[h]) max2[h] = item.temp2;
                if (!hasData[h] || item.temp2 < min2[h]) min2[h] = item.temp2;
                hasData[h] = true;
                cnt[h]++;
                worstEvent[h] = worstOf(worstEvent[h], item.event);
            }
        }

        for (int h = 0; h < 24; h++) {
            xLabels.add(String.format(Locale.getDefault(), "%02d:00", h));
            if (hasData[h]) {
                float avg1 = sum1[h] / cnt[h];
                float avg2 = sum2[h] / cnt[h];
                set1.addEntry(new Entry(h, avg1));
                set1Max.addEntry(new Entry(h, max1[h]));
                set1Min.addEntry(new Entry(h, min1[h]));
                set2.addEntry(new Entry(h, avg2));
                set2Max.addEntry(new Entry(h, max2[h]));
                set2Min.addEntry(new Entry(h, min2[h]));
                if ("disconnected".equals(worstEvent[h])) {
                    setDisc.addEntry(new Entry(h, avg1));
                } else if ("warning".equals(worstEvent[h])) {
                    setWarn.addEntry(new Entry(h, avg1));
                }
            }
        }
    }

    /** 1주 뷰: 일별 avg/max/min (raw 데이터) */
    private void buildDailySlots(List<TemperatureModel> items,
                                 LineDataSet set1, LineDataSet set1Max, LineDataSet set1Min,
                                 LineDataSet set2, LineDataSet set2Max, LineDataSet set2Min,
                                 LineDataSet setWarn, LineDataSet setDisc) {
        final int days = 7;
        float[]   sum1 = new float[days], sum2 = new float[days];
        float[]   max1 = new float[days], max2 = new float[days];
        float[]   min1 = new float[days], min2 = new float[days];
        int[]     cnt  = new int[days];
        boolean[] hasData    = new boolean[days];
        String[]  worstEvent = new String[days];

        Map<String, Integer> dateSlot = new HashMap<>();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);      c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.DAY_OF_YEAR, -(days - 1));
        for (int i = 0; i < days; i++) {
            dateSlot.put(FMT_DATE.format(c.getTime()), i);
            c.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (items != null) {
            for (TemperatureModel item : items) {
                if (item.createdAt == null || item.createdAt.length() < 10) continue;
                Integer slot = dateSlot.get(item.createdAt.substring(0, 10));
                if (slot == null) continue;
                sum1[slot] += item.temp1;
                sum2[slot] += item.temp2;
                if (!hasData[slot] || item.temp1 > max1[slot]) max1[slot] = item.temp1;
                if (!hasData[slot] || item.temp1 < min1[slot]) min1[slot] = item.temp1;
                if (!hasData[slot] || item.temp2 > max2[slot]) max2[slot] = item.temp2;
                if (!hasData[slot] || item.temp2 < min2[slot]) min2[slot] = item.temp2;
                hasData[slot] = true;
                cnt[slot]++;
                worstEvent[slot] = worstOf(worstEvent[slot], item.event);
            }
        }

        c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);      c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.DAY_OF_YEAR, -(days - 1));

        for (int i = 0; i < days; i++) {
            xLabels.add(DAY_OF_WEEK[c.get(Calendar.DAY_OF_WEEK) - 1]);
            if (hasData[i]) {
                float avg1 = sum1[i] / cnt[i];
                float avg2 = sum2[i] / cnt[i];
                set1.addEntry(new Entry(i, avg1));
                set1Max.addEntry(new Entry(i, max1[i]));
                set1Min.addEntry(new Entry(i, min1[i]));
                set2.addEntry(new Entry(i, avg2));
                set2Max.addEntry(new Entry(i, max2[i]));
                set2Min.addEntry(new Entry(i, min2[i]));
                if ("disconnected".equals(worstEvent[i])) {
                    setDisc.addEntry(new Entry(i, avg1));
                } else if ("warning".equals(worstEvent[i])) {
                    setWarn.addEntry(new Entry(i, avg1));
                }
            }
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    /** 1개월 뷰: 주별 avg/max/min (서버 일별 집계 → 클라이언트에서 주별 집계) */
    private void buildWeeklySlots(List<TemperatureModel> items,
                                  LineDataSet set1, LineDataSet set1Max, LineDataSet set1Min,
                                  LineDataSet set2, LineDataSet set2Max, LineDataSet set2Min,
                                  LineDataSet setWarn, LineDataSet setDisc) {
        final int weeks = 5;
        float[]   sumAvg1 = new float[weeks], sumAvg2 = new float[weeks];
        float[]   max1    = new float[weeks], max2    = new float[weeks];
        float[]   min1    = new float[weeks], min2    = new float[weeks];
        int[]     cnt     = new int[weeks];
        boolean[] hasData    = new boolean[weeks];
        String[]  worstEvent = new String[weeks];

        // date → day_index (0..29)
        Map<String, Integer> dateDay = new HashMap<>();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);      c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.DAY_OF_YEAR, -29);
        for (int d = 0; d < 30; d++) {
            dateDay.put(FMT_DATE.format(c.getTime()), d);
            c.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (items != null) {
            for (TemperatureModel item : items) {
                if (item.createdAt == null || item.createdAt.length() < 10) continue;
                Integer day = dateDay.get(item.createdAt.substring(0, 10));
                if (day == null) continue;
                int slot = Math.min(day / 7, weeks - 1);

                float v1avg = item.temp1;
                float v1max = item.temp1Max != null ? item.temp1Max : item.temp1;
                float v1min = item.temp1Min != null ? item.temp1Min : item.temp1;
                float v2avg = item.temp2;
                float v2max = item.temp2Max != null ? item.temp2Max : item.temp2;
                float v2min = item.temp2Min != null ? item.temp2Min : item.temp2;

                sumAvg1[slot] += v1avg;
                sumAvg2[slot] += v2avg;
                if (!hasData[slot] || v1max > max1[slot]) max1[slot] = v1max;
                if (!hasData[slot] || v1min < min1[slot]) min1[slot] = v1min;
                if (!hasData[slot] || v2max > max2[slot]) max2[slot] = v2max;
                if (!hasData[slot] || v2min < min2[slot]) min2[slot] = v2min;
                hasData[slot] = true;
                cnt[slot]++;
                worstEvent[slot] = worstOf(worstEvent[slot], item.event);
            }
        }

        SimpleDateFormat dateFmt = new SimpleDateFormat("M/d", Locale.getDefault());
        c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);      c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.DAY_OF_YEAR, -29);

        for (int i = 0; i < weeks; i++) {
            xLabels.add(dateFmt.format(c.getTime()));
            if (hasData[i]) {
                float avg1 = sumAvg1[i] / cnt[i];
                float avg2 = sumAvg2[i] / cnt[i];
                set1.addEntry(new Entry(i, avg1));
                set1Max.addEntry(new Entry(i, max1[i]));
                set1Min.addEntry(new Entry(i, min1[i]));
                set2.addEntry(new Entry(i, avg2));
                set2Max.addEntry(new Entry(i, max2[i]));
                set2Min.addEntry(new Entry(i, min2[i]));
                if ("disconnected".equals(worstEvent[i])) {
                    setDisc.addEntry(new Entry(i, avg1));
                } else if ("warning".equals(worstEvent[i])) {
                    setWarn.addEntry(new Entry(i, avg1));
                }
            }
            c.add(Calendar.DAY_OF_YEAR, 7);
        }
    }

    /** 1년 뷰: 월별 avg/max/min (서버 일별 집계 → 클라이언트에서 월별 집계) */
    private void buildMonthlySlots(List<TemperatureModel> items,
                                   LineDataSet set1, LineDataSet set1Max, LineDataSet set1Min,
                                   LineDataSet set2, LineDataSet set2Max, LineDataSet set2Min,
                                   LineDataSet setWarn, LineDataSet setDisc) {
        float[]   sumAvg1 = new float[12], sumAvg2 = new float[12];
        float[]   max1    = new float[12], max2    = new float[12];
        float[]   min1    = new float[12], min2    = new float[12];
        int[]     cnt     = new int[12];
        boolean[] hasData    = new boolean[12];
        String[]  worstEvent = new String[12];

        Map<String, Integer> monthSlot = new HashMap<>();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);      c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.MONTH, -11);
        for (int i = 0; i < 12; i++) {
            monthSlot.put(FMT_MONTH.format(c.getTime()), i);
            c.add(Calendar.MONTH, 1);
        }

        if (items != null) {
            for (TemperatureModel item : items) {
                if (item.createdAt == null || item.createdAt.length() < 7) continue;
                Integer slot = monthSlot.get(item.createdAt.substring(0, 7));
                if (slot == null) continue;

                float v1avg = item.temp1;
                float v1max = item.temp1Max != null ? item.temp1Max : item.temp1;
                float v1min = item.temp1Min != null ? item.temp1Min : item.temp1;
                float v2avg = item.temp2;
                float v2max = item.temp2Max != null ? item.temp2Max : item.temp2;
                float v2min = item.temp2Min != null ? item.temp2Min : item.temp2;

                sumAvg1[slot] += v1avg;
                sumAvg2[slot] += v2avg;
                if (!hasData[slot] || v1max > max1[slot]) max1[slot] = v1max;
                if (!hasData[slot] || v1min < min1[slot]) min1[slot] = v1min;
                if (!hasData[slot] || v2max > max2[slot]) max2[slot] = v2max;
                if (!hasData[slot] || v2min < min2[slot]) min2[slot] = v2min;
                hasData[slot] = true;
                cnt[slot]++;
                worstEvent[slot] = worstOf(worstEvent[slot], item.event);
            }
        }

        c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.add(Calendar.MONTH, -11);
        for (int i = 0; i < 12; i++) {
            xLabels.add(MONTH_NAMES[c.get(Calendar.MONTH)]);
            if (hasData[i]) {
                float avg1 = sumAvg1[i] / cnt[i];
                float avg2 = sumAvg2[i] / cnt[i];
                set1.addEntry(new Entry(i, avg1));
                set1Max.addEntry(new Entry(i, max1[i]));
                set1Min.addEntry(new Entry(i, min1[i]));
                set2.addEntry(new Entry(i, avg2));
                set2Max.addEntry(new Entry(i, max2[i]));
                set2Min.addEntry(new Entry(i, min2[i]));
                if ("disconnected".equals(worstEvent[i])) {
                    setDisc.addEntry(new Entry(i, avg1));
                } else if ("warning".equals(worstEvent[i])) {
                    setWarn.addEntry(new Entry(i, avg1));
                }
            }
            c.add(Calendar.MONTH, 1);
        }
    }

    // ── Y축 범위 보정 ───────────────────────────────────────────────

    /**
     * 평균(avg)뿐 아니라 min/max를 모두 포함하도록 Y축 범위를 명시적으로 설정.
     * - 데이터 전체 범위가 너무 좁으면(예: 1°C 미만) 강제로 일정 높이를 확보해
     *   min/max 캡이 시각적으로 분리되어 보이도록 한다.
     * - 위아래로 패딩을 줘서 캡이 차트 가장자리에 붙지 않게 한다.
     */
    private void applyYAxisRange(LineDataSet set1Max, LineDataSet set1Min,
                                 LineDataSet set2Max, LineDataSet set2Min) {
        YAxis yAxis = chart.getAxisLeft();

        float globalMax = -Float.MAX_VALUE;
        float globalMin =  Float.MAX_VALUE;
        boolean hasAny = false;

        for (LineDataSet ds : new LineDataSet[]{set1Max, set2Max}) {
            for (Entry e : ds.getValues()) {
                if (e.getY() > globalMax) globalMax = e.getY();
                hasAny = true;
            }
        }
        for (LineDataSet ds : new LineDataSet[]{set1Min, set2Min}) {
            for (Entry e : ds.getValues()) {
                if (e.getY() < globalMin) globalMin = e.getY();
                hasAny = true;
            }
        }

        if (!hasAny) {
            yAxis.resetAxisMinimum();
            yAxis.resetAxisMaximum();
            return;
        }

        float range = globalMax - globalMin;
        // 데이터 변동 폭이 좁으면 최소 8°C 범위로 확장 (min/max 캡 시인성 확보)
        final float MIN_RANGE = 8f;
        if (range < MIN_RANGE) {
            float center = (globalMax + globalMin) / 2f;
            globalMax = center + MIN_RANGE / 2f;
            globalMin = center - MIN_RANGE / 2f;
            range = MIN_RANGE;
        }

        // 위아래 15% 여백
        float pad = range * 0.15f;
        yAxis.setAxisMinimum(globalMin - pad);
        yAxis.setAxisMaximum(globalMax + pad);
    }

    // ── 공용 유틸 ───────────────────────────────────────────────────

    private LineDataSet makeLineDataSet(String label, int color) {
        LineDataSet ds = new LineDataSet(new ArrayList<>(), label);
        ds.setColor(color);
        ds.setLineWidth(2f);
        ds.setDrawCircles(false);
        ds.setDrawValues(false);
        ds.setMode(LineDataSet.Mode.LINEAR);
        return ds;
    }

    /** avg 라인: Catmull-Rom 스플라인 + 실선 */
    private LineDataSet toSmoothedSet(LineDataSet src, String label, int color, int steps) {
        List<Entry> smooth = catmullRomSpline(src.getValues(), steps);
        LineDataSet ds = new LineDataSet(smooth, label);
        ds.setColor(color);
        ds.setLineWidth(2f);
        ds.setDrawCircles(false);
        ds.setDrawValues(false);
        ds.setMode(LineDataSet.Mode.LINEAR);
        return ds;
    }

    /** Catmull-Rom 스플라인 보간 */
    private List<Entry> catmullRomSpline(List<Entry> pts, int steps) {
        int n = pts.size();
        if (n < 2) return new ArrayList<>(pts);
        List<Entry> out = new ArrayList<>((n - 1) * steps + 1);
        for (int i = 0; i < n - 1; i++) {
            Entry p0 = pts.get(Math.max(0, i - 1));
            Entry p1 = pts.get(i);
            Entry p2 = pts.get(i + 1);
            Entry p3 = pts.get(Math.min(n - 1, i + 2));
            for (int s = 0; s < steps; s++) {
                float t  = (float) s / steps;
                float t2 = t * t, t3 = t2 * t;
                float y = 0.5f * (2 * p1.getY()
                        + (-p0.getY() + p2.getY()) * t
                        + (2 * p0.getY() - 5 * p1.getY() + 4 * p2.getY() - p3.getY()) * t2
                        + (-p0.getY() + 3 * p1.getY() - 3 * p2.getY() + p3.getY()) * t3);
                float x = p1.getX() + (p2.getX() - p1.getX()) * t;
                out.add(new Entry(x, y));
            }
        }
        out.add(new Entry(pts.get(n - 1).getX(), pts.get(n - 1).getY()));
        return out;
    }

    /** 이벤트 마커 전용 데이터셋 */
    private LineDataSet makeEventDataSet(String label, int color) {
        LineDataSet ds = new LineDataSet(new ArrayList<>(), label);
        ds.setColor(Color.TRANSPARENT);
        ds.setLineWidth(0f);
        ds.setDrawCircles(true);
        ds.setCircleColor(color);
        ds.setCircleRadius(7f);
        ds.setDrawCircleHole(true);
        ds.setCircleHoleRadius(3.5f);
        ds.setCircleHoleColor(Color.WHITE);
        ds.setDrawValues(false);
        return ds;
    }

    /** 두 이벤트 중 더 심각한 것 반환 */
    private String worstOf(String a, String b) {
        if ("warning".equals(a)      || "warning".equals(b))      return "warning";
        if ("disconnected".equals(a) || "disconnected".equals(b)) return "disconnected";
        return "normal";
    }

    private void updateEventChip(String event) {
        if (event == null) return;
        int bgColor, textColor;
        String label;
        switch (event) {
            case "warning":
                bgColor   = R.color.color_warning_bg;
                textColor = R.color.color_warning_text;
                label     = "경고";
                break;
            case "critical":
                bgColor   = R.color.color_critical_bg;
                textColor = R.color.color_critical_text;
                label     = "위험";
                break;
            default:
                bgColor   = R.color.color_normal_bg;
                textColor = R.color.color_normal_text;
                label     = "정상";
                break;
        }
        chipEventStatus.setText(label);
        chipEventStatus.setChipBackgroundColor(
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), bgColor)));
        chipEventStatus.setTextColor(ContextCompat.getColor(requireContext(), textColor));
    }

    // ── 범위 오버레이 (회색 점선 + 가로 캡) ─────────────────────────

    /** LineChart 위에 RangeOverlay를 자식 뷰로 얹고 제스처 시 함께 invalidate */
    private void setupRangeOverlay() {
        rangeOverlay = new RangeOverlay(requireContext());
        chart.addView(rangeOverlay,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

        chart.setOnChartGestureListener(new OnChartGestureListener() {
            @Override public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture g) {}
            @Override public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture g) {}
            @Override public void onChartLongPressed(MotionEvent me) {}
            @Override public void onChartDoubleTapped(MotionEvent me) {}
            @Override public void onChartSingleTapped(MotionEvent me) {}
            @Override public void onChartFling(MotionEvent me1, MotionEvent me2, float vx, float vy) {
                if (rangeOverlay != null) rangeOverlay.invalidate();
            }
            @Override public void onChartScale(MotionEvent me, float sx, float sy) {
                if (rangeOverlay != null) rangeOverlay.invalidate();
            }
            @Override public void onChartTranslate(MotionEvent me, float dX, float dY) {
                if (rangeOverlay != null) rangeOverlay.invalidate();
            }
        });
    }

    /**
     * 차트 위에 얹는 투명 오버레이.
     * 각 슬롯에서 min→max 사이를 회색 점선으로 잇고,
     * min/max 위치에 짧은 가로 캡(데이터셋 색)을 그린다.
     */
    private class RangeOverlay extends View {

        private final List<Bar> bars = new ArrayList<>();
        private final Paint dashedPaint;
        private final Paint capPaint;
        private final Path  dashPath = new Path();
        private final float capHalfWidth;

        RangeOverlay(Context ctx) {
            super(ctx);
            float density = getResources().getDisplayMetrics().density;

            dashedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dashedPaint.setStyle(Paint.Style.STROKE);
            dashedPaint.setStrokeWidth(density * 1f);
            dashedPaint.setColor(COLOR_RANGE_DASH);
            dashedPaint.setPathEffect(
                    new DashPathEffect(new float[]{density * 4f, density * 3f}, 0f));

            capPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            capPaint.setStyle(Paint.Style.STROKE);
            capPaint.setStrokeWidth(density * 2.5f);
            capPaint.setStrokeCap(Paint.Cap.ROUND);

            capHalfWidth = density * 6f; // 캡 길이 = 12dp

            setClickable(false);
            setFocusable(false);
        }

        void update(LineDataSet maxSet1, LineDataSet minSet1,
                    LineDataSet maxSet2, LineDataSet minSet2) {
            bars.clear();
            addBars(maxSet1.getValues(), minSet1.getValues(), COLOR_FRAME);
            addBars(maxSet2.getValues(), minSet2.getValues(), COLOR_AMBIENT);
            invalidate();
        }

        private void addBars(List<Entry> maxEs, List<Entry> minEs, int color) {
            int n = Math.min(maxEs.size(), minEs.size());
            for (int i = 0; i < n; i++) {
                float hi = maxEs.get(i).getY();
                float lo = minEs.get(i).getY();
                if (hi == lo) continue; // 변동 없으면 스킵
                bars.add(new Bar(maxEs.get(i).getX(), lo, hi, color));
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (bars.isEmpty() || chart.getData() == null) return;

            Transformer t = chart.getTransformer(YAxis.AxisDependency.LEFT);
            float[] pts = new float[2];

            for (Bar bar : bars) {
                pts[0] = bar.x; pts[1] = bar.hi;
                t.pointValuesToPixel(pts);
                float sx = pts[0], syHi = pts[1];

                pts[0] = bar.x; pts[1] = bar.lo;
                t.pointValuesToPixel(pts);
                float syLo = pts[1];

                // 회색 점선 (min ↔ max 세로 연결)
                dashPath.reset();
                dashPath.moveTo(sx, syHi);
                dashPath.lineTo(sx, syLo);
                canvas.drawPath(dashPath, dashedPaint);

                // 짧은 가로 캡 (max 위치, min 위치) - 데이터셋 색
                capPaint.setColor(bar.color);
                canvas.drawLine(sx - capHalfWidth, syHi, sx + capHalfWidth, syHi, capPaint);
                canvas.drawLine(sx - capHalfWidth, syLo, sx + capHalfWidth, syLo, capPaint);
            }
        }

        private final class Bar {
            final float x, lo, hi;
            final int color;
            Bar(float x, float lo, float hi, int color) {
                this.x = x; this.lo = lo; this.hi = hi; this.color = color;
            }
        }
    }
}
