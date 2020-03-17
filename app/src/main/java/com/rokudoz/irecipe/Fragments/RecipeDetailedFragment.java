package com.rokudoz.irecipe.Fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
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
import com.rokudoz.irecipe.Models.Comment;
import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.Models.Instruction;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Models.ScheduledMeal;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.Models.UserWhoFaved;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.Adapters.ParentCommentAdapter;
import com.rokudoz.irecipe.Utils.Adapters.RecipeIngredientsAdapter;
import com.rokudoz.irecipe.Utils.Adapters.RecipeInstructionsAdapter;
import com.rokudoz.irecipe.Utils.Adapters.RecipeDetailedViewPagerAdapter;
import com.rokudoz.irecipe.Utils.ScheduleNotifReceiver;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class RecipeDetailedFragment extends Fragment implements RecipeInstructionsAdapter.OnItemClickListener, ParentCommentAdapter.OnItemClickListener {
    private static final String TAG = "RecipeDetailedFragment";
    // Hold a reference to the current animator,
    // so that it can be canceled mid-way.
    private Animator currentAnimator;

    private int mYear, mMonth, mDate, mHour, mMin;

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
    private String recipeCategory = "";
    private String userFavDocId = "";
    private Boolean isRecipeFavorite = false;
    private List<String> imageUrls;
    private List<Ingredient> userShoppingIngredientList = new ArrayList<>();
    private List<Ingredient> userIngredientList = new ArrayList<>();


    private ViewPager viewPager;
    private RecipeDetailedViewPagerAdapter recipeDetailedViewPagerAdapter;

    private RecyclerView commentRecyclerView;
    private ParentCommentAdapter commentAdapter;
    private RecyclerView.LayoutManager commentLayoutManager;

    private RecyclerView instructionsRecyclewView;
    private RecipeInstructionsAdapter instructionsAdapter;
    private RecyclerView.LayoutManager instructionsLayoutManager;

    private RecyclerView ingredientsRecyclewView;
    private RecipeIngredientsAdapter ingredientsAdapter;
    private RecyclerView.LayoutManager ingredientsLayoutManager;

    private NestedScrollView nestedScrollView;

    private MaterialButton mDeleteRecipeBtn;
    private TextView tvTitle, tvDescription, mFavoriteNumber, tvMissingIngredientsNumber, tvCreatorName, tvDuration, tvComplexity;
    private ImageView mFavoriteIcon;
    private CircleImageView mCreatorImage;
    private Button mAddCommentBtn;
    private ExtendedFloatingActionButton mAddMissingIngredientsFAB;
    private FloatingActionButton fab_AddToSchedule;
    private EditText mCommentEditText;
    private List<Ingredient> recipeIngredientList = new ArrayList<>();
    private List<Instruction> recipeInstructionList = new ArrayList<>();
    private ArrayList<Comment> commentList = new ArrayList<>();
    private Integer numberOfFav;
    private ArrayList<String> newItemsToAdd = new ArrayList<>();
    private User mUser;

    private View view;
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
        mAddCommentBtn = view.findViewById(R.id.recipeDetailed_addComment_btn);
        commentRecyclerView = view.findViewById(R.id.comment_recycler_view);
        instructionsRecyclewView = view.findViewById(R.id.recipeDetailed_instructions_recycler_view);
        ingredientsRecyclewView = view.findViewById(R.id.recipeDetailed_ingredients_recycler_view);
        mCommentEditText = view.findViewById(R.id.recipeDetailed_et_commentInput);
        mFavoriteIcon = view.findViewById(R.id.imageview_favorite_icon);
        mFavoriteNumber = view.findViewById(R.id.recipeDetailed_numberOfFaved);
        tvMissingIngredientsNumber = view.findViewById(R.id.missing_ingredientsNumber);
        mAddMissingIngredientsFAB = view.findViewById(R.id.fab_addMissingIngredients);
        fab_AddToSchedule = view.findViewById(R.id.recipeDetailed_addRecipeToSchedule);
        nestedScrollView = view.findViewById(R.id.nestedScrollView);
        tvMissingIngredientsNumber.setVisibility(View.INVISIBLE);
        mDeleteRecipeBtn = view.findViewById(R.id.recipeDetailed_deleteRecipe_MaterialBtn);
        tvCreatorName = view.findViewById(R.id.recipeDetailed_creatorName_TextView);
        mCreatorImage = view.findViewById(R.id.recipeDetailed_creatorImage_ImageView);
        tvDuration = view.findViewById(R.id.recipeDetailed_duration);
        tvComplexity = view.findViewById(R.id.recipeDetailed_complexity);
        mAddMissingIngredientsFAB.hide();

        RecipeDetailedFragmentArgs recipeDetailedFragmentArgs = RecipeDetailedFragmentArgs.fromBundle(getArguments());
        getRecipeArgsPassed(recipeDetailedFragmentArgs);

        BottomNavigationView navBar = getActivity().findViewById(R.id.bottom_navigation);
        navBar.setVisibility(View.VISIBLE);

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
                    fab_AddToSchedule.hide();
                } else {
                    fab_AddToSchedule.show();
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
                if (isRecipeFavorite) {
                    isRecipeFavorite = false;
                    setFavoriteIcon(isRecipeFavorite);
                    currentRecipeSubCollection.document(mUser.getUser_id()).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            if (getContext() != null)
                                Toast.makeText(getContext(), "Removed from favorites", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    isRecipeFavorite = true;
                    setFavoriteIcon(isRecipeFavorite);
                    UserWhoFaved userWhoFaved = new UserWhoFaved(mUser.getUser_id(), mUser.getName(), mUser.getUserProfilePicUrl(), null);
                    currentRecipeSubCollection.document(mUser.getUser_id()).set(userWhoFaved);
                    Toast.makeText(getContext(), "Added " + title + " to favorites", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onClick: ADDED + " + title + " " + documentID + " to favorites");
                }
            }
        });

        fab_AddToSchedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Show Date Picker dialog and then TimePicker dialog to schedule the recipe accordingly
                final Calendar calendar = Calendar.getInstance();
                mYear = calendar.get(Calendar.YEAR);
                mMonth = calendar.get(Calendar.MONTH);
                mDate = calendar.get(Calendar.DATE);
                DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, final int year, final int month, final int dayOfMonth) {
                        mHour = calendar.get(Calendar.HOUR_OF_DAY);
                        mMin = calendar.get(Calendar.MINUTE);
                        TimePickerDialog timePickerDialog = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                final DateFormat dateFormat = new SimpleDateFormat("dd, MMMM, YYYY", Locale.getDefault());

                                final Date date = new GregorianCalendar(year, month, dayOfMonth, hourOfDay, minute).getTime();

                                final ScheduledMeal scheduleEvent = new ScheduledMeal(documentID, date, dateFormat.format(date), recipeCategory);

                                usersRef.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("ScheduleEvents").add(scheduleEvent)
                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                            @Override
                                            public void onSuccess(DocumentReference documentReference) {
                                                Log.d(TAG, "onSuccess: ADDED " + scheduleEvent.toString());
                                                scheduleNotification(getActivity(), 0, date.getTime(), documentID);
                                                Toast.makeText(getContext(), "Added to meal schedule", Toast.LENGTH_SHORT).show();
                                            }
                                        });

                            }
                        }, mHour, mMin, true);
                        timePickerDialog.show();
                    }
                }, mYear, mMonth, mDate);
                datePickerDialog.show();

            }
        });

        buildRecyclerView();
        getCurrentUserDetails(view);

        return view; // HAS TO BE THE LAST ONE ---------------------------------
    }

    private void scheduleNotification(Context context, int reqCode, long time, String recipe_id) {
        Intent intent = new Intent(context, ScheduleNotifReceiver.class);
        intent.putExtra("recipe_id", recipe_id);
        intent.putExtra("recipe_category", recipeCategory);
        intent.putExtra("recipe_title", title);
        PendingIntent pending = PendingIntent.getBroadcast(context, reqCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager != null) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending);
        }
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


        final Comment comment = new Comment(documentID, FirebaseAuth.getInstance().getCurrentUser().getUid(), mUser.getName(), mUser.getUserProfilePicUrl()
                , commentText, null);
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
        commentRecyclerView.setHasFixedSize(true);
        commentLayoutManager = new LinearLayoutManager(getContext());
        commentAdapter = new ParentCommentAdapter(getContext(), commentList);
        commentRecyclerView.setLayoutManager(commentLayoutManager);
        commentRecyclerView.setAdapter(commentAdapter);
        commentAdapter.setOnItemClickListener(RecipeDetailedFragment.this);

        instructionsRecyclewView.setHasFixedSize(true);
        instructionsLayoutManager = new LinearLayoutManager(getContext());
        instructionsAdapter = new RecipeInstructionsAdapter(recipeInstructionList);
        instructionsRecyclewView.setLayoutManager(instructionsLayoutManager);
        instructionsRecyclewView.setAdapter(instructionsAdapter);
        instructionsAdapter.setOnItemClickListener(RecipeDetailedFragment.this);

        ingredientsRecyclewView.setHasFixedSize(true);
        ingredientsLayoutManager = new LinearLayoutManager(getContext());
        ingredientsAdapter = new RecipeIngredientsAdapter(recipeIngredientList);
        ingredientsRecyclewView.setLayoutManager(ingredientsLayoutManager);
        ingredientsRecyclewView.setAdapter(ingredientsAdapter);
    }

    private void getCommentsFromDb() {

        DocumentReference currentRecipeRef = recipeRef.document(documentID);
        CollectionReference commentRef = currentRecipeRef.collection("Comments");

        Query commentQuery = null;
        if (mLastQueriedDocument != null) {
            commentQuery = commentRef.orderBy("comment_timeStamp", Query.Direction.ASCENDING)
                    .startAfter(mLastQueriedDocument); // Necessary so we don't have the same results multiple times
        } else {
            commentQuery = commentRef.orderBy("comment_timeStamp", Query.Direction.ASCENDING);
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
                                final Comment comment = document.toObject(Comment.class);
                                if (!newItemsToAdd.contains(document.getId())) {

                                    Log.d(TAG, "onEvent: doc id" + document.getId());

                                    comment.setDocumentID(document.getId());
                                    commentList.add(0, comment);
                                    Log.d(TAG, "onEvent: currrent commentID " + document.getId());
                                    newItemsToAdd.add(document.getId());
                                    commentAdapter.notifyDataSetChanged();
                                }
                            }
                            if (queryDocumentSnapshots.getDocuments().size() != 0) {
                                mLastQueriedDocument = queryDocumentSnapshots.getDocuments()
                                        .get(queryDocumentSnapshots.getDocuments().size() - 1);
                            }
                        }
                        commentAdapter.notifyDataSetChanged();
                        Log.d(TAG, "onEvent: querrysize " + queryDocumentSnapshots.size() + " ListSize: " + commentList.size());
                    }
                });
    }

    private void getCurrentUserDetails(final View view) {
        usersRef.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "onEvent: ", e);
                    return;
                }
                mUser = documentSnapshot.toObject(User.class);
                currentUserImageUrl = mUser.getUserProfilePicUrl();
                currentUserName = mUser.getName();
                loggedInUserDocumentId = documentSnapshot.getId();

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
        usersRef.document(userDocId).collection("ShoppingList").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "onEvent: ", e);
                    return;
                }
                for (DocumentChange documentSnapshot : queryDocumentSnapshots.getDocumentChanges()) {
                    Ingredient ingredient = documentSnapshot.getDocument().toObject(Ingredient.class);
                    if (!userShoppingIngredientList.contains(ingredient)) {
                        userShoppingIngredientList.add(ingredient);
                    }
                    Log.d(TAG, "onEvent: Got USER SHOPPING LIST ingredient" + ingredient.toString());
                }
                getRecipeDocument(view);
            }
        });

    }

    private void getRecipeDocument(final View view) {
        recipeRef.document(documentID).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable final FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "onEvent: ", e);
                    return;
                }
                if (documentSnapshot != null) {
                    final Recipe recipe = documentSnapshot.toObject(Recipe.class);
                    if (recipe.getTitle() != null) {
                        title = recipe.getTitle();
                        tvTitle.setText(title);
                    }
                    if (recipe.getDuration() != null && recipe.getDurationType() != null) {
                        tvDuration.setText("" + recipe.getDuration() + " " + recipe.getDurationType());
                    }
                    if (recipe.getComplexity() != null) {
                        tvComplexity.setText("Complexity: " + recipe.getComplexity());
                    }
                    if (recipe.getDescription() != null) {
                        String description = recipe.getDescription();
                        tvDescription.setText(description);
                    }
                    if (recipe.getImageUrls_list() != null) {
                        imageUrls = recipe.getImageUrls_list();
                    }
                    if (recipe.getCategory() != null) {
                        recipeCategory = recipe.getCategory();
                    }
                    if (recipe.getIngredient_list() != null) {
                        List<Ingredient> ingredientList = recipe.getIngredient_list();
                        for (Ingredient ingredient : ingredientList) {
                            if (!recipeIngredientList.contains(ingredient)) {
                                recipeIngredientList.add(ingredient);
                                ingredientsAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                    if (recipe.getInstruction_list() != null) {
                        List<Instruction> instructionList = recipe.getInstruction_list();
                        for (Instruction instruction : instructionList) {
                            if (!recipeInstructionList.contains(instruction)) {
                                recipeInstructionList.add(instruction);
                                instructionsAdapter.notifyDataSetChanged();
                            }
                        }
                    }

                    if (recipeDetailedViewPagerAdapter != null) {
                        recipeDetailedViewPagerAdapter.notifyDataSetChanged();
                    }
                    //Check if the user liked the recipe or not
                    recipeRef.document(documentID).collection("UsersWhoFaved").document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                                @Override
                                public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                                    if (e != null) {
                                        Log.w(TAG, "onEvent: ", e);
                                        return;
                                    }
                                    if (documentSnapshot != null) {
                                        UserWhoFaved userWhoFaved = documentSnapshot.toObject(UserWhoFaved.class);
                                        if (userWhoFaved != null && userWhoFaved.getUserID().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                                            isRecipeFavorite = true;
                                            setFavoriteIcon(true);
                                        } else {
                                            isRecipeFavorite = false;
                                            setFavoriteIcon(false);
                                        }
                                    }
                                }
                            });

                    //Setup nr of likes TextView
                    recipeRef.document(documentID).collection("UsersWhoFaved").orderBy("mFaveTimestamp", Query.Direction.DESCENDING).limit(1)
                            .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                @Override
                                public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                                    if (e != null) {
                                        Log.w(TAG, "onEvent: ", e);
                                        return;
                                    }
                                    if (queryDocumentSnapshots != null && queryDocumentSnapshots.size() > 0) {
                                        UserWhoFaved userWhoFaved = queryDocumentSnapshots.getDocuments().get(0).toObject(UserWhoFaved.class);
                                        if (userWhoFaved != null) {
                                            StringBuilder favText = new StringBuilder();

                                            if (recipe.getNumber_of_likes() == 1) {
                                                if (userWhoFaved.getUserID().equals(mUser.getUser_id())) {
                                                    favText.append("You like this");
                                                } else {
                                                    favText.append(userWhoFaved.getUser_name()).append(" likes this");
                                                }
                                            } else if (recipe.getNumber_of_likes() == 2) {
                                                if (userWhoFaved.getUserID().equals(mUser.getUser_id())) {
                                                    favText.append("You");
                                                } else {
                                                    favText.append(userWhoFaved.getUser_name());
                                                }
                                                favText.append(" and ").append(recipe.getNumber_of_likes() - 1).append(" other");
                                            } else if (recipe.getNumber_of_likes() > 2) {
                                                if (userWhoFaved.getUserID().equals(mUser.getUser_id())) {
                                                    favText.append("You");
                                                } else {
                                                    favText.append(userWhoFaved.getUser_name());
                                                }
                                                favText.append(" and ").append(recipe.getNumber_of_likes() - 1).append(" others");
                                            }
                                            mFavoriteNumber.setText(favText);
                                            mFavoriteNumber.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    Navigation.findNavController(view).navigate(RecipeDetailedFragmentDirections
                                                            .actionRecipeDetailedFragmentToUsersWhoLiked(documentID, "Recipes"));
                                                }
                                            });
                                        }
                                    } else if (queryDocumentSnapshots != null && queryDocumentSnapshots.size() == 0) {
                                        mFavoriteNumber.setText("0");
                                    } else
                                        Log.d(TAG, "onEvent: NULL");
                                }
                            });

                    ////////////////////////////////////////////////////////// LOGIC TO GET RECIPES INGREDIENTS AND INSTRUCTIONS HERE
                    if (recipe.getCreator_docId() != null && recipe.getCreator_docId().equals(mUser.getUser_id())) {
                        mDeleteRecipeBtn.setVisibility(View.VISIBLE);
                        mDeleteRecipeBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(getActivity()
                                        , R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog_Centered);
                                materialAlertDialogBuilder.setMessage("Are you sure you want to delete your recipe?");
                                materialAlertDialogBuilder.setCancelable(true);
                                materialAlertDialogBuilder.setPositiveButton(
                                        "Yes",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                recipeRef.document(documentID).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        Toast.makeText(getActivity(), "Successfully deleted", Toast.LENGTH_SHORT).show();
                                                        Navigation.findNavController(view).popBackStack();
                                                    }
                                                });
                                                dialog.cancel();
                                            }
                                        });
                                materialAlertDialogBuilder.setNegativeButton(
                                        "No",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.cancel();
                                            }
                                        });
                                materialAlertDialogBuilder.show();
                            }
                        });
                    }

                    if (recipe.getCreator_name() != null && recipe.getCreator_imageUrl() != null) {
                        tvCreatorName.setText(recipe.getCreator_name());
                        Glide.with(mCreatorImage).load(recipe.getCreator_imageUrl()).centerCrop().into(mCreatorImage);

                        tvCreatorName.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (recipe.getCreator_docId().equals(mUser.getUser_id())) {
                                    Navigation.findNavController(view).navigate(RecipeDetailedFragmentDirections.actionRecipeDetailedFragmentToProfileFragment());
                                } else {
                                    Navigation.findNavController(view).navigate(RecipeDetailedFragmentDirections
                                            .actionRecipeDetailedFragmentToUserProfileFragment2(recipe.getCreator_docId()));
                                }
                            }
                        });
                        mCreatorImage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (recipe.getCreator_docId().equals(mUser.getUser_id())) {
                                    Navigation.findNavController(view).navigate(RecipeDetailedFragmentDirections.actionRecipeDetailedFragmentToProfileFragment());
                                } else {
                                    Navigation.findNavController(view).navigate(RecipeDetailedFragmentDirections
                                            .actionRecipeDetailedFragmentToUserProfileFragment2(recipe.getCreator_docId()));
                                }
                            }
                        });
                    }

                    getMissingRecipeIngredients();

                    setupViewPager(view);
                }
            }
        });

        getCommentsFromDb();
    }


    private void getMissingRecipeIngredients() {

        nrOfMissingIngredients = 0;
        final List<Ingredient> recipeIngredientsToAddToShoppingList = new ArrayList<>();

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
                    }
                }

            } else {
                recipeIngredientsToAddToShoppingList.add(ing);
                nrOfMissingIngredients++;
            }
        }

        if (nrOfMissingIngredients > 0 && !userShoppingIngredientList.containsAll(recipeIngredientsToAddToShoppingList)) {
            tvMissingIngredientsNumber.setVisibility(View.VISIBLE);
            if (nrOfMissingIngredients == 1) {
                tvMissingIngredientsNumber.setText("Missing " + nrOfMissingIngredients + " ingredient");
            } else {
                tvMissingIngredientsNumber.setText("Missing " + nrOfMissingIngredients + " ingredients");
            }
            tvMissingIngredientsNumber.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                    alert.setTitle("Missing ingredients");
                    // Create TextView
                    final TextView textView = new TextView(getActivity());
                    StringBuilder missing = new StringBuilder();
                    for (Ingredient ingredient : recipeIngredientsToAddToShoppingList) {
                        missing.append("         ").append(ingredient.getName()).append("\n");
                    }
                    textView.setText(missing);
                    alert.setView(textView);

                    alert.show();
                }
            });
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
                    Toast.makeText(getActivity(), "Added " + nrOfMissingIngredients + " missing ingredients to your shopping list"
                            , Toast.LENGTH_SHORT).show();
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
                    if (nrOfMissingIngredients == 1) {
                        tvMissingIngredientsNumber.setText(nrOfMissingIngredients + " missing ingredient is in your shopping list");
                    } else {
                        tvMissingIngredientsNumber.setText(nrOfMissingIngredients + " missing ingredients are in your shopping list");
                    }
                }
            }
        }
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

        Glide.with(getActivity()).load(imageUrl).centerCrop().into(expandedImageView);
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

    @Override
    public void onItemImageClick(int position) {
        zoomImageFromThumb(instructionsRecyclewView.getChildAt(position), recipeInstructionList.get(position).getImgUrl());
    }

    @Override
    public void onItemClick(int position) {

    }

    @Override
    public void onUserClick(int position) {
        Comment comment = commentList.get(position);
        Bundle args = new Bundle();
        args.putString("documentID", comment.getUser_id());
        Navigation.findNavController(view).navigate(R.id.userProfileFragment2, args);
    }

    @Override
    public void onEditClick(int position) {
        final Comment comment = commentList.get(position);

        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(getActivity(), R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog_Centered);
        materialAlertDialogBuilder.setMessage("Are you sure you want to delete this comment?");
        materialAlertDialogBuilder.setCancelable(true);
        materialAlertDialogBuilder.setPositiveButton(
                "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //Delete user from friends
                        db.collection("Recipes").document(comment.getRecipe_documentID()).collection("Comments").document(comment.getDocumentID())
                                .delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                commentList.remove(comment);
                                commentAdapter.notifyDataSetChanged();
                                Toast.makeText(getContext(), "Deleted comment", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "onSuccess: Deleted comm");
                            }
                        });
                        dialog.cancel();
                    }
                });

        materialAlertDialogBuilder.setNegativeButton(
                "No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        materialAlertDialogBuilder.show();
    }
}
