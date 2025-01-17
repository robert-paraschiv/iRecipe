package com.rokudoz.irecipe;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.rokudoz.irecipe.Models.Friend;
import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.Utils.Adapters.SearchRecipeAdapter;

import java.util.ArrayList;
import java.util.List;

public class SelectRecipeActivity extends AppCompatActivity implements SearchRecipeAdapter.OnItemClickListener {

    private static final String TAG = "SelectRecipeActivity";

    //FireBase
    private FirebaseAuth.AuthStateListener mAuthListener;
    private User mUser;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference recipeRef = db.collection("Recipes");
    private CollectionReference usersReference = db.collection("Users");
    private FirebaseStorage mStorageRef;
    private ListenerRegistration userDetailsListener, currentSubCollectionListener, recipesListener;

    private RecyclerView mRecyclerView;
    private SearchRecipeAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private ArrayList<String> mDocumentIDs = new ArrayList<>();
    private ArrayList<Recipe> mRecipeList = new ArrayList<>();
    private List<String> userFavRecipesList = new ArrayList<>();
    private List<String> friends_userID_list = new ArrayList<>();
    private List<Friend> friendList = new ArrayList<>();
    private String loggedInUserDocumentId = "";
    private String userFavDocId = "";


    private String postText = "";
    String postID = "";
    String post_imageUrl = "";
    String post_text = "";
    String post_privacy = "";
    String post_referencedRecipe = "";

    String coming_from = "";

    private DocumentSnapshot mLastQueriedDocument;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_recipe);

        if (getIntent() != null) {
            coming_from = getIntent().getStringExtra("coming_from");

            if (getIntent().getStringExtra("coming_from").equals("AddPostActivity")) {
                if (getIntent().getStringExtra("post_text") != null) {
                    postText = getIntent().getStringExtra("post_text");
                }
                if (getIntent().getStringExtra("post_imageUrl") != null) {
                    post_imageUrl = getIntent().getStringExtra("post_imageUrl");
                }
            }
            if (getIntent().getStringExtra("coming_from").equals("EditPostActivity")) {
                if (getIntent().getStringExtra("post_id") != null) {
                    postID = getIntent().getStringExtra("post_id");
                }
                if (getIntent().getStringExtra("post_imageUrl") != null) {
                    post_imageUrl = getIntent().getStringExtra("post_imageUrl");
                }
                if (getIntent().getStringExtra("post_text") != null) {
                    post_text = getIntent().getStringExtra("post_text");
                }
                if (getIntent().getStringExtra("post_privacy") != null) {
                    post_privacy = getIntent().getStringExtra("post_privacy");
                }
                if (getIntent().getStringExtra("post_referencedRecipe") != null) {
                    post_referencedRecipe = getIntent().getStringExtra("post_referencedRecipe");
                }
            }


        }

        setUpSearchView();

        mStorageRef = FirebaseStorage.getInstance();

        getCurrentUserDetails();
//        buildRecyclerView();
    }

    private void setUpSearchView() {
        SearchView searchView = findViewById(R.id.selectRecipeActivity_SearchView);
        searchView.requestFocus();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mAdapter.getFilter().filter(newText);
                return false;
            }

        });

        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showInputMethod(v.findFocus());
                }
            }
        });
    }


    private void buildRecyclerView() {
        mRecyclerView = findViewById(R.id.searchActivity_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);

        mAdapter = new SearchRecipeAdapter(mRecipeList);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(this);
    }

    private void showInputMethod(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, 0);
        }
    }

    private void getCurrentUserDetails() {
        usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                List<Ingredient> userIngredient_list = new ArrayList<>();
                User user = documentSnapshot.toObject(User.class);

                mUser = documentSnapshot.toObject(User.class);
                loggedInUserDocumentId = documentSnapshot.getId();
                if (!friends_userID_list.contains(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                    friends_userID_list.add(FirebaseAuth.getInstance().getCurrentUser().getUid());
                }

                usersReference.document(user.getUser_id()).collection("FriendList").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots) {
                            Friend friend = queryDocumentSnapshot.toObject(Friend.class);
                            if (!friendList.contains(friend)) {
                                if (friend.getFriend_status().equals("friends") || friend.getFriend_status().equals("friend_request_accepted"))
                                    friendList.add(friend);
                            }
                        }
                        for (Friend friend : friendList) {
                            if (!friends_userID_list.contains(friend.getFriend_user_id()))
                                friends_userID_list.add(friend.getFriend_user_id());
                        }
                        performQuery();

                    }
                });
            }
        });
    }

    private void performQuery() {
        Query recipesQuery = null;

        //NORMAL
        if (mLastQueriedDocument != null) {
            recipesQuery = recipeRef.whereEqualTo("privacy", "Everyone")
                    .startAfter(mLastQueriedDocument); // Necessary so we don't have the same results multiple times
//                                    .limit(3);
        } else {
            recipesQuery = recipeRef.whereEqualTo("privacy", "Everyone");
//                                    .limit(3);
        }


        PerformMainQuery(recipesQuery);
    }

    private void PerformMainQuery(Query notesQuery) {

        recipesListener = notesQuery.addSnapshotListener(SelectRecipeActivity.this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots,
                                @javax.annotation.Nullable FirebaseFirestoreException e) {
                if (queryDocumentSnapshots != null) {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Recipe recipe = document.toObject(Recipe.class);
                        recipe.setDocumentId(document.getId());

                        if (userFavRecipesList != null && userFavRecipesList.contains(document.getId())) {
                            recipe.setFavorite(true);
                        } else {
                            recipe.setFavorite(false);
                        }
                        if (!mDocumentIDs.contains(document.getId())) {
                            mDocumentIDs.add(document.getId());
                            mRecipeList.add(recipe);
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
                buildRecyclerView();
                mAdapter.notifyDataSetChanged();

            }
        });
    }

    @Override
    public void onItemClick(int position) {

        if (coming_from.equals("AddPostActivity")) {
            String id = mRecipeList.get(position).getDocumentId();
            Intent intent = new Intent(SelectRecipeActivity.this, AddPostActivity.class);
            intent.putExtra("post_text", postText);
            intent.putExtra("recipe_doc_id", id);
            intent.putExtra("post_imageUrl", post_imageUrl);
            startActivity(intent);
            finish();
        } else if (coming_from.equals("EditPostActivity")) {
            String id = mRecipeList.get(position).getDocumentId();
            Intent intent = new Intent(SelectRecipeActivity.this, EditPostActivity.class);
            intent.putExtra("post_id", postID);
            intent.putExtra("post_imageUrl", post_imageUrl);
            intent.putExtra("post_text", post_text);
            intent.putExtra("post_privacy", post_privacy);
            intent.putExtra("post_referencedRecipe", id);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onFavoriteClick(int position) {
    }
}

