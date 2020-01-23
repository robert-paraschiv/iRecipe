package com.rokudoz.irecipe.Fragments.Messages;


import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.rokudoz.irecipe.Fragments.FeedFragmentDirections;
import com.rokudoz.irecipe.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class AllMessagesFragment extends Fragment {


    private View view;

    public AllMessagesFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_all_messages, container, false);

        BottomNavigationView navBar = getActivity().findViewById(R.id.bottom_navigation);
        navBar.setVisibility(View.GONE);

        FloatingActionButton openSelectFriend = view.findViewById(R.id.allMessages_addConversation_fab);
        openSelectFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(view).navigate(AllMessagesFragmentDirections.actionAllMessagesFragmentToSelectFriendToOpenConverstationFragment());
            }
        });
        return view;
    }

}
