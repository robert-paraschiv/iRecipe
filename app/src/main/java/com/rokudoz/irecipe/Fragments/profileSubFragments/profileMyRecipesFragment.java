package com.rokudoz.irecipe.Fragments.profileSubFragments;


import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

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
import com.rokudoz.irecipe.Account.LoginActivity;
import com.rokudoz.irecipe.Fragments.HomeFragmentDirections;
import com.rokudoz.irecipe.Fragments.ProfileFragment;
import com.rokudoz.irecipe.Fragments.ProfileFragmentDirections;
import com.rokudoz.irecipe.Fragments.homeSubFragments.homeBreakfastFragment;
import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.Models.UserWhoFaved;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.RecipeAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class profileMyRecipesFragment extends Fragment implements RecipeAdapter.OnItemClickListener{
    private static final String TAG = "profileMyRecipesFragmen";

    private View view;
    private ProgressBar pbLoading;

    private RecyclerView mRecyclerView;
    private RecipeAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    //FireBase
    private FirebaseAuth.AuthStateListener mAuthListener;
    private User mUser;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference recipeRef = db.collection("Recipes");
    private CollectionReference usersReference = db.collection("Users");
    private ListenerRegistration currentSubCollectionListener, userDetailsListener, recipesListener;
    private FirebaseStorage mStorageRef;

    private ArrayList<String> mDocumentIDs = new ArrayList<>();
    private ArrayList<Recipe> mRecipeList = new ArrayList<>();
    private List<String> userFavRecipesList = new ArrayList<>();
    private String userFavDocId = "";

    private DocumentSnapshot mLastQueriedDocument;


    public profileMyRecipesFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null) {
                parent.removeView(view);
            }
        }
        try {
            view = inflater.inflate(R.layout.fragment_profile_my_recipes, container, false);
        } catch (InflateException e) {
            Log.e(TAG, "onCreateView: ", e);
        }

        mUser = new User();
        pbLoading = view.findViewById(R.id.profileMyRecipesFragment_pbLoading);
        mRecyclerView = view.findViewById(R.id.profileMyRecipesFragment_recycler_view);

        pbLoading.setVisibility(View.VISIBLE);
        mStorageRef = FirebaseStorage.getInstance();


        mUser.setUser_id(FirebaseAuth.getInstance().getCurrentUser().getUid());

        buildRecyclerView();
        performQuery();

        return view;
    }

    private void buildRecyclerView() {
        Log.d(TAG, "buildRecyclerView: ");
            mRecyclerView.setHasFixedSize(true);
            mLayoutManager = new LinearLayoutManager(getContext());

            mAdapter = new RecipeAdapter(mRecipeList);

            mRecyclerView.setLayoutManager(mLayoutManager);
            mRecyclerView.setAdapter(mAdapter);

            mAdapter.setOnItemClickListener(profileMyRecipesFragment.this);
    }


    private void performQuery() {
                        Query recipesQuery = null;
                        if (mLastQueriedDocument != null) {
                            recipesQuery = recipeRef.whereEqualTo("creator_docId", mUser.getUser_id())
                                    .startAfter(mLastQueriedDocument); // Necessary so we don't have the same results multiple times
//                                    .limit(3);
                        } else {
                            recipesQuery = recipeRef.whereEqualTo("creator_docId", mUser.getUser_id());
//                                    .limit(3);
                        }
                        PerformMainQuery(recipesQuery);
                        pbLoading.setVisibility(View.INVISIBLE);
                        initializeRecyclerViewAdapterOnClicks();
    }

    private void PerformMainQuery(Query notesQuery) {

        recipesListener = notesQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
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

                            ////////////////////////////////////////////////////////// LOGIC TO GET RECIPES HERE

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
                Log.d(TAG, "onItemClick: CLICKED " + title + " id " + id);

                Navigation.findNavController(view).navigate(ProfileFragmentDirections.actionProfileFragmentToRecipeDetailedFragment(id));

            }

            @Override
            public void onFavoriteClick(final int position) {
                String id = mDocumentIDs.get(position);
                String title = mRecipeList.get(position).getTitle();
                DocumentReference currentRecipeRef = recipeRef.document(id);
                final CollectionReference currentRecipeSubCollection = currentRecipeRef.collection("UsersWhoFaved");

                mUser.setFavoriteRecipes(userFavRecipesList);
                DocumentReference favRecipesRef = usersReference.document(mUser.getUser_id());

                Log.d(TAG, "onFavoriteClick: " + mRecipeList.get(position).getDocumentId());

                if (userFavRecipesList == null) {
                    userFavRecipesList = new ArrayList<>();
                }
                if (userFavRecipesList.contains(id)) {
                    userFavRecipesList.remove(id);
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
                    userFavRecipesList.add(id);
                    mRecipeList.get(position).setFavorite(true);
                    UserWhoFaved userWhoFaved = new UserWhoFaved(mUser.getUser_id(), null);
                    currentRecipeSubCollection.add(userWhoFaved);
                    Toast.makeText(getContext(), "Added " + title + " to favorites", Toast.LENGTH_SHORT).show();
                }

                mUser.setFavoriteRecipes(userFavRecipesList);
                favRecipesRef.update("favoriteRecipes", userFavRecipesList);


                mAdapter.notifyDataSetChanged();
            }

        });
    }


    @Override
    public void onItemClick(int position) {

    }

    @Override
    public void onFavoriteClick(int position) {

    }
}