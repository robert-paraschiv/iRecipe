package com.rokudoz.irecipe.Fragments.profileSubFragments;


import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.rokudoz.irecipe.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class profileMyRecipesFragment extends Fragment {


    public profileMyRecipesFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile_my_recipes, container, false);
    }

}
