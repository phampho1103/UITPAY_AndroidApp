package com.example.uitpay.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.uitpay.R;
import com.example.uitpay.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                            ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textHome;
        final TextView nameView = binding.textName;
        final ImageView userImageView = binding.userImage;

        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        homeViewModel.getName().observe(getViewLifecycleOwner(), nameView::setText);
        homeViewModel.getUserImage().observe(getViewLifecycleOwner(), imageUrl -> {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Log.d("HomeFragment", "Loading image URL: " + imageUrl);
                Glide.with(requireContext())
                     .load(imageUrl)
                     .centerCrop()
                     .into(userImageView);
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}