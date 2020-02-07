package com.rokudoz.irecipe.Fragments;


import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
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
import com.google.firebase.firestore.WriteBatch;
import com.rokudoz.irecipe.EditPostActivity;
import com.rokudoz.irecipe.Models.Comment;
import com.rokudoz.irecipe.Models.FavoritePost;
import com.rokudoz.irecipe.Models.Post;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.Models.UserWhoFaved;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.Adapters.PostParentCommentAdapter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostDetailedFragment extends Fragment {
    private static final String TAG = "PostDetailedFragment";

    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;


    private ImageView postImage, recipeImage, postFavoriteIcon;
    private CircleImageView creatorImage;
    private TextView creatorName, postDescription, postCreationDate, recipeTitle, postFavoriteNumber;
    private MaterialButton editPostBtn, addCommentBtn;
    private EditText commentEditText;
    private RecyclerView commentRecyclerView;
    private RelativeLayout recipeLayout;

    private String documentID = "";
    private View view;

    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ArrayList<Comment> commentList = new ArrayList<>();

    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration numberofFavListener, commentListener;
    private DocumentSnapshot mLastQueriedDocument;

    private CollectionReference recipeRef = db.collection("Recipes");
    private CollectionReference usersRef = db.collection("Users");
    private CollectionReference postsRef = db.collection("Posts");

    private User mUser = new User();


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
        editPostBtn = view.findViewById(R.id.postDetailed_editPost_MaterialBtn);
        addCommentBtn = view.findViewById(R.id.postDetailed_addComment_btn);
        commentEditText = view.findViewById(R.id.postDetailed_et_commentInput);
        commentRecyclerView = view.findViewById(R.id.postDetailed_comment_recycler_view);
        recipeLayout = view.findViewById(R.id.postDetailed_recipeLayout);

        PostDetailedFragmentArgs postDetailedFragmentArgs = PostDetailedFragmentArgs.fromBundle(getArguments());
        documentID = postDetailedFragmentArgs.getDocumentID();


        DocumentReference currentRecipeRef = postsRef.document(documentID);
        final CollectionReference currentRecipeSubCollection = currentRecipeRef.collection("UsersWhoFaved");

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

        usersRef.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e == null && documentSnapshot != null) {
                    mUser = documentSnapshot.toObject(User.class);
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

    @Override
    public void onStop() {
        super.onStop();
        DetachFireStoreListeners();
    }

    private void DetachFireStoreListeners() {
        if (numberofFavListener != null) {
            numberofFavListener.remove();
            numberofFavListener = null;
        }
        if (commentListener != null) {
            commentListener.remove();
            commentListener = null;
        }
    }

    private void addComment() {
        String commentText = commentEditText.getText().toString();

        final Comment comment = new Comment(documentID, FirebaseAuth.getInstance().getCurrentUser().getUid(), mUser.getName()
                , mUser.getUserProfilePicUrl(), commentText, null);
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
                                comment.setDocumentID(document.getId());
                                if (!commentList.contains(comment)) {
                                    commentList.add(0, comment);
                                    mAdapter.notifyDataSetChanged();

                                }

                            }
                            if (queryDocumentSnapshots.getDocuments().size() != 0) {
                                mLastQueriedDocument = queryDocumentSnapshots.getDocuments()
                                        .get(queryDocumentSnapshots.getDocuments().size() - 1);
                            }
                        }
                        mAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void getPostDetails() {
        final String currentUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        postsRef.document(documentID).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "onEvent: ", e);
                    return;
                }

                if (documentSnapshot != null) {
                    final Post post = documentSnapshot.toObject(Post.class);
                    if (post != null)
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

                    if (post.getText() != null)
                        postDescription.setText(post.getText());

                    if (post.getImageUrl() != null) {
                        Glide.with(postImage).load(post.getImageUrl()).centerCrop().into(postImage);
                    }

                    if (post.getCreator_name() != null)
                        creatorName.setText(post.getCreator_name());
                    if (post.getCreator_imageUrl() != null && !post.getCreator_imageUrl().equals("")) {
                        Glide.with(creatorImage).load(post.getCreator_imageUrl()).centerCrop().into(creatorImage);
                        creatorImage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Navigation.findNavController(view).navigate(PostDetailedFragmentDirections
                                        .actionPostDetailedToUserProfileFragment2(post.getCreatorId()));
                            }
                        });
                    }
                    //Check if current user liked the post or not
                    postsRef.document(post.getDocumentId()).collection("UsersWhoFaved").document(mUser.getUser_id())
                            .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                                @Override
                                public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                                    if (e != null) {
                                        Log.w(TAG, "onEvent: ", e);
                                        return;
                                    }
                                    if (documentSnapshot != null) {
                                        UserWhoFaved userWhoFaved = documentSnapshot.toObject(UserWhoFaved.class);
                                        if (userWhoFaved != null && userWhoFaved.getUserID().equals(mUser.getUser_id())) {
                                            post.setFavorite(true);
                                            postFavoriteIcon.setImageResource(R.drawable.ic_favorite_red_24dp);
                                            mAdapter.notifyDataSetChanged();
                                        } else {
                                            post.setFavorite(false);
                                            postFavoriteIcon.setImageResource(R.drawable.ic_favorite_border_black_24dp);
                                            mAdapter.notifyDataSetChanged();
                                        }
                                    } else {
                                        Log.d(TAG, "onEvent: NULL");
                                    }
                                }
                            });
                    //Set nr of likes TextView
                    postsRef.document(post.getDocumentId()).collection("UsersWhoFaved").orderBy("mFaveTimestamp", Query.Direction.DESCENDING)
                            .limit(1).addSnapshotListener(new EventListener<QuerySnapshot>() {
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

                                    if (post.getNumber_of_likes() == 1) {
                                        if (userWhoFaved.getUserID().equals(mUser.getUser_id())) {
                                            favText.append("You like this");
                                        } else {
                                            favText.append(userWhoFaved.getUser_name()).append(" likes this");
                                        }
                                    } else if (post.getNumber_of_likes() == 2) {
                                        if (userWhoFaved.getUserID().equals(mUser.getUser_id())) {
                                            favText.append("You");
                                        } else {
                                            favText.append(userWhoFaved.getUser_name());
                                        }
                                        favText.append(" and ").append(post.getNumber_of_likes() - 1).append(" other");
                                    } else if (post.getNumber_of_likes() > 2) {
                                        if (userWhoFaved.getUserID().equals(mUser.getUser_id())) {
                                            favText.append("You");
                                        } else {
                                            favText.append(userWhoFaved.getUser_name());
                                        }
                                        favText.append(" and ").append(post.getNumber_of_likes() - 1).append(" others");
                                    }
                                    postFavoriteNumber.setText(favText);
                                    postFavoriteNumber.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Navigation.findNavController(view).navigate(PostDetailedFragmentDirections
                                                    .actionPostDetailedToUsersWhoLiked(documentID, "Posts"));
                                        }
                                    });
                                }
                            } else if (queryDocumentSnapshots != null && queryDocumentSnapshots.size() == 0) {
                                postFavoriteNumber.setText("0");
                            } else
                                Log.d(TAG, "onEvent: NULL");
                        }
                    });

                    postFavoriteIcon.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ////////////////////////////////////////////////
                            String id = documentID;
                            DocumentReference currentRecipeRef = postsRef.document(id);
                            final CollectionReference currentRecipeSubCollection = currentRecipeRef.collection("UsersWhoFaved");

                            DocumentReference currentUserRef = usersRef.document(currentUserID);


                            if (post.getFavorite()) {
                                currentUserRef.collection("FavoritePosts").document(id).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d(TAG, "onSuccess: Deleted from user FAV posts");
                                    }
                                });
                                post.setFavorite(false);
                                postFavoriteIcon.setImageResource(R.drawable.ic_favorite_border_black_24dp);

                                currentRecipeSubCollection.document(currentUserID).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d(TAG, "onSuccess: deleted user from post users who faved");
                                    }
                                });
                            } else {
                                post.setFavorite(true);
                                postFavoriteIcon.setImageResource(R.drawable.ic_favorite_red_24dp);
                                UserWhoFaved userWhoFaved = new UserWhoFaved(currentUserID, mUser.getName(), mUser.getUserProfilePicUrl(), null);
                                FavoritePost favoritePost = new FavoritePost(null);
                                WriteBatch batch = db.batch();
                                batch.set(currentRecipeSubCollection.document(currentUserID), userWhoFaved);
                                batch.set(currentUserRef.collection("FavoritePosts").document(id), favoritePost);
                                batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d(TAG, "onFavoriteClick: Added to favorites");
                                    }
                                });
                            }
                        }
                    });

                    recipeRef.document(post.getReferenced_recipe_docId()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            Recipe recipe = documentSnapshot.toObject(Recipe.class);
                            recipeTitle.setText(recipe.getTitle());
                            Glide.with(recipeImage).load(recipe.getImageUrls_list().get(0)).centerCrop().into(recipeImage);
                        }
                    });

                    recipeLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Navigation.findNavController(view).navigate(PostDetailedFragmentDirections
                                    .actionPostDetailedToRecipeDetailedFragment(post.getReferenced_recipe_docId()));
                        }
                    });

                    if (FirebaseAuth.getInstance().getCurrentUser().getUid().equals(post.getCreatorId())) {
                        editPostBtn.setVisibility(View.VISIBLE);
                        editPostBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(getActivity(), EditPostActivity.class);
                                intent.putExtra("post_id", documentID);
                                intent.putExtra("post_imageUrl", post.getImageUrl());
                                intent.putExtra("post_text", post.getText());
                                intent.putExtra("post_privacy", post.getPrivacy());
                                intent.putExtra("post_referencedRecipe", post.getReferenced_recipe_docId());
                                startActivity(intent);
                            }
                        });
                    } else {
                        editPostBtn.setVisibility(View.GONE);
                    }
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
