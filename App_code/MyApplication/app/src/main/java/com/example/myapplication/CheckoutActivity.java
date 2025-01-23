package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import javax.annotation.Nullable;

public class CheckoutActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ListenerRegistration cardListener;
    private static final String PAYMENT_CARD_RFID = "906249822851";  // 카드의 고유 RFID 값을 지정
    private int totalAmount;  // 총 금액을 저장할 변수
    private TextView totalAmountText;  // 총 금액을 표시할 TextView
    private ImageButton backButton;  // 뒤로가기 이미지 버튼

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        db = FirebaseFirestore.getInstance();

        // MainActivity에서 전달받은 총 금액을 가져옴
        totalAmount = getIntent().getIntExtra("TOTAL_AMOUNT", 0);

        // 총 금액을 표시하는 TextView를 설정
        totalAmountText = findViewById(R.id.totalAmountText);
        totalAmountText.setText("총 결제 금액: " + totalAmount + "원");

        // 뒤로가기 이미지 버튼 설정
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            // 메인 화면으로 돌아가기
            Intent intent = new Intent(CheckoutActivity.this, MainActivity.class);
            startActivity(intent);
            finish();  // 현재 액티비티 종료
        });

        // Firestore에서 실시간으로 결제 카드의 RFID를 확인하는 메서드 호출
        listenForCardRFID();
    }

    private void listenForCardRFID() {
        cardListener = db.collection("products").document(PAYMENT_CARD_RFID)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Toast.makeText(CheckoutActivity.this, "실시간 데이터를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            // 결제 완료 메시지 표시
                            Toast.makeText(CheckoutActivity.this, "결제가 완료되었습니다.", Toast.LENGTH_SHORT).show();
                            // Firestore에서 제품 삭제
                            clearCartItemsFromFirestore();
                            finish();;
                        }
                    }
                });
    }

    private void clearCartItemsFromFirestore() {
        db.collection("products")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            db.collection("products").document(document.getId()).delete();
                        }
                        // 결제 완료 후 메인 화면으로 돌아가기
                        Intent intent = new Intent(CheckoutActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 액티비티가 종료될 때 Firestore 리스너를 해제
        if (cardListener != null) {
            cardListener.remove();
        }
    }
}