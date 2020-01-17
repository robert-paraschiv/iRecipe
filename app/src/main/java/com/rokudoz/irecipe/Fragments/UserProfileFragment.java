package com.rokudoz.irecipe.Fragments;


import android.net.Uri;
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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.rokudoz.irecipe.Fragments.profileSubFragments.profileMyRecipesFragment;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.Models.UserWhoFaved;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.RecipeAdapter;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserProfileFragment extends Fragment implements RecipeAdapter.OnItemClickListener {
    private static final String TAG = "UserProfileFragment";

    private String documentID;
    private String userProfilePicUrl = "";
    private User mUser;
    private TextView UserNameTv;
    private TextView UserUsernameTv;
    private TextView UserDescriptionTv;
    private CircleImageView mProfileImage;
    private View view;
    private ArrayList<String> mDocumentIDs = new ArrayList<>();
    private ArrayList<Recipe> mRecipeList = new ArrayList<>();
    private List<String> userFavRecipesList = new ArrayList<>();
    private String userFavDocId = "";

    private DocumentSnapshot mLastQueriedDocument;
    private RecyclerView mRecyclerView;
    private RecipeAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    //Firebase
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference usersReference = db.collection("Users");
    private CollectionReference recipeRef = db.collection("Recipes");

    public UserProfileFragment() {
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
            view = inflater.inflate(R.layout.fragment_user_profile, container, false);
        } catch (InflateException e) {
            Log.e(TAG, "onCreateView: ", e);
        }


        UserNameTv = view.findViewById(R.id.userprofileFragment_user_name_TextView);
        UserUsernameTv = view.findViewById(R.id.userprofileFragment_userName_TextView);
        UserDescriptionTv = view.findViewById(R.id.userprofileFragment_user_description_TextView);
        mProfileImage = view.findViewById(R.id.userprofileFragment_profileImage);
        mRecyclerView = view.findViewById(R.id.userprofile_recycler_view);


        UserProfileFragmentArgs userProfileFragmentArgs = UserProfileFragmentArgs.fromBundle(getArguments());
        getRecipeArgsPassed(userProfileFragmentArgs);

        mUser = new User();

        mUser.setUser_id(FirebaseAuth.getInstance().getCurrentUser().getUid());

        buildRecyclerView();
        performQuery();

        return view;
    }

    private void getRecipeArgsPassed(UserProfileFragmentArgs userProfileFragmentArgs) {
        documentID = userProfileFragmentArgs.getDocumentID();

        getCreatorInfo();
    }

    private void getCreatorInfo() {
        usersReference.document(documentID).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e == null) {
                    User user = documentSnapshot.toObject(User.class);
                    userProfilePicUrl = user.getUserProfilePicUrl();

                    UserNameTv.setText(user.getName());
                    UserUsernameTv.setText(user.getUsername());
                    UserDescriptionTv.setText(user.getDescription());

                    if (userProfilePicUrl != null && !userProfilePicUrl.equals("")) {
                        Picasso.get()
                                .load(userProfilePicUrl)
                                .error(R.drawable.ic_account_circle_black_24dp)
                                .fit()
                                .centerCrop()
                                .into(mProfileImage);

                    } else {
                        Picasso.get()
                                .load(R.drawable.ic_account_circle_black_24dp)
                                .placeholder(R.drawable.ic_account_circle_black_24dp)
                                .into(mProfileImage);


                        Toast.makeText(getContext(), "empty", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void buildRecyclerView() {
        Log.d(TAG, "buildRecyclerView: ");
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getContext());

        mAdapter = new RecipeAdapter(mRecipeList);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(UserProfileFragment.this);
    }

    private void performQuery() {
        Query recipesQuery = null;
        if (mLastQueriedDocument != null) {
            recipesQuery = recipeRef.whereEqualTo("creator_docId", documentID).whereEqualTo("privacy","Everyone")
                    .startAfter(mLastQueriedDocument); // Necessary so we don't have the same results multiple times
//                                    .limit(3);
        } else {
            recipesQuery = recipeRef.whereEqualTo("creator_docId", documentID).whereEqualTo("privacy","Everyone");
//                                    .limit(3);
        }
        PerformMainQuery(recipesQuery);
        initializeRecyclerViewAdapterOnClicks();
    }

    private void PerformMainQuery(Query notesQuery) {

        notesQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
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

                Navigation.findNavController(view).navigate(UserProfileFragmentDirections.actionUserProfileFragment2ToRecipeDetailedFragment(id));

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

                    currentRecipeSubCollection.whereEqualTo("userID", mUser.getUser_id())
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
