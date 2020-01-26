package com.rokudoz.irecipe.Fragments.Messages;


import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.rokudoz.irecipe.Models.Conversation;
import com.rokudoz.irecipe.Models.Message;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.Adapters.ConversationAdapter;
import com.rokudoz.irecipe.Utils.Adapters.MessageAdapter;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

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

    //Firebase
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference usersReference = db.collection("Users");

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
        getMessages();
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
        usersReference.document(currentUserId).collection("Conversations").document(friendUserId)
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

            final Conversation conversationForCurrentUser = new Conversation(friendUserId, text, "message_sent", null,false);
            final Conversation conversationForFriendUser = new Conversation(currentUserId, text, "message_received", null,false);

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
        usersReference.document(friendUserId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "onEvent: ", e);
                    return;
                }
                userFriend = documentSnapshot.toObject(User.class);

                friendName.setText(userFriend.getName());
                Picasso.get().load(userFriend.getUserProfilePicUrl()).fit().centerCrop().into(friendImage);
            }
        });
    }

}
