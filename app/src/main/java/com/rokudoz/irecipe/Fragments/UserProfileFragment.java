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
import android.widget.RelativeLayout;
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

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserProfileFragment extends Fragment implements PostAdapter.OnItemClickListener {
    private static final String TAG = "UserProfileFragment";

    private String documentID;
    private String userProfilePicUrl = "";
    private User mUser;
    private User otherUser = new User();
    private TextView UserNameTv, UserUsernameTv, UserDescriptionTv;
    private MaterialButton mAddFriendButton, mAcceptFriendReqButton, mDeclineFriendReqButton, messageUser;
    private RelativeLayout acceptDeclineLayout;
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
    private CollectionReference recipesRef = db.collection("Recipes");

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
        messageUser = view.findViewById(R.id.userprofile_messageUser_MaterialButton);
        mDeclineFriendReqButton = view.findViewById(R.id.userprofile_declineFriendReq_MaterialButton);
        mAcceptFriendReqButton = view.findViewById(R.id.userprofile_acceptFriend_MaterialButton);
        acceptDeclineLayout = view.findViewById(R.id.accept_decline_layout);

        mUser = new User();

        BottomNavigationView navBar = getActivity().findViewById(R.id.bottom_navigation);
        navBar.setVisibility(View.GONE);

        UserProfileFragmentArgs userProfileFragmentArgs = UserProfileFragmentArgs.fromBundle(getArguments());
        getArgsPassed(userProfileFragmentArgs);


        buildRecyclerView();

        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void getCurrentUserDetails() {
        usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "onEvent: ", e);
                    return;
                }
                final User user = documentSnapshot.toObject(User.class);

                mUser = documentSnapshot.toObject(User.class);
                loggedInUserDocumentId = documentSnapshot.getId();
                userFavPostList = new ArrayList<>();

                if (!friends_userID_list.contains(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                    friends_userID_list.add(FirebaseAuth.getInstance().getCurrentUser().getUid());
                }

                usersReference.document(user.getUser_id()).collection("FriendList").addSnapshotListener(new EventListener<QuerySnapshot>() {
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
                        usersReference.document(user.getUser_id()).collection("FavoritePosts").addSnapshotListener(new EventListener<QuerySnapshot>() {
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
            usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("FriendList").document(documentID).addSnapshotListener(new EventListener<DocumentSnapshot>() {
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
                            acceptDeclineLayout.setVisibility(View.GONE);

                            messageUser.setVisibility(View.VISIBLE);
                            messageUser.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Navigation.findNavController(view).navigate(UserProfileFragmentDirections.actionUserProfileFragment2ToMessageFragment(documentID));
                                }
                            });

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
                                                    //Delete user from friends
                                                    WriteBatch batch = db.batch();
                                                    batch.delete(usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("FriendList")
                                                            .document(documentID));
                                                    batch.delete(usersReference.document(documentID).collection("FriendList")
                                                            .document(FirebaseAuth.getInstance().getCurrentUser().getUid()));
                                                    batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            if (getActivity() != null)
                                                                Toast.makeText(getActivity(), "Removed from friend list", Toast.LENGTH_SHORT).show();
                                                            mAddFriendButton.setText("Add Friend");
                                                            acceptDeclineLayout.setVisibility(View.GONE);
                                                            mAddFriendButton.setEnabled(true);
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
                            acceptDeclineLayout.setVisibility(View.GONE);
                            messageUser.setVisibility(View.GONE);

                            mAddFriendButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    //Delete user from friends
                                    WriteBatch batch = db.batch();
                                    batch.delete(usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                            .collection("FriendList").document(documentID));
                                    batch.delete(usersReference.document(documentID).collection("FriendList")
                                            .document(FirebaseAuth.getInstance().getCurrentUser().getUid()));
                                    batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            if (getActivity() != null)
                                                Toast.makeText(getActivity(), "Cancelled friend request", Toast.LENGTH_SHORT).show();
                                            mAddFriendButton.setText("Add Friend");
                                            mAddFriendButton.setEnabled(true);
                                        }
                                    });
                                }
                            });
                        } else if (friend.getFriend_status().equals("friend_request_received")) {
                            mAddFriendButton.setVisibility(View.GONE);
                            acceptDeclineLayout.setVisibility(View.VISIBLE);
                            messageUser.setVisibility(View.GONE);

                            mAcceptFriendReqButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Friend friendForCurrentUser = new Friend(documentID, otherUser.getName(), otherUser.getUserProfilePicUrl(), "friends", null);
                                    Friend friendForOtherUser = new Friend(mUser.getUser_id(), mUser.getName(), mUser.getUserProfilePicUrl(), "friend_request_accepted", null);
                                    WriteBatch batch = db.batch();
                                    batch.set(usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("FriendList")
                                            .document(documentID), friendForCurrentUser);
                                    batch.set(usersReference.document(documentID).collection("FriendList")
                                            .document(FirebaseAuth.getInstance().getCurrentUser().getUid()), friendForOtherUser);
                                    batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            if (getActivity() != null)
                                                Toast.makeText(getActivity(), "Accepted friend request", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            });

                            mDeclineFriendReqButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Friend friendForCurrentUser = new Friend(documentID, otherUser.getName(), otherUser.getUserProfilePicUrl(), "friend_request_declined", null);
                                    Friend friendForOtherUser = new Friend(mUser.getUser_id(), mUser.getName(), mUser.getUserProfilePicUrl(), "friend_request_declined", null);
                                    WriteBatch batch = db.batch();
                                    batch.set(usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("FriendList")
                                            .document(documentID), friendForCurrentUser);
                                    batch.set(usersReference.document(documentID).collection("FriendList")
                                            .document(FirebaseAuth.getInstance().getCurrentUser().getUid()), friendForOtherUser);
                                    batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            if (getActivity() != null)
                                                Toast.makeText(getActivity(), "Declined friend request", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            });
                        } else if (friend.getFriend_status().equals("friend_request_declined")) {
                            mAddFriendButton.setText("Add friend");
                            mAddFriendButton.setVisibility(View.VISIBLE);
                            messageUser.setVisibility(View.GONE);
                            acceptDeclineLayout.setVisibility(View.GONE);
                            mAddFriendButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Friend friendForCurrentUser = new Friend(documentID, otherUser.getName(), otherUser.getUserProfilePicUrl(), "friend_request_sent", null);
                                    Friend friendForOtherUser = new Friend(mUser.getUser_id(), mUser.getName(), mUser.getUserProfilePicUrl(), "friend_request_received", null);
                                    WriteBatch batch = db.batch();
                                    batch.set(usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("FriendList")
                                            .document(documentID), friendForCurrentUser);
                                    batch.set(usersReference.document(documentID).collection("FriendList")
                                            .document(FirebaseAuth.getInstance().getCurrentUser().getUid()), friendForOtherUser);
                                    batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            if (getActivity() != null)
                                                Toast.makeText(getActivity(), "Sent friend request", Toast.LENGTH_SHORT).show();
                                            mAddFriendButton.setText("Cancel Friend request");
                                        }
                                    });
                                }
                            });
                        }
                        Log.d(TAG, "onSuccess: FOUND IN FRIEND LIST " + friend.toString());
                    } else {
                        mAddFriendButton.setText("Add friend");
                        mAddFriendButton.setVisibility(View.VISIBLE);
                        messageUser.setVisibility(View.GONE);
                        acceptDeclineLayout.setVisibility(View.GONE);
                        mAddFriendButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Friend friendForCurrentUser = new Friend(documentID, otherUser.getName(), otherUser.getUserProfilePicUrl(), "friend_request_sent", null);
                                Friend friendForOtherUser = new Friend(mUser.getUser_id(), mUser.getName(), mUser.getUserProfilePicUrl(), "friend_request_received", null);
                                WriteBatch batch = db.batch();
                                batch.set(usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("FriendList")
                                        .document(documentID), friendForCurrentUser);
                                batch.set(usersReference.document(documentID).collection("FriendList")
                                        .document(FirebaseAuth.getInstance().getCurrentUser().getUid()), friendForOtherUser);
                                batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        if (getActivity() != null)
                                            Toast.makeText(getActivity(), "Sent friend request", Toast.LENGTH_SHORT).show();
                                        mAddFriendButton.setText("Cancel Friend request");
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

    private void getArgsPassed(UserProfileFragmentArgs userProfileFragmentArgs) {
        documentID = userProfileFragmentArgs.getDocumentID();
        getCurrentUserDetails();
        getFriendInfo();
    }

    private void getFriendInfo() {
        usersReference.document(documentID).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                User user = documentSnapshot.toObject(User.class);
                otherUser = documentSnapshot.toObject(User.class);
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
    }

    private void PerformMainQuery(Query postsQuery) {
        postsQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots,
                                @javax.annotation.Nullable FirebaseFirestoreException e) {
                if (queryDocumentSnapshots != null) {
                    for (final QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        final Post post = document.toObject(Post.class);
                        post.setDocumentId(document.getId());

                        //Check if current user liked the post or not
                        postsRef.document(post.getDocumentId()).collection("UsersWhoFaved").document(FirebaseAuth.getInstance().getCurrentUser().getUid())
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
                                                mAdapter.notifyDataSetChanged();
                                            } else {
                                                post.setFavorite(false);
                                                mAdapter.notifyDataSetChanged();
                                            }
                                        } else {
                                            Log.d(TAG, "onEvent: NULL");
                                        }
                                    }
                                });
                        //Get post referenced Recipe details
                        recipesRef.document(post.getReferenced_recipe_docId()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                            @Override
                            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                                if (e != null) {
                                    Log.w(TAG, "onEvent: ", e);
                                    return;
                                }
                                if (documentSnapshot != null) {
                                    Recipe recipe = documentSnapshot.toObject(Recipe.class);
                                    post.setRecipe_name(recipe.getTitle());
                                    post.setRecipe_imageUrl(recipe.getImageUrls_list().get(0));
                                    mAdapter.notifyDataSetChanged();
                                }
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
                    mPostList.add(new Post("", "", "", "", 0, 0
                            , "This user hasn't posted anything yet :)", ""
                            , false, "Everyone", null));
                    Log.d(TAG, "EMPTY: ");
                }
                mAdapter.notifyDataSetChanged();

            }
        });
    }

    @Override
    public void onItemClick(int position) {
        String id = mPostList.get(position).getDocumentId();
        if (id != null && !id.equals(""))
            Navigation.findNavController(view).navigate(UserProfileFragmentDirections.actionUserProfileFragment2ToPostDetailed(id));
    }

    @Override
    public void onFavoriteClick(int position) {
        ////////////////////////////////////////////////
        final String id = mPostList.get(position).getDocumentId();
        if (id != null) {
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
                UserWhoFaved userWhoFaved = new UserWhoFaved(mUser.getUser_id(), mUser.getName(), mUser.getUserProfilePicUrl(), null);
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
    }

    @Override
    public void onCommentClick(int position) {
        String id = mPostList.get(position).getDocumentId();
        if (id != null)
            Navigation.findNavController(view).navigate(UserProfileFragmentDirections.actionUserProfileFragment2ToPostComments(id));
    }
}
