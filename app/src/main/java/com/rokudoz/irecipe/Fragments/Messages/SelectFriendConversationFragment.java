package com.rokudoz.irecipe.Fragments.Messages;


import android.os.Bundle;

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

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.rokudoz.irecipe.Models.Friend;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.Adapters.FriendAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class SelectFriendConversationFragment extends Fragment implements FriendAdapter.OnItemClickListener {
    private static final String TAG = "SelectFriendToOpenConve";

    private View view;

    private RecyclerView mRecyclerView;
    private FriendAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    //FireBase
    private FirebaseAuth.AuthStateListener mAuthListener;
    private User mUser;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference recipeRef = db.collection("Recipes");
    private CollectionReference usersReference = db.collection("Users");
    private ListenerRegistration userDetailsListener, userFriendListListener;
    private FirebaseStorage mStorageRef;

    private ArrayList<Friend> mFriendList = new ArrayList<>();
    private List<String> userFavRecipesList = new ArrayList<>();
    private String userFavDocId = "";

    private DocumentSnapshot mLastQueriedDocument;


    public SelectFriendConversationFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null) {
                parent.removeView(view);
            }
        }
        try {
            view = inflater.inflate(R.layout.fragment_select_friend_to_open_converstation, container, false);
        } catch (InflateException e) {
            Log.e(TAG, "onCreateView: ", e);
        }

        if (getActivity() != null) {
            BottomNavigationView navBar = getActivity().findViewById(R.id.bottom_navigation);
            navBar.setVisibility(View.VISIBLE);
            getActivity().findViewById(R.id.banner_cardView).setVisibility(View.INVISIBLE);
        }

        mUser = new User();
        mRecyclerView = view.findViewById(R.id.selectFriend_recycler_view);

        mStorageRef = FirebaseStorage.getInstance();


        buildRecyclerView();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        getCurrentUserDetails();
    }

    @Override
    public void onStop() {
        super.onStop();
        DetachFireStoreListeners();
    }

    private void DetachFireStoreListeners() {
        if (userDetailsListener != null) {
            userDetailsListener.remove();
            userDetailsListener = null;
        }
        if (userFriendListListener != null) {
            userFriendListListener.remove();
            userFriendListListener = null;
        }
    }

    private void getCurrentUserDetails() {
        userDetailsListener = usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "onEvent: ", e);
                    return;
                }
                mUser = documentSnapshot.toObject(User.class);
                performQuery();
            }
        });
    }

    private void performQuery() {

        List<String> acceptedStatusList = new ArrayList<>();
        acceptedStatusList.add("friends");
        acceptedStatusList.add("friend_request_received");
        acceptedStatusList.add("friend_request_accepted");

        userFriendListListener = usersReference.document(mUser.getUser_id()).collection("FriendList").whereIn("friend_status", acceptedStatusList)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "onEvent: ", e);
                            return;
                        }
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            Friend friend = documentSnapshot.toObject(Friend.class);
                            if (!mFriendList.contains(friend)) {
                                mFriendList.add(friend);
                                mAdapter.notifyItemInserted(mFriendList.size() - 1);
                            }
                        }
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

        mAdapter.setOnItemClickListener(SelectFriendConversationFragment.this);
    }

    @Override
    public void onFriendClick(int position) {
        String id = mFriendList.get(position).getFriend_user_id();
        Log.d(TAG, "onItemClick: CLICKED " + " id " + id);
        if (Navigation.findNavController(view).getCurrentDestination().getId() == R.id.selectFriendToOpenConverstationFragment)
        Navigation.findNavController(view)
                .navigate(SelectFriendConversationFragmentDirections.actionSelectFriendToOpenConverstationFragmentToMessageFragment(id));
    }
}
