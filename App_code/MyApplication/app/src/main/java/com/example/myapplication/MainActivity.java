package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CartAdapter cartAdapter;
    private List<CartItem> cartItemList;
    private TextView totalAmountText, cartTitle;
    private FirebaseFirestore db;
    private ListenerRegistration firestoreListener;
    int totalAmount = 0;  // 총 금액 변수 초기화

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        totalAmountText = findViewById(R.id.totalAmountText);
        cartTitle = findViewById(R.id.cartTitle);
        Button checkoutButton = findViewById(R.id.checkoutButton);

        checkoutButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CheckoutActivity.class);
            intent.putExtra("TOTAL_AMOUNT", totalAmount);
            startActivity(intent);  // 결제 화면으로 전환
        });

        db = FirebaseFirestore.getInstance();

        // 장바구니 아이템 초기화
        cartItemList = new ArrayList<>();

        // 어댑터 초기화 및 RecyclerView에 설정
        cartAdapter = new CartAdapter(cartItemList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(cartAdapter);

        // Firestore에서 실시간으로 장바구니 아이템 불러오기
        listenToCartItemsFromFirestore();
    }

    // Firestore의 실시간 업데이트를 수신하는 메서드
    private void listenToCartItemsFromFirestore() {
        firestoreListener = db.collection("products")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(MainActivity.this, "실시간 데이터를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        cartItemList.clear();  // 기존 목록 초기화
                        boolean hasItems = false;  // 아이템 존재 여부를 체크
                        totalAmount = 0;  // **매번 초기화**

                        for (DocumentSnapshot document : value.getDocuments()) {
                            String productId = document.getId(); // 고유 RFID 값
                            int count = document.getLong("count").intValue(); // 제품 개수

                            // 제품 정보를 고유값 기준으로 가져오기
                            CartItem cartItem = getProductInfoByRFID(productId, count);
                            if (cartItem != null) {
                                cartItemList.add(cartItem);
                                hasItems = true;  // 제품이 있으면 true로 변경
                            }
                        }

                        if (hasItems) {
                            updateTotalAmount();  // 합계 계산 및 표시
                            cartAdapter.notifyDataSetChanged();  // 어댑터에 데이터 변경 알리기
                        } else {
                            // 장바구니에 제품이 없을 경우
                            cartTitle.setText("장바구니가 비어있습니다.");
                            totalAmountText.setText("합계: 0원");
                        }
                    }
                });
    }

    // 합계 금액 계산 및 표시
    private void updateTotalAmount() {
        totalAmount = 0;  // **합계 계산 전 totalAmount를 초기화**
        for (CartItem item : cartItemList) {
            totalAmount += item.getQuantity() * item.getPrice();
        }
        totalAmountText.setText("합계: " + totalAmount + "원");
    }

    // RFID에 따른 제품 이름과 가격을 반환하는 메서드 (하드코딩 예시)
    private CartItem getProductInfoByRFID(String productId, int count) {
        switch (productId) {
            case "598848734028":
                return new CartItem("츄파춥스", count, 200);
            case "599083483996":
                return new CartItem("와우껌", count, 500);
            case "874229826540":
                return new CartItem("자유시간", count, 1000);
            case "882062786969":
                return new CartItem("아이셔", count, 900);
            case "280210350347":
                return new CartItem("박카스젤리", count, 1000);
            // 결제 카드의 RFID 값은 여기서 제외하고 결제 처리에서 별도 처리
            default:
                return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 액티비티가 종료될 때 Firestore 리스너를 해제
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
    }
}
