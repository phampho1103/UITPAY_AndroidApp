package com.example.uitpay;

import android.os.Bundle;
import android.util.Log;
import android.nfc.NfcAdapter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.tech.Ndef;
import android.app.PendingIntent;
import android.content.IntentFilter;
import android.content.Intent;
import android.os.Parcelable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.uitpay.databinding.ActivityMainBinding;
import com.example.uitpay.ui.dashboard.DashboardFragment;
import com.example.uitpay.ui.dashboard.DashboardViewModel;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Khởi tạo Firebase trước
        FirebaseApp.initializeApp(this);

        try {
            // Sau đó mới bật persistence
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception e) {
            Log.e("Firebase", "Lỗi khi bật persistence", e);
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMessages != null && rawMessages.length > 0) {
                NdefMessage message = (NdefMessage) rawMessages[0];
                NdefRecord record = message.getRecords()[0];
                if (record != null) {
                    String payload = new String(record.getPayload());
                    // Loại bỏ 3 byte đầu tiên (header của NDEF)
                    String productId = payload.substring(3);
                    
                    Log.d("NFC", "Đọc được productId: " + productId);
                    
                    NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
                    Fragment currentFragment = navController.getCurrentDestination().getId() == R.id.navigation_dashboard ?
                        getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main)
                            .getChildFragmentManager().getFragments().get(0) : null;
                            
                    if (currentFragment instanceof DashboardFragment) {
                        DashboardViewModel viewModel = new ViewModelProvider((DashboardFragment)currentFragment)
                            .get(DashboardViewModel.class);
                        
                        // Kiểm tra productId trong database trước
                        FirebaseDatabase.getInstance().getReference()
                            .child("phampho1103")
                            .child("products")
                            .orderByChild("productId")
                            .equalTo(productId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (!snapshot.exists()) {
                                        // Kiểm tra tên sản phẩm trong Firestore
                                        viewModel.fetchProductById(productId);
                                    } else {
                                        Toast.makeText(MainActivity.this, 
                                            "Bạn đã thêm sản phẩm này rồi nhé", 
                                            Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e("MainActivity", "Lỗi kiểm tra productId: " + error.getMessage());
                                }
                            });
                    }
                }
            }
        }
    }

}