package com.rokudoz.irecipe;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.rokudoz.irecipe.Account.LoginActivity;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.Utils.RecipeAdapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SearchActivity extends AppCompatActivity implements RecipeAdapter.OnItemClickListener {
    private static final String TAG = "SearchActivity";
    private Boolean userSigned = false;
    private ProgressBar pbLoading;

    //Firebase
    private FirebaseAuth.AuthStateListener mAuthListener;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference recipeRef = db.collection("Recipes");
    private CollectionReference usersReference = db.collection("Users");
    private FirebaseStorage mStorageRef;

    private RecyclerView mRecyclerView;
    private RecipeAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private ArrayList<Recipe> mRecipeList = new ArrayList<>();
    private ArrayList<String> mDocumentIDs = new ArrayList<>();

    private DocumentSnapshot mLastQueriedDocument;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        pbLoading = findViewById(R.id.pbLoading);
        pbLoading.setVisibility(View.VISIBLE);
        mStorageRef = FirebaseStorage.getInstance();

        buildRecyclerView();
        setupFirebaseAuth();

    }

    public void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener(mAuthListener);
//        recipeAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(mAuthListener);
        }
    }

    public void buildRecyclerView() {
        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);

        mRecipeList = new ArrayList<>();
        mAdapter = new RecipeAdapter(mRecipeList);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(SearchActivity.this);
    }


    private void performQuery() {

        usersReference.whereEqualTo("user_id", Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        //Gets User Ingredients from database
                        Map<String, Boolean> tags = new HashMap<>();
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            User user = documentSnapshot.toObject(User.class);
                            for (String tag : user.getTags().keySet()) {
                                tags.put(tag, Objects.requireNonNull(user.getTags().get(tag)));
                            }
//                            Toast.makeText(SearchActivity.this, tags.toString(), Toast.LENGTH_SHORT).show();
                        }

                        Query notesQuery = null;
                        if (mLastQueriedDocument != null) {
                            notesQuery = recipeRef.whereGreaterThanOrEqualTo("tags", tags)
                                    .startAfter(mLastQueriedDocument); // Necessary so we don't have the same results multiple times
//                                    .limit(3);
                        } else {
                            notesQuery = recipeRef.whereGreaterThanOrEqualTo("tags", tags);
//                                    .limit(3);
                        }

                        notesQuery.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {

                                    for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                        Recipe recipe = document.toObject(Recipe.class);
                                        mRecipeList.add(recipe);
                                        mDocumentIDs.add(document.getId());
//                        Log.d(TAG, "onComplete: got a new note. Position: " + (mNotes.size() - 1));
                                    }

                                    if (task.getResult().size() != 0) {
                                        mLastQueriedDocument = task.getResult().getDocuments()
                                                .get(task.getResult().size() - 1);
                                    }

                                    mAdapter.notifyDataSetChanged();
                                } else {
                                    Toast.makeText(SearchActivity.this, "FAILED", Toast.LENGTH_SHORT).show();
                                }
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

                                Intent intent = new Intent(SearchActivity.this, RecipeDetailed.class)
                                        .putExtra("documentID", id)
                                        .putExtra("title", title)
                                        .putExtra("description", description)
                                        .putExtra("ingredients", (Serializable) ingredients)
                                        .putExtra("imageUrl", imageUrl);

                                startActivity(intent);
                            }

                            @Override
                            public void onFavoriteCick(int position) {
                                String id = mDocumentIDs.get(position);
                                Toast.makeText(SearchActivity.this, "Favorite click at " + id, Toast.LENGTH_SHORT).show();
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
                                                Toast.makeText(SearchActivity.this, "Deleted from Db", Toast.LENGTH_SHORT).show();
                                                mRecipeList.remove(position);
                                                mDocumentIDs.remove(position);
                                                performQuery();
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(SearchActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(SearchActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
//                                Toast.makeText(SearchActivity.this, "Delete click at " + position, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });

    }


    public void navigateToAddRecipes(View v) {
        Intent intent = new Intent(this, AddRecipesActivity.class);
        startActivity(intent);
    }

    public void navigateToMyIngredients(View v) {
        Intent intent = new Intent(this, MyIngredientsActivity.class);
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
//                        Toast.makeText(SearchActivity.this, "Authenticated with: " + user.getEmail(), Toast.LENGTH_SHORT).show();
                        userSigned = true;
                        performQuery();
                    } else {
                        Toast.makeText(SearchActivity.this, "Email is not Verified\nCheck your Inbox", Toast.LENGTH_SHORT).show();
                        FirebaseAuth.getInstance().signOut();
                    }

                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged: signed_out");
                    Toast.makeText(SearchActivity.this, "Not logged in", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SearchActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
                // ...
            }
        };
    }

    @Override
    public void onItemClick(int position) {

    }

    @Override
    public void onFavoriteCick(int position) {

    }

    @Override
    public void onDeleteClick(int position) {

    }
}