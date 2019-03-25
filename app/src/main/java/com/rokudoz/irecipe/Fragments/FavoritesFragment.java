package com.rokudoz.irecipe.Fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
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
import com.google.firebase.storage.StorageReference;
import com.rokudoz.irecipe.Account.LoginActivity;
import com.rokudoz.irecipe.AddRecipesActivity;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.Models.UserWhoFaved;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.RecipeAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FavoritesFragment extends Fragment implements RecipeAdapter.OnItemClickListener {
    private static final String TAG = "FavoritesFragment";

    public View view;

    private ProgressBar pbLoading;
    private Boolean hasBeenActiveBefore = false;

    //Firebase
    private FirebaseAuth.AuthStateListener mAuthListener;
    private User mUser;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference recipeRef = db.collection("Recipes");
    private CollectionReference usersReference = db.collection("Users");
    private FirebaseStorage mStorageRef;
    private ListenerRegistration registration;

    private RecyclerView mRecyclerView;
    private RecipeAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private ArrayList<String> mDocumentIDs = new ArrayList<>();
    private ArrayList<Recipe> mRecipeList = new ArrayList<>();
    private ArrayList<String> favRecipes = new ArrayList<>();
    private String loggedInUserDocumentId = "";
    private String userFavDocId = "";

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

        return view; // HAS TO BE THE LAST ONE ---------------------------------
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
        if (registration != null) {
            registration.remove();
            registration = null;
        }

    }


    public void buildRecyclerView() {
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getContext());

        mAdapter = new RecipeAdapter(mRecipeList);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(FavoritesFragment.this);
    }


    private void performQuery() {
        usersReference.whereEqualTo("user_id", Objects.requireNonNull(FirebaseAuth.getInstance()
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
                            loggedInUserDocumentId = queryDocumentSnapshots.getDocuments().get(0).getId();
                            favRecipes = mUser.getFavoriteRecipes();
                            for (String tag : user.getTags().keySet()) {
                                tags.put(tag, Objects.requireNonNull(user.getTags().get(tag)));
                            }
//                            Toast.makeText(MainActivity.this, tags.toString(), Toast.LENGTH_SHORT).show();
                        }

                        Query notesQuery = null;
                        if (mLastQueriedDocument != null) {
                            notesQuery = recipeRef.whereEqualTo("favorite", false)
                                    .startAfter(mLastQueriedDocument); // Necessary so we don't have the same results multiple times
//                                    .limit(3);
                        } else {
                            notesQuery = recipeRef.whereEqualTo("favorite", false);
//                                    .limit(3);
                        }

                        notesQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
                            @Override
                            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots,
                                                @javax.annotation.Nullable FirebaseFirestoreException e) {
                                if (queryDocumentSnapshots != null) {
                                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                        Recipe recipe = document.toObject(Recipe.class);
                                        recipe.setDocumentId(document.getId());
                                        if (favRecipes != null && favRecipes.contains(document.getId()) && !mDocumentIDs.contains(document.getId())) {
                                            recipe.setFavorite(true);
                                            mDocumentIDs.add(document.getId()); // Need to add it only if it is fav
                                            mRecipeList.add(recipe);
                                        } else {
                                            recipe.setFavorite(false);
                                        }

//                        Log.d(TAG, "onComplete: got a new note. Position: " + (mNotes.size() - 1));
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
                                String id = mDocumentIDs.get(position);
                                String title = mRecipeList.get(position).getTitle();
                                String description = mRecipeList.get(position).getDescription();
                                String imageUrl = mRecipeList.get(position).getImageUrl();
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

                                RecipeDetailedFragment fragment = RecipeDetailedFragment.newInstance(id, title, description, ingredientsString
                                        , imageUrl, instructions, isFavorite, favRecipes, loggedInUserDocumentId, numberOfFav);

                                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment)
                                        .addToBackStack(null).commit();
                            }

                            @Override
                            public void onFavoriteClick(final int position) {
                                final String id = mDocumentIDs.get(position);
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
                                    userFavDocId = "";

                                    registration = currentRecipeSubCollection.whereEqualTo("userID", mUser.getUser_id())
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

                                                        if (!userFavDocId.equals("") && !mRecipeList.get(position).getFavorite()) {
                                                            currentRecipeSubCollection.document(userFavDocId).delete()
                                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                        @Override
                                                                        public void onSuccess(Void aVoid) {
                                                                            Toast.makeText(getContext(), "Removed from favorites", Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    });
                                                            mRecipeList.remove(position);
                                                            mDocumentIDs.remove(position);
                                                        } else {
                                                            Log.d(TAG, "onFavoriteClick: empty docID");
                                                        }
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

                            @Override
                            public void onDeleteClick(final int position) {
                                Recipe selectedRecipe = mRecipeList.get(position);
                                final String id = mDocumentIDs.get(position);

                                //Deleting image from FirebaseStorage
                                StorageReference imageRef = mStorageRef.getReferenceFromUrl(selectedRecipe.getImageUrl());
                                imageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        //Deleting Document of the item selected
                                        recipeRef.document(id).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Toast.makeText(getContext(), "Deleted from Db", Toast.LENGTH_SHORT).show();
                                                mRecipeList.remove(position);
                                                mDocumentIDs.remove(position);
                                                performQuery();
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
//                                Toast.makeText(MainActivity.this, "Delete click at " + position, Toast.LENGTH_SHORT).show();
                            }
                        });

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

    @Override
    public void onDeleteClick(int position) {

    }
}
