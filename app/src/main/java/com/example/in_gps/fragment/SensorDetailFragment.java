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
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
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
    // 슬롯 키 생성용 (타임존 무관하게 Locale.US 사용)
    private static final SimpleDateFormat FMT_DATE  = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat FMT_MONTH = new SimpleDateFormat("yyyy-MM",    Locale.US);

    private static final int    COLOR_FRAME   = Color.parseColor("#FF5722");
    private static final int    COLOR_AMBIENT = Color.parseColor("#2196F3");
    private static final int    COLOR_AXIS    = Color.parseColor("#9E9E9E");
    private static final int    COLOR_NOW     = Color.parseColor("#E53935");
    private static final int    COLOR_WARN    = Color.parseColor("#FF6D00");  // 경고 마커
    private static final int    COLOR_DISC    = Color.parseColor("#757575");  // 연결 끊김 마커

    private static final String[] DAY_OF_WEEK = {"일", "월", "화", "수", "목", "금", "토"};
    private static final String[] MONTH_NAMES = {
        "1월","2월","3월","4월","5월","6월","7월","8월","9월","10월","11월","12월"
    };

    private SensorDetailViewModel viewModel;
    private LineChart chart;
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

        LineDataSet set1    = makeLineDataSet("코어 온도", COLOR_FRAME);
        LineDataSet set2    = makeLineDataSet("표면 온도", COLOR_AMBIENT);
        LineDataSet setWarn = makeEventDataSet("경고",     COLOR_WARN);
        LineDataSet setDisc = makeEventDataSet("연결 끊김", COLOR_DISC);

        int totalSlots;
        if (currentDays == 1) {
            totalSlots = 24;
            buildHourlySlots(items, set1, set2, setWarn, setDisc);
        } else if (currentDays == 7) {
            totalSlots = 7;
            buildDailySlots(items, set1, set2, setWarn, setDisc, 7);
        } else if (currentDays <= 31) {
            totalSlots = 30;
            buildDailySlots(items, set1, set2, setWarn, setDisc, 30);
        } else {
            totalSlots = 12;
            buildMonthlySlots(items, set1, set2, setWarn, setDisc);
        }

        XAxis xAxis = chart.getXAxis();
        xAxis.removeAllLimitLines();
        if (currentDays == 1) {
            xAxis.setLabelCount(8, true);
        } else if (currentDays == 7) {
            xAxis.setLabelCount(7, true);
        } else if (currentDays <= 31) {
            xAxis.setLabelCount(6, true);
        } else {
            xAxis.setLabelCount(12, true);
        }

        addNowIndicator(xAxis, totalSlots);

        if (tvChartPeriodLabel != null) {
            String label;
            switch (currentDays) {
                case 1:   label = "오늘 24시간 · 1시간 평균"; break;
                case 7:   label = "최근 7일 · 일별 평균";     break;
                case 30:  label = "최근 30일 · 일별 평균";    break;
                default:  label = "최근 12개월 · 월별 평균";  break;
            }
            tvChartPeriodLabel.setText(label);
        }

        // X축 범위 명시
        chart.getXAxis().setAxisMinimum(-0.5f);
        chart.getXAxis().setAxisMaximum(totalSlots - 0.5f);

        // Catmull-Rom 스플라인으로 부드러운 라인 생성
        int splineSteps = (currentDays == 1) ? 4 : 10;
        LineDataSet smoothSet1 = toSmoothedSet(set1, "코어 온도", COLOR_FRAME,   splineSteps);
        LineDataSet smoothSet2 = toSmoothedSet(set2, "표면 온도", COLOR_AMBIENT, splineSteps);

        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(smoothSet1);
        dataSets.add(smoothSet2);
        if (setWarn.getEntryCount() > 0) dataSets.add(setWarn);
        if (setDisc.getEntryCount() > 0) dataSets.add(setDisc);

        chart.setData(new LineData(dataSets));
        chart.invalidate();
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

    private void buildHourlySlots(List<TemperatureModel> items,
                                  LineDataSet set1, LineDataSet set2,
                                  LineDataSet setWarn, LineDataSet setDisc) {
        float[]  sum1       = new float[24];
        float[]  sum2       = new float[24];
        int[]    cnt        = new int[24];
        String[] worstEvent = new String[24];

        // createdAt 포맷: "yyyy-MM-ddTHH:mm:ss" 또는 "yyyy-MM-dd HH:mm:ss"
        // 두 형식 모두 position 11~12 가 시(HH) → substring 으로 바로 추출
        if (items != null) {
            for (TemperatureModel item : items) {
                if (item.createdAt == null || item.createdAt.length() < 13) continue;
                int h;
                try { h = Integer.parseInt(item.createdAt.substring(11, 13)); }
                catch (NumberFormatException e) { continue; }
                if (h < 0 || h >= 24) continue;
                sum1[h] += item.temp1;
                sum2[h] += item.temp2;
                cnt[h]++;
                worstEvent[h] = worstOf(worstEvent[h], item.event);
            }
        }

        for (int h = 0; h < 24; h++) {
            xLabels.add(String.format(Locale.getDefault(), "%02d:00", h));
            if (cnt[h] > 0) {
                float avg1 = sum1[h] / cnt[h];
                set1.addEntry(new Entry(h, avg1));
                set2.addEntry(new Entry(h, sum2[h] / cnt[h]));
                if ("disconnected".equals(worstEvent[h])) {
                    setDisc.addEntry(new Entry(h, avg1));
                } else if ("warning".equals(worstEvent[h])) {
                    setWarn.addEntry(new Entry(h, avg1));
                }
            }
        }
    }

    private void buildDailySlots(List<TemperatureModel> items,
                                 LineDataSet set1, LineDataSet set2,
                                 LineDataSet setWarn, LineDataSet setDisc,
                                 int days) {
        float[]  sum1       = new float[days];
        float[]  sum2       = new float[days];
        int[]    cnt        = new int[days];
        String[] worstEvent = new String[days];

        // "yyyy-MM-dd" 문자열 → 슬롯 인덱스 맵 (타임존·포맷 무관)
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
                // createdAt 앞 10자 = "yyyy-MM-dd" (T 포맷·공백 포맷 모두 동일)
                if (item.createdAt == null || item.createdAt.length() < 10) continue;
                Integer slot = dateSlot.get(item.createdAt.substring(0, 10));
                if (slot == null) continue;
                sum1[slot] += item.temp1;
                sum2[slot] += item.temp2;
                cnt[slot]++;
                worstEvent[slot] = worstOf(worstEvent[slot], item.event);
            }
        }

        SimpleDateFormat dateFmt = new SimpleDateFormat("M/dd", Locale.getDefault());
        c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);      c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.DAY_OF_YEAR, -(days - 1));

        for (int i = 0; i < days; i++) {
            xLabels.add(days == 7
                    ? DAY_OF_WEEK[c.get(Calendar.DAY_OF_WEEK) - 1]
                    : dateFmt.format(c.getTime()));
            if (cnt[i] > 0) {
                float avg1 = sum1[i] / cnt[i];
                set1.addEntry(new Entry(i, avg1));
                set2.addEntry(new Entry(i, sum2[i] / cnt[i]));
                if ("disconnected".equals(worstEvent[i])) {
                    setDisc.addEntry(new Entry(i, avg1));
                } else if ("warning".equals(worstEvent[i])) {
                    setWarn.addEntry(new Entry(i, avg1));
                }
            }
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private void buildMonthlySlots(List<TemperatureModel> items,
                                   LineDataSet set1, LineDataSet set2,
                                   LineDataSet setWarn, LineDataSet setDisc) {
        float[]  sum1       = new float[12];
        float[]  sum2       = new float[12];
        int[]    cnt        = new int[12];
        String[] worstEvent = new String[12];

        // "yyyy-MM" 문자열 → 슬롯 인덱스 맵 (타임존·포맷 무관)
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
                // createdAt 앞 7자 = "yyyy-MM" (T 포맷·공백 포맷 모두 동일)
                if (item.createdAt == null || item.createdAt.length() < 7) continue;
                Integer slot = monthSlot.get(item.createdAt.substring(0, 7));
                if (slot == null) continue;
                sum1[slot] += item.temp1;
                sum2[slot] += item.temp2;
                cnt[slot]++;
                worstEvent[slot] = worstOf(worstEvent[slot], item.event);
            }
        }

        c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.add(Calendar.MONTH, -11);
        for (int i = 0; i < 12; i++) {
            xLabels.add(MONTH_NAMES[c.get(Calendar.MONTH)]);
            if (cnt[i] > 0) {
                float avg1 = sum1[i] / cnt[i];
                set1.addEntry(new Entry(i, avg1));
                set2.addEntry(new Entry(i, sum2[i] / cnt[i]));
                if ("disconnected".equals(worstEvent[i])) {
                    setDisc.addEntry(new Entry(i, avg1));
                } else if ("warning".equals(worstEvent[i])) {
                    setWarn.addEntry(new Entry(i, avg1));
                }
            }
            c.add(Calendar.MONTH, 1);
        }
    }

    // ── 공용 유틸 ───────────────────────────────────────────────────

    private LineDataSet makeLineDataSet(String label, int color) {
        LineDataSet ds = new LineDataSet(new ArrayList<>(), label);
        ds.setColor(color);
        ds.setCircleColor(color);
        ds.setLineWidth(2f);
        ds.setCircleRadius(3f);
        ds.setDrawCircleHole(false);
        ds.setDrawValues(false);
        ds.setMode(LineDataSet.Mode.LINEAR);
        return ds;
    }

    /** Catmull-Rom 스플라인 보간으로 부드러운 LineDataSet 생성 */
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

    /** Catmull-Rom 스플라인: 각 구간 사이에 steps개의 보간 포인트 삽입 */
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
                float x  = p1.getX() + (p2.getX() - p1.getX()) * t;
                out.add(new Entry(x, y));
            }
        }
        out.add(new Entry(pts.get(n - 1).getX(), pts.get(n - 1).getY()));
        return out;
    }

    /** 이벤트 마커 전용 데이터셋: 선 없이 큰 원형 마커만 표시 */
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

    /** 두 이벤트 중 더 심각한 것을 반환 (warning > disconnected > normal) */
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
}
