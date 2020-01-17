package com.rokudoz.irecipe.Fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
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
import com.rokudoz.irecipe.Models.Instruction;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.Models.UserWhoFaved;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.ParentCommentAdapter;
import com.rokudoz.irecipe.Utils.RecipeDetailedViewPagerAdapter;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior.ESTIMATE;

public class RecipeDetailedFragment extends Fragment {
    private static final String TAG = "RecipeDetailedFragment";
    // Hold a reference to the current animator,
    // so that it can be canceled mid-way.
    private Animator currentAnimator;

    // The system "short" animation time duration, in milliseconds. This
    // duration is ideal for subtle animations or animations that occur
    // very frequently.
    private int shortAnimationDuration;
    private int nrOfMissingIngredients = 0;
    private Boolean showFab = false;
    private String documentID = "";
    private String currentUserImageUrl = "";
    private String currentUserName = "";
    private String loggedInUserDocumentId = "";
    private String title = "";
    private String userFavDocId = "";
    private Boolean isRecipeFavorite = false;
    private List<String> imageUrls;
    private List<Ingredient> userShoppingIngredientList = new ArrayList<>();
    private List<Ingredient> userIngredientList = new ArrayList<>();
    private List<Ingredient> recipeIngredientsToAddToShoppingList = new ArrayList<>();

    private ViewPager viewPager;
    private RecipeDetailedViewPagerAdapter recipeDetailedViewPagerAdapter;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private LinearLayout mInstructionsLinearLayout;
    private NestedScrollView nestedScrollView;

    private MaterialButton mDeleteRecipeBtn;
    private TextView tvTitle, tvDescription, tvIngredients, mFavoriteNumber, tvMissingIngredientsNumber, tvCreatorName;
    private ImageView mImageView, mFavoriteIcon, mCreatorImage;
    private Button mAddCommentBtn;
    private ExtendedFloatingActionButton mAddMissingIngredientsFAB;
    private EditText mCommentEditText;

    private ArrayList<Comment> commentList = new ArrayList<>();
    private List<String> userFavRecipesList = new ArrayList<>();
    private List<Instruction> instructionsAddedToLayout = new ArrayList<>();
    private Integer numberOfFav;
    private ArrayList<String> newItemsToAdd = new ArrayList<>();
    private User mUser;

    View view;
    private int oldScrollYPosition = 0;
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
        view = inflater.inflate(R.layout.fragment_recipe_detailed, container, false);

        mUser = new User();
        imageUrls = new ArrayList<>();
        tvTitle = view.findViewById(R.id.tvTitle);
        tvDescription = view.findViewById(R.id.tvDescription);
        tvIngredients = view.findViewById(R.id.tvIngredientsList);
        mAddCommentBtn = view.findViewById(R.id.recipeDetailed_addComment_btn);
        mRecyclerView = view.findViewById(R.id.comment_recycler_view);
        mCommentEditText = view.findViewById(R.id.recipeDetailed_et_commentInput);
        mInstructionsLinearLayout = view.findViewById(R.id.recipeDetailed_instructions_linearLayout);
        mFavoriteIcon = view.findViewById(R.id.imageview_favorite_icon);
        mFavoriteNumber = view.findViewById(R.id.recipeDetailed_numberOfFaved);
        tvMissingIngredientsNumber = view.findViewById(R.id.missing_ingredientsNumber);
        mAddMissingIngredientsFAB = view.findViewById(R.id.fab_addMissingIngredients);
        nestedScrollView = view.findViewById(R.id.nestedScrollView);
        tvMissingIngredientsNumber.setVisibility(View.INVISIBLE);
        mDeleteRecipeBtn = view.findViewById(R.id.recipeDetailed_deleteRecipe_MaterialBtn);
        tvCreatorName = view.findViewById(R.id.recipeDetailed_creatorName_TextView);
        mCreatorImage = view.findViewById(R.id.recipeDetailed_creatorImage_ImageView);
        mAddMissingIngredientsFAB.hide();


        RecipeDetailedFragmentArgs recipeDetailedFragmentArgs = RecipeDetailedFragmentArgs.fromBundle(getArguments());
        getRecipeArgsPassed(recipeDetailedFragmentArgs);


        DocumentReference currentRecipeRef = recipeRef.document(documentID);
        final CollectionReference currentRecipeSubCollection = currentRecipeRef.collection("UsersWhoFaved");

        // Retrieve and cache the system's default "short" animation time.
        shortAnimationDuration = getResources().getInteger(
                android.R.integer.config_shortAnimTime);

//        Hide add missing ingredients FAB on scroll
        nestedScrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (scrollY > oldScrollY) {
                    mAddMissingIngredientsFAB.hide();
                } else {
                    Log.d(TAG, "onScrollChange: " + nrOfMissingIngredients);
                    if (showFab)
                        mAddMissingIngredientsFAB.show();
                }
            }
        });


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
                if (userFavRecipesList == null) {
                    userFavRecipesList = new ArrayList<>();
                }
                if (userFavRecipesList.contains(documentID)) {
                    userFavRecipesList.remove(documentID);
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
                    userFavRecipesList.add(documentID);
                    isRecipeFavorite = true;
                    UserWhoFaved userWhoFaved = new UserWhoFaved(mUser.getUser_id(), null);
                    currentRecipeSubCollection.add(userWhoFaved);
                    Toast.makeText(getContext(), "Added " + title + " to favorites", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onClick: ADDED + " + title + " " + documentID + " to favorites");
                }
                setFavoriteIcon(isRecipeFavorite);

                DocumentReference favRecipesRef = usersRef.document(loggedInUserDocumentId);
                favRecipesRef.update("favoriteRecipes", userFavRecipesList);
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
        getCurrentUserDetails(view);

        return view; // HAS TO BE THE LAST ONE ---------------------------------
    }


    private void zoomImageFromThumb(final View imageButton, String imageUrl) {
        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        if (currentAnimator != null) {
            currentAnimator.cancel();
        }

        // Load the high-resolution "zoomed-in" image.
        final ImageView expandedImageView = (ImageView) view.findViewById(
                R.id.expanded_image);

        Picasso.get().load(imageUrl).into(expandedImageView);
//        expandedImageView.setImageResource(imageResId);

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        imageButton.getGlobalVisibleRect(startBounds);
        view.findViewById(R.id.container)
                .getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        float startScale;
        if ((float) finalBounds.width() / finalBounds.height()
                > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        imageButton.setAlpha(0f);
        expandedImageView.setVisibility(View.VISIBLE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        expandedImageView.setPivotX(0f);
        expandedImageView.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        AnimatorSet set = new AnimatorSet();
        set
                .play(ObjectAnimator.ofFloat(expandedImageView, View.X,
                        startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.Y,
                        startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X,
                        startScale, 1f))
                .with(ObjectAnimator.ofFloat(expandedImageView,
                        View.SCALE_Y, startScale, 1f));
        set.setDuration(shortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                currentAnimator = null;
            }
        });
        set.start();
        currentAnimator = set;

        // Upon clicking the zoomed-in image, it should zoom back down
        // to the original bounds and show the thumbnail instead of
        // the expanded image.
        final float startScaleFinal = startScale;
        expandedImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentAnimator != null) {
                    currentAnimator.cancel();
                }

                // Animate the four positioning/sizing properties in parallel,
                // back to their original values.
                AnimatorSet set = new AnimatorSet();
                set.play(ObjectAnimator
                        .ofFloat(expandedImageView, View.X, startBounds.left))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.Y, startBounds.top))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.SCALE_X, startScaleFinal))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.SCALE_Y, startScaleFinal));
                set.setDuration(shortAnimationDuration);
                set.setInterpolator(new DecelerateInterpolator());
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        imageButton.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        currentAnimator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        imageButton.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        currentAnimator = null;
                    }
                });
                set.start();
                currentAnimator = set;
            }
        });
    }

    private void setupViewPager(View view) {

        viewPager = view.findViewById(R.id.view_pager);
        recipeDetailedViewPagerAdapter = new RecipeDetailedViewPagerAdapter(getContext(), imageUrls);
        viewPager.setAdapter(recipeDetailedViewPagerAdapter);
    }

    @Override
    public void onStop() {
        super.onStop();
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

        currentUserDetailsListener = usersRef.document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "onEvent: ", e);
                            return;
                        }

                        mUser = documentSnapshot.toObject(User.class);
                        currentUserImageUrl = mUser.getUserProfilePicUrl();
                        currentUserName = mUser.getName();
                        userFavRecipesList = mUser.getFavoriteRecipes();
                        loggedInUserDocumentId = documentSnapshot.getId();
                        if (userFavRecipesList != null && !userFavRecipesList.isEmpty())
                            isRecipeFavorite = userFavRecipesList.contains(documentID);

                        setFavoriteIcon(isRecipeFavorite);

                        getUserIngredientList(mUser.getUser_id(), view);

                    }
                });

    }

    private void getUserIngredientList(String user_id, final View view) {
        usersRef.document(user_id).collection("Ingredients").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "onEvent: ", e);
                    return;
                }
                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    Ingredient ingredient = documentSnapshot.toObject(Ingredient.class);
                    if (!userIngredientList.contains(ingredient)) {
                        userIngredientList.add(ingredient);
                    }
                }
                getUserShoppingList(mUser.getUser_id(), view);
            }
        });
    }

    private void getUserShoppingList(final String userDocId, final View view) {

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

        recipeRef.document(documentID).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Recipe recipe = documentSnapshot.toObject(Recipe.class);
                title = recipe.getTitle();
                String description = recipe.getDescription();
                imageUrls = recipe.getImageUrls_list();
                if (recipeDetailedViewPagerAdapter != null)
                    recipeDetailedViewPagerAdapter.notifyDataSetChanged();

                tvTitle.setText(title);
                tvDescription.setText(description);

                StringBuilder ingredientsToPutInTV = new StringBuilder();
                ////////////////////////////////////////////////////////// LOGIC TO GET RECIPES INGREDIENTS AND INSTRUCTIONS HERE

                tvIngredients.setText(ingredientsToPutInTV.toString());

                if (recipe.getCreator_docId().equals(mUser.getUser_id())) {
                    mDeleteRecipeBtn.setVisibility(View.VISIBLE);
                    mDeleteRecipeBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            recipeRef.document(documentID).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Toast.makeText(getActivity(), "Successfully deleted", Toast.LENGTH_SHORT).show();
                                    Navigation.findNavController(view).popBackStack();
                                }
                            });
                        }
                    });
                }

                getRecipeCreatorDetails(recipe.getCreator_docId());
                getRecipeIngredients();


                setupViewPager(view);
            }
        });

        getCommentsFromDb();
    }

    private void getRecipeCreatorDetails(final String creator_docId) {
        usersRef.document(creator_docId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                User user = documentSnapshot.toObject(User.class);
                tvCreatorName.setText(user.getName());
                Picasso.get().load(user.getUserProfilePicUrl()).centerCrop().fit().into(mCreatorImage);

                tvCreatorName.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Navigation.findNavController(view).navigate(RecipeDetailedFragmentDirections.actionRecipeDetailedFragmentToUserProfileFragment2(creator_docId));
                    }
                });
                mCreatorImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Navigation.findNavController(view).navigate(RecipeDetailedFragmentDirections.actionRecipeDetailedFragmentToUserProfileFragment2(creator_docId));
                    }
                });
            }
        });
    }

    private void getRecipeIngredients() {
        final List<Ingredient> recipeIngredientList = new ArrayList<>();

        //Get recipe Ingredients from RecipeIngredients Collection
        recipeRef.document(documentID).collection("RecipeIngredients").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "onEvent: ", e);
                    return;
                }
                for (QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots) {
                    Ingredient ingredient = queryDocumentSnapshot.toObject(Ingredient.class);
                    if (!recipeIngredientList.contains(ingredient)) {
                        recipeIngredientList.add(ingredient);
                    }

                }

                for (int i = 0; i < recipeIngredientList.size(); i++) {
                    Log.d(TAG, "onEvent: " + recipeIngredientList.get(i).toString());
                }
                for (Ingredient ing : recipeIngredientList) {
                    if (userIngredientList.contains(ing)) {
                        if (!userIngredientList.get(userIngredientList.indexOf(ing)).getOwned()) {
                            if (!recipeIngredientsToAddToShoppingList.contains(ing)) {
                                if (userShoppingIngredientList.contains(ing)) {
                                    // If user has the ingredient in shopping list and it is checked as true, set it as owned
                                    if (userShoppingIngredientList.get(userShoppingIngredientList.indexOf(ing)).getOwned())
                                        ing.setOwned(true);

                                }
                                recipeIngredientsToAddToShoppingList.add(ing);
                                nrOfMissingIngredients++;

                                Log.d(TAG, "onEvent: INGREDIENT NOT IN COMMON " + ing.toString());
                            } else {
                                recipeIngredientsToAddToShoppingList.remove(ing);
                                nrOfMissingIngredients--;
                            }
                        }

                    } else {
                        recipeIngredientsToAddToShoppingList.add(ing);
                        nrOfMissingIngredients++;
                    }
                }

                if (nrOfMissingIngredients > 0 && !userShoppingIngredientList.containsAll(recipeIngredientsToAddToShoppingList)) {
                    tvMissingIngredientsNumber.setVisibility(View.VISIBLE);
                    tvMissingIngredientsNumber.setText("Missing " + nrOfMissingIngredients + " ingredients");
                    mAddMissingIngredientsFAB.setText("+" + nrOfMissingIngredients);
                    showFab = true;
                    mAddMissingIngredientsFAB.show();
                    mAddMissingIngredientsFAB.setOnClickListener(new View.OnClickListener() {
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
                            mAddMissingIngredientsFAB.hide();
                            showFab = false;
                            tvMissingIngredientsNumber.setVisibility(View.INVISIBLE);
                        }
                    });
                }
                // If the user has the missing ingredients in the shopping list, don't show button, but show message that they are in the list
                else if (nrOfMissingIngredients > 0 && userShoppingIngredientList.containsAll(recipeIngredientsToAddToShoppingList)) {
                    for (Ingredient ing : recipeIngredientsToAddToShoppingList) {
                        if (!recipeIngredientsToAddToShoppingList.get(recipeIngredientsToAddToShoppingList.indexOf(ing)).getOwned()) {
                            tvMissingIngredientsNumber.setVisibility(View.VISIBLE);
                            tvMissingIngredientsNumber.setText(nrOfMissingIngredients + " missing ingredients are in your shopping list");
                        }
                    }
                }

                if (nrOfMissingIngredients > 0) {
                    Log.d(TAG, "onEvent: usershopping size: " + userShoppingIngredientList.size()
                            + " recipe size : " + recipeIngredientsToAddToShoppingList.size());
                    for (Ingredient testinguser : userShoppingIngredientList)
                        Log.d(TAG, "onEvent: usershoppinglist: " + userShoppingIngredientList.toString());
                    for (Ingredient testingrecipe : recipeIngredientsToAddToShoppingList)
                        Log.d(TAG, "onEvent: recipeshoppinglist: " + recipeIngredientsToAddToShoppingList.toString());
                }

                tvIngredients.setText("Ingredients: \n");
                for (int i = 0; i < recipeIngredientList.size(); i++) {
                    tvIngredients.append(" " + recipeIngredientList.get(i).getName() + " " + Math.round(recipeIngredientList.get(i).getQuantity())
                            + " " + recipeIngredientList.get(i).getQuantity_type() + "\n");
                }

                getRecipeInstructions(documentID);
            }
        });
    }

    private void getRecipeInstructions(String documentID) {
        final List<Instruction> recipeInstructionList = new ArrayList<>();

        recipeRef.document(documentID).collection("RecipeInstructions").orderBy("stepNumber").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "onEvent: ", e);
                    return;
                }
                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    Instruction instruction = documentSnapshot.toObject(Instruction.class);
                    if (!recipeInstructionList.contains(instruction)) {
                        recipeInstructionList.add(instruction);
                    }
                }

                for (Instruction ins : recipeInstructionList) {
                    if (!instructionsAddedToLayout.contains(ins)) {
                        addInstructionLayout(ins);
                        instructionsAddedToLayout.add(ins);
                    }
                }

            }
        });
    }

    private void addInstructionLayout(final Instruction instruction) {
        if (getActivity() != null) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(4, 4, 4, 4);
            final LinearLayout linearLayout = new LinearLayout(getContext());
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            linearLayout.setLayoutParams(params);

            TextView stepNumberTextView = new TextView(getActivity());
            stepNumberTextView.setText("Step " + instruction.getStepNumber());
            stepNumberTextView.setTextSize(18);
            stepNumberTextView.setPadding(22, 25, 0, 8);
            stepNumberTextView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            linearLayout.addView(stepNumberTextView);

            TextView instructionTextView = new TextView(getActivity());
            instructionTextView.setText(instruction.getText());
            instructionTextView.setTextSize(14);
            instructionTextView.setPadding(16, 0, 0, 12);
            instructionTextView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            linearLayout.addView(instructionTextView);

            if (instruction.getImgUrl() != null && !instruction.getImgUrl().equals("")) {
                final ImageButton instructionImageView = new ImageButton(getActivity());
                instructionImageView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 450));
                Picasso.get().load(instruction.getImgUrl()).fit().centerCrop().into(instructionImageView);
                linearLayout.addView(instructionImageView);

                instructionImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //ZOOM
                        zoomImageFromThumb(instructionImageView, instruction.getImgUrl());
                    }
                });
            }


            mInstructionsLinearLayout.addView(linearLayout);
        }
    }


}
