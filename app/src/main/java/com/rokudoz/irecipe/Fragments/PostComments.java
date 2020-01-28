package com.rokudoz.irecipe.Fragments;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.rokudoz.irecipe.Models.Comment;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.Adapters.PostParentCommentAdapter;

import java.util.ArrayList;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 */
public class PostComments extends Fragment {
    private static final String TAG = "PostComments";

    private String documentID ="";
    private String loggedInUserId ="";

    private MaterialButton addCommentBtn;
    private TextInputEditText commentTextInput;

    private RecyclerView commentRecyclerView;
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

    private View view;

    public PostComments() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_post_comments, container, false);

        PostCommentsArgs postCommentsArgs = PostCommentsArgs.fromBundle(getArguments());
        documentID = postCommentsArgs.getDocumentID();
        loggedInUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        commentRecyclerView = view.findViewById(R.id.commentFragment_recycler_view);
        addCommentBtn = view.findViewById(R.id.commentFragment_send_MaterialBtn);
        commentTextInput = view.findViewById(R.id.commentFragment_input_TextInput);


        addCommentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addComment();
            }
        });


        buildRecyclerView();
        getCommentsFromDb();
        return view;
    }

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

    private void buildRecyclerView() {
        commentRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getContext());
        mAdapter = new PostParentCommentAdapter(getContext(), commentList);
        commentRecyclerView.setLayoutManager(mLayoutManager);
        commentRecyclerView.setAdapter(mAdapter);
    }

    private void addComment() {
        String commentText = Objects.requireNonNull(commentTextInput.getText()).toString();

        final Comment comment = new Comment(documentID, FirebaseAuth.getInstance().getCurrentUser().getUid(), commentText, null);
        DocumentReference currentRecipeRef = postsRef.document(documentID);
        CollectionReference commentRef = currentRecipeRef.collection("Comments");

        commentRef.add(comment)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        commentTextInput.setText("");
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


}
