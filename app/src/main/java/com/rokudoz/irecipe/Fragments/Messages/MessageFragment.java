package com.rokudoz.irecipe.Fragments.Messages;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
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
import com.rokudoz.irecipe.MainActivity;
import com.rokudoz.irecipe.Models.Conversation;
import com.rokudoz.irecipe.Models.Message;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.Adapters.ConversationAdapter;
import com.rokudoz.irecipe.Utils.Adapters.MessageAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 */
public class MessageFragment extends Fragment {
    private static final String TAG = "MessageFragment";
    private ImageView friendImage;
    private TextView friendName;
    private TextInputEditText textInputEditText;
    private MaterialButton sendButton;

    private View view;
    private String friendUserId = "";
    private String currentUserId = "";

    private List<Message> messageList = new ArrayList<>();

    private RecyclerView mRecyclerView;
    private MessageAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private User userFriend = new User();
    private User mUser = new User();

    //Firebase
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference usersReference = db.collection("Users");
    private ListenerRegistration messagesListener, friendDetailsListener;

    public MessageFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_message, container, false);

        friendImage = view.findViewById(R.id.message_friendImage_ImageView);
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
        DetachFireStoreListeners();
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
                        createNotificationChannel();

                        String click_action = intent.getStringExtra("click_action");
                        String messageBody = intent.getStringExtra("messageBody");
                        String messageTitle = intent.getStringExtra("messageTitle");

                        Intent resultIntent = new Intent(click_action);
                        resultIntent.putExtra("friend_id", friend_id);
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity(), getString(R.string.default_notification_channel_id))
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setContentTitle(messageTitle)
                                .setContentText(messageBody)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                .setAutoCancel(true);

                        PendingIntent resultPendingIntent = PendingIntent.getActivity(getContext(), 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                        builder.setContentIntent(resultPendingIntent);


                        int mNotificationId = (int) System.currentTimeMillis();

                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getActivity());
                        notificationManager.notify(mNotificationId, builder.build());
                    }
                }
        }
    };

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Foodify";
            String description = "For friend requests";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(getString(R.string.default_notification_channel_id), name,
                    importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviours after this
            NotificationManager notificationManager =
                    Objects.requireNonNull(getActivity()).getSystemService(NotificationManager.class);
            Objects.requireNonNull(notificationManager).createNotificationChannel(channel);
        }
    }

    private void DetachFireStoreListeners() {
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
        if (friendDetailsListener != null) {
            friendDetailsListener.remove();
            friendDetailsListener = null;
        }
    }

    private void buildRecyclerView() {
        Log.d(TAG, "buildRecyclerView: ");
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getContext());
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setStackFromEnd(true);

        mAdapter = new MessageAdapter(messageList);

        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

    }


    private void getMessages() {
        messagesListener = usersReference.document(currentUserId).collection("Conversations").document(friendUserId)
                .collection(friendUserId).orderBy("timestamp", Query.Direction.ASCENDING).addSnapshotListener(new EventListener<QuerySnapshot>() {
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
                                messageList.add(message);
                            } else {
                                messageList.set(messageList.indexOf(message), message);
                            }
                            mAdapter.notifyDataSetChanged();
                            mRecyclerView.scrollToPosition(messageList.size() - 1);
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

            final Conversation conversationForCurrentUser = new Conversation(friendUserId, userFriend.getName(), userFriend.getUserProfilePicUrl(), text, "message_sent", null, false);
            final Conversation conversationForFriendUser = new Conversation(currentUserId, mUser.getName(), mUser.getUserProfilePicUrl(), text, "message_received", null, false);

            textInputEditText.setText("");


            //Send message to db in batch
            WriteBatch batch = db.batch();
            String messageID = usersReference.document(currentUserId).collection("Conversations").document(friendUserId).collection(friendUserId).document().getId();
            Log.d(TAG, "sendMessage: " + messageID);
            batch.set(usersReference.document(currentUserId).collection("Conversations").document(friendUserId), conversationForCurrentUser);
            batch.set(usersReference.document(friendUserId).collection("Conversations").document(currentUserId), conversationForFriendUser);
            batch.set(usersReference.document(currentUserId).collection("Conversations").document(friendUserId).collection(friendUserId).document(messageID)
                    , messageForCurrentUser);
            batch.set(usersReference.document(friendUserId).collection("Conversations").document(currentUserId).collection(currentUserId).document(messageID)
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

        friendDetailsListener = usersReference.document(friendUserId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
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
