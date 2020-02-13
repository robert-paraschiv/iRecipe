package com.rokudoz.irecipe.Fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
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
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.Models.UserWhoFaved;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.Adapters.RecipeAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FavoritesFragment extends Fragment implements RecipeAdapter.OnItemClickListener {
    private static final String TAG = "FavoritesFragment";

    public View view;

    private ProgressBar pbLoading;
    private Boolean hasBeenActiveBefore = false;

    //Ads
    AdLoader adLoader;
    List<UnifiedNativeAd> nativeAds = new ArrayList<>();
    private static final int NUMBER_OF_ADS = 5;
    private int nrRecipesLoaded = 0;
    private int nrOfAdsLoaded = 0;
    private int indexOfAdToLoad = 0;
    private int indexToAd = 3;


    //Firebase
    private FirebaseAuth.AuthStateListener mAuthListener;
    private User mUser;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference recipeRef = db.collection("Recipes");
    private CollectionReference usersReference = db.collection("Users");
    private FirebaseStorage mStorageRef;
    private ListenerRegistration favoriteRecipesListener, userDetailsListener, recipesListener, recipeLikesListener;

    private RecyclerView mRecyclerView;
    private RecipeAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private ArrayList<Object> mRecipeList = new ArrayList<>();

    private DocumentSnapshot mLastQueriedDocument;

    public static FavoritesFragment newInstance() {
        FavoritesFragment fragment = new FavoritesFragment();
        return fragment;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_favorites, container, false);
        }
        mUser = new User();
        pbLoading = view.findViewById(R.id.favoritesFragment_pbLoading);
        mRecyclerView = view.findViewById(R.id.favoritesRecycler_view);

        pbLoading.setVisibility(View.VISIBLE);
        mStorageRef = FirebaseStorage.getInstance();

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
            mRecipeList.add(mRecipeList.size(), nativeAds.get(indexOfAdToLoad));
            mAdapter.notifyDataSetChanged();
            indexOfAdToLoad++;
        } else {
            indexOfAdToLoad = 0;
            mRecipeList.add(mRecipeList.size(), nativeAds.get(indexOfAdToLoad));
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
//        recipeAdapter.startListening();
    }


    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(mAuthListener);
        }
        DetachFireStoreListeners();
        Log.d(TAG, "onStop: ");
    }


    private void DetachFireStoreListeners() {
        if (userDetailsListener != null) {
            userDetailsListener.remove();
            userDetailsListener = null;
        }
        if (favoriteRecipesListener != null) {
            favoriteRecipesListener.remove();
            favoriteRecipesListener = null;
        }
        if (recipesListener != null) {
            recipesListener.remove();
            recipesListener = null;
        }
        if (recipeLikesListener != null) {
            recipeLikesListener.remove();
            recipeLikesListener = null;
        }
    }


    public void buildRecyclerView() {
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getContext());

        mAdapter = new RecipeAdapter(mRecipeList);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(FavoritesFragment.this);

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


    private void performQuery() {
        userDetailsListener = usersReference.whereEqualTo("user_id", Objects.requireNonNull(FirebaseAuth.getInstance()
                .getCurrentUser()).getUid())
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "onEvent: ", e);
                            return;
                        }
                        //Gets User Ingredients from database
                        Map<String, Boolean> tags = new HashMap<>();
                        for (DocumentChange documentSnapshot : queryDocumentSnapshots.getDocumentChanges()) {
                            User user = documentSnapshot.getDocument().toObject(User.class);
                            mUser = documentSnapshot.getDocument().toObject(User.class);
                        }

                        Query notesQuery = null;
                        if (mLastQueriedDocument != null) {
                            notesQuery = recipeRef.whereEqualTo("favorite", false)
                                    .startAfter(mLastQueriedDocument).limit(10);
                        } else {
                            notesQuery = recipeRef.whereEqualTo("favorite", false).limit(10);
                        }

                        recipesListener = notesQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
                            @Override
                            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots,
                                                @javax.annotation.Nullable FirebaseFirestoreException e) {
                                if (queryDocumentSnapshots != null) {
                                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                        final Recipe recipe = document.toObject(Recipe.class);
                                        recipe.setDocumentId(document.getId());

                                        recipeLikesListener = recipeRef.document(recipe.getDocumentId()).collection("UsersWhoFaved").addSnapshotListener(new EventListener<QuerySnapshot>() {
                                            @Override
                                            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                                                if (e != null) {
                                                    Log.w(TAG, "onEvent: ", e);
                                                    return;
                                                }
                                                Boolean fav = false;
                                                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                                    if (documentSnapshot.getId().equals(mUser.getUser_id())) {
                                                        fav = true;
                                                    }
                                                }
                                                recipe.setNumber_of_likes(queryDocumentSnapshots.size());
                                                recipe.setFavorite(fav);
                                                if (fav) {
                                                    if (!mRecipeList.contains(recipe)) {
                                                        nrRecipesLoaded++;

                                                        mRecipeList.add(recipe);
                                                        if (nrRecipesLoaded >= 3) {
                                                            insertAdsInRecyclerView();
                                                            nrRecipesLoaded = 0;
                                                        }
                                                    } else {
                                                        mRecipeList.set(mRecipeList.indexOf(recipe), recipe);
                                                        mAdapter.notifyDataSetChanged();
                                                    }
                                                }
                                            }
                                        });
                                        recipeRef.document(recipe.getDocumentId()).collection("Comments").addSnapshotListener(new EventListener<QuerySnapshot>() {
                                            @Override
                                            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                                                if (e != null) {
                                                    Log.w(TAG, "onEvent: ", e);
                                                    return;
                                                }
                                                if (queryDocumentSnapshots != null) {
                                                    recipe.setNumber_of_comments(queryDocumentSnapshots.size());
                                                    mAdapter.notifyDataSetChanged();
                                                }
                                            }
                                        });
                                    }
                                    if (queryDocumentSnapshots.getDocuments().size() != 0) {
                                        mLastQueriedDocument = queryDocumentSnapshots.getDocuments()
                                                .get(queryDocumentSnapshots.getDocuments().size() - 1);
                                    }
                                }
                                mAdapter.notifyDataSetChanged();

                            }
                        });

                        pbLoading.setVisibility(View.INVISIBLE);
                        mAdapter.setOnItemClickListener(new RecipeAdapter.OnItemClickListener() {
                            @Override
                            public void onItemClick(int position) {
                                Recipe recipe = (Recipe) mRecipeList.get(position);
                                String id = recipe.getDocumentId();
                                Navigation.findNavController(view).navigate(FavoritesFragmentDirections.actionFavoritesFragmentToRecipeDetailedFragment(id));

                            }

                            @Override
                            public void onFavoriteClick(final int position) {
                                Recipe recipe = (Recipe) mRecipeList.get(position);
                                final String id = recipe.getDocumentId();
                                String title = recipe.getTitle();
                                DocumentReference currentRecipeRef = recipeRef.document(id);
                                final CollectionReference currentRecipeSubCollection = currentRecipeRef.collection("UsersWhoFaved");

                                if (recipe.getFavorite()) {
                                    recipe.setFavorite(false);
                                    mRecipeList.remove(position);
                                    currentRecipeSubCollection.document(mUser.getUser_id()).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            if (getContext() != null) {
                                                Toast.makeText(getContext(), "Removed from favorites", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });

                                } else {
                                    recipe.setFavorite(true);
                                    UserWhoFaved userWhoFaved = new UserWhoFaved(mUser.getUser_id(), mUser.getName(), mUser.getUserProfilePicUrl(), null);
                                    currentRecipeSubCollection.document(mUser.getUser_id()).set(userWhoFaved);
                                    Toast.makeText(getContext(), "Added " + title + " to favorites", Toast.LENGTH_SHORT).show();
                                }
                                mAdapter.notifyDataSetChanged();
                            }

                        });

                    }
                });

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

                        //If use is authenticated, perform query
                        performQuery();
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
