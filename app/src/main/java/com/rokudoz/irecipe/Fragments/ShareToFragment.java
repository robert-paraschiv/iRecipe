package com.rokudoz.irecipe.Fragments;

import android.app.ProgressDialog;
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
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.rokudoz.irecipe.Models.Conversation;
import com.rokudoz.irecipe.Models.Friend;
import com.rokudoz.irecipe.Models.Message;
import com.rokudoz.irecipe.Models.Post;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.Adapters.ConversationAdapter;
import com.rokudoz.irecipe.Utils.Adapters.FriendAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ShareToFragment extends Fragment implements ConversationAdapter.OnItemClickListener, FriendAdapter.OnItemClickListener {
    private static final String TAG = "ShareToFragment";

    private View view;
    private String documentID, type;

    private User mUser;

    private RecyclerView conversationsRecyclerView, friendListRecyclerView;
    private ConversationAdapter conversationAdapter;
    private FriendAdapter friendAdapter;
    private RecyclerView.LayoutManager conversationsLayoutManager, friendsLayoutManager;

    //FireBase
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference usersReference = db.collection("Users");
    private CollectionReference recipesRef = db.collection("Recipes");
    private CollectionReference postsRef = db.collection("Posts");
    private ListenerRegistration userConversationsListener, userFriendListListener, currentUserDetailsListener;

    private List<Conversation> conversationList = new ArrayList<>();
    private List<Friend> friendList = new ArrayList<>();


    public ShareToFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null)
                parent.removeView(view);
        }
        try {
            view = inflater.inflate(R.layout.fragment_share_to, container, false);
        } catch (InflateException e) {
            Log.e(TAG, "onCreateView: ", e);
        }

        conversationsRecyclerView = view.findViewById(R.id.shareToFragment_conversationList_rv);
        friendListRecyclerView = view.findViewById(R.id.shareToFragment_friendList_rv);

        if (getArguments() != null) {
            ShareToFragmentArgs shareToFragmentArgs = ShareToFragmentArgs.fromBundle(getArguments());
            documentID = shareToFragmentArgs.getDocumentID();
            type = shareToFragmentArgs.getType();
        }

        buildRecyclerViews();
        return view;
    }

    private void buildRecyclerViews() {
        conversationsRecyclerView.setHasFixedSize(true);
        conversationsLayoutManager = new LinearLayoutManager(getContext());
        conversationAdapter = new ConversationAdapter(getContext(), conversationList);
        conversationsRecyclerView.setLayoutManager(conversationsLayoutManager);
        conversationsRecyclerView.setAdapter(conversationAdapter);
        conversationAdapter.setOnItemClickListener(ShareToFragment.this);

        friendListRecyclerView.setHasFixedSize(true);
        friendsLayoutManager = new LinearLayoutManager(getContext());
        friendAdapter = new FriendAdapter(friendList);
        friendListRecyclerView.setLayoutManager(friendsLayoutManager);
        friendListRecyclerView.setAdapter(friendAdapter);
        friendAdapter.setOnItemClickListener(ShareToFragment.this);
    }

    @Override
    public void onStart() {
        super.onStart();
        getConversations();
        getCurrentUserDetails();
    }

    private void getCurrentUserDetails() {
        currentUserDetailsListener = usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                        if (e == null && documentSnapshot != null) {
                            mUser = documentSnapshot.toObject(User.class);
                        }
                    }
                });
    }

    private void getFriendList() {
        List<String> acceptedStatusList = new ArrayList<>();
        acceptedStatusList.add("friends");
        acceptedStatusList.add("friend_request_received");
        acceptedStatusList.add("friend_request_accepted");

        userFriendListListener = usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("FriendList")
                .whereIn("friend_status", acceptedStatusList).addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e == null && queryDocumentSnapshots != null && queryDocumentSnapshots.size() > 0) {
                            for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                Friend friend = documentSnapshot.toObject(Friend.class);
                                if (!friendList.contains(friend)
                                        && !conversationList.contains(new Conversation(friend.getFriend_user_id(), null,
                                        null, null, null, null, null))) {
                                    friendList.add(friend);
                                }
                            }
                            friendAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private void getConversations() {
        userConversationsListener = usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("Conversations")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "onEvent: ", e);
                            return;
                        }
                        if (queryDocumentSnapshots != null) {
                            for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                Conversation conversation = documentSnapshot.toObject(Conversation.class);
                                if (!conversationList.contains(conversation)) {
                                    conversationList.add(conversation);
                                } else {
                                    conversationList.set(conversationList.indexOf(conversation), conversation);
                                }
                            }
                        }
                        //Sort conversations by date, desc
                        Collections.sort(conversationList, new Comparator<Conversation>() {
                            @Override
                            public int compare(Conversation o1, Conversation o2) {
                                if (o1.getDate() == null || o2.getDate() == null)
                                    return 0;
                                return o2.getDate().compareTo(o1.getDate());
                            }
                        });
                        conversationAdapter.notifyDataSetChanged();
                        getFriendList();
                    }
                });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (userConversationsListener != null) {
            userConversationsListener.remove();
            userConversationsListener = null;
        }
        if (userFriendListListener != null) {
            userFriendListListener.remove();
            userFriendListListener = null;
        }
        if (currentUserDetailsListener != null) {
            currentUserDetailsListener.remove();
            currentUserDetailsListener = null;
        }
    }

    @Override
    public void onConversationClick(int position) {
        final Conversation conversation = conversationList.get(position);
        if (conversation != null) {
            final String friendUserId = conversation.getUserId();
            final String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            sendMessage(friendUserId, currentUserId);
        }
    }

    @Override
    public void onDeleteClick(int position) {

    }

    @Override
    public void onPictureClick(int position) {

    }

    @Override
    public void onFriendClick(int position) {
        Friend friend = friendList.get(position);
        if (friend != null) {
            final String friendUserId = friend.getFriend_user_id();
            final String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            sendMessage(friendUserId, currentUserId);
        }
    }


    private void sendMessage(final String friendUserId, final String currentUserId) {
        final ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setTitle("Sending...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        usersReference.document(friendUserId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                final User userFriend = documentSnapshot.toObject(User.class);

                if (type.equals("post")) {
                    postsRef.document(documentID).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            final Post post = documentSnapshot.toObject(Post.class);
                            post.setDocumentId(documentSnapshot.getId());
                            final Message messageForCurrentUser = new Message(currentUserId, friendUserId, "You sent a post", "message_sent", null, false,
                                    "post", null, post);
                            final Message messageForFriendUser = new Message(currentUserId, friendUserId, "You received a post", "message_received", null, false
                                    , "post", null, post);

                            final Conversation conversationForCurrentUser = new Conversation(friendUserId, userFriend.getName(), userFriend.getUserProfilePicUrl(), "You sent a post"
                                    , "message_sent", null, false);
                            final Conversation conversationForFriendUser = new Conversation(currentUserId, mUser.getName(), mUser.getUserProfilePicUrl(), "Sent you a post"
                                    , "message_received", null, false);

                            //Send message to db in batch
                            WriteBatch batch = db.batch();
                            String messageID = usersReference.document(currentUserId).collection("Conversations").document(friendUserId).collection(friendUserId)
                                    .document().getId();
                            Log.d(TAG, "sendMessage: " + messageID);
                            batch.set(usersReference.document(currentUserId).collection("Conversations").document(friendUserId), conversationForCurrentUser);
                            batch.set(usersReference.document(friendUserId).collection("Conversations").document(currentUserId), conversationForFriendUser);
                            batch.set(usersReference.document(currentUserId).collection("Conversations").document(friendUserId).collection(friendUserId)
                                            .document(messageID)
                                    , messageForCurrentUser);
                            batch.set(usersReference.document(friendUserId).collection("Conversations").document(currentUserId).collection(currentUserId)
                                            .document(messageID)
                                    , messageForFriendUser);

                            batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d(TAG, "onSuccess: added message");
                                    progressDialog.cancel();
                                    if (Navigation.findNavController(view).getCurrentDestination().getId() == R.id.shareToFragment)
                                        Navigation.findNavController(view).navigate(ShareToFragmentDirections.actionShareToFragmentToMessageFragment(friendUserId));
                                }
                            });
                        }
                    });
                } else if (type.equals("recipe")) {
                    recipesRef.document(documentID).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            Recipe recipe = documentSnapshot.toObject(Recipe.class);
                            recipe.setDocumentId(documentSnapshot.getId());
                            final Message messageForCurrentUser = new Message(currentUserId, friendUserId, "You sent a recipe", "message_sent", null, false,
                                    "post", recipe, null);
                            final Message messageForFriendUser = new Message(currentUserId, friendUserId, "You received a recipe", "message_received", null, false
                                    , "post", recipe, null);

                            final Conversation conversationForCurrentUser = new Conversation(friendUserId, userFriend.getName(), userFriend.getUserProfilePicUrl(), "You sent a recipe"
                                    , "message_sent", null, false);
                            final Conversation conversationForFriendUser = new Conversation(currentUserId, mUser.getName(), mUser.getUserProfilePicUrl(), "Sent you a recipe"
                                    , "message_received", null, false);

                            //Send message to db in batch
                            WriteBatch batch = db.batch();
                            String messageID = usersReference.document(currentUserId).collection("Conversations").document(friendUserId).collection(friendUserId)
                                    .document().getId();
                            Log.d(TAG, "sendMessage: " + messageID);
                            batch.set(usersReference.document(currentUserId).collection("Conversations").document(friendUserId), conversationForCurrentUser);
                            batch.set(usersReference.document(friendUserId).collection("Conversations").document(currentUserId), conversationForFriendUser);
                            batch.set(usersReference.document(currentUserId).collection("Conversations").document(friendUserId).collection(friendUserId)
                                            .document(messageID)
                                    , messageForCurrentUser);
                            batch.set(usersReference.document(friendUserId).collection("Conversations").document(currentUserId).collection(currentUserId)
                                            .document(messageID)
                                    , messageForFriendUser);

                            batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d(TAG, "onSuccess: added message");
                                    progressDialog.cancel();
                                    if (Navigation.findNavController(view).getCurrentDestination().getId() == R.id.shareToFragment)
                                        Navigation.findNavController(view).navigate(ShareToFragmentDirections.actionShareToFragmentToMessageFragment(friendUserId));
                                }
                            });
                        }
                    });
                }
            }
        });
    }
}