package com.example.uitpay.ui.dashboard;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.tech.Ndef;
import android.app.PendingIntent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Html;
import android.util.TypedValue;
import android.view.Gravity;
import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.uitpay.R;
import com.example.uitpay.databinding.FragmentDashboardBinding;
import com.example.uitpay.model.Product;
import com.example.uitpay.adapter.ProductAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private NfcAdapter nfcAdapter;
    private static final int ENABLE_NFC_REQUEST_CODE = 123;
    private ImageView stageImage;
    private Button testButton;
    private ProductAdapter productAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] intentFiltersArray;
    private String[][] techListsArray;
    private final MutableLiveData<Integer> totalQuantity = new MutableLiveData<>(0);
    private final MutableLiveData<Double> totalPrice = new MutableLiveData<>(0.0);

    public View onCreateView(@NonNull LayoutInflater inflater,
                         ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textDashboard;
        testButton = binding.testButton;
        final ImageView muahangImage = binding.muahangImage;
        stageImage = binding.stageImage;
        
        // Khởi tạo NFC Adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(requireContext());

        // Observe tất cả các LiveData
        dashboardViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        dashboardViewModel.getButtonText().observe(getViewLifecycleOwner(), testButton::setText);
        dashboardViewModel.getStageImageResource().observe(getViewLifecycleOwner(), 
            resource -> stageImage.setImageResource(resource));
        dashboardViewModel.getMuahangImageResource().observe(getViewLifecycleOwner(), 
            resource -> muahangImage.setImageResource(resource));
        dashboardViewModel.getMuahangImageVisibility().observe(getViewLifecycleOwner(),
            visibility -> binding.muahangImage.setVisibility(visibility));
        dashboardViewModel.getSummaryLayoutVisibility().observe(getViewLifecycleOwner(),
            visibility -> binding.summaryLayout.setVisibility(visibility));
        dashboardViewModel.getRecyclerViewVisibility().observe(getViewLifecycleOwner(),
            visibility -> binding.productsRecyclerView.setVisibility(visibility));

        dashboardViewModel.getTotalQuantity().observe(getViewLifecycleOwner(), quantity -> 
            binding.productCountText.setText("Số lượng sản phẩm: " + quantity));

        dashboardViewModel.getTotalPrice().observe(getViewLifecycleOwner(), price -> 
            binding.totalPriceText.setText(String.format("Tổng tiền: %.0f VND", price)));

        dashboardViewModel.getRecheckStatusText().observe(getViewLifecycleOwner(),
            text -> binding.textDashboard.setText(text));

        dashboardViewModel.getPaymentInfoText().observe(getViewLifecycleOwner(),
            text -> {
                if (text != null) {
                    binding.textDashboard.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT));
                    binding.textDashboard.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                    binding.textDashboard.setGravity(Gravity.CENTER);
                    binding.textDashboard.setPadding(16, 32, 16, 32);
                }
            });

        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Integer stage = dashboardViewModel.getCurrentStage().getValue();
                if (stage != null) {
                    if (stage == 1) {
                        if (nfcAdapter == null) {
                            Toast.makeText(requireContext(), 
                                "Thiết bị của bạn không hỗ trợ NFC", 
                                Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (!nfcAdapter.isEnabled()) {
                            showNFCSettings();
                        } else {
                            dashboardViewModel.moveToNextStage();
                            dashboardViewModel.writeInitialData();
                        }
                    } else if (stage == 2) {
                        dashboardViewModel.finishShopping();
                        dashboardViewModel.listenToCheckStatus();
                    } else if (stage == 3) {
                        Boolean isChecked = dashboardViewModel.getIsChecked().getValue();
                        if (isChecked != null && !isChecked) {
                            Toast.makeText(requireContext(),
                                "Đơn hàng vẫn chưa được Recheck",
                                Toast.LENGTH_SHORT).show();
                        } else {
                            String buttonText = dashboardViewModel.getButtonText().getValue();
                            if (buttonText.equals("Hoàn thành recheck")) {
                                // Khi nhấn "Hoàn thành recheck"
                                dashboardViewModel.showPaymentInfo();
                            } else if (buttonText.equals("Thanh toán")) {
                                // Khi nhấn "Thanh toán"
                                DatabaseReference userRef = dashboardViewModel.getDatabaseRef().child("phampho1103");
                                userRef.child("totalprice").get().addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Double totalPrice = task.getResult().getValue(Double.class);
                                        if (totalPrice != null) {
                                            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                                            builder.setMessage(String.format("Bạn có xác nhận thanh toán số tiền %,.0f VND?", totalPrice))
                                                   .setPositiveButton("Đồng ý", (dialog, id) -> {
                                                       dashboardViewModel.processPayment(requireContext(), dialog);
                                                   })
                                                   .setNegativeButton("Từ chối", (dialog, id) -> {
                                                       dialog.dismiss();
                                                   });
                                            builder.create().show();
                                        }
                                    }
                                });
                            }
                        }
                    } else if (stage == 4) {
                        // Reset Dashboard và chuyển về Home
                        dashboardViewModel.resetDashboard();
                        // Chuyển về trang Home
                        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                        navController.navigate(R.id.navigation_home);
                    }
                }
            }
        });

        RecyclerView recyclerView = binding.productsRecyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        productAdapter = new ProductAdapter();
        recyclerView.setAdapter(productAdapter);

        // Lắng nghe thay đổi từ Realtime Database
        DatabaseReference productsRef = FirebaseDatabase.getInstance().getReference()
            .child("phampho1103")
            .child("products");

        productsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot productSnapshot : snapshot.getChildren()) {
                    Product product = new Product();
                    product.setProductId(productSnapshot.child("productId").getValue(String.class));
                    product.setName(productSnapshot.child("name").getValue(String.class));
                    product.setPrice(productSnapshot.child("price").getValue(Double.class));
                    product.setQuantity(productSnapshot.child("quantity").getValue(Integer.class));
                    product.setProductImage(productSnapshot.child("productImage").getValue(String.class));
                    product.setOrigin(productSnapshot.child("origin").getValue(String.class));
                    product.setDescription(productSnapshot.child("description").getValue(String.class));
                    
                    productAdapter.addOrUpdateProduct(product);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("DashboardFragment", "Lỗi đọc dữ liệu: " + error.getMessage());
            }
        });

        return root;
    }

    private void showNFCSettings() {
        Toast.makeText(requireContext(), 
            "Vui lòng bật NFC để tiếp tục", 
            Toast.LENGTH_LONG).show();
        startActivityForResult(
            new Intent(Settings.ACTION_NFC_SETTINGS),
            ENABLE_NFC_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ENABLE_NFC_REQUEST_CODE) {
            if (nfcAdapter.isEnabled()) {
                binding.textDashboard.setText(
                    "Hãy quét điện thoại tới các sản phẩm mà bạn muốn mua");
                binding.muahangImage.setImageResource(R.drawable.muahang2);
                binding.stageImage.setImageResource(R.drawable.stage2);
                testButton.setText("Hoàn thành mua hàng");
            } else {
                Toast.makeText(requireContext(), 
                    "Cần bật NFC để sử dụng tính năng này", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            Intent intent = new Intent(requireContext(), requireActivity().getClass())
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            
            pendingIntent = PendingIntent.getActivity(requireContext(), 0, 
                intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
                
            IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
            try {
                ndef.addDataType("*/*");
            } catch (IntentFilter.MalformedMimeTypeException e) {
                throw new RuntimeException("MimeType không hợp lệ", e);
            }
            
            intentFiltersArray = new IntentFilter[]{ndef};
            techListsArray = new String[][]{new String[]{Ndef.class.getName()}};
            
            nfcAdapter.enableForegroundDispatch(requireActivity(), 
                pendingIntent, intentFiltersArray, techListsArray);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(requireActivity());
        }
    }
}