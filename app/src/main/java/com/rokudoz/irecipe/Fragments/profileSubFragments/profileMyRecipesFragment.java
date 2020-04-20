package com.rokudoz.irecipe.Fragments.profileSubFragments;


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
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
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
import com.rokudoz.irecipe.Fragments.profileSubFragments.ProfileFragmentDirections;
import com.rokudoz.irecipe.Models.FavoriteRecipe;
import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.Models.UserWhoFaved;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.Adapters.RecipeAdapter;
import com.rokudoz.irecipe.Utils.Adapters.RecipeWithAdsAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class profileMyRecipesFragment extends Fragment implements RecipeWithAdsAdapter.OnItemClickListener {
    private static final String TAG = "profileMyRecipesFragmen";

    private View view;
    private ProgressBar pbLoading;

    //Ads
    AdLoader adLoader;
    List<UnifiedNativeAd> nativeAds = new ArrayList<>();
    private static final int NUMBER_OF_ADS = 5;
    private int nrRecipesLoaded = 0;
    private int nrOfAdsLoaded = 0;
    private int indexOfAdToLoad = 0;
    private int indexToAd = 3;


    private RecyclerView mRecyclerView;
    private RecipeWithAdsAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    //FireBase
    private FirebaseAuth.AuthStateListener mAuthListener;
    private User mUser;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference recipeRef = db.collection("Recipes");
    private CollectionReference usersReference = db.collection("Users");
    private ListenerRegistration userDetailsListener, recipesListener;
    private FirebaseStorage mStorageRef;

    private ArrayList<Object> mRecipeList = new ArrayList<>();
    private String userFavDocId = "";

    private DocumentSnapshot mLastQueriedDocument;


    public profileMyRecipesFragment() {
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
            view = inflater.inflate(R.layout.fragment_profile_my_recipes, container, false);
        } catch (InflateException e) {
            Log.e(TAG, "onCreateView: ", e);
        }

        mUser = new User();
        pbLoading = view.findViewById(R.id.profileMyRecipesFragment_pbLoading);
        mRecyclerView = view.findViewById(R.id.profileMyRecipesFragment_recycler_view);

        pbLoading.setVisibility(View.VISIBLE);
        mStorageRef = FirebaseStorage.getInstance();


        buildRecyclerView();

//        loadNativeAds();
        return view;
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
        getCurrentUserDetails();
    }

    @Override
    public void onStop() {
        super.onStop();
        DetachFirestoneListeners();
    }

    private void DetachFirestoneListeners() {
        if (userDetailsListener != null) {
            userDetailsListener.remove();
            userDetailsListener = null;
        }
        if (recipesListener != null) {
            recipesListener.remove();
            recipesListener = null;
        }
    }

    private void buildRecyclerView() {
        Log.d(TAG, "buildRecyclerView: ");
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getContext());

        mAdapter = new RecipeWithAdsAdapter(mRecipeList);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(profileMyRecipesFragment.this);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(1)) {
                    performQuery();
                } else if (!recyclerView.canScrollVertically(-1)) {
                    performQuery();
                }
            }
        });
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
        Query recipesQuery = null;
        if (mLastQueriedDocument != null) {
            recipesQuery = recipeRef.whereEqualTo("creator_docId", mUser.getUser_id())
                    .startAfter(mLastQueriedDocument); // Necessary so we don't have the same results multiple times
//                                    .limit(3);
        } else {
            recipesQuery = recipeRef.whereEqualTo("creator_docId", mUser.getUser_id());
//                                    .limit(3);
        }
        PerformMainQuery(recipesQuery);
        pbLoading.setVisibility(View.INVISIBLE);
    }

    private void PerformMainQuery(Query recipesQuery) {

        recipesListener = recipesQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "onEvent: ", e);
                    return;
                }
                if (queryDocumentSnapshots != null) {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        final Recipe recipe = document.toObject(Recipe.class);
                        recipe.setDocumentId(document.getId());

                        ////////////////////////////////////////////////////////// LOGIC TO GET RECIPES HERE

                        //Check if current user liked the post or not
                        recipeRef.document(recipe.getDocumentId()).collection("UsersWhoFaved").document(mUser.getUser_id())
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
                                                recipe.setFavorite(true);
                                                mAdapter.notifyDataSetChanged();
                                            } else {
                                                recipe.setFavorite(false);
                                                mAdapter.notifyDataSetChanged();
                                            }
                                        } else {
                                            Log.d(TAG, "onEvent: NULL");
                                        }
                                    }
                                });
                        if (!mRecipeList.contains(recipe)) {
                            nrRecipesLoaded++;

                            mRecipeList.add(recipe);
                            if (nrRecipesLoaded >= 3) {
                                insertAdsInRecyclerView();
                                nrRecipesLoaded = 0;
                            }
                        } else {
                            mRecipeList.set(mRecipeList.indexOf(recipe), recipe);
                        }
                        mAdapter.notifyDataSetChanged();

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

    @Override
    public void onItemClick(int position) {
        Recipe recipe = (Recipe) mRecipeList.get(position);
        String id = recipe.getDocumentId();
        String title = recipe.getTitle();
        Log.d(TAG, "onItemClick: CLICKED " + title + " id " + id);
        if (Navigation.findNavController(view).getCurrentDestination().getId() == R.id.profileFragment)
            Navigation.findNavController(view).navigate(ProfileFragmentDirections.actionProfileFragmentToRecipeDetailedFragment(id));
    }

    @Override
    public void onFavoriteClick(int position) {
        Recipe recipe = (Recipe) mRecipeList.get(position);
        String id = recipe.getDocumentId();
        final String title = recipe.getTitle();
        DocumentReference currentRecipeRef = recipeRef.document(id);
        final CollectionReference currentRecipeSubCollection = currentRecipeRef.collection("UsersWhoFaved");

        Log.d(TAG, "onFavoriteClick: " + recipe.getDocumentId());

        if (recipe.getFavorite()) {
            recipe.setFavorite(false);

            WriteBatch writeBatch = db.batch();
            writeBatch.delete(usersReference.document(mUser.getUser_id()).collection("FavoriteRecipes").document(id));
            writeBatch.delete(currentRecipeSubCollection.document(mUser.getUser_id()));

            writeBatch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Toast.makeText(getContext(), "Removed from favorites", Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            recipe.setFavorite(true);
            UserWhoFaved userWhoFaved = new UserWhoFaved(mUser.getUser_id(), mUser.getName(), mUser.getUserProfilePicUrl(), null);
            FavoriteRecipe favoriteRecipe = new FavoriteRecipe(null);

            WriteBatch writeBatch = db.batch();
            writeBatch.set(usersReference.document(mUser.getUser_id()).collection("FavoriteRecipes").document(id), favoriteRecipe);
            writeBatch.set(currentRecipeSubCollection.document(mUser.getUser_id()), userWhoFaved);
            writeBatch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Toast.makeText(getContext(), "Added " + title + " to favorites", Toast.LENGTH_SHORT).show();
                }
            });

        }
        mAdapter.notifyItemChanged(position);
    }
}
