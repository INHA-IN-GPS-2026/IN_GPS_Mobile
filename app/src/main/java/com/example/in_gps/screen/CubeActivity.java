package com.example.in_gps.screen;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.in_gps.R;
import com.example.in_gps.api.RetrofitClient;
import com.example.in_gps.model.TemperatureModel;
import com.example.in_gps.model.TemperatureResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CubeActivity extends AppCompatActivity {

    private GLSurfaceView glView;
    private CubeRenderer renderer;

    private TextView tvX, tvY, tvZ, tvDevice, tvStatus;

    private final Handler handler = new Handler(Looper.getMainLooper());

    // ── DUMMY DATA TEST MODE ────────────────────────────────────────────────
    // ADXL335 실제 범위: X(Roll) -180~+180 / Y(Pitch) -90~+90 / Z(Tilt) -90~+90
    private static final float[][] DUMMY_DATA = {
        // [ X축(Roll) 단독: -180 ~ +180, 30° 간격 한 바퀴 ]
        {   0f,   0f,   0f },   //  1. 기준
        {  30f,   0f,   0f },   //  2. Roll +30
        {  60f,   0f,   0f },   //  3. Roll +60
        {  90f,   0f,   0f },   //  4. Roll +90
        { 120f,   0f,   0f },   //  5. Roll +120
        { 150f,   0f,   0f },   //  6. Roll +150
        { 180f,   0f,   0f },   //  7. Roll ±180 (완전 뒤집기)
        {-150f,   0f,   0f },   //  8. Roll -150
        {-120f,   0f,   0f },   //  9. Roll -120
        { -90f,   0f,   0f },   // 10. Roll -90
        { -60f,   0f,   0f },   // 11. Roll -60
        { -30f,   0f,   0f },   // 12. Roll -30
        // [ Y축(Pitch) 단독: -90 ~ +90, 30° 간격 왕복 ]
        {   0f,   0f,   0f },   // 13. 기준
        {   0f,  30f,   0f },   // 14. Pitch +30
        {   0f,  60f,   0f },   // 15. Pitch +60
        {   0f,  90f,   0f },   // 16. Pitch +90 (최대)
        {   0f,  60f,   0f },   // 17. Pitch +60
        {   0f,  30f,   0f },   // 18. Pitch +30
        {   0f, -30f,   0f },   // 19. Pitch -30
        {   0f, -60f,   0f },   // 20. Pitch -60
        {   0f, -90f,   0f },   // 21. Pitch -90 (최대)
        // [ Z축(Tilt) 단독: -90 ~ +90, 30° 간격 왕복 ]
        {   0f,   0f,   0f },   // 22. 기준
        {   0f,   0f,  30f },   // 23. Tilt +30
        {   0f,   0f,  60f },   // 24. Tilt +60
        {   0f,   0f,  90f },   // 25. Tilt +90 (최대)
        {   0f,   0f, -90f },   // 26. Tilt -90 (최대)
        {   0f,   0f, -60f },   // 27. Tilt -60
        {   0f,   0f, -30f },   // 28. Tilt -30
        // [ 복합 자세 ]
        { 120f,  60f,  45f },   // 29. 복합 (X 범위 활용)
        {-120f, -60f, -45f },   // 30. 복합 반전
    };

    private int dummyIndex = 0;

    private final Runnable dummyRunnable = new Runnable() {
        @Override
        public void run() {
            float[] d = DUMMY_DATA[dummyIndex];
            renderer.setAngles(d[0], d[1], d[2]);

            tvX.setText(String.format("X (Roll)  : %6.1f°", d[0]));
            tvY.setText(String.format("Y (Pitch) : %6.1f°", d[1]));
            tvZ.setText(String.format("Z (Tilt)  : %6.1f°", d[2]));
            tvDevice.setText(String.format("DUMMY %02d / %02d", dummyIndex + 1, DUMMY_DATA.length));
            tvStatus.setText("● DUMMY");
            tvStatus.setTextColor(0xFFFFD700);

            dummyIndex = (dummyIndex + 1) % DUMMY_DATA.length;
            handler.postDelayed(this, INTERVAL_MS);
        }
    };
    // ── DUMMY DATA TEST MODE END ────────────────────────────────────────────


    // ── API 폴링 ────────────────────────────────────────────────────────────
    private static final long INTERVAL_MS = 2000;
    private static final String DEVICE_ID = "esp_32";

    private void startPolling() {
        handler.post(pollRunnable);
    }

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            RetrofitClient.getInstance().getApiService()
                    .getTemperature(DEVICE_ID, 1)
                    .enqueue(new Callback<TemperatureResponse>() {
                        @Override
                        public void onResponse(Call<TemperatureResponse> call,
                                               Response<TemperatureResponse> response) {
                            if (response.isSuccessful()
                                    && response.body() != null
                                    && !response.body().items.isEmpty()) {

                                TemperatureModel data = response.body().items.get(0);

                                renderer.setAngles(data.angleX, data.angleY, data.angleZ);

                                tvX.setText(String.format("X (Roll)  : %6.1f°", data.angleX));
                                tvY.setText(String.format("Y (Pitch) : %6.1f°", data.angleY));
                                tvZ.setText(String.format("Z (Tilt)  : %6.1f°", data.angleZ));
                                tvDevice.setText(data.deviceId);
                                tvStatus.setText("● LIVE");
                                tvStatus.setTextColor(0xFF56D364);
                            }
                        }

                        @Override
                        public void onFailure(Call<TemperatureResponse> call, Throwable t) {
                            Log.e("CubeActivity", "API 오류: " + t.getMessage());
                            tvStatus.setText("● 연결 오류");
                            tvStatus.setTextColor(0xFFFF7B72);
                        }
                    });

            handler.postDelayed(this, INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cube);

        glView   = findViewById(R.id.gl_view);
        tvX      = findViewById(R.id.tv_angle_x);
        tvY      = findViewById(R.id.tv_angle_y);
        tvZ      = findViewById(R.id.tv_angle_z);
        tvDevice = findViewById(R.id.tv_device);
        tvStatus = findViewById(R.id.tv_status);

        // OpenGL ES 2.0 설정
        glView.setEGLContextClientVersion(2);
        renderer = new CubeRenderer();
        glView.setRenderer(renderer);
        glView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        startPolling();                   // LIVE: API 폴링
//        handler.post(dummyRunnable);       // TEST: 더미 데이터 순환
    }

    @Override
    protected void onResume() {
        super.onResume();
        glView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glView.onPause();
        handler.removeCallbacks(dummyRunnable);
        handler.removeCallbacks(pollRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
