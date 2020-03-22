package com.rokudoz.irecipe.Fragments.recipesSubFragments;

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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.rokudoz.irecipe.AddIngredientActivity;
import com.rokudoz.irecipe.AddRecipesActivity;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.Adapters.SectionsPagerAdapter;

public class RecipesFragment extends Fragment {
    private static final String TAG = "RecipesFragment";

    public View view;
    private FloatingActionButton fab_addRecipes;
    private FloatingActionButton fab_addIngredient;

    //Tabs
    private SectionsPagerAdapter sectionsPagerAdapter;
    private ViewPager viewPager;

    public static RecipesFragment newInstance() {
        RecipesFragment fragment = new RecipesFragment();
        return fragment;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_recipes, container, false);
        }
        fab_addRecipes = view.findViewById(R.id.fab_add_recipe);

        MaterialButton searchRecipeBtn = view.findViewById(R.id.recipesFragment_searchRecipe_MaterialBtn);
        searchRecipeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SearchWithFilters.class);
                startActivity(intent);
            }
        });

        viewPager = view.findViewById(R.id.container);
        setupViewPager(viewPager);

        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);

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

    public void navigateToAddIngredient() {
        Intent intent = new Intent(getContext(), AddIngredientActivity.class);
        startActivity(intent);
    }


    private void setupViewPager(ViewPager viewPager) {
        SectionsPagerAdapter adapter = new SectionsPagerAdapter(getChildFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        adapter.addFragment(new recipesBreakfastFragment(), "Breakfast");
        adapter.addFragment(new recipesLunchFragment(), "Lunch");
        adapter.addFragment(new recipesDinnerFragment(), "Dinner");
        viewPager.setAdapter(adapter);
    }


}
