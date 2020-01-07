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
import com.rokudoz.irecipe.AddRecipesActivity;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.Models.UserWhoFaved;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.RecipeAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FeedFragment extends Fragment implements RecipeAdapter.OnItemClickListener {
    private static final String TAG = "FeedFragment";

    public View view;

    private ProgressBar pbLoading;
    private FloatingActionButton fab;

    //FireBase
    private FirebaseAuth.AuthStateListener mAuthListener;
    private User mUser;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference recipeRef = db.collection("Recipes");
    private CollectionReference usersReference = db.collection("Users");
    private FirebaseStorage mStorageRef;
    private ListenerRegistration userDetailsListener, currentSubCollectionListener, recipesListener;

    private RecyclerView mRecyclerView;
    private RecipeAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private ArrayList<String> mDocumentIDs = new ArrayList<>();
    private ArrayList<Recipe> mRecipeList = new ArrayList<>();
    private ArrayList<String> favRecipes = new ArrayList<>();
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


        fab.hide();
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
        if (recipesListener != null) {
            recipesListener.remove();
            recipesListener = null;
        }
    }


    private void buildRecyclerView() {
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getContext());

        mAdapter = new RecipeAdapter(mRecipeList);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(FeedFragment.this);
    }


    private void performQuery() {
        userDetailsListener = usersReference.whereEqualTo("user_id", Objects.requireNonNull(FirebaseAuth.getInstance()
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
                        Log.d(TAG, "onEvent: User ingredientsArray " + userIngredientsArray);
                        Query notesQuery = null;
                        if (mLastQueriedDocument != null) {
                            notesQuery = recipeRef
                                    .startAfter(mLastQueriedDocument); // Necessary so we don't have the same results multiple times
//                                    .limit(3);
                        } else {
                            notesQuery = recipeRef;
//                                    .limit(3);
                        }

                        PerformMainQuery(notesQuery, userIngredientsArray);

                        pbLoading.setVisibility(View.INVISIBLE);

                        initializeRecyclerViewAdapterOnClicks();

                    }
                });

    }

    private void PerformMainQuery(Query notesQuery, final List<String> userIngredientsArray) {
        recipesListener = notesQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
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
                            if (recipe.getIngredient_array() != null && userIngredientsArray != null) {

                                //See if the recipe has only [selected number] of ingredients more than the user has

                                int moreElements = 0;
                                int nrOfElementsInCommon = 0;

                                for (String recipeIngredient : recipe.getIngredient_array())
                                    for (String userIngredient : userIngredientsArray) {
                                        if (recipeIngredient.equals(userIngredient)) {
                                            nrOfElementsInCommon++;
                                        }
                                    }
                                moreElements = recipe.getIngredient_array().size() - nrOfElementsInCommon;

                                if (moreElements <= 3) {
                                    mRecipeList.add(recipe);
                                    Log.d(TAG, "onEvent: Accepted recipe " + recipe.getTitle()
                                            + " because it has " + moreElements + " more elements " + ", ingredients: "
                                            + recipe.getIngredient_array().toString());
                                } else {
                                    mDocumentIDs.remove(document.getId());
                                    Log.d(TAG, "onEvent: Rejected recipe: " + recipe.getTitle()
                                            + " because it has " + moreElements + " more elements " + ", ingredients: "
                                            + recipe.getIngredient_array().toString());
                                }
                            }
                        }
                    }

                    if (queryDocumentSnapshots.getDocuments().size() != 0) {
                        mLastQueriedDocument = queryDocumentSnapshots.getDocuments()
                                .get(queryDocumentSnapshots.getDocuments().size() - 1);
                    }
                    Log.d(TAG, "onEvent: querry size" + queryDocumentSnapshots.size());
                }
                mAdapter.notifyDataSetChanged();

            }
        });
    }

    private void initializeRecyclerViewAdapterOnClicks() {
        mAdapter.setOnItemClickListener(new RecipeAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                String id = mDocumentIDs.get(position);
                String title = mRecipeList.get(position).getTitle();
                String description = mRecipeList.get(position).getDescription();
                List<String> imageUrlList = mRecipeList.get(position).getImageUrl();
                Map<String, Boolean> ingredients = mRecipeList.get(position).getTags();
                String instructions = mRecipeList.get(position).getInstructions();
                Boolean isFavorite = mRecipeList.get(position).getFavorite();
                Integer numberOfFav = mRecipeList.get(position).getNumberOfFaves();

                String ingredientsString = "Ingredients:\n";
                for (String ingredient : ingredients.keySet()) {
                    if (ingredients.get(ingredient)) {
                        ingredientsString += "\n- " + ingredient;
                    }
                }

                // Navigation logic
                String[] favRecipesArray = new String[favRecipes.size()];
                for (int i = 0; i < favRecipes.size(); i++) {
                    favRecipesArray[i] = favRecipes.get(i);
                }
                //Image Url List to Array
                String[] imageUrlArray = new String[imageUrlList.size()];
                for (int i = 0; i < imageUrlList.size(); i++) {
                    imageUrlArray[i] = imageUrlList.get(i);
                }

                if (instructions == null)
                    instructions = " ";
                Navigation.findNavController(view).navigate(FeedFragmentDirections.actionSearchFragmentToRecipeDetailedFragment(id, numberOfFav));

            }

            @Override
            public void onFavoriteClick(final int position) {
                String id = mDocumentIDs.get(position);
                String title = mRecipeList.get(position).getTitle();
                DocumentReference currentRecipeRef = recipeRef.document(id);
                final CollectionReference currentRecipeSubCollection = currentRecipeRef.collection("UsersWhoFaved");

                mUser.setFavoriteRecipes(favRecipes);
                DocumentReference favRecipesRef = usersReference.document(loggedInUserDocumentId);


                Log.d(TAG, "onFavoriteClick: " + mRecipeList.get(position).getDocumentId());

                if (favRecipes == null) {
                    favRecipes = new ArrayList<>();
                }
                if (favRecipes.contains(id)) {
                    favRecipes.remove(id);
                    mRecipeList.get(position).setFavorite(false);

                    currentSubCollectionListener = currentRecipeSubCollection.whereEqualTo("userID", mUser.getUser_id())
                            .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                @Override
                                public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                                    if (e != null) {
                                        Log.w(TAG, "onEvent: ", e);
                                        return;
                                    }
                                    if (queryDocumentSnapshots != null && queryDocumentSnapshots.size() != 0) {
                                        userFavDocId = queryDocumentSnapshots.getDocuments().get(0).getId();
                                        Log.d(TAG, "onEvent: docID" + userFavDocId);
                                    }
                                    if (!mRecipeList.get(position).getFavorite())
                                        if (!userFavDocId.equals("")) {
                                            currentRecipeSubCollection.document(userFavDocId).delete()
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            Toast.makeText(getContext(), "Removed from favorites", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });


                                        } else {
                                            Log.d(TAG, "onFavoriteClick: empty docID");
                                        }

                                }
                            });

                } else {
                    favRecipes.add(id);
                    mRecipeList.get(position).setFavorite(true);
                    UserWhoFaved userWhoFaved = new UserWhoFaved(mUser.getUser_id(), null);
                    currentRecipeSubCollection.add(userWhoFaved);
                    Toast.makeText(getContext(), "Added " + title + " to favorites", Toast.LENGTH_SHORT).show();
                }

                mUser.setFavoriteRecipes(favRecipes);
                favRecipesRef.update("favoriteRecipes", favRecipes);


                mAdapter.notifyDataSetChanged();
            }

        });
    }


    public void navigateToAddRecipes() {
        Intent intent = new Intent(getContext(), AddRecipesActivity.class);
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
                        if (user.getEmail().equals("paraschivlongin@gmail.com")) {
                            fab.show();
                            fab.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    navigateToAddRecipes();
                                }
                            });
                        }
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
