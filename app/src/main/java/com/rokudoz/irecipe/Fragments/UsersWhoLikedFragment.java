package com.rokudoz.irecipe.Fragments;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.rokudoz.irecipe.Fragments.profileSubFragments.profileMyFriendList;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.Models.UserWhoFaved;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.Adapters.FriendAdapter;
import com.rokudoz.irecipe.Utils.Adapters.UserWhoLikedAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 */
public class UsersWhoLikedFragment extends Fragment implements UserWhoLikedAdapter.OnItemClickListener {
    private static final String TAG = "UsersWhoLikedFragment";
    private View view;
    private RecyclerView mRecyclerView;
    private UserWhoLikedAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private String documentID = "";
    private String category = "";

    //FireBase
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference recipeRef = db.collection("Recipes");
    private CollectionReference usersReference = db.collection("Users");
    private ListenerRegistration likesListener;


    private List<UserWhoFaved> mUsersWhoFavedList = new ArrayList<>();

    public UsersWhoLikedFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null) {
                parent.removeView(view);
            }
        }
        try {
            view = inflater.inflate(R.layout.fragment_users_who_liked, container, false);
        } catch (InflateException e) {
            Log.e(TAG, "onCreateView: ", e);
        }

        mRecyclerView = view.findViewById(R.id.usersWhoLiked_recyclerView);

        UsersWhoLikedFragmentArgs usersWhoLikedFragmentArgs = UsersWhoLikedFragmentArgs.fromBundle(Objects.requireNonNull(getArguments()));
        documentID = usersWhoLikedFragmentArgs.getDocumentID();
        category = usersWhoLikedFragmentArgs.getCategory();

        buildRecyclerView();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        getLikes();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (likesListener != null) {
            likesListener.remove();
            likesListener = null;
        }
    }

    private void getLikes() {
        likesListener = db.collection(category).document(documentID).collection("UsersWhoFaved").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e == null && queryDocumentSnapshots != null) {
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        UserWhoFaved userWhoFaved = documentSnapshot.toObject(UserWhoFaved.class);
                        if (!mUsersWhoFavedList.contains(userWhoFaved)) {
                            mUsersWhoFavedList.add(userWhoFaved);
                            mAdapter.notifyDataSetChanged();
                        }
                    }
                }
            }
        });
    }

    private void buildRecyclerView() {
        Log.d(TAG, "buildRecyclerView: ");
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getContext());

        mAdapter = new UserWhoLikedAdapter(mUsersWhoFavedList);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(UsersWhoLikedFragment.this);
    }

    @Override
    public void onItemClick(int position) {
        Log.d(TAG, "onItemClick: " + position);
        if (Navigation.findNavController(view).getCurrentDestination().getId() == R.id.usersWhoLiked)
            Navigation.findNavController(view).navigate(UsersWhoLikedFragmentDirections.
                    actionUsersWhoLikedToUserProfileFragment2(mUsersWhoFavedList.get(position).getUserID()));
    }
}
