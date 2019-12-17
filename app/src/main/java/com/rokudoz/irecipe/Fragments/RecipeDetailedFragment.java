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
import com.rokudoz.irecipe.Account.LoginActivity;
import com.rokudoz.irecipe.Models.Comment;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.Models.UserWhoFaved;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.ParentCommentAdapter;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;

import static com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior.ESTIMATE;

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
    private static final String ARG_NUMBEROFFAVES = "argNumberOfFaves";
    private static final String ARG_LOGGEDINUSERDOCUMENTID = "argLoggedInUserDocumentId";

    private String documentID = "";
    private String currentUserImageUrl = "";
    private String currentUserName = "";
    private String loggedInUserDocumentId = "";
    private String title = "";
    private String userFavDocId = "";
    private Boolean isRecipeFavorite;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;


    private TextView tvTitle, tvDescription, tvIngredients, tvInstructions, mFavoriteNumber;
    private ImageView mImageView, mFavoriteIcon;
    private Button mAddCommentBtn;
    private EditText mCommentEditText;

    private ArrayList<Comment> commentList = new ArrayList<>();
    private ArrayList<String> favRecipes = new ArrayList<>();
    private Integer numberOfFav;
    private ArrayList<String> newItemsToAdd = new ArrayList<>();
    private User mUser;

    private DocumentSnapshot mLastQueriedDocument;

    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration currentSubCollectionListener, usersRefListener, commentListener, numberofFavListener, currentUserDetailsListener;
    private CollectionReference recipeRef = db.collection("Recipes");
    private CollectionReference usersRef = db.collection("Users");

//
//    public static RecipeDetailedFragment newInstance(String id, String title, String description, String ingredients, String imageUrl
//            , String instructions, Boolean isFavorite, ArrayList<String> favRecipes, String loggedInUserDocumentId, Integer numberofFaves) {
//        RecipeDetailedFragment fragment = new RecipeDetailedFragment();
//        Bundle args = new Bundle();
//        args.putString(ARG_ID, id);
//        args.putString(ARG_TITLE, title);
//        args.putString(ARG_DESCRIPTION, description);
//        args.putString(ARG_IMAGEURL, imageUrl);
//        args.putString(ARG_INGREDIENTS, ingredients);
//        args.putString(ARG_INSTRUCTIONS, instructions);
//        args.putBoolean(ARG_ISFAVORITE, isFavorite);
//        args.putStringArrayList(ARG_FAVRECIPES, favRecipes);
//        args.putString(ARG_LOGGEDINUSERDOCUMENTID, loggedInUserDocumentId);
//        args.putInt(ARG_NUMBEROFFAVES, numberofFaves);
//        fragment.setArguments(args);
//        return fragment;
//    }


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
        mFavoriteNumber = view.findViewById(R.id.recipeDetailed_numberOfFaved);


        RecipeDetailedFragmentArgs recipeDetailedFragmentArgs = RecipeDetailedFragmentArgs.fromBundle(getArguments());
        getRecipeArgsPassed(recipeDetailedFragmentArgs);


        DocumentReference currentRecipeRef = recipeRef.document(documentID);
        final CollectionReference currentRecipeSubCollection = currentRecipeRef.collection("UsersWhoFaved");

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
                                    }
                                    if (!isRecipeFavorite)
                                        if (!userFavDocId.equals("")) {
                                            currentRecipeSubCollection.document(userFavDocId).delete()
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            Toast.makeText(getContext(), "Removed from favorites", Toast.LENGTH_SHORT).show();
                                                            Log.d(TAG, "onSuccess: REMOVED " + title + " " + documentID + " from favorites");
                                                        }
                                                    });


                                        } else {
                                            Log.d(TAG, "onFavoriteClick: empty docID");
                                        }

                                }
                            });
                } else {
                    favRecipes.add(documentID);
                    isRecipeFavorite = true;
                    UserWhoFaved userWhoFaved = new UserWhoFaved(mUser.getUser_id(), null);
                    currentRecipeSubCollection.add(userWhoFaved);
                    Toast.makeText(getContext(), "Added " + title + " to favorites", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onClick: ADDED + " + title + " " + documentID + " to favorites");
                }
                setFavoriteIcon(isRecipeFavorite);

                DocumentReference favRecipesRef = usersRef.document(loggedInUserDocumentId);
                favRecipesRef.update("favoriteRecipes", favRecipes);
            }
        });

        numberofFavListener = currentRecipeSubCollection.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    return;
                }
                numberOfFav = queryDocumentSnapshots.size();
                mFavoriteNumber.setText(Integer.toString(numberOfFav));
            }
        });

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
        DetatchFirestoreListeners();
        Log.d(TAG, "onStop: ");

    }

    private void DetatchFirestoreListeners() {
        if (currentSubCollectionListener != null) {
            currentSubCollectionListener.remove();
            currentSubCollectionListener = null;
        }
        if (usersRefListener != null) {
            usersRefListener.remove();
            usersRefListener = null;
        }
        if (commentListener != null) {
            commentListener.remove();
            commentListener = null;
        }
        if (currentUserDetailsListener != null) {
            currentUserDetailsListener.remove();
            currentUserDetailsListener = null;
        }
        if (numberofFavListener != null) {
            numberofFavListener.remove();
            numberofFavListener = null;
        }
    }


    private void addComment() {
        String commentText = mCommentEditText.getText().toString();


        final Comment comment = new Comment(documentID, FirebaseAuth.getInstance().getCurrentUser().getUid(),
                currentUserImageUrl, currentUserName, commentText, null);
        DocumentReference currentRecipeRef = recipeRef.document(documentID);
        CollectionReference commentRef = currentRecipeRef.collection("Comments");

        commentRef.add(comment)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        mCommentEditText.setText("");
                        Toast.makeText(getContext(), "Successfully added comment ", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: ", e);
                    }
                });
    }

    private void getRecipeArgsPassed(RecipeDetailedFragmentArgs recipeDetailedFragmentArgs) {

        documentID = recipeDetailedFragmentArgs.getDocumentID();
        title = recipeDetailedFragmentArgs.getTitle();
        String description = recipeDetailedFragmentArgs.getDescription();
        String imageUrl = recipeDetailedFragmentArgs.getImageUrl();
        String ingredients = recipeDetailedFragmentArgs.getIngredients();
        String instructions = recipeDetailedFragmentArgs.getInstructions();
        isRecipeFavorite = recipeDetailedFragmentArgs.getIsFavorite();

        String[] favRecipesArray = recipeDetailedFragmentArgs.getFavRecipes();
        for (int i = 0; i < favRecipesArray.length; i++) {
            favRecipes.add(i, favRecipesArray[i]);
        }

        numberOfFav = recipeDetailedFragmentArgs.getNumberOfFaves();

        loggedInUserDocumentId = recipeDetailedFragmentArgs.getLoggedInUserDocumentId();

        tvTitle.setText(title);
        tvDescription.setText(description);
        tvIngredients.setText(ingredients);
        if (instructions != null) {
            tvInstructions.setText("Instructions: \n\n" + instructions.replace("newline", "\n"));
        } else {
            tvInstructions.setText("Instructions : \n\n");
        }
        mFavoriteNumber.setText(Integer.toString(numberOfFav));


        setFavoriteIcon(isRecipeFavorite);

        Picasso.get()
                .load(imageUrl)
                .fit()
                .centerCrop()
                .into(mImageView);

    }

    private void setFavoriteIcon(Boolean isFavorite) {
        if (isFavorite) {
            mFavoriteIcon.setImageResource(R.drawable.ic_favorite_red_24dp);
        } else {
            mFavoriteIcon.setImageResource(R.drawable.ic_favorite_border_black_24dp);
        }
    }

    private void buildRecyclerView() {
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getContext());
        mAdapter = new ParentCommentAdapter(getContext(), commentList);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
    }

    private void getCommentsFromDb() {

        DocumentReference currentRecipeRef = recipeRef.document(documentID);
        CollectionReference commentRef = currentRecipeRef.collection("Comments");

        Query commentQuery = null;
        if (mLastQueriedDocument != null) {
            commentQuery = commentRef.orderBy("mCommentTimeStamp", Query.Direction.ASCENDING)
                    .startAfter(mLastQueriedDocument); // Necessary so we don't have the same results multiple times
        } else {
            commentQuery = commentRef.orderBy("mCommentTimeStamp", Query.Direction.ASCENDING);
        }

        commentListener = commentQuery
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "onEvent: ", e);
                            return;
                        }
                        //get comments from commentRef
                        if (queryDocumentSnapshots != null) {
                            for (final QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                final QueryDocumentSnapshot.ServerTimestampBehavior behavior = ESTIMATE;
                                final Comment comment = document.toObject(Comment.class);
//                                commentList.add(new Comment(documentID, comment.getmUserId(), comment.getmImageUrl(), comment.getmName(), comment.getmCommentText()));
                                if (!newItemsToAdd.contains(document.getId())) {

                                    Log.d(TAG, "onEvent: doc id" + document.getId());

                                    final String currentCommentID = document.getId();
                                    usersRefListener = usersRef.whereEqualTo("user_id", comment.getmUserId())
                                            .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                                @Override
                                                public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                                                    User user = null;
                                                    if (queryDocumentSnapshots != null) {
                                                        user = queryDocumentSnapshots.getDocuments().get(0).toObject(User.class);

                                                        //Date date = document.getDate("mCommentTimeStamp", behavior);
                                                        Date date = comment.getmCommentTimeStamp();

                                                        if (!newItemsToAdd.contains(currentCommentID)) {
                                                            Comment commentx = new Comment(documentID, comment.getmUserId(), user.getUserProfilePicUrl()
                                                                    , comment.getmName(), comment.getmCommentText(), date);
                                                            commentx.setDocumentId(document.getId());
                                                            commentList.add(0, commentx);
                                                            Log.d(TAG, "onEvent: currrent commentID " + document.getId());
                                                            newItemsToAdd.add(document.getId());
                                                            mAdapter.notifyDataSetChanged();
                                                        }
                                                    }
                                                }
                                            });

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

        currentUserDetailsListener = usersRef.whereEqualTo("user_id", FirebaseAuth.getInstance().getCurrentUser().getUid())
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
