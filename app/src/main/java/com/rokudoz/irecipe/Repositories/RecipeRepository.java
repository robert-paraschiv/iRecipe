package com.rokudoz.irecipe.Repositories;

import android.util.Log;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Models.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import androidx.lifecycle.MutableLiveData;

public class RecipeRepository {
    private static final String TAG = "RecipeRepository";
    private ArrayList<String> mDocumentIDs = new ArrayList<>();
    private ArrayList<Recipe> mRecipeList = new ArrayList<>();
    private ArrayList<String> favRecipes = new ArrayList<>();
    private String loggedInUserDocumentId = "";
    private String userFavDocId = "";
    private FirebaseAuth.AuthStateListener mAuthListener;
    private User mUser;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference recipeRef = db.collection("Recipes");
    private CollectionReference usersReference = db.collection("Users");
    private FirebaseStorage mStorageRef;
    private DocumentSnapshot mLastQueriedDocument;


    private static RecipeRepository instance;
    private ArrayList<Recipe> dataSet = new ArrayList<>();

    public static RecipeRepository getInstance() {
        if (instance == null) {
            instance = new RecipeRepository();
        }
        return instance;
    }

    public MutableLiveData<ArrayList<Recipe>> getRecipes() {
        final MutableLiveData<ArrayList<Recipe>> data = new MutableLiveData<>();

        usersReference.whereEqualTo("user_id", Objects.requireNonNull(FirebaseAuth.getInstance()
                .getCurrentUser()).getUid())
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots,
                                        @javax.annotation.Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "onEvent: ", e);
                            return;
                        }
                        //Gets User Ingredients from database
                        Map<String, Boolean> tags = new HashMap<>();
                        List<String> userIngredientsArray = new ArrayList<>();

                        for (DocumentChange documentSnapshot : queryDocumentSnapshots.getDocumentChanges()) {
                            User user = documentSnapshot.getDocument().toObject(User.class);
                            mUser = documentSnapshot.getDocument().toObject(User.class);
                            loggedInUserDocumentId = queryDocumentSnapshots.getDocuments().get(0).getId();
                            favRecipes = mUser.getFavoriteRecipes();
                            userIngredientsArray = mUser.getIngredient_array();

                            for (String tag : user.getTags().keySet()) {
                                tags.put(tag, Objects.requireNonNull(user.getTags().get(tag)));
                            }
//                            Toast.makeText(MainActivity.this, tags.toString(), Toast.LENGTH_SHORT).show();
                        }
                        Query recipesQuery = null;
                        if (mLastQueriedDocument != null) {
                            recipesQuery = recipeRef.whereLessThanOrEqualTo("tags", tags)
                                    .startAfter(mLastQueriedDocument); // Necessary so we don't have the same results multiple times
//                                    .limit(3);
                        } else {
                            recipesQuery = recipeRef.whereLessThanOrEqualTo("tags", tags);
//                                    .limit(3);
                        }

                        final List<String> finalUserIngredientsArray = userIngredientsArray;

                        recipesQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
                            @Override
                            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots,
                                                @javax.annotation.Nullable FirebaseFirestoreException e) {
                                if (queryDocumentSnapshots != null) {
                                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                        Recipe recipe = document.toObject(Recipe.class);
                                        recipe.setDocumentId(document.getId());


                                        if (favRecipes != null && favRecipes.contains(document.getId())) {
                                            recipe.setFavorite(true);
                                        } else {
                                            recipe.setFavorite(false);
                                        }
                                        if (!mDocumentIDs.contains(document.getId())) {
                                            mDocumentIDs.add(document.getId());
                                            if (recipe.getIngredient_array() != null && finalUserIngredientsArray != null) {
                                                //Check if recipe contains any ingredient that user has
                                                boolean noElementsInCommon = Collections.disjoint(recipe.getIngredient_array(), finalUserIngredientsArray);
                                                if (!noElementsInCommon && finalUserIngredientsArray.containsAll(recipe.getIngredient_array())) {
                                                    mRecipeList.add(recipe);
                                                    dataSet.add(recipe);
                                                    data.setValue(dataSet);
                                                } else {
                                                    Log.d(TAG, "onEvent: Rejected recipe: " + recipe.getTitle()
                                                            + ", ingredients: " + recipe.getIngredient_array().toString());
                                                }
                                            }
                                        }
                                    }
                                    if (queryDocumentSnapshots.getDocuments().size() != 0) {
                                        mLastQueriedDocument = queryDocumentSnapshots.getDocuments()
                                                .get(queryDocumentSnapshots.getDocuments().size() - 1);
                                    }
                                }
//                mAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                });

        data.setValue(dataSet);
        return data;
    }
}
