package com.rokudoz.irecipe.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.rokudoz.irecipe.AddRecipesActivity;
import com.rokudoz.irecipe.Fragments.homeSubFragments.homeBreakfastFragment;
import com.rokudoz.irecipe.Fragments.homeSubFragments.homeDinnerFragment;
import com.rokudoz.irecipe.Fragments.homeSubFragments.homeLunchFragment;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.SectionsPagerAdapter;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";

    public View view;
    private FloatingActionButton fab_addRecipes;

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
        fab_addRecipes = view.findViewById(R.id.fab_add_recipe);


        viewPager = view.findViewById(R.id.container);
        setupViewPager(viewPager);

        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);

        fab_addRecipes.show();
        fab_addRecipes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToAddRecipes();
            }
        });


        return view; // HAS TO BE THE LAST ONE ---------------------------------
    }

    public void navigateToAddRecipes() {
        Intent intent = new Intent(getContext(), AddRecipesActivity.class);
        startActivity(intent);
    }

//    public void navigateToUpdateRecipes() {
//        Intent intent = new Intent(getContext(), UpdateRecipesActivity.class);
//        startActivity(intent);
//    }


    private void setupViewPager(ViewPager viewPager) {
        SectionsPagerAdapter adapter = new SectionsPagerAdapter(getChildFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        adapter.addFragment(new homeBreakfastFragment(), "Breakfast");
        adapter.addFragment(new homeLunchFragment(), "Lunch");
        adapter.addFragment(new homeDinnerFragment(), "Dinner");
        viewPager.setAdapter(adapter);
    }


}
