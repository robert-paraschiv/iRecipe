package com.rokudoz.irecipe.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
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
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.rokudoz.irecipe.Account.LoginActivity;
import com.rokudoz.irecipe.AddPostActivity;
import com.rokudoz.irecipe.Models.FavoritePost;
import com.rokudoz.irecipe.Models.Friend;
import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.Models.Post;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.Models.UserWhoFaved;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.SearchUserActivity;
import com.rokudoz.irecipe.Utils.Adapters.FeedAdapter;
import com.rokudoz.irecipe.Utils.Adapters.PostAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FeedFragment extends Fragment implements FeedAdapter.OnItemClickListener {
    private static final String TAG = "FeedFragment";

    private static final int NUMBER_OF_ADS = 5;
    private int nrPostsLoaded = 0;
    private int nrOfAdsLoaded = 0;
    private int indexOfAdToLoad = 0;

    public View view;
    private TextView unreadMessagesTv;
    private MaterialCardView messagesCardView;

    private ProgressBar pbLoading;
    private FloatingActionButton fab;
    private int oldScrollYPosition = 0;

    //FireBase
    private FirebaseAuth.AuthStateListener mAuthListener;
    private User mUser;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference postsRef = db.collection("Posts");
    private CollectionReference usersReference = db.collection("Users");
    private CollectionReference recipesRef = db.collection("Recipes");
    private FirebaseStorage mStorageRef;
    private ListenerRegistration userDetailsListener, userFriendListListener, postLikesListener, userUnreadConversationsListener, postsListener,
            postCreatorDetailsListener, postCommentsNumberListener, postLikesNumberListener;

    //Ads
    AdLoader adLoader;
    List<UnifiedNativeAd> nativeAds = new ArrayList<>();

    private RecyclerView mRecyclerView;
    private FeedAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private ArrayList<Object> mPostList = new ArrayList<>();
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

        BottomNavigationView navBar = getActivity().findViewById(R.id.bottom_navigation);
        navBar.setVisibility(View.VISIBLE);

        mUser = new User();
        pbLoading = view.findViewById(R.id.homeFragment_pbLoading);
        fab = view.findViewById(R.id.fab_add_recipe);
        mRecyclerView = view.findViewById(R.id.recycler_view);
        MaterialButton messagesBtn = view.findViewById(R.id.feedFragment_messages_MaterialBtn);
        MaterialButton searchUserBtn = view.findViewById(R.id.feedFragment_searchUser_MaterialBtn);
        RelativeLayout messagesLayout = view.findViewById(R.id.feedFragment_messages_relativeLayout);
        unreadMessagesTv = view.findViewById(R.id.feedFragment_messages_UnreadText);
        messagesCardView = view.findViewById(R.id.feedFragment_messages_materialCard);

        pbLoading.setVisibility(View.VISIBLE);
        mStorageRef = FirebaseStorage.getInstance();

        messagesLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(view).navigate(FeedFragmentDirections.actionFeedFragmentToAllMessagesFragment());
            }
        });
        messagesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(view).navigate(FeedFragmentDirections.actionFeedFragmentToAllMessagesFragment());
            }
        });
        searchUserBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateToSearchUser();
            }
        });

        fab.setVisibility(View.INVISIBLE);
        buildRecyclerView();
        setupFirebaseAuth();
        loadNativeAds();

        return view; // HAS TO BE THE LAST ONE ---------------------------------
    }


    private void loadNativeAds() {
        if (getActivity() != null) {
            AdLoader.Builder builder = new AdLoader.Builder(Objects.requireNonNull(getActivity()), getResources().getString(R.string.admob_unit_id));
            adLoader = builder.forUnifiedNativeAd(new UnifiedNativeAd.OnUnifiedNativeAdLoadedListener() {
                @Override
                public void onUnifiedNativeAdLoaded(UnifiedNativeAd unifiedNativeAd) {
                    nativeAds.add(unifiedNativeAd);
                    if (!adLoader.isLoading()) {
                    }
                }
            }).withAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(int i) {
                    super.onAdFailedToLoad(i);
                    Log.d(TAG, "onAdFailedToLoad: " + i);

                }
            }).build();

            adLoader.loadAds(new AdRequest.Builder()
                    .addTestDevice("2F1C484BD502BA7D51AC78D75751AFE0") // Mi 9T Pro
                    .addTestDevice("B141CB779F883EF84EA9A32A7D068B76") // Redmi 5 Plus
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .build(), NUMBER_OF_ADS);
        }
    }

    private void insertAdsInRecyclerView() {
        if (nativeAds.size() <= 0) {
            return;
        }
        nrOfAdsLoaded++;


        if (indexOfAdToLoad < nativeAds.size()) {
            mPostList.add(mPostList.size(), nativeAds.get(indexOfAdToLoad));
            mAdapter.notifyDataSetChanged();
            indexOfAdToLoad++;
        } else {
            indexOfAdToLoad = 0;
            mPostList.add(mPostList.size(), nativeAds.get(indexOfAdToLoad));
            mAdapter.notifyDataSetChanged();
        }


        if (nrOfAdsLoaded == 5) {
            nativeAds = new ArrayList<>();
            loadNativeAds();
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener(mAuthListener);
        if (FirebaseAuth.getInstance().getCurrentUser() != null)
            getUnreadConversationNr();
    }


    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(mAuthListener);
        }
        DetachFireStoreListeners();
    }

    private void DetachFireStoreListeners() {
        if (userDetailsListener != null) {
            userDetailsListener.remove();
            userDetailsListener = null;
        }
        if (postsListener != null) {
            postsListener.remove();
            postsListener = null;
        }
        if (userFriendListListener != null) {
            userFriendListListener.remove();
            userFriendListListener = null;
        }
        if (postLikesListener != null) {
            postLikesListener.remove();
            postLikesListener = null;
        }
        if (userUnreadConversationsListener != null) {
            userUnreadConversationsListener.remove();
            userUnreadConversationsListener = null;
        }
        if (postCreatorDetailsListener != null) {
            postCreatorDetailsListener.remove();
            postCreatorDetailsListener = null;
        }
        if (postCommentsNumberListener != null) {
            postCommentsNumberListener.remove();
            postCommentsNumberListener = null;
        }
        if (postLikesNumberListener != null) {
            postLikesNumberListener.remove();
            postLikesNumberListener = null;
        }
    }


    private void buildRecyclerView() {
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getContext());

        mAdapter = new FeedAdapter(mPostList);

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
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(1)) {
                    performQuery();
                } else if (!recyclerView.canScrollVertically(0)) {
                    performQuery();
                }
            }
        });
    }

    private void getCurrentUserDetails() {
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

                usersReference.document(user.getUser_id()).collection("FriendList")
                        .addSnapshotListener(new EventListener<QuerySnapshot>() {
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
                                performQuery();
                            }
                        });
            }
        });
    }

    private void getUnreadConversationNr() {
        usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("Conversations")
                .whereEqualTo("type", "message_received").whereEqualTo("read", false).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "onEvent: ", e);
                    return;
                }
                if (queryDocumentSnapshots != null && queryDocumentSnapshots.size() > 0) {
                    String number = "";
                    number = "" + queryDocumentSnapshots.size();
                    unreadMessagesTv.setText(number);
                    messagesCardView.setVisibility(View.VISIBLE);

                }
                if (queryDocumentSnapshots != null && queryDocumentSnapshots.size() == 0) {
                    unreadMessagesTv.setText("");
                    messagesCardView.setVisibility(View.GONE);
                }
            }
        });
    }

    private void performQuery() {
        Query postsQuery = null;
        if (mLastQueriedDocument != null) {
            postsQuery = postsRef.orderBy("creation_date", Query.Direction.DESCENDING).whereIn("creatorId", friends_userID_list)
                    .whereEqualTo("privacy", "Everyone")
                    .startAfter(mLastQueriedDocument) // Necessary so we don't have the same results multiple times
                    .limit(10);
        } else {
            postsQuery = postsRef.orderBy("creation_date", Query.Direction.DESCENDING).whereIn("creatorId", friends_userID_list)
                    .whereEqualTo("privacy", "Everyone")
                    .limit(10);
        }
        PerformMainQuery(postsQuery);
        pbLoading.setVisibility(View.INVISIBLE);
    }

    private void PerformMainQuery(Query postsQuery) {

        postsQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots,
                                @javax.annotation.Nullable FirebaseFirestoreException e) {
                if (queryDocumentSnapshots != null) {
                    for (final QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        final Post post = document.toObject(Post.class);
                        post.setDocumentId(document.getId());

                        //Check if current user liked the post or not
                        postsRef.document(post.getDocumentId()).collection("UsersWhoFaved").document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                                    @Override
                                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                                        if (e != null) {
                                            Log.w(TAG, "onEvent: ", e);
                                            return;
                                        }
                                        if (documentSnapshot != null) {
                                            UserWhoFaved userWhoFaved = documentSnapshot.toObject(UserWhoFaved.class);
                                            if (userWhoFaved != null && userWhoFaved.getUserID().equals(mUser.getUser_id())) {
                                                post.setFavorite(true);
                                                mAdapter.notifyDataSetChanged();
                                            } else {
                                                post.setFavorite(false);
                                                mAdapter.notifyDataSetChanged();
                                            }
                                        } else {
                                            Log.d(TAG, "onEvent: NULL");
                                        }
                                    }
                                });

                        //Get post referenced Recipe details
                        recipesRef.document(post.getReferenced_recipe_docId()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                            @Override
                            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                                if (e != null) {
                                    Log.w(TAG, "onEvent: ", e);
                                    return;
                                }
                                if (documentSnapshot != null) {
                                    Recipe recipe = documentSnapshot.toObject(Recipe.class);
                                    post.setRecipe_name(recipe.getTitle());
                                    post.setRecipe_imageUrl(recipe.getImageUrls_list().get(0));
                                    mAdapter.notifyDataSetChanged();
                                }
                            }
                        });

                        if (!mPostList.contains(post)) {
                            mPostList.add(post);
                            nrPostsLoaded++;
                            if (nrPostsLoaded >= 5 && !adLoader.isLoading()) {
                                insertAdsInRecyclerView();
                                nrPostsLoaded = 0;
                            }
                            mAdapter.notifyDataSetChanged();
                        } else {
                            mPostList.set(mPostList.indexOf(post), post);
                        }
                    }

                    if (queryDocumentSnapshots.getDocuments().size() != 0) {
                        mLastQueriedDocument = queryDocumentSnapshots.getDocuments()
                                .get(queryDocumentSnapshots.getDocuments().size() - 1);
                    }

                } else {
                    Log.d(TAG, "onEvent: Querry result is null");
                }
                if (mPostList.isEmpty()) {
                    mPostList.add(new Post("", "", "", "", 0, 0
                            , "Add friends to see posts just like this one", ""
                            , false, "Everyone", null));
                    Log.d(TAG, "EMPTY: ");
                }
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    private void navigateToAddPost() {
        Intent intent = new Intent(getContext(), AddPostActivity.class);
        startActivity(intent);
    }

    private void navigateToSearchUser() {
        Intent intent = new Intent(getContext(), SearchUserActivity.class);
        startActivity(intent);
    }


    @Override
    public void onItemClick(int position) {
        Post post = (Post) mPostList.get(position);
        String id = post.getDocumentId();
        if (id != null && !id.equals("")) {
            Navigation.findNavController(view).navigate(FeedFragmentDirections.actionFeedFragmentToPostDetailed(id));
        } else {
            if (getActivity() != null)
                Toast.makeText(getActivity(), "This is just a demo card, add a post or add friends", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFavoriteClick(int position) {
        ////////////////////////////////////////////////
        Post post = (Post) mPostList.get(position);
        final String id = post.getDocumentId();
        if (id != null && !id.equals("")) {
            DocumentReference currentRecipeRef = postsRef.document(id);
            final CollectionReference currentRecipeSubCollection = currentRecipeRef.collection("UsersWhoFaved");

            DocumentReference currentUserRef = usersReference.document(loggedInUserDocumentId);

            Log.d(TAG, "onFavoriteClick: " + post.getDocumentId());

            if (post.getFavorite()) {
                WriteBatch batch = db.batch();
                batch.delete(currentUserRef.collection("FavoritePosts").document(id));
                batch.delete(currentRecipeSubCollection.document(mUser.getUser_id()));
                batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "onSuccess: like deleted from db");
                    }
                });
                userFavPostList.remove(id);
                post.setFavorite(false);
                mAdapter.notifyDataSetChanged();

            } else {
                post.setFavorite(true);
                UserWhoFaved userWhoFaved = new UserWhoFaved(mUser.getUser_id(), mUser.getName(), mUser.getUserProfilePicUrl(), null);
                FavoritePost favoritePost = new FavoritePost(null);

                WriteBatch batch = db.batch();
                batch.set(currentRecipeSubCollection.document(mUser.getUser_id()), userWhoFaved);
                batch.set(currentUserRef.collection("FavoritePosts").document(id), favoritePost);
                batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "onFavoriteClick: Added to favorites");
                    }
                });

                mAdapter.notifyDataSetChanged();
            }
        } else {
            if (getActivity() != null)
                Toast.makeText(getActivity(), "This is just a demo card, add a post or add friends", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onCommentClick(int position) {
        Post post = (Post) mPostList.get(position);
        String id = post.getDocumentId();
        if (id != null && !id.equals("")) {
            Navigation.findNavController(view).navigate(FeedFragmentDirections.actionFeedFragmentToPostComments(id));
        } else {
            if (getActivity() != null)
                Toast.makeText(getActivity(), "This is just a demo card, add a post or add friends", Toast.LENGTH_SHORT).show();
        }
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
}
