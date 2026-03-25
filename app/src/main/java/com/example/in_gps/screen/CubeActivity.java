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

    // ── TEST MODE: 각도를 조금씩 증가시켜 큐브 외형 확인 ──────────────────
//    private float testX = 0f, testY = 0f, testZ = 0f;
//    private static final float STEP = 0.5f;   // 한 번에 증가하는 각도 (도)
//    private static final long TEST_INTERVAL_MS = 50; // 업데이트 주기 (ms)
//
//    private final Runnable testRunnable = new Runnable() {
//        @Override
//        public void run() {
//            testX += STEP;
//            testY += STEP * 0.7f;
//            testZ += STEP * 0.4f;
//
//            renderer.setAngles(testX, testY, testZ);
//
//            tvX.setText(String.format("X (Roll)  : %6.1f°", testX));
//            tvY.setText(String.format("Y (Pitch) : %6.1f°", testY));
//            tvZ.setText(String.format("Z (Yaw)   : %6.1f°", testZ));
//            tvDevice.setText("TEST MODE");
//            tvStatus.setText("● TEST");
//            tvStatus.setTextColor(0xFFFFD700);
//
//            handler.postDelayed(this, TEST_INTERVAL_MS);
//        }
//    };
    // ── TEST MODE END ──────────────────────────────────────────────────────


    // ── 기존: API 폴링 ──────────────────────────────────────────────────────
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
                                tvZ.setText(String.format("Z (Yaw)   : %6.1f°", data.angleZ));
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

         startPolling(); // 기존 API 폴링 — 테스트 중 비활성화
//        handler.post(testRunnable); // TEST MODE
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
//        handler.removeCallbacks(testRunnable); // TEST MODE
        handler.removeCallbacks(pollRunnable); // 기존 API 폴링
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}

