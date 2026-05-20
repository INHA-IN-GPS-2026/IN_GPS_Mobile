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
import com.example.in_gps.model.TemperatureModel;
import com.example.in_gps.viewmodel.SensorDetailTestViewModel;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 디바이스 클릭 시 열리는 테스트용 상세 화면.
 *
 *  - /temperature?device_id=X&limit=N  (1초 주기 polling)
 *  - 차트 1: Thermistor 2개 (temp1 / temp2)
 *  - 차트 2: ADXL335 RMS 3개 (rms_x / rms_y / rms_z, mg)
 *  - 현재값 카드: 가장 최신 1건
 */
public class SensorDetailTestFragment extends Fragment {

    private static final String ARG_DEVICE_ID = "device_id";

    private static final int COLOR_TEMP1 = Color.parseColor("#FF5722");
    private static final int COLOR_TEMP2 = Color.parseColor("#2196F3");
    private static final int COLOR_RMSX  = Color.parseColor("#E53935");
    private static final int COLOR_RMSY  = Color.parseColor("#43A047");
    private static final int COLOR_RMSZ  = Color.parseColor("#1E88E5");
    private static final int COLOR_AXIS  = Color.parseColor("#9E9E9E");

    /** Temperature SMA window. 1초 주기 raw → MA_WINDOW초 평균. 짝수보다 홀수가 위상 대칭. */
    private static final int MA_WINDOW = 5;

    /** Catmull-Rom 보간 분할 수. 인접 raw 점 사이에 SPLINE_SUBDIVIDE개의 보간 점을 끼움. */
    private static final int SPLINE_SUBDIVIDE = 4;

    /** 차트 X축 고정 폭(=sliding window 크기). ViewModel의 limit과 동일해야 함. */
    private static final int CHART_X_RANGE = SensorDetailTestViewModel.WINDOW_SIZE;

    private SensorDetailTestViewModel viewModel;

    private LineChart chartTemperature;
    private LineChart chartVibration;
    private TextView tvDeviceId, tvLastUpdate;
    private TextView tvNowTemp1, tvNowTemp2;
    private TextView tvNowRmsX, tvNowRmsY, tvNowRmsZ;

    private final ArrayList<String> xLabels = new ArrayList<>();

    public static SensorDetailTestFragment newInstance(String deviceId) {
        SensorDetailTestFragment fragment = new SensorDetailTestFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DEVICE_ID, deviceId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sensor_detail_test, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvDeviceId       = view.findViewById(R.id.tv_device_id);
        tvLastUpdate     = view.findViewById(R.id.tv_last_update);
        tvNowTemp1       = view.findViewById(R.id.tv_now_temp1);
        tvNowTemp2       = view.findViewById(R.id.tv_now_temp2);
        tvNowRmsX        = view.findViewById(R.id.tv_now_rms_x);
        tvNowRmsY        = view.findViewById(R.id.tv_now_rms_y);
        tvNowRmsZ        = view.findViewById(R.id.tv_now_rms_z);
        chartTemperature = view.findViewById(R.id.chart_temperature);
        chartVibration   = view.findViewById(R.id.chart_vibration);

        setupChart(chartTemperature, "°C");
        setupChart(chartVibration, "mg");

        String deviceId = requireArguments().getString(ARG_DEVICE_ID, "--");
        tvDeviceId.setText(deviceId);

        viewModel = new ViewModelProvider(this,
                new SensorDetailTestViewModel.Factory(deviceId))
                .get(SensorDetailTestViewModel.class);

        viewModel.getRecentData().observe(getViewLifecycleOwner(), this::onDataChanged);
    }

    // ── 차트 공용 설정 ──────────────────────────────────────────────
    private void setupChart(LineChart chart, String yUnit) {
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.getDescription().setEnabled(false);
        chart.setNoDataText("데이터를 불러오는 중...");
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setDrawBorders(false);
        chart.setExtraBottomOffset(8f);

        chart.getLegend().setTextColor(COLOR_AXIS);
        chart.getLegend().setTextSize(11f);

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
        yAxis.setDrawGridLines(true);
        yAxis.setGridColor(Color.parseColor("#22000000"));
        yAxis.setDrawAxisLine(false);
        yAxis.setTextColor(COLOR_AXIS);
        yAxis.setTextSize(10f);
        final String unit = yUnit;
        yAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.US, "%.0f%s", value, unit);
            }
        });

        chart.getAxisRight().setEnabled(false);
    }

    // ── 데이터 갱신 ────────────────────────────────────────────────
    private void onDataChanged(List<TemperatureModel> recentDesc) {
        if (recentDesc == null || recentDesc.isEmpty()) return;

        // 서버는 created_at DESC로 반환. 차트에는 시간순 ASC로 그려야 자연스러움.
        List<TemperatureModel> ordered = new ArrayList<>(recentDesc);
        Collections.reverse(ordered);

        // 현재값(가장 최신) — DESC 첫 row
        TemperatureModel latest = recentDesc.get(0);
        tvNowTemp1.setText(String.format(Locale.US, "%.2f °C", latest.temp1));
        tvNowTemp2.setText(String.format(Locale.US, "%.2f °C", latest.temp2));
        tvNowRmsX.setText(formatMg(latest.rmsX));
        tvNowRmsY.setText(formatMg(latest.rmsY));
        tvNowRmsZ.setText(formatMg(latest.rmsZ));
        tvLastUpdate.setText("최근 갱신: " + extractHHmmss(latest.createdAt));

        // X축 라벨 재구축 (HH:mm:ss).
        // X축은 [0, CHART_X_RANGE-1]로 고정하고, 데이터는 오른쪽 끝(최신)에 정렬되도록
        // 부족한 만큼 앞쪽을 빈 라벨로 패딩한다. 이렇게 해야 데이터가 차오르는 동안에도
        // 축 눈금이 흔들리지 않음.
        xLabels.clear();
        int pad = Math.max(0, CHART_X_RANGE - ordered.size());
        for (int i = 0; i < pad; i++) xLabels.add("");
        for (TemperatureModel m : ordered) {
            xLabels.add(extractHHmmss(m.createdAt));
        }

        renderTemperatureChart(ordered);
        renderVibrationChart(ordered);
    }

    private void renderTemperatureChart(List<TemperatureModel> ordered) {
        LineDataSet ds1 = makeLineDataSet("Thermistor 1", COLOR_TEMP1);
        LineDataSet ds2 = makeLineDataSet("Thermistor 2", COLOR_TEMP2);

        int n = ordered.size();
        float[] temp1 = new float[n];
        float[] temp2 = new float[n];
        for (int i = 0; i < n; i++) {
            TemperatureModel m = ordered.get(i);
            temp1[i] = m.temp1;
            temp2[i] = m.temp2;
        }

        // Thermistor raw에는 ADC 양자화 + 환경 잡음에 의한 jitter가 섞여 시각적으로 떠 보임.
        // SMA(Simple Moving Average)로 고주파 성분을 누른다. 위상 지연 ≈ (MA_WINDOW-1)/2 sec.
        float[] temp1Smoothed = movingAverage(temp1, MA_WINDOW);
        float[] temp2Smoothed = movingAverage(temp2, MA_WINDOW);

        // 오른쪽 정렬: 데이터가 60개 미만이어도 최신값은 항상 X축 우측 끝에 위치.
        int offset = Math.max(0, CHART_X_RANGE - n);
//        for (int i = 0; i < n; i++) {
//            ds1.addEntry(new Entry(offset + i, temp1Smoothed[i]));
//            ds2.addEntry(new Entry(offset + i, temp2Smoothed[i]));
//        }

        /* ── (옵션) Catmull-Rom spline 보간 ─────────────────────────────────────────
         * MA 결과 점 사이를 cubic spline으로 보간해 dense points를 만들고 LINEAR로 잇는다.
         * spline은 노이즈를 제거하지 않으므로 반드시 MA 뒤에 적용해야 jitter를 따라가는
         * 곡선이 되지 않는다. Catmull-Rom은 control points를 정확히 지나며 끝점만
         * reflect/clamp 처리(`P[-1]=P[0]`, `P[N]=P[N-1]`).
         *
         * 활성화 방법: 위의 단순 entry 루프(`for (int i = 0; i < n; i++) ...`)를 지우고
         * 아래 블록의 주석을 해제. dense 점은 빽빽하므로 circle 마커는 끄는 편이 가독성에 좋다.
         *
         * float[][] temp1Dense = catmullRomSpline(temp1Smoothed, SPLINE_SUBDIVIDE);
         * float[][] temp2Dense = catmullRomSpline(temp2Smoothed, SPLINE_SUBDIVIDE);
         * ds1.setDrawCircles(false);
         * ds2.setDrawCircles(false);
         * for (float[] p : temp1Dense) ds1.addEntry(new Entry(offset + p[0], p[1]));
         * for (float[] p : temp2Dense) ds2.addEntry(new Entry(offset + p[0], p[1]));
         *
         * 참고 — LineDataSet.Mode.CUBIC_BEZIER는 MPAndroidChart 렌더 시점에 자동 처리되지만
         * segment 경계에서 미세 오버슈트가 생길 수 있어, 직접 Catmull-Rom으로 점을 만들어
         * LINEAR로 그리는 방식을 선택. */
        float[][] temp1Dense = catmullRomSpline(temp1Smoothed, SPLINE_SUBDIVIDE);
        float[][] temp2Dense = catmullRomSpline(temp2Smoothed, SPLINE_SUBDIVIDE);
        ds1.setDrawCircles(false);
        ds2.setDrawCircles(false);
        for (float[] p : temp1Dense) ds1.addEntry(new Entry(offset + p[0], p[1]));
        for (float[] p : temp2Dense) ds2.addEntry(new Entry(offset + p[0], p[1]));
        List<ILineDataSet> sets = new ArrayList<>();
        sets.add(ds1);
        sets.add(ds2);
        chartTemperature.setData(new LineData(sets));
        chartTemperature.getXAxis().setAxisMinimum(-0.5f);
        chartTemperature.getXAxis().setAxisMaximum(CHART_X_RANGE - 0.5f);
        chartTemperature.notifyDataSetChanged();
        chartTemperature.invalidate();
    }

    private void renderVibrationChart(List<TemperatureModel> ordered) {
        LineDataSet dsX = makeLineDataSet("RMS X", COLOR_RMSX);
        LineDataSet dsY = makeLineDataSet("RMS Y", COLOR_RMSY);
        LineDataSet dsZ = makeLineDataSet("RMS Z", COLOR_RMSZ);

        int offset = Math.max(0, CHART_X_RANGE - ordered.size());
        for (int i = 0; i < ordered.size(); i++) {
            TemperatureModel m = ordered.get(i);
            if (m.rmsX != null) dsX.addEntry(new Entry(offset + i, m.rmsX));
            if (m.rmsY != null) dsY.addEntry(new Entry(offset + i, m.rmsY));
            if (m.rmsZ != null) dsZ.addEntry(new Entry(offset + i, m.rmsZ));
        }

        List<ILineDataSet> sets = new ArrayList<>();
        sets.add(dsX);
        sets.add(dsY);
        sets.add(dsZ);
        chartVibration.setData(new LineData(sets));
        chartVibration.getXAxis().setAxisMinimum(-0.5f);
        chartVibration.getXAxis().setAxisMaximum(CHART_X_RANGE - 0.5f);
        chartVibration.notifyDataSetChanged();
        chartVibration.invalidate();
    }

    // ── 헬퍼 ───────────────────────────────────────────────────────
    private LineDataSet makeLineDataSet(String label, int color) {
        LineDataSet ds = new LineDataSet(new ArrayList<>(), label);
        ds.setColor(color);
        ds.setCircleColor(color);
        ds.setCircleRadius(2.5f);
        ds.setLineWidth(2f);
        ds.setDrawCircles(true);
        ds.setDrawCircleHole(false);
        ds.setDrawValues(false);
        ds.setMode(LineDataSet.Mode.LINEAR);
        return ds;
    }

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
    private static String formatMg(Integer mg) {
        return mg == null ? "-- mg" : (mg + " mg");
    }

    /**
     * Catmull-Rom spline 보간. 길이 N인 입력 y[] 의 인접 두 점 사이에 `subdivide` 등분의
     * 보간 점을 끼워 넣고, 원본 점도 모두 포함한 (x, y) 쌍 시퀀스를 반환한다.
     * x는 원본 인덱스를 기준으로 한 실수값 ([0, N-1] 범위).
     * 끝점은 P[-1] = P[0], P[N] = P[N-1]로 reflect/clamp 처리.
     *
     * @return shape = [(N-1)*subdivide + 1][2], 각 원소 {x, y}
     */
    private static float[][] catmullRomSpline(float[] values, int subdivide) {
        int n = values.length;
        if (n == 0) return new float[0][2];
        if (n == 1) return new float[][]{ {0f, values[0]} };
        if (subdivide < 1) subdivide = 1;

        int outLen = (n - 1) * subdivide + 1;
        float[][] out = new float[outLen][2];
        int idx = 0;
        for (int i = 0; i < n - 1; i++) {
            float p0 = values[Math.max(0, i - 1)];
            float p1 = values[i];
            float p2 = values[i + 1];
            float p3 = values[Math.min(n - 1, i + 2)];
            for (int s = 0; s < subdivide; s++) {
                float t = (float) s / subdivide;
                float t2 = t * t;
                float t3 = t2 * t;
                // 표준 Catmull-Rom basis (tension = 0.5)
                float y = 0.5f * (
                        (2f * p1)
                                + (-p0 + p2) * t
                                + (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2
                                + (-p0 + 3f * p1 - 3f * p2 + p3) * t3
                );
                out[idx][0] = i + t;
                out[idx][1] = y;
                idx++;
            }
        }
        out[outLen - 1][0] = n - 1;
        out[outLen - 1][1] = values[n - 1];
        return out;
    }

    /**
     * 길이 N의 시퀀스에 대한 trailing SMA. out[i] = mean(values[i-w+1 .. i]).
     * 시작 구간(i < w-1)은 가용한 점들만으로 평균(=ramp-up). 시퀀스 길이는 보존된다.
     */
    private static float[] movingAverage(float[] values, int window) {
        int n = values.length;
        float[] out = new float[n];
        if (n == 0 || window <= 1) {
            System.arraycopy(values, 0, out, 0, n);
            return out;
        }
        float sum = 0f;
        for (int i = 0; i < n; i++) {
            sum += values[i];
            if (i >= window) sum -= values[i - window];
            int cnt = Math.min(i + 1, window);
            out[i] = sum / cnt;
        }
        return out;
    }

    /** "YYYY-MM-DD HH:MM:SS" 또는 ISO 형식에서 HH:MM:SS만 추출. */
    private static String extractHHmmss(String ts) {
        if (ts == null) return "";
        // 형식: "YYYY-MM-DDTHH:MM:SS" 또는 "YYYY-MM-DD HH:MM:SS"
        int len = ts.length();
        if (len >= 19) {
            return ts.substring(11, 19);
        }
        return ts;
    }
}
