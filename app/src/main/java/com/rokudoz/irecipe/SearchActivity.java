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
import com.rokudoz.irecipe.Account.LoginActivity;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.Utils.RecipeAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SearchActivity extends AppCompatActivity implements RecipeAdapter.OnItemClickListener {
    private static final String TAG = "SearchActivity";
    private Boolean userSigned = false;
    private ProgressBar pbLoading;

    //vars
    private View mParentLayout;
    private RecyclerView mRecyclerView;
    private ArrayList<Recipe> mNotes = new ArrayList<>();
    private DocumentSnapshot mLastQueriedDocument;

    //Firebase
    private FirebaseAuth.AuthStateListener mAuthListener;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference recipeRef = db.collection("Recipes");
    private CollectionReference usersReference = db.collection("Users");

    private RecipeAdapter recipeAdapter;

    ArrayList<String> selectedIngredients;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        selectedIngredients = new ArrayList<>();
        pbLoading = findViewById(R.id.pbLoading);
        pbLoading.setVisibility(View.VISIBLE);
        mRecyclerView = findViewById(R.id.recycler_view);
        setupFirebaseAuth();
        initRecyclerView();
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

    private void initRecyclerView(){
        if(recipeAdapter == null){
            recipeAdapter = new RecipeAdapter(SearchActivity.this, mNotes);
        }
        mRecyclerView.setLayoutManager(new LinearLayoutManager(SearchActivity.this));
        mRecyclerView.setAdapter(recipeAdapter);

        
    }


    private void setupRecyclerView() {

        usersReference.whereEqualTo("user_id", FirebaseAuth.getInstance().getCurrentUser().getUid()).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                        Map<String, Boolean> tags = new HashMap<>();
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            User user = documentSnapshot.toObject(User.class);
                            for (String tag : user.getTags().keySet()) {
                                tags.put(tag, user.getTags().get(tag));
                            }
                            Toast.makeText(SearchActivity.this, tags.toString(), Toast.LENGTH_SHORT).show();
                        }

                        Query notesQuery = null;
                        if(mLastQueriedDocument != null){
                            notesQuery = recipeRef
                                    .whereEqualTo("tags", tags);
                        }
                        else{
                            notesQuery = recipeRef
                                    .whereEqualTo("tags", tags);
                        }

                        notesQuery.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if(task.isSuccessful()){

                                    for(QueryDocumentSnapshot document: task.getResult()){
                                        Recipe recipe = document.toObject(Recipe.class);
                                        mNotes.add(recipe);
//                        Log.d(TAG, "onComplete: got a new note. Position: " + (mNotes.size() - 1));
                                    }

                                    if(task.getResult().size() != 0){
                                        mLastQueriedDocument = task.getResult().getDocuments()
                                                .get(task.getResult().size() -1);
                                    }

                                    recipeAdapter.notifyDataSetChanged();
                                }
                                else{
                                    Toast.makeText(SearchActivity.this, "FAILED", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });


                        pbLoading.setVisibility(View.INVISIBLE);
                        recipeAdapter.setOnItemClickListener(new RecipeAdapter.OnItemClickListener() {
                            @Override
                            public void onItemClick(DocumentSnapshot documentSnapshot, int position) {
                                Recipe recipe = documentSnapshot.toObject(Recipe.class);
                                String id = documentSnapshot.getId();
                                String path = documentSnapshot.getReference().getPath();
                                Intent intent = new Intent(SearchActivity.this, RecipeDetailed.class).putExtra("documentID", id);
                                //Toast.makeText(SearchActivity.this, "Position: " + position + "ID: " + id + "Path: " +path , Toast.LENGTH_SHORT).show();
                                startActivity(intent);
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
                        setupRecyclerView();
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
    public void onItemClick(DocumentSnapshot documentSnapshot, int position) {

    }
}