package com.example.rssi_check;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Collections;


public class MainActivity extends AppCompatActivity {

    private static final String HM10_MAC_ADDRESS = "34:03:DE:4F:54:B2"; // HM-10 MAC 주소
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private TextView rssiTextView;
    private Handler handler = new Handler();
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        rssiTextView = findViewById(R.id.rssi_value);

        // BluetoothManager를 통해 BluetoothAdapter 초기화
        bluetoothAdapter = ((android.bluetooth.BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        // 권한 확인 후 BLE 스캔 시작
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            startBleScan();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    // BLE 스캔을 시작하는 메서드
    private void startBleScan() {
        if (bluetoothLeScanner == null) {
            Toast.makeText(this, "블루투스 스캐너가 초기화되지 않았습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();

        // BLE 스캔 시작
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        bluetoothLeScanner.startScan(null, scanSettings, bleScanCallback);
    }

    // 스캔 결과를 처리하는 콜백
    private final ScanCallback bleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            BluetoothDevice device = result.getDevice();
            int rssi = result.getRssi();

            // 특정 HM-10 장치인지 확인
            if (HM10_MAC_ADDRESS.equals(device.getAddress())) {
                Log.i("MainActivity", "HM-10 장치 발견, RSSI: " + rssi);
                runOnUiThread(() -> rssiTextView.setText("RSSI: " + rssi));
                db.collection("rssi").document("check_rssi")
                        .set(Collections.singletonMap("data", rssi))  // 문서가 없으면 새로 생성
                        .addOnSuccessListener(aVoid -> {
                        })
                        .addOnFailureListener(e -> {
                            Log.e("FirestoreError", "연결 상태 업데이트 실패", e);
                            Toast.makeText(MainActivity.this, "연결 상태 업데이트에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        });

            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e("MainActivity", "스캔 실패, 오류 코드: " + errorCode);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 액티비티 종료 시 스캔 중단
        if (bluetoothLeScanner != null) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            bluetoothLeScanner.stopScan(bleScanCallback);
        }
    }
}
