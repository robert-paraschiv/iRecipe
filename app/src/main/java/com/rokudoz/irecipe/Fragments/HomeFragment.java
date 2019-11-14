package com.rokudoz.irecipe.Fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
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
import com.rokudoz.irecipe.Viewmodels.HomeFragmentViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HomeFragment extends Fragment implements RecipeAdapter.OnItemClickListener {
    private static final String TAG = "HomeFragment";

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
    private ListenerRegistration currentSubCollectionListener, userDetailsListener, recipesListener;

    private RecyclerView mRecyclerView;
    private RecipeAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    //MVVM
    private HomeFragmentViewModel homeFragmentViewModel;

    private ArrayList<String> mDocumentIDs = new ArrayList<>();
    private ArrayList<Recipe> mRecipeList = new ArrayList<>();
    private ArrayList<String> favRecipes = new ArrayList<>();
    private String loggedInUserDocumentId = "";
    private String userFavDocId = "";

    private DocumentSnapshot mLastQueriedDocument;

    public static HomeFragment newInstance() {
        HomeFragment fragment = new HomeFragment();
        return fragment;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_home, container, false);
        }
        Log.d(TAG, "onCreateView: ");
        mUser = new User();
        getUserDocID();
        pbLoading = view.findViewById(R.id.homeFragment_pbLoading);
        fab = view.findViewById(R.id.fab_add_recipe);
        mRecyclerView = view.findViewById(R.id.recycler_view);

        pbLoading.setVisibility(View.VISIBLE);
        mStorageRef = FirebaseStorage.getInstance();


        homeFragmentViewModel = ViewModelProviders.of(this).get(HomeFragmentViewModel.class);
        homeFragmentViewModel.init();
        homeFragmentViewModel.getRecipes().observe(this, new Observer<ArrayList<Recipe>>() {
            @Override
            public void onChanged(ArrayList<Recipe> recipes) {
                mAdapter.notifyDataSetChanged();
                mRecipeList = recipes;
                pbLoading.setVisibility(View.GONE);
                Log.d(TAG, "onChanged: NOTIFIED HOMEFRAGMENT ADAPTER");
            }
        });

        fab.hide();
        buildRecyclerView();

        return view; // HAS TO BE THE LAST ONE 
    }


    private void getUserDocID() {
        usersReference.whereEqualTo("user_id", Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "onEvent: ", e);
                            return;
                        }
                        if (queryDocumentSnapshots != null) {
                            mUser = queryDocumentSnapshots.getDocuments().get(0).toObject(User.class);
                            loggedInUserDocumentId = queryDocumentSnapshots.getDocuments().get(0).getId();
                            favRecipes = mUser.getFavoriteRecipes();
                        }
                    }
                });
    }

    @Override
    public void onStop() {
        super.onStop();
        DetachFirestoreListeners();
        Log.d(TAG, "onStop: ");
    }

    private void DetachFirestoreListeners() {
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

        mAdapter = new RecipeAdapter(homeFragmentViewModel.getRecipes().getValue());

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(HomeFragment.this);
    }

    @Override
    public void onItemClick(int position) {
        String id = homeFragmentViewModel.getRecipes().getValue().get(position).getDocumentId();
        String title = mRecipeList.get(position).getTitle();
        String description = mRecipeList.get(position).getDescription();
        String imageUrl = mRecipeList.get(position).getImageUrl();
        Map<String, Boolean> ingredients = mRecipeList.get(position).getTags();
        String instructions = mRecipeList.get(position).getInstructions();
        Boolean isFavorite = mRecipeList.get(position).getFavorite();
        Integer numberOfFaves = mRecipeList.get(position).getNumberOfFaves();

        String ingredientsString = "Ingredients:\n";
        for (String ingredient : ingredients.keySet()) {
            if (ingredients.get(ingredient)) {
                ingredientsString += "\n- " + ingredient;
            }
        }

        RecipeDetailedFragment fragment = RecipeDetailedFragment.newInstance(position,id, title, description, ingredientsString
                , imageUrl, instructions, isFavorite, favRecipes, loggedInUserDocumentId, numberOfFaves);

        getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment)
                .addToBackStack(null).commit();
    }

    @Override
    public void onFavoriteClick(final int position) {
        String id = homeFragmentViewModel.getRecipes().getValue().get(position).getDocumentId();
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
            homeFragmentViewModel.getRecipes().getValue().get(position).setFavorite(false);

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
                                Log.d(TAG, "onEvent: docID " + userFavDocId);

                                if (!userFavDocId.equals("") && !mRecipeList.get(position).getFavorite()) {
                                    currentRecipeSubCollection.document(userFavDocId).delete()
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    if (getContext() != null)
                                                        Toast.makeText(getContext(), "Removed from favorites", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                } else {
                                    Log.d(TAG, "onFavoriteClick: empty docID");
                                }
                            }
                        }
                    });
        } else {
            favRecipes.add(id);
            mRecipeList.get(position).setFavorite(true);
            homeFragmentViewModel.getRecipes().getValue().get(position).setFavorite(true);
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
        final String id = homeFragmentViewModel.getRecipes().getValue().get(position).getDocumentId();

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
    }
}
