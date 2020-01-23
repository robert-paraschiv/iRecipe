package com.rokudoz.irecipe.Fragments.profileSubFragments;


import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.rokudoz.irecipe.Fragments.ProfileFragmentDirections;
import com.rokudoz.irecipe.Models.Friend;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.Adapters.FriendAdapter;

import java.util.ArrayList;
import java.util.List;

public class profileMyFriendList extends Fragment implements FriendAdapter.OnItemClickListener {
    private static final String TAG = "profileMyFriendList";
    private View view;
    private ProgressBar pbLoading;

    private RecyclerView mRecyclerView;
    private FriendAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    //FireBase
    private FirebaseAuth.AuthStateListener mAuthListener;
    private User mUser;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference recipeRef = db.collection("Recipes");
    private CollectionReference usersReference = db.collection("Users");
    private ListenerRegistration currentSubCollectionListener, userDetailsListener, recipesListener;
    private FirebaseStorage mStorageRef;

    private ArrayList<Friend> mFriendList = new ArrayList<>();
    private List<String> userFavRecipesList = new ArrayList<>();
    private String userFavDocId = "";

    private DocumentSnapshot mLastQueriedDocument;

    public profileMyFriendList() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        // Inflate the layout for this fragment
        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null) {
                parent.removeView(view);
            }
        }
        try {
            view = inflater.inflate(R.layout.fragment_profile_my_friend_list, container, false);
        } catch (InflateException e) {
            Log.e(TAG, "onCreateView: ", e);
        }

        mUser = new User();
        pbLoading = view.findViewById(R.id.profileFriendListFragment_pbLoading);
        mRecyclerView = view.findViewById(R.id.profileFriendListFragment_recycler_view);

        pbLoading.setVisibility(View.VISIBLE);
        mStorageRef = FirebaseStorage.getInstance();


        buildRecyclerView();
        getCurrentUserDetails();

        return view;
    }

    private void getCurrentUserDetails() {
        usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                mUser = documentSnapshot.toObject(User.class);
                performQuery();
            }
        });
    }

    private void performQuery() {
        pbLoading.setVisibility(View.INVISIBLE);
        initializeRecyclerViewAdapterOnClicks();

        List<String> acceptedStatusList = new ArrayList<>();
        acceptedStatusList.add("friends");
        acceptedStatusList.add("friend_request_received");
        acceptedStatusList.add("friend_request_accepted");

        usersReference.document(mUser.getUser_id()).collection("FriendList").whereIn("friend_status", acceptedStatusList).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            Friend friend = documentSnapshot.toObject(Friend.class);
                            if (!mFriendList.contains(friend)) {
                                mFriendList.add(friend);
                            }
                        }

                        mAdapter.notifyDataSetChanged();
                    }
                });


    }

    private void buildRecyclerView() {
        Log.d(TAG, "buildRecyclerView: ");
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getContext());

        mAdapter = new FriendAdapter(mFriendList);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(profileMyFriendList.this);
    }

    private void initializeRecyclerViewAdapterOnClicks() {
        mAdapter.setOnItemClickListener(new FriendAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                String id = mFriendList.get(position).getFriend_user_id();
                Log.d(TAG, "onItemClick: CLICKED " + " id " + id);

                Navigation.findNavController(view).navigate(ProfileFragmentDirections.actionProfileFragmentToUserProfileFragment2(id));
            }
        });
    }

    @Override
    public void onItemClick(int position) {

    }
}
