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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.rokudoz.irecipe.Account.LoginActivity;
import com.rokudoz.irecipe.Models.Comment;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.CommentAdapter;
import com.squareup.picasso.Picasso;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class RecipeDetailedFragment extends Fragment {
    private static final String TAG = "RecipeDetailedFragment";

    private static final String ARG_ID = "argId";
    private static final String ARG_TITLE = "argTitle";
    private static final String ARG_DESCRIPTION = "argDescription";
    private static final String ARG_IMAGEURL = "argImageurl";
    private static final String ARG_INGREDIENTS = "argIngredients";
    private static final String ARG_INSTRUCTIONS = "argInstructions";
    private static final String ARG_ISFAVORITE = "argIsFavorite";
    private static final String ARG_FAVRECIPES = "argFavRecipes";
    private static final String ARG_LOGGEDINUSERDOCUMENTID = "argLoggedInUserDocumentId";

    private String documentID = "";
    private String currentUserImageUrl = "";
    private String currentUserName = "";
    private String loggedInUserDocumentId = "";
    private String title = "";
    private Boolean isRecipeFavorite;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private TextView tvTitle, tvDescription, tvIngredients, tvInstructions;
    private ImageView mImageView, mFavoriteIcon;
    private Button mAddCommentBtn;
    private EditText mCommentEditText;

    private ArrayList<Comment> commentList = new ArrayList<>();
    private ArrayList<String> favRecipes = new ArrayList<>();
    private ArrayList<String> newItemsToAdd = new ArrayList<>();
    private User mUser;

    private DocumentSnapshot mLastQueriedDocument;

    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference recipesRef = db.collection("Recipes");
    private CollectionReference usersRef = db.collection("Users");
    private CollectionReference commentRef = db.collection("Comments");

    public static RecipeDetailedFragment newInstance(String id, String title, String description, String ingredients, String imageUrl
            , String instructions, Boolean isFavorite, ArrayList<String> favRecipes, String loggedInUserDocumentId) {
        RecipeDetailedFragment fragment = new RecipeDetailedFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, id);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESCRIPTION, description);
        args.putString(ARG_IMAGEURL, imageUrl);
        args.putString(ARG_INGREDIENTS, ingredients);
        args.putString(ARG_INSTRUCTIONS, instructions);
        args.putBoolean(ARG_ISFAVORITE, isFavorite);
        args.putStringArrayList(ARG_FAVRECIPES, favRecipes);
        args.putString(ARG_LOGGEDINUSERDOCUMENTID, loggedInUserDocumentId);
        fragment.setArguments(args);
        return fragment;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recipe_detailed, container, false);

        mUser = new User();
        tvTitle = view.findViewById(R.id.tvTitle);
        tvDescription = view.findViewById(R.id.tvDescription);
        tvIngredients = view.findViewById(R.id.tvIngredientsList);
        mImageView = view.findViewById(R.id.recipeDetailed_image);
        mAddCommentBtn = view.findViewById(R.id.recipeDetailed_addComment_btn);
        mRecyclerView = view.findViewById(R.id.comment_recycler_view);
        mCommentEditText = view.findViewById(R.id.recipeDetailed_et_commentInput);
        tvInstructions = view.findViewById(R.id.tvInstructions);
        mFavoriteIcon = view.findViewById(R.id.imageview_favorite_icon);

        mAddCommentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mCommentEditText.getText().toString().trim().equals("")) {
                    addComment();
                } else {
                    Toast.makeText(getContext(), "Comment cannot be empty", Toast.LENGTH_SHORT).show();
                }
            }
        });
        mFavoriteIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (favRecipes == null) {
                    favRecipes = new ArrayList<>();
                }
                if (favRecipes.contains(documentID)) {
                    favRecipes.remove(documentID);
                    isRecipeFavorite = false;
                    Toast.makeText(getContext(), "Removed " + title + " from favorites", Toast.LENGTH_SHORT).show();
                } else {
                    favRecipes.add(documentID);
                    isRecipeFavorite = true;
                    Toast.makeText(getContext(), "Added " + title + " to favorites", Toast.LENGTH_SHORT).show();
                }
                setFavoriteIcon(isRecipeFavorite);
                DocumentReference favRecipesRef = usersRef.document(loggedInUserDocumentId);
                favRecipesRef.update("favoriteRecipes", favRecipes);
            }
        });

        getRecipeArgsPassed();
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
    }

    private void addComment() {
        String commentText = mCommentEditText.getText().toString();

        String date = Timestamp.now().toDate().toString();

        final Comment comment = new Comment(documentID, FirebaseAuth.getInstance().getCurrentUser().getUid(),
                currentUserImageUrl, currentUserName, commentText, date);


        commentRef.add(comment)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        mCommentEditText.setText("");
                        Toast.makeText(getContext(), "Successfully added comment ", Toast.LENGTH_SHORT).show();
                        mAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: ", e);
                    }
                });
    }

    private void getRecipeArgsPassed() {
        if (getArguments() != null) {
            documentID = getArguments().getString(ARG_ID);
            title = getArguments().getString(ARG_TITLE);
            String description = getArguments().getString(ARG_DESCRIPTION);
            String imageUrl = getArguments().getString(ARG_IMAGEURL);
            String ingredients = getArguments().getString(ARG_INGREDIENTS);
            String instructions = getArguments().getString(ARG_INSTRUCTIONS);
            isRecipeFavorite = getArguments().getBoolean(ARG_ISFAVORITE);
            favRecipes = getArguments().getStringArrayList(ARG_FAVRECIPES);
            loggedInUserDocumentId = getArguments().getString(ARG_LOGGEDINUSERDOCUMENTID);

            tvTitle.setText(title);
            tvDescription.setText(description);
            tvIngredients.setText(ingredients);
            tvInstructions.setText("Instructions: \n\n" + instructions);


            setFavoriteIcon(isRecipeFavorite);

            Picasso.get()
                    .load(imageUrl)
                    .fit()
                    .centerCrop()
                    .into(mImageView);
        }
    }

    private void setFavoriteIcon(Boolean isFavorite) {
        if (isFavorite) {
            mFavoriteIcon.setImageResource(R.drawable.ic_star_gold_24dp);
        } else {
            mFavoriteIcon.setImageResource(R.drawable.ic_star_border_black_24dp);
        }
    }

    private void buildRecyclerView() {
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getContext());
        mAdapter = new CommentAdapter(commentList);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
    }

    private void getCommentsFromDb() {

        Query commentQuery = null;
        if (mLastQueriedDocument != null) {
            commentQuery = commentRef.whereEqualTo("mRecipeDocumentId", documentID)
                    .startAfter(mLastQueriedDocument); // Necessary so we don't have the same results multiple times
        } else {
            commentQuery = commentRef.whereEqualTo("mRecipeDocumentId", documentID);
        }

        commentQuery
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "onEvent: ", e);
                            return;
                        }
                        //get comments from commentRef
                        if (queryDocumentSnapshots != null) {
                            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                final Comment comment = document.toObject(Comment.class);
//                                commentList.add(new Comment(documentID, comment.getmUserId(), comment.getmImageUrl(), comment.getmName(), comment.getmCommentText()));
                                if (!newItemsToAdd.contains(document.getId())) {
                                    newItemsToAdd.add(document.getId());

                                    usersRef.whereEqualTo("user_id", comment.getmUserId())
                                            .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                                @Override
                                                public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                                                    User user = null;
                                                    if (queryDocumentSnapshots != null) {
                                                        user = queryDocumentSnapshots.getDocuments().get(0).toObject(User.class);


                                                    commentList.add(new Comment(documentID, comment.getmUserId(), user.getUserProfilePicUrl()
                                                            , comment.getmName(), comment.getmCommentText(),comment.getmCommentTimeStamp()));
                                                    Log.d(TAG, "onEvent: setting user profile pic ");
                                                    mAdapter.notifyDataSetChanged();}
                                                }
                                            });

//                                    commentList.add(new Comment(documentID, comment.getmUserId(), userImg[0], comment.getmName(), comment.getmCommentText()));
                                }

                            }
                            if (queryDocumentSnapshots.getDocuments().size() != 0) {
                                mLastQueriedDocument = queryDocumentSnapshots.getDocuments()
                                        .get(queryDocumentSnapshots.getDocuments().size() - 1);
                            }
                        }
                        mAdapter.notifyDataSetChanged();
                        Log.d(TAG, "onEvent: querrysize " + queryDocumentSnapshots.size() + " ListSize: " + commentList.size());
                    }
                });



    }

    private void getCurrentUserDetails() {
        usersRef.whereEqualTo("user_id", FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "onEvent: ", e);
                            return;
                        }

                        for (DocumentChange documentSnapshot : queryDocumentSnapshots.getDocumentChanges()) {
                            mUser = documentSnapshot.getDocument().toObject(User.class);
                            currentUserImageUrl = mUser.getUserProfilePicUrl();
                            currentUserName = mUser.getName();
                        }

                        getCommentsFromDb();
                    }
                });
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
                        getCurrentUserDetails();
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


}
