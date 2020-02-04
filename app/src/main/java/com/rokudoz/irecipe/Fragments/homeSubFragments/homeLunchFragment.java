package com.rokudoz.irecipe.Fragments.homeSubFragments;

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
import com.google.firebase.storage.FirebaseStorage;
import com.rokudoz.irecipe.AddRecipesActivity;
import com.rokudoz.irecipe.Fragments.HomeFragmentDirections;
import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.Models.UserWhoFaved;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.Adapters.RecipeAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class homeLunchFragment extends Fragment implements RecipeAdapter.OnItemClickListener {
    private static final String TAG = "homeLunchFragment";

    public View view;

    private ProgressBar pbLoading;

    //FireBase
    private FirebaseAuth.AuthStateListener mAuthListener;
    private User mUser;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference recipeRef = db.collection("Recipes");
    private CollectionReference usersReference = db.collection("Users");
    private FirebaseStorage mStorageRef;
    private ListenerRegistration userDetailsListener, userIngredientsListener, recipesListener, recipesIngredientsListener, privateRecipesListener, privateRecipeIngredientsListener;

    private RecyclerView mRecyclerView;
    private RecipeAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private List<Recipe> mRecipeList = new ArrayList<>();
    private List<Ingredient> userIngredientList = new ArrayList<>();
    private String loggedInUserDocumentId = "";
    private String userFavDocId = "";

    private DocumentSnapshot mLastQueriedDocument;

    public static homeLunchFragment newInstance() {
        homeLunchFragment fragment = new homeLunchFragment();
        return fragment;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_home_lunch, container, false);
        }
        mUser = new User();
        pbLoading = view.findViewById(R.id.homeFragment_pbLoading);
        mRecyclerView = view.findViewById(R.id.recycler_view);

        pbLoading.setVisibility(View.VISIBLE);
        mStorageRef = FirebaseStorage.getInstance();
        buildRecyclerView();
        getUserIngredients();

        return view; // HAS TO BE THE LAST ONE ---------------------------------
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
                        performQuery();
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
    }


    private void buildRecyclerView() {
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getContext());

        mAdapter = new RecipeAdapter(mRecipeList);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(homeLunchFragment.this);
    }


    private void performQuery() {
        userDetailsListener = usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "onEvent: ", e);
                            return;
                        }
                        List<Ingredient> userIngredient_list = new ArrayList<>();
                        User user = documentSnapshot.toObject(User.class);

                        mUser = documentSnapshot.toObject(User.class);
                        loggedInUserDocumentId = documentSnapshot.getId();

                        Query recipesQuery = null;
                        if (mLastQueriedDocument != null) {
                            recipesQuery = recipeRef.whereEqualTo("category", "lunch").whereEqualTo("privacy", "Everyone")
                                    .startAfter(mLastQueriedDocument); // Necessary so we don't have the same results multiple times
//                                    .limit(3);
                        } else {
                            recipesQuery = recipeRef.whereEqualTo("category", "lunch").whereEqualTo("privacy", "Everyone");
//                                    .limit(3);
                        }

                        PerformMainQuery(recipesQuery, userIngredient_list);
                        pbLoading.setVisibility(View.INVISIBLE);

                        initializeRecyclerViewAdapterOnClicks();
                    }
                });

    }

    private void PerformMainQuery(Query notesQuery, final List<Ingredient> userIngredient_list) {

        recipesListener = notesQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots,
                                @javax.annotation.Nullable FirebaseFirestoreException e) {
                if (queryDocumentSnapshots != null) {
                    for (final QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        final Recipe recipe = document.toObject(Recipe.class);
                        recipe.setDocumentId(document.getId());

                        if (!mRecipeList.contains(recipe)) {
                            ////////////////////////////////////////////////////////// LOGIC TO GET RECIPES HERE
                            final List<Ingredient> recipeIngredientList = new ArrayList<>();
                            final List<String> missingIngredients = new ArrayList<>();

                            recipesIngredientsListener = recipeRef.document(recipe.getDocumentId()).collection("RecipeIngredients")
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
                                                if (!recipeIngredientList.contains(ingredient)) {
                                                    recipeIngredientList.add(ingredient);
                                                }
                                            }
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
                                            if (numberOfMissingIngredients < 3) {
                                                recipeRef.document(recipe.getDocumentId()).collection("UsersWhoFaved").addSnapshotListener(new EventListener<QuerySnapshot>() {
                                                    @Override
                                                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                                                        if (e != null) {
                                                            Log.w(TAG, "onEvent: ", e);
                                                            return;
                                                        }
                                                        if (queryDocumentSnapshots != null) {
                                                            Boolean fav = false;
                                                            for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                                                if (documentSnapshot.getId().equals(mUser.getUser_id())) {
                                                                    fav = true;
                                                                }
                                                            }
                                                            recipe.setFavorite(fav);
                                                            recipe.setNrOfLikes(queryDocumentSnapshots.size());
                                                            mAdapter.notifyDataSetChanged();
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
                                                            recipe.setNrOfComments(queryDocumentSnapshots.size());
                                                            mAdapter.notifyDataSetChanged();
                                                        }
                                                    }
                                                });
                                                if (!mRecipeList.contains(recipe)) {
                                                    recipe.setNrOfMissingIngredients(numberOfMissingIngredients);
                                                    recipe.setMissingIngredients(missingIngredients);
                                                    mRecipeList.add(recipe);
                                                } else {
                                                    recipe.setNrOfMissingIngredients(numberOfMissingIngredients);
                                                    recipe.setMissingIngredients(missingIngredients);
                                                    mRecipeList.set(mRecipeList.indexOf(recipe), recipe);
                                                }
                                                Collections.sort(mRecipeList);
                                                mAdapter.notifyDataSetChanged();
                                            }
                                        }
                                    });
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
        //Get Recipes where the recipes created by the logged in user are private
        Query privateRecipesQuery = null;
        if (mLastQueriedDocument != null) {
            privateRecipesQuery = recipeRef.whereEqualTo("category", "lunch").whereEqualTo("creator_docId", loggedInUserDocumentId)
                    .startAfter(mLastQueriedDocument); // Necessary so we don't have the same results multiple times
//                                    .limit(3);
        } else {
            privateRecipesQuery = recipeRef.whereEqualTo("category", "lunch").whereEqualTo("creator_docId", loggedInUserDocumentId);
//                                    .limit(3);
        }
        privateRecipesListener = privateRecipesQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (queryDocumentSnapshots != null) {
                    for (final QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        final Recipe recipe = document.toObject(Recipe.class);
                        recipe.setDocumentId(document.getId());

                        if (!mRecipeList.contains(recipe)) {

                            ////////////////////////////////////////////////////////// LOGIC TO GET RECIPES HERE

                            final List<Ingredient> recipeIngredientList = new ArrayList<>();
                            final List<String> missingIngredients = new ArrayList<>();

                            privateRecipeIngredientsListener = recipeRef.document(recipe.getDocumentId()).collection("RecipeIngredients")
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
                                                if (!recipeIngredientList.contains(ingredient)) {
                                                    recipeIngredientList.add(ingredient);
                                                }
                                            }
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
                                            if (numberOfMissingIngredients < 3) {
                                                if (!mRecipeList.contains(recipe)) {
                                                    recipe.setNrOfMissingIngredients(numberOfMissingIngredients);
                                                    recipe.setMissingIngredients(missingIngredients);
                                                    recipeRef.document(recipe.getDocumentId()).collection("UsersWhoFaved").addSnapshotListener(new EventListener<QuerySnapshot>() {
                                                        @Override
                                                        public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                                                            if (e != null) {
                                                                Log.w(TAG, "onEvent: ", e);
                                                                return;
                                                            }
                                                            if (queryDocumentSnapshots != null) {
                                                                Boolean fav = false;
                                                                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                                                    if (documentSnapshot.getId().equals(mUser.getUser_id())) {
                                                                        fav = true;
                                                                    }
                                                                }
                                                                recipe.setFavorite(fav);
                                                                recipe.setNrOfLikes(queryDocumentSnapshots.size());
                                                                mAdapter.notifyDataSetChanged();
                                                            }
                                                        }
                                                    });
                                                    mRecipeList.add(recipe);
                                                } else {
                                                    recipeRef.document(recipe.getDocumentId()).collection("UsersWhoFaved").addSnapshotListener(new EventListener<QuerySnapshot>() {
                                                        @Override
                                                        public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                                                            if (e != null) {
                                                                Log.w(TAG, "onEvent: ", e);
                                                                return;
                                                            }
                                                            if (queryDocumentSnapshots != null) {
                                                                Boolean fav = false;
                                                                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                                                    if (documentSnapshot.getId().equals(mUser.getUser_id())) {
                                                                        fav = true;
                                                                    }
                                                                }
                                                                recipe.setFavorite(fav);
                                                                recipe.setNrOfLikes(queryDocumentSnapshots.size());
                                                                mAdapter.notifyDataSetChanged();
                                                            }
                                                        }
                                                    });
                                                    recipe.setNrOfMissingIngredients(numberOfMissingIngredients);
                                                    recipe.setMissingIngredients(missingIngredients);
                                                    mRecipeList.set(mRecipeList.indexOf(recipe), recipe);
                                                }
                                                Collections.sort(mRecipeList);
                                                mAdapter.notifyDataSetChanged();
                                            }
                                        }
                                    });
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
        mAdapter.setOnItemClickListener(new RecipeAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                String id = mRecipeList.get(position).getDocumentId();
                String title = mRecipeList.get(position).getTitle();
                Log.d(TAG, "onItemClick: CLICKED " + title + " id " + id);

                Navigation.findNavController(view).navigate(HomeFragmentDirections.actionHomeFragmentToRecipeDetailedFragment(id));

            }

            @Override
            public void onFavoriteClick(final int position) {
                String id = mRecipeList.get(position).getDocumentId();
                String title = mRecipeList.get(position).getTitle();
                DocumentReference currentRecipeRef = recipeRef.document(id);
                final CollectionReference currentRecipeSubCollection = currentRecipeRef.collection("UsersWhoFaved");

                Log.d(TAG, "onFavoriteClick: " + mRecipeList.get(position).getDocumentId());

                if (mRecipeList.get(position).getFavorite()) {
                    mRecipeList.get(position).setFavorite(false);

                    currentRecipeSubCollection.document(mUser.getUser_id()).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(getContext(), "Removed from favorites", Toast.LENGTH_SHORT).show();
                        }
                    });

                } else {
                    mRecipeList.get(position).setFavorite(true);
                    UserWhoFaved userWhoFaved = new UserWhoFaved(mUser.getUser_id(), null);
                    currentRecipeSubCollection.document(mUser.getUser_id()).set(userWhoFaved);
                    Toast.makeText(getContext(), "Added " + title + " to favorites", Toast.LENGTH_SHORT).show();
                }

                mAdapter.notifyDataSetChanged();
            }

        });
    }


    public void navigateToAddRecipes() {
        Intent intent = new Intent(getContext(), AddRecipesActivity.class);
        startActivity(intent);
    }


    @Override
    public void onItemClick(int position) {

    }

    @Override
    public void onFavoriteClick(int position) {

    }

}
