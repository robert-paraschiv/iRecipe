package com.rokudoz.irecipe.Fragments.profileSubFragments;


import android.os.Bundle;

import androidx.annotation.Nullable;
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
import com.rokudoz.irecipe.Fragments.ProfileFragmentDirections;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.Models.UserWhoFaved;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.Adapters.RecipeAdapter;

import java.util.ArrayList;
import java.util.List;

public class profileMyRecipesFragment extends Fragment implements RecipeAdapter.OnItemClickListener {
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
    private ListenerRegistration userDetailsListener, recipesListener;
    private FirebaseStorage mStorageRef;

    private ArrayList<Recipe> mRecipeList = new ArrayList<>();
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


        buildRecyclerView();
        getCurrentUserDetails();

        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        DetachFirestoneListeners();
    }

    private void DetachFirestoneListeners() {
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
        Log.d(TAG, "buildRecyclerView: ");
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getContext());

        mAdapter = new RecipeAdapter(mRecipeList);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(profileMyRecipesFragment.this);
    }

    private void getCurrentUserDetails() {
        userDetailsListener = usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "onEvent: ", e);
                    return;
                }
                mUser = documentSnapshot.toObject(User.class);
                performQuery();
            }
        });
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

    private void PerformMainQuery(Query recipesQuery) {

        recipesListener = recipesQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "onEvent: ", e);
                    return;
                }
                if (queryDocumentSnapshots != null) {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        final Recipe recipe = document.toObject(Recipe.class);
                        recipe.setDocumentId(document.getId());

                        if (!mRecipeList.contains(recipe)) {
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
                            ////////////////////////////////////////////////////////// LOGIC TO GET RECIPES HERE

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

                Navigation.findNavController(view).navigate(ProfileFragmentDirections.actionProfileFragmentToRecipeDetailedFragment(id));

            }

            @Override
            public void onFavoriteClick(final int position) {
                String id = mRecipeList.get(position).getDocumentId();
                String title = mRecipeList.get(position).getTitle();
                DocumentReference currentRecipeRef = recipeRef.document(id);
                final CollectionReference currentRecipeSubCollection = currentRecipeRef.collection("UsersWhoFaved");

                DocumentReference favRecipesRef = usersReference.document(mUser.getUser_id());

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
                    UserWhoFaved userWhoFaved = new UserWhoFaved(mUser.getUser_id(), mUser.getName(), mUser.getUserProfilePicUrl(), null);
                    currentRecipeSubCollection.document(mUser.getUser_id()).set(userWhoFaved);
                    Toast.makeText(getContext(), "Added " + title + " to favorites", Toast.LENGTH_SHORT).show();
                }

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
