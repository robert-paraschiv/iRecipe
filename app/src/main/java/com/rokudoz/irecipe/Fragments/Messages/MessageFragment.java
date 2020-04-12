package com.rokudoz.irecipe.Fragments.Messages;


import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
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
import androidx.recyclerview.widget.SimpleItemAnimator;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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
import com.rokudoz.irecipe.App;
import com.rokudoz.irecipe.Models.Conversation;
import com.rokudoz.irecipe.Models.Message;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.Adapters.MessageAdapter;
import com.rokudoz.irecipe.Utils.DirectReplyReceiver;
import com.rokudoz.irecipe.Utils.LinearLayoutManagerWrapper;
import com.rokudoz.irecipe.Utils.LastSeen;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 */
public class MessageFragment extends Fragment implements MessageAdapter.OnItemClickListener {
    private static final String TAG = "MessageFragment";
    private static int NR_OF_MESSAGES_TO_LIMIT = 20;

    //
    private final static int INTERVAL = 1000 * 60 * 1; //2 minutes
    Handler mHandler = new Handler();
    long date = 0L;
    boolean online = false;
    LastSeen lastSeen = new LastSeen();

    //Firebase RealTime db
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference usersRef = database.getReference("Users");

    //Sound for sent message
    private MediaPlayer mediaPlayer;

    private ImageView friendImage;
    private TextView friendName, friendOnlineStatus, dateTV;
    private TextInputEditText textInputEditText;
    private MaterialButton sendButton, backBtn;

    private MaterialCardView dateCard;

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
    private ListenerRegistration messagesListener, currentUserDetailsListener, friendDetailsListener;

    public MessageFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_message, container, false);
        }

        gotMessagesFirstTime = false;
        mediaPlayer = MediaPlayer.create(getContext(), R.raw.insight);

        backBtn = view.findViewById(R.id.messageFragment_backBtn);
        friendImage = view.findViewById(R.id.message_friendImage_ImageView);
        friendOnlineStatus = view.findViewById(R.id.fragment_message_onlineStatus);
        friendName = view.findViewById(R.id.message_friendName_TextView);
        textInputEditText = view.findViewById(R.id.message_input_TextInput);
        sendButton = view.findViewById(R.id.message_send_MaterialBtn);
        mRecyclerView = view.findViewById(R.id.message_recycler_view);
        dateTV = view.findViewById(R.id.messageFragment_dateTV);
        dateCard = view.findViewById(R.id.messageFragment_date_card);

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

        if (getActivity() != null) {
            BottomNavigationView navBar = getActivity().findViewById(R.id.bottom_navigation);
            navBar.setVisibility(View.GONE);
            getActivity().findViewById(R.id.banner_cardView).setVisibility(View.GONE);
        }


        buildRecyclerView();


        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null)
                    hideSoftKeyboard(getActivity());
                Navigation.findNavController(view).navigate(MessageFragmentDirections.actionMessageFragmentToAllMessagesFragment());
            }
        });
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });


        return view;
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        if (activity.getCurrentFocus() != null)
            inputMethodManager.hideSoftInputFromWindow(
                    activity.getCurrentFocus().getWindowToken(), 0);
    }

    Runnable mHandlerTask = new Runnable() {
        @Override
        public void run() {
            if (!online)
                checkLastSeen();
            mHandler.postDelayed(mHandlerTask, INTERVAL);
        }
    };

    private void checkLastSeen() {
        Log.d(TAG, "checkLastSeen: " + date);
        if (date != 0) {
            friendOnlineStatus.setVisibility(View.VISIBLE);
            friendOnlineStatus.setText(lastSeen.getLastSeen(date));
        }
    }

    void startRepeatingTask() {
        mHandlerTask.run();
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(mHandlerTask);
    }

    private void getFriendStatus() {
        //Friend Online Status
        usersRef.child(friendUserId).child("online").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue(Boolean.class) != null) {
                    online = dataSnapshot.getValue(Boolean.class);
                    if (online) {
                        friendOnlineStatus.setVisibility(View.VISIBLE);
                        friendOnlineStatus.setText("Online");
                        stopRepeatingTask();
                    } else {
//                        friendOnlineStatus.setText("Offline");
                        startRepeatingTask();

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
        usersRef.child(friendUserId).child("last_seen").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue(Long.class) != null) {
                    date = dataSnapshot.getValue(Long.class);
                    Log.d(TAG, "onDataChange: " + date + "|| " + date + "   ||| " + System.currentTimeMillis());
                    if (date != 0 && !online) {
                        friendOnlineStatus.setVisibility(View.VISIBLE);
                        friendOnlineStatus.setText(lastSeen.getLastSeen(date));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getActivity() != null)
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver((mMessageReceiver),
                    new IntentFilter("MessageNotification")
            );
        getFriendStatus();
        getFriendDetails();
        getMessages();
    }

    @Override
    public void onStop() {
        super.onStop();

        DetachListeners();

        if (getActivity() != null)
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    private void DetachListeners() {
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
        if (currentUserDetailsListener != null) {
            currentUserDetailsListener.remove();
            currentUserDetailsListener = null;
        }
        if (friendDetailsListener != null) {
            friendDetailsListener.remove();
            friendDetailsListener = null;
        }
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
        int notificationID = 0;
        char[] chars = friend_id.toCharArray();
        for (Character c : chars) {
            notificationID += c - 'a' + 1;
        }
        final int finalNotificationID = notificationID;
        usersReference.document(friend_id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot != null) {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        Glide.with(Objects.requireNonNull(getActivity())).asBitmap().load(user.getUserProfilePicUrl()).apply(RequestOptions.circleCropTransform())
                                .into(new CustomTarget<Bitmap>() {
                                    @Override
                                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                        String click_action = intent.getStringExtra("click_action");
                                        String messageBody = intent.getStringExtra("messageBody");
                                        String messageTitle = intent.getStringExtra("messageTitle");

                                        androidx.core.app.RemoteInput remoteInput = new androidx.core.app.RemoteInput.Builder("key_text_reply")
                                                .setLabel("Send message").build();
                                        Intent replyIntent = new Intent(getContext(), DirectReplyReceiver.class);
                                        replyIntent.putExtra("friend_id_messageFragment", friend_id);
                                        replyIntent.putExtra("coming_from", "MessageFragment");
                                        replyIntent.putExtra("notification_id", finalNotificationID);
                                        PendingIntent replyPendingIntent = PendingIntent.getBroadcast(getContext(), finalNotificationID, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                                        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                                                R.drawable.ic_send_black_24dp,
                                                "Reply",
                                                replyPendingIntent
                                        ).addRemoteInput(remoteInput).build();

                                        Person user = new Person.Builder().setName(messageTitle).setIcon(IconCompat.createWithBitmap(resource)).build();
                                        messagingStyle = new NotificationCompat.MessagingStyle(user);
                                        messagingStyle.setConversationTitle("Chat");

                                        NotificationCompat.MessagingStyle.Message message =
                                                new NotificationCompat.MessagingStyle.Message(messageBody, System.currentTimeMillis(), user);
                                        messagingStyle.addMessage(message);

                                        NotificationCompat.Builder builder = new NotificationCompat.Builder(Objects.requireNonNull(getContext()), App.CHANNEL_MESSAGES)
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
                                        resultIntent.putExtra("notification_id", finalNotificationID);
                                        PendingIntent resultPendingIntent = PendingIntent.getActivity(getContext(), finalNotificationID, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                                        builder.setContentIntent(resultPendingIntent);


                                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getContext());
                                        notificationManager.notify(finalNotificationID, builder.build());
                                    }

                                    @Override
                                    public void onLoadCleared(@Nullable Drawable placeholder) {

                                    }
                                });
                    }
                }
            }
        });
    }


    private void buildRecyclerView() {
        Log.d(TAG, "buildRecyclerView: ");
        mRecyclerView.setHasFixedSize(true);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManagerWrapper(getActivity(), LinearLayoutManager.VERTICAL, false);
        linearLayoutManager.setStackFromEnd(true);

        mAdapter = new MessageAdapter(messageList, getActivity());
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
        ((SimpleItemAnimator) Objects.requireNonNull(mRecyclerView.getItemAnimator())).setSupportsChangeAnimations(false);

        mAdapter.setOnItemClickListener(MessageFragment.this);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (!recyclerView.canScrollVertically(-1)) { //Up
                    if (gotMessagesFirstTime)
                        getMoreMessages();
                }
                DateFormat dateFormat = new SimpleDateFormat("MMMM dd, YYYY", Locale.getDefault());
                Message currentMessage = messageList.get(linearLayoutManager.findLastCompletelyVisibleItemPosition());
                if (currentMessage.getTimestamp() != null) {
                    dateTV.setText(dateFormat.format(currentMessage.getTimestamp()));
                    if (dateFormat.format(System.currentTimeMillis()).equals(dateFormat.format(currentMessage.getTimestamp()))) {
                        dateCard.setVisibility(View.INVISIBLE);
                    } else {
                        dateCard.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
    }


    private void getMessages() {
        messageList.clear();
        messagesDocumentSnapshots.clear();
        gotMessagesFirstTime = false;

        messagesListener = usersReference.document(currentUserId).collection("Conversations").document(friendUserId)
                .collection(friendUserId).orderBy("timestamp", Query.Direction.DESCENDING).limit(NR_OF_MESSAGES_TO_LIMIT).addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "onEvent: ", e);
                            return;
                        }
                        if (queryDocumentSnapshots != null) {
                            for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                Message message = documentSnapshot.toObject(Message.class);
                                message.setDocumentId(documentSnapshot.getId());
                                if (messageList.contains(message)) {
                                    messageList.set(messageList.indexOf(message), message);
                                    //                        mAdapter.notifyDataSetChanged();
                                    mAdapter.notifyItemChanged(messageList.indexOf(message));
                                } else {
                                    if (gotMessagesFirstTime) {
                                        messageList.add(message);
                                        mAdapter.notifyItemInserted(messageList.size() - 1);
                                        //                            mRecyclerView.smoothScrollToPosition(messageList.size() - 1);
                                        messagesDocumentSnapshots.add(documentSnapshot);
                                    } else {
                                        messageList.add(0, message);
                                        messagesDocumentSnapshots.add(0, documentSnapshot);
                                        mAdapter.notifyItemInserted(0);
                                    }
                                    mRecyclerView.smoothScrollToPosition(messageList.size() - 1);
                                }
                                if (queryDocumentSnapshots.getDocuments().size() != 0) {
                                    mLastQueriedDocument = messagesDocumentSnapshots.get(0);
                                }
                            }
                            gotMessagesFirstTime = true;
                        }

                    }
                });
    }

    private void getMoreMessages() {
        Query query = null;
        if (mLastQueriedDocument != null) {
            query = usersReference.document(currentUserId).collection("Conversations").document(friendUserId)
                    .collection(friendUserId).orderBy("timestamp", Query.Direction.DESCENDING).limit(NR_OF_MESSAGES_TO_LIMIT).startAfter(mLastQueriedDocument);
        } else {
            query = usersReference.document(currentUserId).collection("Conversations").document(friendUserId)
                    .collection(friendUserId).orderBy("timestamp", Query.Direction.DESCENDING).limit(NR_OF_MESSAGES_TO_LIMIT);
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
                            mAdapter.notifyItemInserted(0);
                        } else {
                            messageList.set(messageList.indexOf(message), message);
                            mAdapter.notifyItemChanged(messageList.indexOf(message));
                        }

                        if (queryDocumentSnapshots.getDocuments().size() != 0) {
                            mLastQueriedDocument = messagesDocumentSnapshots.get(0);
                        }
                    }
//                    mRecyclerView.scrollToPosition(queryDocumentSnapshots.size());
                }
            }
        });
    }

    private void sendMessage() {
        if (textInputEditText.getText().toString().trim().equals("")) {
            Toast.makeText(getActivity(), "Can't send empty message", Toast.LENGTH_SHORT).show();
        } else {
            String text = textInputEditText.getText().toString();
            final Message messageForCurrentUser = new Message(currentUserId, friendUserId, text, "message_sent", null, false,
                    "message", null, null);
            final Message messageForFriendUser = new Message(currentUserId, friendUserId, text, "message_received", null, false
                    , "message", null, null);

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
//                    mediaPlayer.start();
                }
            });


        }
    }

    private void getFriendDetails() {
        //Get current user details
        currentUserDetailsListener = usersReference.document(currentUserId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
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

        //Get friend user details
        friendDetailsListener = usersReference.document(friendUserId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "onEvent: ", e);
                    return;
                }
                if (documentSnapshot != null) {
                    userFriend = documentSnapshot.toObject(User.class);
                    if (userFriend != null) {
                        friendName.setText(userFriend.getName());
                        Glide.with(friendImage).load(userFriend.getUserProfilePicUrl()).centerCrop().into(friendImage);
                    }
                }

            }
        });
    }

    @Override
    public void onPostRecipeMessageClick(int position) {
        if (messageList.get(position).getPost() != null && messageList.get(position).getRecipe() == null) {
            Navigation.findNavController(view).navigate(MessageFragmentDirections
                    .actionMessageFragmentToPostDetailed(messageList.get(position).getPost().getDocumentId()));
        } else if (messageList.get(position).getPost() == null && messageList.get(position).getRecipe() != null) {
            Navigation.findNavController(view).navigate(MessageFragmentDirections
                    .actionMessageFragmentToRecipeDetailedFragment(messageList.get(position).getRecipe().getDocumentId()));
        }
    }
}


