package com.rokudoz.irecipe.Fragments;


import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
import com.rokudoz.irecipe.Models.Comment;
import com.rokudoz.irecipe.Models.FavoritePost;
import com.rokudoz.irecipe.Models.Post;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.Models.UserWhoFaved;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.PostParentCommentAdapter;
import com.rokudoz.irecipe.Utils.RecipeParentCommentAdapter;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostDetailedFragment extends Fragment {
    private static final String TAG = "PostDetailedFragment";

    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;


    private ImageView creatorImage, postImage, recipeImage, postFavoriteIcon;
    private TextView creatorName, postDescription, postCreationDate, recipeTitle, postFavoriteNumber;
    private MaterialButton deletePost, addCommentBtn;
    private EditText commentEditText;
    private RecyclerView commentRecyclerView;
    private MaterialCardView recipeCardView;

    private String documentID = "";
    private List<String> userFavPostList = new ArrayList<>();
    private View view;

    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ArrayList<Comment> commentList = new ArrayList<>();
    private ArrayList<String> newItemsToAdd = new ArrayList<>();

    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration currentSubCollectionListener, userRefListener, numberofFavListener, commentListener;
    private DocumentSnapshot mLastQueriedDocument;

    private CollectionReference recipeRef = db.collection("Recipes");
    private CollectionReference usersRef = db.collection("Users");
    private CollectionReference postsRef = db.collection("Posts");


    public PostDetailedFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_post_detailed, container, false);

        creatorImage = view.findViewById(R.id.postDetailed_creatorImage_ImageView);
        postImage = view.findViewById(R.id.postDetailed_ImageView);
        recipeImage = view.findViewById(R.id.postDetailed_recipeImage);
        creatorName = view.findViewById(R.id.postDetailed_creatorName_TextView);
        postDescription = view.findViewById(R.id.postDetailed_tvDescription);
        postCreationDate = view.findViewById(R.id.postDetailed_creationDate_text_view);
        recipeTitle = view.findViewById(R.id.postDetailed_recipe_name_TextView);
        postFavoriteIcon = view.findViewById(R.id.postDetailed_imageView_favorite_icon);
        postFavoriteNumber = view.findViewById(R.id.postDetailed_numberOfFaved);
        deletePost = view.findViewById(R.id.postDetailed_deleteRecipe_MaterialBtn);
        addCommentBtn = view.findViewById(R.id.postDetailed_addComment_btn);
        commentEditText = view.findViewById(R.id.postDetailed_et_commentInput);
        commentRecyclerView = view.findViewById(R.id.postDetailed_comment_recycler_view);
        recipeCardView = view.findViewById(R.id.postDetailed_recipe_cardView);

        PostDetailedFragmentArgs postDetailedFragmentArgs = PostDetailedFragmentArgs.fromBundle(getArguments());
        documentID = postDetailedFragmentArgs.getDocumentID();


        DocumentReference currentRecipeRef = postsRef.document(documentID);
        final CollectionReference currentRecipeSubCollection = currentRecipeRef.collection("UsersWhoFaved");


        numberofFavListener = currentRecipeSubCollection.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    return;
                }
                int numberOfFav = queryDocumentSnapshots.size();
                postFavoriteNumber.setText(Integer.toString(numberOfFav));
            }
        });

        addCommentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (commentEditText.getText().toString().trim().equals("")) {
                    Toast.makeText(getActivity(), "Comment cannot be empty", Toast.LENGTH_SHORT).show();
                } else {
                    addComment();
                }
            }
        });

        buildRecyclerView();
        getCommentsFromDb();
        getPostDetails();

        ///////////
        return view;
    }

    private void buildRecyclerView() {
        commentRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getContext());
        mAdapter = new PostParentCommentAdapter(getContext(), commentList);
        commentRecyclerView.setLayoutManager(mLayoutManager);
        commentRecyclerView.setAdapter(mAdapter);
    }


    private void addComment() {
        String commentText = commentEditText.getText().toString();


        final Comment comment = new Comment(documentID, FirebaseAuth.getInstance().getCurrentUser().getUid(),
                "", "", commentText, null);
        DocumentReference currentRecipeRef = postsRef.document(documentID);
        CollectionReference commentRef = currentRecipeRef.collection("Comments");

        commentRef.add(comment)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        commentEditText.setText("");
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

    private void getCommentsFromDb() {

        DocumentReference currentRecipeRef = postsRef.document(documentID);
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
                                final Comment comment = document.toObject(Comment.class);
                                if (!newItemsToAdd.contains(document.getId())) {

                                    Log.d(TAG, "onEvent: doc id" + document.getId());

                                    comment.setDocumentId(document.getId());
                                    commentList.add(0, comment);
                                    Log.d(TAG, "onEvent: currrent commentID " + document.getId());
                                    newItemsToAdd.add(document.getId());
                                    mAdapter.notifyDataSetChanged();

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

    private void getPostDetails() {
        postsRef.document(documentID).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                final Post post = documentSnapshot.toObject(Post.class);
                post.setDocumentId(documentSnapshot.getId());

                Date date = post.getCreation_date();
                if (date != null) {
                    DateFormat dateFormat = new SimpleDateFormat("HH:mm, MMM d", Locale.getDefault());
                    String creationDate = dateFormat.format(date);
                    long time = date.getTime();
                    if (post.getCreation_date() != null && !post.getCreation_date().equals("")) {
                        postCreationDate.setText(getTimeAgo(time));
                    }
                }

                postDescription.setText(post.getText());

                if (post.getImageUrl() != null) {
                    Picasso.get()
                            .load(post.getImageUrl())
                            .fit()
                            .centerCrop()
                            .into(postImage);
                }

                usersRef.document(post.getCreatorId()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        final User user = documentSnapshot.toObject(User.class);
                        creatorName.setText(user.getName());
                        Picasso.get()
                                .load(user.getUserProfilePicUrl())
                                .fit()
                                .centerCrop()
                                .into(creatorImage);
                        creatorImage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Navigation.findNavController(view).navigate(PostDetailedFragmentDirections.actionPostDetailedToUserProfileFragment2(user.getUser_id()));
                            }
                        });
                        final String loggedInUserDocId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        usersRef.document(loggedInUserDocId).collection("FavoritePosts").get()
                                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                    @Override
                                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                        if (queryDocumentSnapshots != null) {
                                            for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                                String favPostID = documentSnapshot.getId();
                                                if (!userFavPostList.contains(favPostID))
                                                    userFavPostList.add(favPostID);
                                            }
                                            if (userFavPostList.contains(post.getDocumentId())) {
                                                post.setFavorite(true);
                                                postFavoriteIcon.setImageResource(R.drawable.ic_favorite_red_24dp);
                                            } else {
                                                Log.d(TAG, "onSuccess: NOT COINAIN");
                                                post.setFavorite(false);
                                                postFavoriteIcon.setImageResource(R.drawable.ic_favorite_border_black_24dp);
                                            }
                                            Log.d(TAG, "onSuccess: userfav : " + userFavPostList + " PSOT ID : " + post.getDocumentId());

                                            postFavoriteIcon.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    ////////////////////////////////////////////////

                                                    String id = documentID;
                                                    DocumentReference currentRecipeRef = postsRef.document(id);
                                                    final CollectionReference currentRecipeSubCollection = currentRecipeRef.collection("UsersWhoFaved");

                                                    DocumentReference currentUserRef = usersRef.document(loggedInUserDocId);


                                                    if (userFavPostList == null) {
                                                        userFavPostList = new ArrayList<>();
                                                    }
                                                    if (userFavPostList.contains(id)) {
                                                        currentUserRef.collection("FavoritePosts").document(id).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                Log.d(TAG, "onSuccess: Deleted from user FAV posts");
                                                            }
                                                        });
                                                        userFavPostList.remove(id);
                                                        post.setFavorite(false);
                                                        postFavoriteIcon.setImageResource(R.drawable.ic_favorite_border_black_24dp);

                                                        currentRecipeSubCollection.document(loggedInUserDocId).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                Log.d(TAG, "onSuccess: deleted user from post users who faved");
                                                            }
                                                        });

                                                    } else {
                                                        userFavPostList.add(id);
                                                        post.setFavorite(true);
                                                        postFavoriteIcon.setImageResource(R.drawable.ic_favorite_red_24dp);
                                                        UserWhoFaved userWhoFaved = new UserWhoFaved(loggedInUserDocId, null);
                                                        currentRecipeSubCollection.document(loggedInUserDocId).set(userWhoFaved);

                                                        FavoritePost favoritePost = new FavoritePost(null);
                                                        currentUserRef.collection("FavoritePosts").document(id).set(favoritePost);
                                                        Log.d(TAG, "onClick: added to favorites");
                                                    }
                                                }
                                            });
                                        }
                                    }
                                });
                    }
                });
                recipeRef.document(post.getReferenced_recipe_docId()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Recipe recipe = documentSnapshot.toObject(Recipe.class);
                        recipeTitle.setText(recipe.getTitle());
                        Picasso.get()
                                .load(recipe.getImageUrls_list().get(0))
                                .fit()
                                .centerCrop()
                                .into(recipeImage);
                    }
                });

                recipeCardView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Navigation.findNavController(view).navigate(PostDetailedFragmentDirections.actionPostDetailedToRecipeDetailedFragment(post.getReferenced_recipe_docId()));
                    }
                });

                if (FirebaseAuth.getInstance().getCurrentUser().getUid().equals(post.getCreatorId())) {
                    deletePost.setVisibility(View.VISIBLE);
                    deletePost.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(getActivity(), R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog_Centered);
                            materialAlertDialogBuilder.setMessage("Are you sure you want to delete your post?");
                            materialAlertDialogBuilder.setCancelable(true);
                            materialAlertDialogBuilder.setPositiveButton(
                                    "Yes",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {

                                            postsRef.document(documentID).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Navigation.findNavController(view).navigate(PostDetailedFragmentDirections.actionPostDetailedToFeedFragment());
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
                } else {
                    deletePost.setVisibility(View.GONE);
                }


            }
        });
    }


    // Convert Creation date long into "time ago"
    private static String getTimeAgo(long time) {
        if (time < 1000000000000L) {
            // if timestamp given in seconds, convert to millis
            time *= 1000;
        }

        long now = System.currentTimeMillis();
        if (time > now || time <= 0) {
            return null;
        }

        // TODO: localize
        final long diff = now - time;
        if (diff < MINUTE_MILLIS) {
            return "just now";
        } else if (diff < 2 * MINUTE_MILLIS) {
            return "a minute ago";
        } else if (diff < 50 * MINUTE_MILLIS) {
            return diff / MINUTE_MILLIS + " minutes ago";
        } else if (diff < 90 * MINUTE_MILLIS) {
            return "an hour ago";
        } else if (diff < 24 * HOUR_MILLIS) {
            return diff / HOUR_MILLIS + " hours ago";
        } else if (diff < 48 * HOUR_MILLIS) {
            return "yesterday";
        } else {
            return diff / DAY_MILLIS + " days ago";
        }
    }

}
