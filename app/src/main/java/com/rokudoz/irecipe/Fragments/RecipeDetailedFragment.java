package com.rokudoz.irecipe.Fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.Models.UserWhoFaved;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.ParentCommentAdapter;
import com.rokudoz.irecipe.Utils.RecipeDetailedViewPagerAdapter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior.ESTIMATE;

public class RecipeDetailedFragment extends Fragment {
    private static final String TAG = "RecipeDetailedFragment";


    private String documentID = "";
    private String currentUserImageUrl = "";
    private String currentUserName = "";
    private String loggedInUserDocumentId = "";
    private String title = "";
    private String userFavDocId = "";
    private Boolean isRecipeFavorite;
    public List<String> imageUrls;
    private List<Ingredient> userShoppingIngredientList = new ArrayList<>();
    private List<Ingredient> recipeIngredientsToAddToShoppingList = new ArrayList<>();

    private ViewPager viewPager;
    private RecipeDetailedViewPagerAdapter recipeDetailedViewPagerAdapter;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;


    private TextView tvTitle, tvDescription, tvIngredients, tvInstructions, mFavoriteNumber, tvMissingIngredientsNumber;
    private ImageView mImageView, mFavoriteIcon;
    private Button mAddCommentBtn;
    private ExtendedFloatingActionButton mAddMissingingredientsFAB;
    private EditText mCommentEditText;

    private ArrayList<Comment> commentList = new ArrayList<>();
    private ArrayList<String> favRecipes = new ArrayList<>();
    private Integer numberOfFav;
    private ArrayList<String> newItemsToAdd = new ArrayList<>();
    private User mUser;

    private DocumentSnapshot mLastQueriedDocument;

    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration currentSubCollectionListener, usersRefListener, commentListener, numberofFavListener, currentUserDetailsListener,
            userShoppingListListener;
    private CollectionReference recipeRef = db.collection("Recipes");
    private CollectionReference usersRef = db.collection("Users");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recipe_detailed, container, false);

        mUser = new User();
        imageUrls = new ArrayList<>();
        tvTitle = view.findViewById(R.id.tvTitle);
        tvDescription = view.findViewById(R.id.tvDescription);
        tvIngredients = view.findViewById(R.id.tvIngredientsList);
//        mImageView = view.findViewById(R.id.recipeDetailed_image);
        mAddCommentBtn = view.findViewById(R.id.recipeDetailed_addComment_btn);
        mRecyclerView = view.findViewById(R.id.comment_recycler_view);
        mCommentEditText = view.findViewById(R.id.recipeDetailed_et_commentInput);
        tvInstructions = view.findViewById(R.id.tvInstructions);
        mFavoriteIcon = view.findViewById(R.id.imageview_favorite_icon);
        mFavoriteNumber = view.findViewById(R.id.recipeDetailed_numberOfFaved);
        tvMissingIngredientsNumber = view.findViewById(R.id.missing_ingredientsNumber);
        mAddMissingingredientsFAB = view.findViewById(R.id.fab_addMissingIngredients);

        tvMissingIngredientsNumber.setVisibility(View.INVISIBLE);
        mAddMissingingredientsFAB.setVisibility(View.INVISIBLE);

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
                                                            if (getContext() != null)
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
        setupFirebaseAuth(view);

        return view; // HAS TO BE THE LAST ONE ---------------------------------
    }

    private void setupViewPager(View view) {

        viewPager = view.findViewById(R.id.view_pager);
        recipeDetailedViewPagerAdapter = new RecipeDetailedViewPagerAdapter(getContext(), imageUrls);
        viewPager.setAdapter(recipeDetailedViewPagerAdapter);
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
        if (userShoppingListListener != null) {
            userShoppingListListener.remove();
            userShoppingListListener = null;
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
        numberOfFav = recipeDetailedFragmentArgs.getNumberOfFaves();
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

    private void getCurrentUserDetails(final View view) {

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
                            String userDocId = documentSnapshot.getDocument().getId();
                            currentUserImageUrl = mUser.getUserProfilePicUrl();
                            currentUserName = mUser.getName();
                            favRecipes = mUser.getFavoriteRecipes();
                            loggedInUserDocumentId = documentSnapshot.getDocument().getId();
                            isRecipeFavorite = favRecipes.contains(documentID);
                            setFavoriteIcon(isRecipeFavorite);

                            getUserShoppingList(userDocId, view);
                        }


                    }
                });
    }

    private void getUserShoppingList(String userDocId, final View view) {
//        usersRef.document(userDocId).collection("ShoppingList").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
//            @Override
//            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
//                if (queryDocumentSnapshots != null) {
//                    for (QueryDocumentSnapshot querySnapshot : queryDocumentSnapshots) {
//                        Ingredient ingredient = querySnapshot.toObject(Ingredient.class);
//                        userShoppingIngredientList.add(ingredient);
//                        Log.d(TAG, "onSuccess: ADDED TO USERSHOPPING LIST " + ingredient.toString());
//                    }
//                }
//                getRecipeDocument(view);
//            }
//        });


        userShoppingListListener = usersRef.document(userDocId).collection("ShoppingList").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "onEvent: ", e);
                }
                for (DocumentChange documentSnapshot : queryDocumentSnapshots.getDocumentChanges()) {
                    Ingredient ingredient = documentSnapshot.getDocument().toObject(Ingredient.class);
                    userShoppingIngredientList.add(ingredient);
                    Log.d(TAG, "onEvent: ADDED TO USER SHOPPING LIST " + ingredient.toString());
                }
                getRecipeDocument(view);
            }
        });
    }

    private void getRecipeDocument(final View view) {

        recipeRef.document(documentID).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "onEvent: ", e);
                    return;
                }
                Recipe recipe = documentSnapshot.toObject(Recipe.class);
                title = recipe.getTitle();
                String description = recipe.getDescription();
                imageUrls = recipe.getImageUrl();
                if (recipeDetailedViewPagerAdapter != null)
                    recipeDetailedViewPagerAdapter.notifyDataSetChanged();

                Map<String, Float> ingredientsWithQuantity = recipe.getIngredient_quantity();
                Map<String, Boolean> recipe_ingredients_tag = recipe.getTags();
                String instructions = recipe.getInstructions();

                tvTitle.setText(title);
                tvDescription.setText(description);

                StringBuilder ingredientsToPutInTV = new StringBuilder();
                if (ingredientsWithQuantity != null)
                    for (String ingredient : ingredientsWithQuantity.keySet()) {
                        ingredientsToPutInTV.append(ingredient).append(" ").append(ingredientsWithQuantity.get(ingredient)).append("\n");
                    }
                tvIngredients.setText(ingredientsToPutInTV.toString());

                int nrOfMissingIngredients = 0;

                for (String ingredientName : recipe_ingredients_tag.keySet()) {
                    if (mUser.getTags().get(ingredientName) != recipe_ingredients_tag.get(ingredientName)
                            && recipe_ingredients_tag.get(ingredientName)) {
                        Ingredient ingredient = new Ingredient(ingredientName, Objects.requireNonNull(ingredientsWithQuantity).get(ingredientName), "gram", false);
                        if (!recipeIngredientsToAddToShoppingList.contains(ingredient)) {
                            if (userShoppingIngredientList.contains(ingredient)) {
                                if (userShoppingIngredientList.get(userShoppingIngredientList.indexOf(ingredient)).getOwned())
                                    ingredient.setOwned(true);
                                recipeIngredientsToAddToShoppingList.add(ingredient);

                            }
                            nrOfMissingIngredients++;

                            Log.d(TAG, "onEvent: INGREDIENT NOT IN COMMON " + ingredient.toString());
                        } else {
                            recipeIngredientsToAddToShoppingList.remove(ingredient);
                            nrOfMissingIngredients--;
                        }

                    }
                }
                if (nrOfMissingIngredients > 0 && !userShoppingIngredientList.containsAll(recipeIngredientsToAddToShoppingList)) {
                    tvMissingIngredientsNumber.setVisibility(View.VISIBLE);
                    tvMissingIngredientsNumber.setText("Missing " + nrOfMissingIngredients + " ingredients");
                    mAddMissingingredientsFAB.setVisibility(View.VISIBLE);
                    mAddMissingingredientsFAB.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // HERE WE IMPLEMENTS
                            for (final Ingredient ingredient : recipeIngredientsToAddToShoppingList) {
                                if (!userShoppingIngredientList.contains(ingredient)) {
                                    usersRef.document(loggedInUserDocumentId).collection("ShoppingList")
                                            .add(ingredient).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                        @Override
                                        public void onSuccess(DocumentReference documentReference) {
                                            Log.d(TAG, "onSuccess: ADDED TO SHOPPING LIST " + ingredient.toString());
                                            userShoppingIngredientList.add(ingredient);
                                        }
                                    });
                                }
                            }
                            mAddMissingingredientsFAB.setVisibility(View.INVISIBLE);
                            tvMissingIngredientsNumber.setVisibility(View.INVISIBLE);

                        }
                    });
                }
                // If the user has the missing ingredients in the shopping list, don't show button, but show message that they are in the list
                else if (nrOfMissingIngredients > 0 && userShoppingIngredientList.containsAll(recipeIngredientsToAddToShoppingList)) {
                    for (Ingredient ing : recipeIngredientsToAddToShoppingList) {
                        if (!recipeIngredientsToAddToShoppingList.get(recipeIngredientsToAddToShoppingList.indexOf(ing)).getOwned()){
                            tvMissingIngredientsNumber.setVisibility(View.VISIBLE);
                            tvMissingIngredientsNumber.setText(nrOfMissingIngredients + " missing ingredients are in your shopping list");
                        }
                    }
                }
                setupViewPager(view);
            }
        });


        getCommentsFromDb();
    }

    /*
        ----------------------------- Firebase setup ---------------------------------
     */
    private void setupFirebaseAuth(final View view) {
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
                        getCurrentUserDetails(view);
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
