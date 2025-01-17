package com.rokudoz.irecipe.Fragments.recipesSubFragments;

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
import com.rokudoz.irecipe.Fragments.recipesSubFragments.RecipesFragmentDirections;
import com.rokudoz.irecipe.Models.FavoriteRecipe;
import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.Models.UserWhoFaved;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.Adapters.RecipeAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.rokudoz.irecipe.Fragments.recipesSubFragments.RecipesFragment.NR_OF_MISSING_INGREDIENTS;

public class recipesBreakfastFragment extends Fragment implements RecipeAdapter.OnItemClickListener {
    private static final String TAG = "recipesBreakfastFragment";


    public View view;

    //Ads
    AdLoader adLoader;
    List<UnifiedNativeAd> nativeAds = new ArrayList<>();
    private static final int NUMBER_OF_ADS = 5;
    private int nrRecipesLoaded = 0;
    private int nrOfAdsLoaded = 0;
    private int indexOfAdToLoad = 0;
    private int indexToAd = 3;

    private ProgressBar pbLoading;

    //FireBase
    private FirebaseAuth.AuthStateListener mAuthListener;
    private User mUser;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference recipeRef = db.collection("Recipes");
    private CollectionReference usersReference = db.collection("Users");
    private FirebaseStorage mStorageRef;
    private ListenerRegistration userDetailsListener, userIngredientsListener, recipesListener, recipeFavListener, privateRecipesListener, privateRecipeIngredientsListener, privateRecipeFavListener, recipesIngredientsListener;

    private RecyclerView mRecyclerView;
    private RecipeAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private List<Recipe> mRecipeList = new ArrayList<>();
    private List<Ingredient> userIngredientList = new ArrayList<>();
    private String loggedInUserDocumentId = "";
    private String userFavDocId = "";

    private DocumentSnapshot mLastQueriedDocument;

    public static recipesBreakfastFragment newInstance() {
        recipesBreakfastFragment fragment = new recipesBreakfastFragment();
        return fragment;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_home_breakfast, container, false);
        }
        mUser = new User();
        pbLoading = view.findViewById(R.id.homeFragment_pbLoading);
        mRecyclerView = view.findViewById(R.id.recycler_view);

        pbLoading.setVisibility(View.VISIBLE);
        mStorageRef = FirebaseStorage.getInstance();

        buildRecyclerView();

        return view; // HAS TO BE THE LAST ONE ---------------------------------
    }

    @Override
    public void onStart() {
        super.onStart();
        getUserIngredients();
    }

    private void getUserIngredients() {
        userIngredientsListener = usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("Ingredients")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "onEvent: ", e);
                            return;
                        }
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            Ingredient ingredient = documentSnapshot.toObject(Ingredient.class);
                            ingredient.setDocumentId(documentSnapshot.getId());
                            if (!userIngredientList.contains(ingredient)) {
                                userIngredientList.add(ingredient);
                            }
                        }
                        getCurrentUserDetails();
                    }
                });
    }


    @Override
    public void onStop() {
        super.onStop();
        DetachFireStoreListeners();
        Log.d(TAG, "onStop: ");
    }

    private void DetachFireStoreListeners() {
        if (userDetailsListener != null) {
            userDetailsListener.remove();
            userDetailsListener = null;
        }
        if (userIngredientsListener != null) {
            userIngredientsListener.remove();
            userIngredientsListener = null;
        }
        if (recipesListener != null) {
            recipesListener.remove();
            recipesListener = null;
        }
        if (recipeFavListener != null) {
            recipeFavListener.remove();
            recipeFavListener = null;
        }
        if (privateRecipesListener != null) {
            privateRecipesListener.remove();
            privateRecipesListener = null;
        }
        if (recipesIngredientsListener != null) {
            recipesIngredientsListener.remove();
            recipesIngredientsListener = null;
        }
        if (privateRecipeIngredientsListener != null) {
            privateRecipeIngredientsListener.remove();
            privateRecipeIngredientsListener = null;
        }
        if (privateRecipeFavListener != null) {
            privateRecipeFavListener.remove();
            privateRecipeFavListener = null;
        }
    }


    private void buildRecyclerView() {
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getContext());

        mAdapter = new RecipeAdapter(mRecipeList);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(recipesBreakfastFragment.this);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(1)) {
                    PerformMainQuery();
                    Log.d(TAG, "onScrollStateChanged: CANT SCROLL");
                }
                if (!recyclerView.canScrollVertically(-1)) {
                    PerformMainQuery();
                    Log.d(TAG, "onScrollStateChanged: CANT SCROLL");
                }
            }
        });
    }


    private void getCurrentUserDetails() {
        userDetailsListener = usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "onEvent: ", e);
                            return;
                        }

                        mUser = documentSnapshot.toObject(User.class);
                        loggedInUserDocumentId = documentSnapshot.getId();

                        PerformMainQuery();
                        pbLoading.setVisibility(View.INVISIBLE);

                    }
                });

    }

    private void PerformMainQuery() {
        Query recipesQuery;
        if (mLastQueriedDocument != null) {
            recipesQuery = recipeRef.whereEqualTo("category", "breakfast").whereEqualTo("privacy", "Everyone")
                    .orderBy("number_of_likes", Query.Direction.DESCENDING)
                    .startAfter(mLastQueriedDocument).limit(10);
        } else {
            recipesQuery = recipeRef.whereEqualTo("category", "breakfast").whereEqualTo("privacy", "Everyone")
                    .orderBy("number_of_likes", Query.Direction.DESCENDING).limit(10);
        }

        recipesListener = recipesQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots,
                                @javax.annotation.Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.e(TAG, "onEvent: ", e);
                    return;
                }
                if (queryDocumentSnapshots != null) {
                    for (final QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        final Recipe recipe = document.toObject(Recipe.class);
                        recipe.setDocumentId(document.getId());


                        ////////////////////////////////////////////////////////// LOGIC TO GET RECIPES HERE
                        List<Ingredient> recipeIngredientList = new ArrayList<>();
                        if (recipe.getIngredient_list() != null)
                            recipeIngredientList = recipe.getIngredient_list();

                        final List<String> missingIngredients = new ArrayList<>();

                        int numberOfMissingIngredients = 0;
                        for (Ingredient ingredient : recipeIngredientList) {
                            if (userIngredientList.contains(ingredient)) {
                                if (!userIngredientList.get(userIngredientList.indexOf(ingredient)).getOwned()) {
                                    numberOfMissingIngredients++;
                                    missingIngredients.add(ingredient.getName());
                                }

                            } else {
                                missingIngredients.add(ingredient.getName());
                                numberOfMissingIngredients++;
                            }
                        }
                        Log.d(TAG, "onEvent: " + recipe.getTitle() + " NR OF MISSING INGREDIENTS " + numberOfMissingIngredients);
                        if (numberOfMissingIngredients <= NR_OF_MISSING_INGREDIENTS) {
                            //Check if the recipe is favorite or not
                            recipeFavListener = recipeRef.document(recipe.getDocumentId()).collection("UsersWhoFaved").document(mUser.getUser_id())
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
                                recipe.setNrOfMissingIngredients(numberOfMissingIngredients);
                                recipe.setMissingIngredients(missingIngredients);

                                mRecipeList.add(recipe);
                            } else {
                                recipe.setNrOfMissingIngredients(numberOfMissingIngredients);
                                recipe.setMissingIngredients(missingIngredients);
                                mRecipeList.set(mRecipeList.indexOf(recipe), recipe);
                            }
//                            Collections.sort(mRecipeList);
                            mAdapter.notifyDataSetChanged();
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


        //Get Recipes where the recipes created by the logged in user are private
        Query privateRecipesQuery = null;
        if (mLastQueriedDocument != null) {
            privateRecipesQuery = recipeRef.whereEqualTo("category", "breakfast").whereEqualTo("creator_docId", loggedInUserDocumentId)
                    .orderBy("number_of_likes", Query.Direction.DESCENDING);
//                    .startAfter(mLastQueriedDocument); // Necessary so we don't have the same results multiple times
//                                    .limit(3);
        } else {
            privateRecipesQuery = recipeRef.whereEqualTo("category", "breakfast").whereEqualTo("creator_docId", loggedInUserDocumentId)
                    .orderBy("number_of_likes", Query.Direction.DESCENDING);
//                                    .limit(3);
        }
        privateRecipesListener = privateRecipesQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.e(TAG, "onEvent: ", e);
                    return;
                }
                if (queryDocumentSnapshots != null) {
                    for (final QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        final Recipe recipe = document.toObject(Recipe.class);
                        recipe.setDocumentId(document.getId());

                        ////////////////////////////////////////////////////////// LOGIC TO GET RECIPES HERE

                        List<Ingredient> recipeIngredientList = new ArrayList<>();
                        if (recipe.getIngredient_list() != null)
                            recipeIngredientList = recipe.getIngredient_list();
                        final List<String> missingIngredients = new ArrayList<>();

                        int numberOfMissingIngredients = 0;
                        for (Ingredient ingredient : recipeIngredientList) {
                            if (userIngredientList.contains(ingredient)) {
                                if (!userIngredientList.get(userIngredientList.indexOf(ingredient)).getOwned()) {
                                    numberOfMissingIngredients++;
                                    missingIngredients.add(ingredient.getName());
                                }

                            } else {
                                missingIngredients.add(ingredient.getName());
                                numberOfMissingIngredients++;
                            }
                        }
                        Log.d(TAG, "onEvent: " + recipe.getTitle() + " NR OF MISSING INGREDIENTS " + numberOfMissingIngredients);
                        if (numberOfMissingIngredients <= NR_OF_MISSING_INGREDIENTS) {
                            //Check if current user liked the post or not
                            privateRecipeFavListener = recipeRef.document(recipe.getDocumentId()).collection("UsersWhoFaved").document(mUser.getUser_id())
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
                                recipe.setNrOfMissingIngredients(numberOfMissingIngredients);
                                recipe.setMissingIngredients(missingIngredients);

                                mRecipeList.add(recipe);
                            } else {
                                recipe.setNrOfMissingIngredients(numberOfMissingIngredients);
                                recipe.setMissingIngredients(missingIngredients);
                                mRecipeList.set(mRecipeList.indexOf(recipe), recipe);
                            }
//                            Collections.sort(mRecipeList);
                            mAdapter.notifyDataSetChanged();
                        }


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
        if (Navigation.findNavController(view).getCurrentDestination().getId() == R.id.recipesFragment)
            Navigation.findNavController(view).navigate(RecipesFragmentDirections.actionRecipesFragmentToRecipeDetailedFragment(id));
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
