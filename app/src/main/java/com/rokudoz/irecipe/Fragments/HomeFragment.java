package com.rokudoz.irecipe.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.SectionsPagerAdapter;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";

    public View view;

    //Tabs
    private SectionsPagerAdapter sectionsPagerAdapter;
    private ViewPager viewPager;

    public static HomeFragment newInstance() {
        HomeFragment fragment = new HomeFragment();
        return fragment;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_home, container, false);
        }

        viewPager = view.findViewById(R.id.container);
        setupViewPager(viewPager);

        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);


        return view; // HAS TO BE THE LAST ONE ---------------------------------
    }



    private void setupViewPager(ViewPager viewPager){
        SectionsPagerAdapter adapter = new SectionsPagerAdapter(getChildFragmentManager());
        adapter.addFragment(new homeBreakfastFragment(),"Breakfast");
        adapter.addFragment(new homeLunchFragment(),"Lunch");
        adapter.addFragment(new homeDinnerFragment(),"Dinner");
        viewPager.setAdapter(adapter);
    }


}
