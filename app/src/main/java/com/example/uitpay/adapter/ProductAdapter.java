package com.example.uitpay.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.uitpay.R;
import com.example.uitpay.model.Product;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    private List<Product> products = new ArrayList<>();
    private Context context;

    public void setProducts(List<Product> newProducts) {
        products.clear();
        products.addAll(newProducts);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
            .inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = products.get(position);
        
        // Thêm click listener cho imageView
        holder.imageView.setOnClickListener(v -> showProductDetail(product));
        
        holder.nameTextView.setText(product.getName());
        holder.priceTextView.setText(String.format("%,.0f VND", product.getPrice()));
        holder.quantityTextView.setText("Số lượng: " + product.getQuantity());
        
        // Load ảnh trực tiếp bằng Glide
        Glide.with(holder.itemView.getContext())
            .load(product.getProductImage())
            .into(holder.imageView);
        
        // Log để debug
        Log.d("ProductAdapter", "Loading image URL: " + product.getProductImage());
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public void addOrUpdateProduct(Product newProduct) {
        if (newProduct == null || newProduct.getProductId() == null) {
            Log.e("ProductAdapter", "Product hoặc ProductId null");
            return;
        }

        // Chỉ kiểm tra trùng productId
        for (Product existingProduct : products) {
            if (existingProduct.getProductId().equals(newProduct.getProductId())) {
                Log.d("ProductAdapter", "Sản phẩm đã tồn tại với productId: " + newProduct.getProductId());
                Toast.makeText(context, "Bạn đã thêm sản phẩm này rồi nhé", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        // Nếu không trùng productId, thêm mới
        products.add(newProduct);
        notifyItemInserted(products.size() - 1);
        Log.d("ProductAdapter", "Thêm sản phẩm mới: " + newProduct.getName());
    }

    private void updateProductQuantity(Product product) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
            .child("phampho1103")
            .child("products");
        
        ref.orderByChild("productId").equalTo(product.getProductId())
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("productId", product.getProductId());
                        updates.put("name", product.getName());
                        updates.put("quantity", product.getQuantity());
                        
                        child.getRef().updateChildren(updates)
                            .addOnSuccessListener(aVoid -> 
                                Log.d("ProductAdapter", "Cập nhật số lượng thành công"))
                            .addOnFailureListener(e -> 
                                Log.e("ProductAdapter", "Lỗi cập nhật số lượng: " + e.getMessage()));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("ProductAdapter", "Lỗi truy vấn database: " + error.getMessage());
                }
            });
    }

    private void showProductDetail(Product product) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_product_detail, null);

        ImageView productImage = dialogView.findViewById(R.id.detail_product_image);
        TextView nameText = dialogView.findViewById(R.id.detail_product_name);
        TextView priceText = dialogView.findViewById(R.id.detail_product_price);
        TextView originText = dialogView.findViewById(R.id.detail_product_origin);
        TextView descriptionText = dialogView.findViewById(R.id.detail_product_description);

        // Load ảnh sản phẩm
        if (product.getProductImage() != null) {
            Glide.with(context)
                .load(product.getProductImage())
                .into(productImage);
        }

        // Set các thông tin khác
        nameText.setText("Tên sản phẩm: " + (product.getName() != null ? product.getName() : ""));
        priceText.setText(String.format("Giá: %,.0f VND", product.getPrice()));
        originText.setText("Xuất xứ: " + (product.getOrigin() != null ? product.getOrigin() : ""));
        descriptionText.setText("Mô tả: " + (product.getDescription() != null ? product.getDescription() : ""));

        builder.setView(dialogView)
               .setNegativeButton("Thoát", (dialog, id) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView nameTextView;
        TextView priceTextView;
        TextView quantityTextView;

        ProductViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.product_image);
            nameTextView = itemView.findViewById(R.id.product_name);
            priceTextView = itemView.findViewById(R.id.product_price);
            quantityTextView = itemView.findViewById(R.id.product_quantity);
        }
    }
} 