package com.rokudoz.irecipe.Fragments.Messages;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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
import com.rokudoz.irecipe.App;
import com.rokudoz.irecipe.MainActivity;
import com.rokudoz.irecipe.Models.Conversation;
import com.rokudoz.irecipe.Models.Message;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.Adapters.ConversationAdapter;
import com.rokudoz.irecipe.Utils.Adapters.MessageAdapter;
import com.rokudoz.irecipe.Utils.DirectReplyReceiver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * A simple {@link Fragment} subclass.
 */
public class MessageFragment extends Fragment {
    private static final String TAG = "MessageFragment";

    //Firebase RealTime db
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference usersRef = database.getReference("Users");

    private ImageView friendImage;
    private TextView friendName, friendOnlineStatus;
    private TextInputEditText textInputEditText;
    private MaterialButton sendButton;

    private View view;
    private String friendUserId = "";
    private String currentUserId = "";

    private List<Message> messageList = new ArrayList<>();

    public static NotificationCompat.MessagingStyle messagingStyle;
    private RecyclerView mRecyclerView;
    private MessageAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private User userFriend = new User();
    private User mUser = new User();

    //Firebase
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference usersReference = db.collection("Users");
    private List<DocumentSnapshot> messagesDocumentSnapshots = new ArrayList<>();
    private DocumentSnapshot mLastQueriedDocument;
    private boolean gotMessagesFirstTime = false;

    public MessageFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_message, container, false);

        friendImage = view.findViewById(R.id.message_friendImage_ImageView);
        friendOnlineStatus = view.findViewById(R.id.fragment_message_onlineStatus);
        friendName = view.findViewById(R.id.message_friendName_TextView);
        textInputEditText = view.findViewById(R.id.message_input_TextInput);
        sendButton = view.findViewById(R.id.message_send_MaterialBtn);
        mRecyclerView = view.findViewById(R.id.message_recycler_view);

        MessageFragmentArgs messageFragmentArgs = MessageFragmentArgs.fromBundle(getArguments());
        friendUserId = messageFragmentArgs.getUserId();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d(TAG, "onCreateView: friend id " + friendUserId);

        friendImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(view).navigate(MessageFragmentDirections.actionMessageFragmentToUserProfileFragment2(friendUserId));
            }
        });
        BottomNavigationView navBar = getActivity().findViewById(R.id.bottom_navigation);
        navBar.setVisibility(View.GONE);


        buildRecyclerView();
        getFriendDetails();

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });


        //Friend Online Status
        usersRef.child(friendUserId).child("online").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue(Boolean.class) != null) {
                    boolean online = dataSnapshot.getValue(Boolean.class);
                    if (online) {
                        friendOnlineStatus.setText("Online");
                    } else {
                        friendOnlineStatus.setText("Offline");
                    }
                    Log.d(TAG, "onDataChange: online= " + online);
                } else {
                    friendOnlineStatus.setText("Offline");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getActivity() != null)
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver((mMessageReceiver),
                    new IntentFilter("MessageNotification")
            );
        getMessages();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getActivity() != null)
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getActivity() != null)
                if (intent.hasExtra("friend_id")) {
                    String friend_id = intent.getStringExtra("friend_id");
                    Log.d(TAG, "onReceive: friendUserId: " + friendUserId + " friend_id " + friend_id);
                    if (!friend_id.equals(friendUserId)) {

                        sendNotification(intent, friend_id);
                    }
                }
        }
    };

    private void sendNotification(final Intent intent, final String friend_id) {
        int  notificationID = 0;
        char[] chars = friend_id.toCharArray();
        for (Character c : chars) {
            notificationID += c - 'a' + 1;
        }
        String click_action = intent.getStringExtra("click_action");
        String messageBody = intent.getStringExtra("messageBody");
        String messageTitle = intent.getStringExtra("messageTitle");

        androidx.core.app.RemoteInput remoteInput = new androidx.core.app.RemoteInput.Builder("key_text_reply")
                .setLabel("Send message").build();
        Intent replyIntent = new Intent(getContext(), DirectReplyReceiver.class);
        replyIntent.putExtra("friend_id_messageFragment", friend_id);
        replyIntent.putExtra("coming_from", "MessageFragment");
        replyIntent.putExtra("notification_id", notificationID);
        PendingIntent replyPendingIntent = PendingIntent.getBroadcast(getContext(), notificationID, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                R.drawable.ic_send_black_24dp,
                "Reply",
                replyPendingIntent
        ).addRemoteInput(remoteInput).build();

        Person user = new Person.Builder().setName(messageTitle).build();
        messagingStyle = new NotificationCompat.MessagingStyle(user);
        messagingStyle.setConversationTitle("Chat");

        NotificationCompat.MessagingStyle.Message message =
                new NotificationCompat.MessagingStyle.Message(messageBody, System.currentTimeMillis(), user);
        messagingStyle.addMessage(message);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), App.CHANNEL_MESSAGES)
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setStyle(messagingStyle)
                .addAction(replyAction)
                .setColor(Color.BLUE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true);

        Intent resultIntent = new Intent(click_action);
        resultIntent.putExtra("friend_id", friend_id);
        resultIntent.putExtra("coming_from", "MessageFragment");
        resultIntent.putExtra("notification_id", notificationID);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(getContext(), notificationID, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);


        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getContext());
        notificationManager.notify(notificationID, builder.build());
    }

    private void buildRecyclerView() {
        Log.d(TAG, "buildRecyclerView: ");
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getContext());
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setStackFromEnd(true);

        mAdapter = new MessageAdapter(messageList, getActivity());

        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (!recyclerView.canScrollVertically(-1)) { //Up
                    if (gotMessagesFirstTime)
                        getMessagesAgain();
                }
            }
        });
    }


    private void getMessages() {
        usersReference.document(currentUserId).collection("Conversations").document(friendUserId)
                .collection(friendUserId).orderBy("timestamp", Query.Direction.DESCENDING).limit(15).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "onEvent: ", e);
                    return;
                }
                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    Message message = documentSnapshot.toObject(Message.class);
                    message.setDocumentId(documentSnapshot.getId());
                    if (!messageList.contains(message)) {
                        if (gotMessagesFirstTime) {
                            messageList.add(message);
                            mAdapter.notifyItemInserted(messageList.size() - 1);
                            messagesDocumentSnapshots.add(documentSnapshot);
                        } else {
                            messageList.add(0, message);
                            messagesDocumentSnapshots.add(0, documentSnapshot);
                            mAdapter.notifyItemInserted(0);
                        }
                    } else {
                        messageList.set(messageList.indexOf(message), message);
                        mAdapter.notifyDataSetChanged();
//                        mAdapter.notifyItemChanged(messageList.indexOf(message));
                    }

                    mRecyclerView.scrollToPosition(messageList.size() - 1);

                    if (queryDocumentSnapshots.getDocuments().size() != 0) {
                        mLastQueriedDocument = messagesDocumentSnapshots.get(0);
                    }
                }
                gotMessagesFirstTime = true;
            }
        });
    }

    private void getMessagesAgain() {
        Query query = null;
        if (mLastQueriedDocument != null) {
            query = usersReference.document(currentUserId).collection("Conversations").document(friendUserId)
                    .collection(friendUserId).orderBy("timestamp", Query.Direction.DESCENDING).limit(15).startAfter(mLastQueriedDocument);
        } else {
            query = usersReference.document(currentUserId).collection("Conversations").document(friendUserId)
                    .collection(friendUserId).orderBy("timestamp", Query.Direction.DESCENDING).limit(15);
        }
        query.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                if (queryDocumentSnapshots != null && queryDocumentSnapshots.size() > 0) {
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Message message = documentSnapshot.toObject(Message.class);
                        message.setDocumentId(documentSnapshot.getId());
                        if (!messageList.contains(message)) {
                            messageList.add(0, message);
                            messagesDocumentSnapshots.add(0, documentSnapshot);

                        } else {
                            messageList.set(messageList.indexOf(message), message);
                        }
                        mAdapter.notifyDataSetChanged();

                        if (queryDocumentSnapshots.getDocuments().size() != 0) {
                            mLastQueriedDocument = messagesDocumentSnapshots.get(0);
                        }
                    }
                    mRecyclerView.scrollToPosition(queryDocumentSnapshots.size());
                }
            }
        });
    }

    private void sendMessage() {
        if (textInputEditText.getText().toString().trim().equals("")) {
            Toast.makeText(getActivity(), "Can't send empty message", Toast.LENGTH_SHORT).show();
        } else {
            String text = textInputEditText.getText().toString();
            final Message messageForCurrentUser = new Message(currentUserId, friendUserId, text, "message_sent", null, false);
            final Message messageForFriendUser = new Message(currentUserId, friendUserId, text, "message_received", null, false);

            final Conversation conversationForCurrentUser = new Conversation(friendUserId, userFriend.getName(), userFriend.getUserProfilePicUrl(), text
                    , "message_sent", null, false);
            final Conversation conversationForFriendUser = new Conversation(currentUserId, mUser.getName(), mUser.getUserProfilePicUrl(), text
                    , "message_received", null, false);

            textInputEditText.setText("");


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
                }
            });


        }
    }

    private void getFriendDetails() {

        usersReference.document(currentUserId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "onEvent: ", e);
                    return;
                }
                if (documentSnapshot != null) {
                    mUser = documentSnapshot.toObject(User.class);
                }
            }
        });

        usersReference.document(friendUserId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "onEvent: ", e);
                    return;
                }
                userFriend = documentSnapshot.toObject(User.class);

                friendName.setText(userFriend.getName());
                Glide.with(friendImage).load(userFriend.getUserProfilePicUrl()).centerCrop().into(friendImage);
            }
        });
    }

}
