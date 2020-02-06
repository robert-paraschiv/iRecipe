package com.rokudoz.irecipe.Fragments;


import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.rokudoz.irecipe.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class UsersWhoLiked extends Fragment {


    public UsersWhoLiked() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_users_who_liked, container, false);
    }

}
