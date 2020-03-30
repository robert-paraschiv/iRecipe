package com.rokudoz.irecipe.Fragments.Messages;


import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
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
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.Adapters.ConversationAdapter;
import com.rokudoz.irecipe.Utils.ConversationViewDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 */
public class AllMessagesFragment extends Fragment implements ConversationAdapter.OnItemClickListener {
    private static final String TAG = "profileMyFriendList";
    private View view;

    ProgressBar progressBar;

    private RecyclerView mRecyclerView;
    private ConversationAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    //FireBase
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference usersReference = db.collection("Users");
    private ListenerRegistration userConversationsListener;

    private ArrayList<Conversation> conversationList = new ArrayList<>();

    private DocumentSnapshot mLastQueriedDocument;

    public AllMessagesFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null)
                parent.removeView(view);
        }
        try {
            view = inflater.inflate(R.layout.fragment_all_messages, container, false);
        } catch (InflateException e) {
            Log.e(TAG, "onCreateView: ", e);
        }

        progressBar = view.findViewById(R.id.allmessages_pb);
        mRecyclerView = view.findViewById(R.id.allMessages_recycler_view);

        BottomNavigationView navBar = getActivity().findViewById(R.id.bottom_navigation);
        navBar.setVisibility(View.GONE);

        FloatingActionButton openSelectFriend = view.findViewById(R.id.allMessages_addConversation_fab);
        openSelectFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(view).navigate(AllMessagesFragmentDirections.actionAllMessagesFragmentToSelectFriendToOpenConverstationFragment());
            }
        });

        MaterialButton backBtn = view.findViewById(R.id.allMessages_backBtn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(view).navigate(AllMessagesFragmentDirections.actionAllMessagesFragmentToFeedFragment());
            }
        });

        buildRecyclerView();


        return view;
    }


    @Override
    public void onStop() {
        super.onStop();
        if (userConversationsListener != null) {
            userConversationsListener.remove();
            userConversationsListener = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        performQuery();
    }

    private void buildRecyclerView() {
        Log.d(TAG, "buildRecyclerView: ");
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getContext());

        mAdapter = new ConversationAdapter(getContext(), conversationList);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(AllMessagesFragment.this);
    }


    private void performQuery() {
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
                            progressBar.setVisibility(View.GONE);
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
                        mAdapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onItemClick(int position) {
        String id = conversationList.get(position).getUserId();
        Log.d(TAG, "onItemClick: CLICKED " + " id " + id);

        Navigation.findNavController(view).navigate(AllMessagesFragmentDirections.actionAllMessagesFragmentToMessageFragment(id));
    }

    @Override
    public void onDeleteClick(final int position) {
        final Conversation conversation = conversationList.get(position);

        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(getActivity()
                , R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog_Centered);
        materialAlertDialogBuilder.setMessage("You will lose all the messages with this user. Are you sure you want to delete this conversation? ");
        materialAlertDialogBuilder.setCancelable(true);
        materialAlertDialogBuilder.setPositiveButton(
                "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //Delete conversation
                        usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("Conversations")
                                .document(conversation.getUserId()).collection(conversation.getUserId()).get()
                                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                    @Override
                                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                        WriteBatch batch = db.batch();
                                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                            batch.delete(documentSnapshot.getReference());
                                        }
                                        batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("Conversations")
                                                        .document(conversation.getUserId()).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        Toast.makeText(getContext(), "Deleted conversation", Toast.LENGTH_SHORT).show();
                                                        conversationList.remove(position);
                                                        mAdapter.notifyItemRemoved(position);
                                                        mAdapter.notifyItemChanged(position);
                                                    }
                                                });
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

    @Override
    public void onPictureClick(int position) {
        final Conversation conversation = conversationList.get(position);
        final ConversationViewDialog conversationViewDialog = new ConversationViewDialog();
        Glide.with(Objects.requireNonNull(getActivity())).asBitmap().load(conversation.getUser_profilePic()).apply(RequestOptions.circleCropTransform())
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        conversationViewDialog.showDialog(getActivity(), conversation.getUser_name(), conversation.getUserId(), resource, view);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
    }
}
