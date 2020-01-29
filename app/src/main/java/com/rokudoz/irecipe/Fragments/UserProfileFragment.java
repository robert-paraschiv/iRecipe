package com.rokudoz.irecipe.Fragments;


import android.content.DialogInterface;
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

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
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
import com.google.firebase.firestore.WriteBatch;
import com.rokudoz.irecipe.Models.FavoritePost;
import com.rokudoz.irecipe.Models.Friend;
import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.Models.Post;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.Models.UserWhoFaved;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.Adapters.PostAdapter;
import com.rokudoz.irecipe.Utils.Adapters.RecipeAdapter;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserProfileFragment extends Fragment implements PostAdapter.OnItemClickListener {
    private static final String TAG = "UserProfileFragment";

    private String documentID;
    private String userProfilePicUrl = "";
    private User mUser;
    private TextView UserNameTv, UserUsernameTv, UserDescriptionTv;
    private MaterialButton mAddFriendButton;
    private CircleImageView mProfileImage;
    private View view;
    private ArrayList<Post> mPostList = new ArrayList<>();
    private List<String> userFavPostList = new ArrayList<>();
    private List<String> friends_userID_list = new ArrayList<>();
    private List<Friend> friendList = new ArrayList<>();
    private String loggedInUserDocumentId = "";
    private String userFavDocId = "";

    private DocumentSnapshot mLastQueriedDocument;
    private RecyclerView mRecyclerView;
    private PostAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    //Firebase
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference usersReference = db.collection("Users");
    private CollectionReference postsRef = db.collection("Posts");
    private ListenerRegistration userDetailsListener, userFriendListListener, userFavoritePostsListener, userFriendListener, postsListener,
            postCreatorDetailsListener, postCommentsNumberListener, postLikesNumberListener;

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
        mAddFriendButton = view.findViewById(R.id.userprofile_addFriend_MaterialButton);
        MaterialButton messageUser = view.findViewById(R.id.userprofile_messageUser_MaterialButton);

        mUser = new User();

        BottomNavigationView navBar = getActivity().findViewById(R.id.bottom_navigation);
        navBar.setVisibility(View.VISIBLE);

        UserProfileFragmentArgs userProfileFragmentArgs = UserProfileFragmentArgs.fromBundle(getArguments());
        getRecipeArgsPassed(userProfileFragmentArgs);

        if (documentID.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            messageUser.setVisibility(View.GONE);
        } else {
            messageUser.setVisibility(View.VISIBLE);
            messageUser.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Navigation.findNavController(view).navigate(UserProfileFragmentDirections.actionUserProfileFragment2ToMessageFragment(documentID));
                }
            });
        }


        buildRecyclerView();

        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        DetachFirestoneListeners();
    }

    private void DetachFirestoneListeners() {
        if (postsListener != null) {
            postsListener.remove();
            postsListener = null;
        }
        if (userDetailsListener != null) {
            userDetailsListener.remove();
            userDetailsListener = null;
        }
        if (userFavoritePostsListener != null) {
            userFavoritePostsListener.remove();
            userFavoritePostsListener = null;
        }
        if (userFriendListListener != null) {
            userFriendListListener.remove();
            userFriendListListener = null;
        }
        if (userFriendListener != null) {
            userFriendListener.remove();
            userFriendListener = null;
        }
        if (postCreatorDetailsListener != null) {
            postCreatorDetailsListener.remove();
            postCreatorDetailsListener = null;
        }
        if (postCommentsNumberListener != null) {
            postCommentsNumberListener.remove();
            postCommentsNumberListener = null;
        }
        if (postLikesNumberListener != null) {
            postLikesNumberListener.remove();
            postLikesNumberListener = null;
        }
    }

    private void getCurrentUserDetails() {

        userDetailsListener = usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "onEvent: ", e);
                    return;
                }
                List<Ingredient> userIngredient_list = new ArrayList<>();
                final User user = documentSnapshot.toObject(User.class);

                mUser = documentSnapshot.toObject(User.class);
                loggedInUserDocumentId = documentSnapshot.getId();
                userFavPostList = new ArrayList<>();

                if (!friends_userID_list.contains(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                    friends_userID_list.add(FirebaseAuth.getInstance().getCurrentUser().getUid());
                }

                userFriendListListener = usersReference.document(user.getUser_id()).collection("FriendList").addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "onEvent: ", e);
                            return;
                        }
                        for (QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots) {
                            Friend friend = queryDocumentSnapshot.toObject(Friend.class);
                            if (!friendList.contains(friend)) {
                                if (friend.getFriend_status().equals("friends") || friend.getFriend_status().equals("friend_request_accepted"))
                                    friendList.add(friend);
                            }
                        }
                        for (Friend friend : friendList) {
                            if (!friends_userID_list.contains(friend.getFriend_user_id()))
                                friends_userID_list.add(friend.getFriend_user_id());
                        }
                        userFavoritePostsListener = usersReference.document(user.getUser_id()).collection("FavoritePosts").addSnapshotListener(new EventListener<QuerySnapshot>() {
                            @Override
                            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                                if (e != null) {
                                    Log.w(TAG, "onEvent: ", e);
                                    return;
                                }
                                if (queryDocumentSnapshots != null) {
                                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                        String favPostID = documentSnapshot.getId();
                                        if (!userFavPostList.contains(favPostID))
                                            userFavPostList.add(favPostID);
                                    }

                                    performQuery();
                                }

                            }
                        });

                    }
                });


            }
        });

        // ADD TO FRIEND LIST
        if (!documentID.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            userFriendListener = usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("FriendList").document(documentID).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                    if (e != null) {
                        Log.w(TAG, "onEvent: ", e);
                        return;
                    }
                    final Friend friend = documentSnapshot.toObject(Friend.class);
                    if (friend != null) {
                        if (friend.getFriend_status().equals("friends") || friend.getFriend_status().equals("friend_request_accepted")) {
                            Log.d(TAG, "onSuccess: WE FRIENDS ALREADY");
                            mAddFriendButton.setText("Unfriend");
                            mAddFriendButton.setVisibility(View.VISIBLE);
                            mAddFriendButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                    MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(getActivity(), R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog_Centered);
                                    materialAlertDialogBuilder.setMessage("Are you sure you want to remove this user from your friend list?");
                                    materialAlertDialogBuilder.setCancelable(true);
                                    materialAlertDialogBuilder.setPositiveButton(
                                            "Yes",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("FriendList").document(documentID)
                                                            .delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            usersReference.document(documentID).collection("FriendList")
                                                                    .document(FirebaseAuth.getInstance().getCurrentUser().getUid()).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                @Override
                                                                public void onSuccess(Void aVoid) {
                                                                    if (getActivity() != null)
                                                                        Toast.makeText(getActivity(), "Removed from friend list", Toast.LENGTH_SHORT).show();
                                                                    mAddFriendButton.setText("Add Friend");
                                                                    mAddFriendButton.setEnabled(true);
                                                                }
                                                            });
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
                        } else if (friend.getFriend_status().equals("friend_request_sent")) {
                            mAddFriendButton.setText("Cancel Friend request");
                            mAddFriendButton.setVisibility(View.VISIBLE);
                            mAddFriendButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("FriendList").document(documentID)
                                            .delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            usersReference.document(documentID).collection("FriendList")
                                                    .document(FirebaseAuth.getInstance().getCurrentUser().getUid()).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Toast.makeText(getActivity(), "Cancelled friend request", Toast.LENGTH_SHORT).show();
                                                    mAddFriendButton.setText("Add Friend");
                                                    mAddFriendButton.setEnabled(true);
                                                }
                                            });
                                        }
                                    });
                                }
                            });
                        } else if (friend.getFriend_status().equals("friend_request_received")) {
                            mAddFriendButton.setText("Accept Friend request");
                            mAddFriendButton.setVisibility(View.VISIBLE);
                            mAddFriendButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Friend friendForCurrentUser = new Friend(documentID, "friends", null);
                                    usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("FriendList").document(documentID)
                                            .set(friendForCurrentUser).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Friend friendForOtherUser = new Friend(FirebaseAuth.getInstance().getCurrentUser().getUid(), "friend_request_accepted", null);
                                            usersReference.document(documentID).collection("FriendList")
                                                    .document(FirebaseAuth.getInstance().getCurrentUser().getUid()).set(friendForOtherUser).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Toast.makeText(getActivity(), "Accepted friend request", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                    });
                                }
                            });
                        }
                        Log.d(TAG, "onSuccess: FOUND IN FRIEND LIST " + friend.toString());
                    } else {
                        mAddFriendButton.setText("Add friend");
                        mAddFriendButton.setVisibility(View.VISIBLE);
                        mAddFriendButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Friend friendForCurrentUser = new Friend(documentID, "friend_request_sent", null);
                                usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("FriendList").document(documentID)
                                        .set(friendForCurrentUser).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Friend friendForOtherUser = new Friend(FirebaseAuth.getInstance().getCurrentUser().getUid(), "friend_request_received", null);
                                        usersReference.document(documentID).collection("FriendList")
                                                .document(FirebaseAuth.getInstance().getCurrentUser().getUid()).set(friendForOtherUser).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Toast.makeText(getActivity(), "Sent friend request", Toast.LENGTH_SHORT).show();
                                                mAddFriendButton.setText("Cancel Friend request");
                                            }
                                        });
                                    }
                                });
                            }
                        });

                    }

                }
            });

        } else {
            mAddFriendButton.setVisibility(View.GONE);
        }

    }

    private void getRecipeArgsPassed(UserProfileFragmentArgs userProfileFragmentArgs) {
        documentID = userProfileFragmentArgs.getDocumentID();
        getCurrentUserDetails();
        getCreatorInfo();
    }

    private void getCreatorInfo() {
        usersReference.document(documentID).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                User user = documentSnapshot.toObject(User.class);
                userProfilePicUrl = user.getUserProfilePicUrl();

                UserNameTv.setText(user.getName());
                UserUsernameTv.setText(user.getUsername());
                UserDescriptionTv.setText(user.getDescription());

                if (userProfilePicUrl != null && !userProfilePicUrl.equals("")) {
                    Glide.with(mProfileImage).load(userProfilePicUrl).centerCrop().into(mProfileImage);
                } else {
                    Glide.with(mProfileImage).load(R.drawable.ic_account_circle_black_24dp).centerCrop().into(mProfileImage);

                    Toast.makeText(getContext(), "empty", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void buildRecyclerView() {
        Log.d(TAG, "buildRecyclerView: ");
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getContext());

        mAdapter = new PostAdapter(mPostList);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(UserProfileFragment.this);
    }

    private void performQuery() {
        Query postsQuery = null;
        if (mLastQueriedDocument != null) {
            postsQuery = postsRef.whereEqualTo("creatorId", documentID).whereEqualTo("privacy", "Everyone")
                    .startAfter(mLastQueriedDocument); // Necessary so we don't have the same results multiple times
//                                    .limit(3);
        } else {
            postsQuery = postsRef.whereEqualTo("creatorId", documentID).whereEqualTo("privacy", "Everyone");
//                                    .limit(3);
        }
        PerformMainQuery(postsQuery);
        initializeRecyclerViewAdapterOnClicks();
    }

    private void PerformMainQuery(Query postsQuery) {

        postsListener = postsQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots,
                                @javax.annotation.Nullable FirebaseFirestoreException e) {
                if (queryDocumentSnapshots != null) {
                    for (final QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        final Post post = document.toObject(Post.class);
                        post.setDocumentId(document.getId());

                        postsRef.document(post.getDocumentId()).collection("UsersWhoFaved").addSnapshotListener(new EventListener<QuerySnapshot>() {
                            @Override
                            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                                if (e != null) {
                                    Log.w(TAG, "onEvent: ", e);
                                    return;
                                }
                                Boolean fav = false;
                                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                    if (documentSnapshot.getId().equals(mUser.getUser_id())) {
                                        fav = true;
                                    }
                                }
                                post.setFavorite(fav);
                            }
                        });
                        //Get post creator details
                        usersReference.document(post.getCreatorId()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                            @Override
                            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                                if (e != null) {
                                    Log.w(TAG, "onEvent: ", e);
                                    return;
                                }
                                User user = documentSnapshot.toObject(User.class);
                                post.setCreator_name(user.getName());
                                post.setCreator_imageUrl(user.getUserProfilePicUrl());
                                mAdapter.notifyDataSetChanged();
                            }
                        });
                        //Get post comments number
                        postsRef.document(post.getDocumentId()).collection("Comments").addSnapshotListener(new EventListener<QuerySnapshot>() {
                            @Override
                            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                                if (e != null) {
                                    Log.w(TAG, "onEvent: ", e);
                                    return;
                                }
                                post.setNumber_of_comments(queryDocumentSnapshots.size());
                                mAdapter.notifyDataSetChanged();
                            }
                        });
                        //Get post likes number
                        postsRef.document(post.getDocumentId()).collection("UsersWhoFaved").addSnapshotListener(new EventListener<QuerySnapshot>() {
                            @Override
                            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                                if (e != null) {
                                    Log.w(TAG, "onEvent: ", e);
                                    return;
                                }
                                post.setNumber_of_likes(queryDocumentSnapshots.size());
                                mAdapter.notifyDataSetChanged();
                            }
                        });

                        if (!mPostList.contains(post)) {
                            mPostList.add(post);
                            mAdapter.notifyDataSetChanged();
                        } else {
                            mPostList.set(mPostList.indexOf(post), post);
                        }
                    }

                    if (queryDocumentSnapshots.getDocuments().size() != 0) {
                        mLastQueriedDocument = queryDocumentSnapshots.getDocuments()
                                .get(queryDocumentSnapshots.getDocuments().size() - 1);
                    }
                } else {
                    Log.d(TAG, "onEvent: Querry result is null");
                }
                if (mPostList.isEmpty()) {
                    mPostList.add(new Post("", "", "Add friends to see posts just like this one", ""
                            , false, "Everyone", null));
                    Log.d(TAG, "EMPTY: ");
                }
                mAdapter.notifyDataSetChanged();

            }
        });
    }

    private void initializeRecyclerViewAdapterOnClicks() {
        mAdapter.setOnItemClickListener(new PostAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                String id = mPostList.get(position).getDocumentId();
                Navigation.findNavController(view).navigate(UserProfileFragmentDirections.actionUserProfileFragment2ToPostDetailed(id));
            }

            @Override
            public void onFavoriteClick(final int position) {

                ////////////////////////////////////////////////
                final String id = mPostList.get(position).getDocumentId();
                DocumentReference currentRecipeRef = postsRef.document(id);
                final CollectionReference currentRecipeSubCollection = currentRecipeRef.collection("UsersWhoFaved");

                DocumentReference currentUserRef = usersReference.document(loggedInUserDocumentId);

                Log.d(TAG, "onFavoriteClick: " + mPostList.get(position).getDocumentId());

                if (mPostList.get(position).getFavorite()) {
                    WriteBatch batch = db.batch();
                    batch.delete(currentUserRef.collection("FavoritePosts").document(id));
                    batch.delete(currentRecipeSubCollection.document(mUser.getUser_id()));
                    batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "onSuccess: like deleted from db");
                        }
                    });
                    userFavPostList.remove(id);
                    mPostList.get(position).setFavorite(false);
                    mAdapter.notifyDataSetChanged();

                } else {
                    mPostList.get(position).setFavorite(true);
                    UserWhoFaved userWhoFaved = new UserWhoFaved(mUser.getUser_id(), null);
                    FavoritePost favoritePost = new FavoritePost(null);
                    WriteBatch batch = db.batch();
                    batch.set(currentRecipeSubCollection.document(mUser.getUser_id()), userWhoFaved);
                    batch.set(currentUserRef.collection("FavoritePosts").document(id), favoritePost);
                    batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "onFavoriteClick: Added to favorites");
                        }
                    });
                    mAdapter.notifyDataSetChanged();
                }

            }

            @Override
            public void onCommentClick(int position) {
                String id = mPostList.get(position).getDocumentId();
                Navigation.findNavController(view).navigate(UserProfileFragmentDirections.actionUserProfileFragment2ToPostComments(id));
            }

        });
    }


    @Override
    public void onItemClick(int position) {

    }

    @Override
    public void onFavoriteClick(int position) {

    }

    @Override
    public void onCommentClick(int position) {

    }
}
