package com.rokudoz.irecipe.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.rokudoz.irecipe.Account.LoginActivity;
import com.rokudoz.irecipe.AddPostActivity;
import com.rokudoz.irecipe.Models.FavoritePost;
import com.rokudoz.irecipe.Models.Friend;
import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.Models.Post;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.Models.UserWhoFaved;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.Adapters.PostAdapter;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FeedFragment extends Fragment implements PostAdapter.OnItemClickListener {
    private static final String TAG = "FeedFragment";

    public View view;

    private ProgressBar pbLoading;
    private FloatingActionButton fab;
    private int oldScrollYPosition = 0;

    //FireBase
    private FirebaseAuth.AuthStateListener mAuthListener;
    private User mUser;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference postsRef = db.collection("Posts");
    private CollectionReference usersReference = db.collection("Users");
    private FirebaseStorage mStorageRef;
    private ListenerRegistration userDetailsListener, currentSubCollectionListener, postsListener;

    private RecyclerView mRecyclerView;
    private PostAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private ArrayList<String> mDocumentIDs = new ArrayList<>();
    private ArrayList<Post> mPostList = new ArrayList<>();
    private List<String> userFavPostList = new ArrayList<>();
    private List<String> friends_userID_list = new ArrayList<>();
    private List<Friend> friendList = new ArrayList<>();
    private String loggedInUserDocumentId = "";
    private String userFavDocId = "";

    private DocumentSnapshot mLastQueriedDocument;

    public static FeedFragment newInstance() {
        FeedFragment fragment = new FeedFragment();
        return fragment;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_feed, container, false);
        }
        mUser = new User();
        pbLoading = view.findViewById(R.id.homeFragment_pbLoading);
        fab = view.findViewById(R.id.fab_add_recipe);
        mRecyclerView = view.findViewById(R.id.recycler_view);

        pbLoading.setVisibility(View.VISIBLE);
        mStorageRef = FirebaseStorage.getInstance();


        fab.setVisibility(View.INVISIBLE);
        buildRecyclerView();
        setupFirebaseAuth();


        return view; // HAS TO BE THE LAST ONE ---------------------------------
    }


    @Override
    public void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener(mAuthListener);
    }


    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(mAuthListener);
        }
        DetatchFirestoreListeners();
    }

    private void DetatchFirestoreListeners() {
        if (currentSubCollectionListener != null) {
            currentSubCollectionListener.remove();
            currentSubCollectionListener = null;
        }
        if (userDetailsListener != null) {
            userDetailsListener.remove();
            userDetailsListener = null;
        }
        if (postsListener != null) {
            postsListener.remove();
            postsListener = null;
        }
    }


    private void buildRecyclerView() {
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getContext());

        mAdapter = new PostAdapter(mPostList);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(FeedFragment.this);


        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy > 0) {
                    // Scroll Down
                    if (fab.isShown()) {
                        fab.hide();
                    }
                } else if (dy < 0) {
                    // Scroll Up
                    if (!fab.isShown()) {
                        fab.show();
                    }
                }
            }
        });
    }

    private void getCurrentUserDetails() {
//        usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
//            @Override
//            public void onSuccess(final DocumentSnapshot documentSnapshot) {
//                List<Ingredient> userIngredient_list = new ArrayList<>();
//                final User user = documentSnapshot.toObject(User.class);
//
//                mUser = documentSnapshot.toObject(User.class);
//                loggedInUserDocumentId = documentSnapshot.getId();
//                userFavPostList = new ArrayList<>();
//
//                if (!friends_userID_list.contains(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
//                    friends_userID_list.add(FirebaseAuth.getInstance().getCurrentUser().getUid());
//                }
//
//                usersReference.document(user.getUser_id()).collection("FriendList").get()
//                        .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
//                            @Override
//                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
//                                for (QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots) {
//                                    Friend friend = queryDocumentSnapshot.toObject(Friend.class);
//                                    if (!friendList.contains(friend)) {
//                                        if (friend.getFriend_status().equals("friends") || friend.getFriend_status().equals("friend_request_accepted"))
//                                            friendList.add(friend);
//                                    }
//                                }
//                                for (Friend friend : friendList) {
//                                    if (!friends_userID_list.contains(friend.getFriend_user_id()))
//                                        friends_userID_list.add(friend.getFriend_user_id());
//                                }
//
//                                usersReference.document(user.getUser_id()).collection("FavoritePosts").get()
//                                        .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
//                                            @Override
//                                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
//                                                if (queryDocumentSnapshots != null) {
//                                                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
//                                                        String favPostID = documentSnapshot.getId();
//                                                        if (!userFavPostList.contains(favPostID))
//                                                            userFavPostList.add(favPostID);
//                                                    }
//
//                                                    performQuery();
//                                                }
//                                            }
//                                        });
//
//
//                            }
//                        });
//            }
//        });

        usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "onEvent: ", e);
                    return;
                }
                List<Ingredient> userIngredient_list = new ArrayList<>();
                final User user = documentSnapshot.toObject(User.class);

                mUser = documentSnapshot.toObject(User.class);
                loggedInUserDocumentId = documentSnapshot.getId();
                userFavPostList = new ArrayList<>();

                if (!friends_userID_list.contains(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                    friends_userID_list.add(FirebaseAuth.getInstance().getCurrentUser().getUid());
                }

                usersReference.document(user.getUser_id()).collection("FriendList").addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "onEvent: ", e);
                            return;
                        }
                        for (QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots) {
                            Friend friend = queryDocumentSnapshot.toObject(Friend.class);
                            if (!friendList.contains(friend)) {
                                if (friend.getFriend_status().equals("friends") || friend.getFriend_status().equals("friend_request_accepted"))
                                    friendList.add(friend);
                            }
                        }
                        for (Friend friend : friendList) {
                            if (!friends_userID_list.contains(friend.getFriend_user_id()))
                                friends_userID_list.add(friend.getFriend_user_id());
                        }
                        usersReference.document(user.getUser_id()).collection("FavoritePosts").addSnapshotListener(new EventListener<QuerySnapshot>() {
                            @Override
                            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                                if (e != null) {
                                    Log.w(TAG, "onEvent: ", e);
                                    return;
                                }
                                if (queryDocumentSnapshots != null) {
                                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                        String favPostID = documentSnapshot.getId();
                                        if (!userFavPostList.contains(favPostID))
                                            userFavPostList.add(favPostID);
                                    }

                                    performQuery();
                                }

                            }
                        });

                    }
                });


            }
        });
    }

    private void performQuery() {
        Query postsQuery = null;
        if (mLastQueriedDocument != null) {
            postsQuery = postsRef.orderBy("creation_date", Query.Direction.DESCENDING).whereIn("creatorId", friends_userID_list).whereEqualTo("privacy", "Everyone")
                    .startAfter(mLastQueriedDocument); // Necessary so we don't have the same results multiple times
//                                    .limit(3);
        } else {
            postsQuery = postsRef.orderBy("creation_date", Query.Direction.DESCENDING).whereIn("creatorId", friends_userID_list).whereEqualTo("privacy", "Everyone");
//                                    .limit(3);
        }
        PerformMainQuery(postsQuery);
        pbLoading.setVisibility(View.INVISIBLE);

        initializeRecyclerViewAdapterOnClicks();


    }

    private void PerformMainQuery(Query notesQuery) {

        postsListener = notesQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots,
                                @javax.annotation.Nullable FirebaseFirestoreException e) {
                if (queryDocumentSnapshots != null) {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Post post = document.toObject(Post.class);
                        post.setDocumentId(document.getId());

                        if (userFavPostList != null && userFavPostList.contains(document.getId())) {
                            post.setFavorite(true);
                        } else {
                            post.setFavorite(false);
                        }
                        if (!mDocumentIDs.contains(document.getId())) {
                            mDocumentIDs.add(document.getId());
                            mPostList.add(post);
                        } else {
                            Log.d(TAG, "onEvent: Already Contains docID");
                        }

                    }

                    if (queryDocumentSnapshots.getDocuments().size() != 0) {
                        mLastQueriedDocument = queryDocumentSnapshots.getDocuments()
                                .get(queryDocumentSnapshots.getDocuments().size() - 1);
                    }
                } else {
                    Log.d(TAG, "onEvent: Querry result is null");
                }
                mAdapter.notifyDataSetChanged();

            }
        });
    }

    private void initializeRecyclerViewAdapterOnClicks() {
        mAdapter.setOnItemClickListener(new PostAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                String id = mDocumentIDs.get(position);
                Navigation.findNavController(view).navigate(FeedFragmentDirections.actionFeedFragmentToPostDetailed(id));
            }

            @Override
            public void onFavoriteClick(final int position) {

                ////////////////////////////////////////////////

                String id = mDocumentIDs.get(position);
                DocumentReference currentRecipeRef = postsRef.document(id);
                final CollectionReference currentRecipeSubCollection = currentRecipeRef.collection("UsersWhoFaved");

                mUser.setFavoriteRecipes(userFavPostList);
                DocumentReference currentUserRef = usersReference.document(loggedInUserDocumentId);


                Log.d(TAG, "onFavoriteClick: " + mPostList.get(position).getDocumentId());

                if (userFavPostList == null) {
                    userFavPostList = new ArrayList<>();
                }
                if (userFavPostList.contains(id)) {
                    currentUserRef.collection("FavoritePosts").document(id).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "onSuccess: Deleted from user FAV posts");
                        }
                    });
                    userFavPostList.remove(id);
                    mPostList.get(position).setFavorite(false);

                    currentRecipeSubCollection.document(mUser.getUser_id()).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "onSuccess: Deleted userwhoFaved");
                        }
                    });

                } else {
                    userFavPostList.add(id);
                    mPostList.get(position).setFavorite(true);
                    UserWhoFaved userWhoFaved = new UserWhoFaved(mUser.getUser_id(), null);
                    currentRecipeSubCollection.document(mUser.getUser_id()).set(userWhoFaved);

                    FavoritePost favoritePost = new FavoritePost(null);
                    currentUserRef.collection("FavoritePosts").document(id).set(favoritePost);
                    Log.d(TAG, "onFavoriteClick: Added to favorites");
                }


                mAdapter.notifyDataSetChanged();


                /////////////////////////////////////
            }

        });
    }


    public void navigateToAddPost() {
        Intent intent = new Intent(getContext(), AddPostActivity.class);
        startActivity(intent);
    }

    /*
        ----------------------------- Firebase setup ---------------------------------
     */
    private void setupFirebaseAuth() {
        Log.d(TAG, "setupFirebaseAuth: started");

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {

                    //check if email is verified
                    if (user.isEmailVerified()) {
//                        Log.d(TAG, "onAuthStateChanged: signed_in: " + user.getUid());
//                        Toast.makeText(MainActivity.this, "Authenticated with: " + user.getEmail(), Toast.LENGTH_SHORT).show();
//                        fab.show();
                        fab.setVisibility(View.VISIBLE);
                        fab.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                navigateToAddPost();
                            }
                        });

                        //If use is authenticated, perform query
                        getCurrentUserDetails();
                    } else {
                        Toast.makeText(getContext(), "Email is not Verified\nCheck your Inbox", Toast.LENGTH_SHORT).show();
                        FirebaseAuth.getInstance().signOut();
                    }

                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged: signed_out");
                    Toast.makeText(getContext(), "Not logged in", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    getActivity().finish();
                }
                // ...
            }
        };
    }

    @Override
    public void onItemClick(int position) {

    }

    @Override
    public void onFavoriteClick(int position) {

    }

}
