package com.example.uitpay.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> mText;
    private final MutableLiveData<String> mName;
    private final MutableLiveData<String> mUserImage;
    private final FirebaseFirestore db;

    public HomeViewModel() {
        mText = new MutableLiveData<>();
        mName = new MutableLiveData<>();
        mUserImage = new MutableLiveData<>();
        db = FirebaseFirestore.getInstance();
        
        // Lấy dữ liệu từ Firestore
        db.collection("user")
          .document("id001")
          .get()
          .addOnSuccessListener(documentSnapshot -> {
              if (documentSnapshot.exists()) {
                  Long sotien = documentSnapshot.getLong("sotien");
                  String name = documentSnapshot.getString("name");
                  String userImage = documentSnapshot.getString("userimage");
                  
                  mText.setValue("Số tiền hiện tại: " + sotien + " VND");
                  mName.setValue("Xin chào, " + name);
                  mUserImage.setValue(userImage);
              }
          })
          .addOnFailureListener(e -> {
              mText.setValue("Lỗi khi lấy dữ liệu: " + e.getMessage());
              mName.setValue("Lỗi khi lấy dữ liệu");
              mUserImage.setValue(null);
          });
    }

    public LiveData<String> getText() {
        return mText;
    }

    public LiveData<String> getName() {
        return mName;
    }

    public LiveData<String> getUserImage() {
        return mUserImage;
    }
}