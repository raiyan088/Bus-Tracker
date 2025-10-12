package com.rr.bubtbustracker.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.rr.bubtbustracker.fragment.LoginFragment;
import com.rr.bubtbustracker.fragment.SignUpFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fa) {
        super(fa);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) return new LoginFragment();
        else return new SignUpFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
