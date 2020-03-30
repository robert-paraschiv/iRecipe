package com.rokudoz.irecipe;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.rokudoz.irecipe.Models.Friend;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Utils.Adapters.SearchFilterDialog;
import com.rokudoz.irecipe.Utils.Adapters.SearchRecipeAdapter;
import com.rokudoz.irecipe.Utils.ConversationViewDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.rokudoz.irecipe.App.SETTINGS_PREFS_NAME;

public class SearchRecipeActivity extends AppCompatActivity implements SearchRecipeAdapter.OnItemClickListener {
    private static final String TAG = "SearchRecipeActivity";

    public static final String FILTER_PREFS_NAME = "FilterPrefs";


    //FireBase
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference recipeRef = db.collection("Recipes");
    private CollectionReference usersReference = db.collection("Users");

    FloatingActionButton filterFab;
    private List<Recipe> recipeList = new ArrayList<>();

    private RecyclerView mRecyclerView;
    private SearchRecipeAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_recipe);

        filterFab = findViewById(R.id.searchRecipeFilter_filterFab);
        filterFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SearchFilterDialog searchFilterDialog = new SearchFilterDialog();
                searchFilterDialog.showDialog(SearchRecipeActivity.this);

            }
        });
        setupSearchView();
        buildRecyclerView();
    }


    private void buildRecyclerView() {
        mRecyclerView = findViewById(R.id.searchRecipeActivity_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);

        mAdapter = new SearchRecipeAdapter(recipeList);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(this);
    }

    private void setupSearchView() {
        SearchView searchView = findViewById(R.id.searchUser_SearchView);
        searchView.setIconified(false);
        searchView.requestFocus();
        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchDb(newText);
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

    private void showInputMethod(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, 0);
        }
    }

    private void searchDb(String filter) {
        if (filter.trim().equals("")) {
            recipeList.clear();
            mAdapter.notifyDataSetChanged();
        } else {

            SharedPreferences sharedPreferences = getSharedPreferences(FILTER_PREFS_NAME, MODE_PRIVATE);
            Boolean breakfast = sharedPreferences.getBoolean("Breakfast", false);
            Boolean lunch = sharedPreferences.getBoolean("Lunch", false);
            Boolean dinner = sharedPreferences.getBoolean("Dinner", false);
            Boolean fixIngredients = sharedPreferences.getBoolean("FixIngredients", false);

            Query query = null;

            if (breakfast && lunch && dinner) {
                List<String> categories = new ArrayList<>();
                categories.add("breakfast");
                categories.add("lunch");
                categories.add("dinner");
                query = recipeRef.orderBy("title").startAfter(filter).endAt(filter + "\uf8ff").whereIn("category", categories);

            } else if (!breakfast && lunch && dinner) {
                List<String> categories = new ArrayList<>();
                categories.add("lunch");
                categories.add("dinner");
                query = recipeRef.orderBy("title").startAfter(filter).endAt(filter + "\uf8ff").whereIn("category", categories);

            } else if (breakfast && lunch && !dinner) {
                List<String> categories = new ArrayList<>();
                categories.add("breakfast");
                categories.add("lunch");
                query = recipeRef.orderBy("title").startAfter(filter).endAt(filter + "\uf8ff").whereIn("category", categories);

            } else if (breakfast && !lunch && dinner) {
                List<String> categories = new ArrayList<>();
                categories.add("breakfast");
                categories.add("dinner");
                query = recipeRef.orderBy("title").startAfter(filter).endAt(filter + "\uf8ff").whereIn("category", categories);

            } else if (breakfast && !lunch && !dinner) {
                query = recipeRef.orderBy("title").startAfter(filter).endAt(filter + "\uf8ff")
                        .whereEqualTo("category", "breakfast");
            } else if (!breakfast && lunch && !dinner) {
                query = recipeRef.orderBy("title").startAfter(filter).endAt(filter + "\uf8ff")
                        .whereEqualTo("category", "lunch");
            } else if (!breakfast && !lunch && dinner) {
                query = recipeRef.orderBy("title").startAfter(filter).endAt(filter + "\uf8ff")
                        .whereEqualTo("category", "dinner");
            } else if (!breakfast && !lunch && !dinner) {
                query = recipeRef.orderBy("title").startAfter(filter).endAt(filter + "\uf8ff");
            }
            Log.d(TAG, "applyFilter: " + breakfast + " " + lunch + " " + dinner + " " + fixIngredients);


            query.addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                    if (e == null && queryDocumentSnapshots != null && queryDocumentSnapshots.size() > 0) {
                        recipeList.clear();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots) {
                            Recipe recipe = queryDocumentSnapshot.toObject(Recipe.class);
                            recipe.setDocumentId(queryDocumentSnapshot.getId());
                            if (!recipeList.contains(recipe)) {
                                recipeList.add(recipe);
                                mAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                }
            });
        }

    }

    @Override
    public void onItemClick(int position) {
        String id = recipeList.get(position).getDocumentId();
        Log.d(TAG, "onItemClick: CLICKED " + " id " + id);

        Intent intent = new Intent(SearchRecipeActivity.this, MainActivity.class);
        intent.putExtra("recipe_id", id);
        startActivity(intent);
        finish();
    }

    @Override
    public void onFavoriteClick(int position) {

    }
}