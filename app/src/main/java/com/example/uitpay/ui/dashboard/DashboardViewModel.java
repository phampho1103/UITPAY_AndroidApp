package com.example.uitpay.ui.dashboard;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import java.util.HashMap;
import java.util.Map;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.ServerValue;
import com.example.uitpay.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.uitpay.model.Product;
import android.view.View;
import androidx.annotation.NonNull;

public class DashboardViewModel extends ViewModel {

    private final MutableLiveData<String> mText;
    private final MutableLiveData<Integer> currentStage;
    private final MutableLiveData<String> buttonText;
    private final MutableLiveData<Integer> stageImageResource;
    private final MutableLiveData<Integer> muahangImageResource;
    private final MutableLiveData<Integer> muahangImageVisibility;
    private final MutableLiveData<Integer> summaryLayoutVisibility;
    private DatabaseReference databaseRef;
    private FirebaseFirestore db;
    private MutableLiveData<Product> newProduct = new MutableLiveData<>();
    private final MutableLiveData<Integer> totalQuantity = new MutableLiveData<>(0);
    private final MutableLiveData<Double> totalPrice = new MutableLiveData<>(0.0);

    public DashboardViewModel() {
        mText = new MutableLiveData<>();
        currentStage = new MutableLiveData<>(1);
        buttonText = new MutableLiveData<>("Bắt đầu mua hàng");
        stageImageResource = new MutableLiveData<>(R.drawable.stage1);
        muahangImageResource = new MutableLiveData<>(R.drawable.muahang1);
        muahangImageVisibility = new MutableLiveData<>(View.VISIBLE);
        summaryLayoutVisibility = new MutableLiveData<>(View.GONE);
        mText.setValue("Đi đến các UIT Store để bắt đầu mua hàng..");

        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            databaseRef = database.getReference();
            
            // Kiểm tra kết nối
            databaseRef.child(".info/connected").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    boolean connected = snapshot.getValue(Boolean.class);
                    Log.d("Firebase", "Kết nối: " + connected);
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e("Firebase", "Lỗi kết nối: " + error.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e("Firebase", "Lỗi khởi tạo database: " + e.getMessage());
        }

        db = FirebaseFirestore.getInstance();
    }

    public void writeUserData(String productId) {
        if (databaseRef == null) {
            databaseRef = FirebaseDatabase.getInstance().getReference();
        }

        DatabaseReference productsRef = databaseRef.child("phampho1103").child("products");
        
        // Kiểm tra tất cả các sản phẩm để tìm productId trùng
        productsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean productExists = false;
                
                for (DataSnapshot child : snapshot.getChildren()) {
                    String existingProductId = child.child("productId").getValue(String.class);
                    if (existingProductId != null && existingProductId.equals(productId)) {
                        productExists = true;
                        Log.d("Firebase", "ProductId đã tồn tại: " + productId);
                        break;
                    }
                }
                
                if (!productExists) {
                    addNewProduct(productId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Lỗi kiểm tra productId: " + error.getMessage());
            }
        });
    }

    private void addNewProduct(String productId) {
        String key = databaseRef.child("phampho1103")
            .child("products")
            .push()
            .getKey();
        
        if (key != null) {
            db.collection("product")
                .document(productId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Product product = documentSnapshot.toObject(Product.class);
                        if (product != null) {
                            Map<String, Object> productData = new HashMap<>();
                            productData.put("productId", productId);
                            productData.put("name", product.getName());
                            productData.put("quantity", 1);
                            
                            databaseRef.child("phampho1103")
                                .child("products")
                                .child(key)
                                .setValue(productData);
                        }
                    }
                });
        }
    }

    public LiveData<String> getText() {
        return mText;
    }

    public LiveData<Integer> getCurrentStage() {
        return currentStage;
    }

    public LiveData<String> getButtonText() {
        return buttonText;
    }

    public LiveData<Integer> getStageImageResource() {
        return stageImageResource;
    }

    public LiveData<Integer> getMuahangImageResource() {
        return muahangImageResource;
    }

    public LiveData<Integer> getMuahangImageVisibility() {
        return muahangImageVisibility;
    }

    public LiveData<Integer> getSummaryLayoutVisibility() {
        return summaryLayoutVisibility;
    }

    public void moveToNextStage() {
        int stage = currentStage.getValue();
        if (stage == 1) {
            currentStage.setValue(2);
            buttonText.setValue("Hoàn thành mua hàng");
            stageImageResource.setValue(R.drawable.stage2);
            muahangImageResource.setValue(R.drawable.muahang2);
            mText.setValue("Hãy quét điện thoại tới các sản phẩm mà bạn muốn mua");
            summaryLayoutVisibility.setValue(View.VISIBLE);
        }
    }

    public void fetchProductById(String productId) {
        db.collection("product")
            .document(productId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Product product = documentSnapshot.toObject(Product.class);
                    if (product != null) {
                        product.setProductId(productId);
                        product.setQuantity(1);
                        newProduct.setValue(product);
                        hidemuahangImage();
                        
                        // Cập nhật tổng quantity và price
                        DatabaseReference userRef = databaseRef.child("phampho1103");
                        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                int currentQuantity = snapshot.child("quantity").getValue(Integer.class);
                                double currentTotalPrice = snapshot.child("totalprice").getValue(Double.class);
                                
                                int newQuantity = currentQuantity + 1;
                                double newTotalPrice = currentTotalPrice + product.getPrice();
                                
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("quantity", newQuantity);
                                updates.put("totalprice", newTotalPrice);
                                
                                totalQuantity.setValue(newQuantity);
                                totalPrice.setValue(newTotalPrice);
                                
                                userRef.updateChildren(updates);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("Firebase", "Lỗi đọc dữ liệu: " + error.getMessage());
                            }
                        });
                        
                        // Thêm sản phẩm vào node products như cũ
                        String key = databaseRef.child("phampho1103")
                            .child("products")
                            .push()
                            .getKey();
                        
                        if (key != null) {
                            Map<String, Object> productData = new HashMap<>();
                            productData.put("productId", productId);
                            productData.put("name", product.getName());
                            productData.put("quantity", product.getQuantity());
                            productData.put("price", product.getPrice());
                            productData.put("productImage", product.getProductImage());
                            
                            databaseRef.child("phampho1103")
                                .child("products")
                                .child(key)
                                .setValue(productData);
                        }
                    }
                }
            });
    }

    public LiveData<Product> getNewProduct() {
        return newProduct;
    }

    public void hidemuahangImage() {
        muahangImageVisibility.setValue(View.GONE);
    }

    public void writeInitialData() {
        if (databaseRef == null) {
            databaseRef = FirebaseDatabase.getInstance().getReference();
        }

        Map<String, Object> initialData = new HashMap<>();
        initialData.put("timestamp", ServerValue.TIMESTAMP);
        initialData.put("status", "started");
        initialData.put("quantity", 0);
        initialData.put("totalprice", 0);
        initialData.put("isChecked", false);
        initialData.put("isBuying", true);
        
        totalQuantity.setValue(0);
        totalPrice.setValue(0.0);
        
        databaseRef.child("phampho1103")
            .setValue(initialData);
    }

    public LiveData<Integer> getTotalQuantity() {
        return totalQuantity;
    }

    public LiveData<Double> getTotalPrice() {
        return totalPrice;
    }
}