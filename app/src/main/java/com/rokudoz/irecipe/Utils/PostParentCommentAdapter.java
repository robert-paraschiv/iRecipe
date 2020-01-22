package com.rokudoz.irecipe.Utils;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.rokudoz.irecipe.Fragments.PostDetailedFragmentDirections;
import com.rokudoz.irecipe.Models.Comment;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import javax.annotation.Nullable;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostParentCommentAdapter
        extends RecyclerView.Adapter<PostParentCommentAdapter.CommentViewHolder>
        implements View.OnClickListener {

    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;


    private static final String TAG = "RecipeParentCommentAdapter";
    private Integer expandedPosition = -1;
    private ArrayList<Comment> mCommentList;
    private PostChildCommentAdapter childAdapter;
    private User mUser;
    Context ctx;

    public class CommentViewHolder extends RecyclerView.ViewHolder {
        public CircleImageView mImageView;
        public TextView mName;
        public TextView mCommentText;
        public TextView mCommentTimeStamp;
        public MaterialButton mAddReplyBtn;
        public TextInputEditText mReplyText;
        RelativeLayout llExpandArea;
        RecyclerView rv_child;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.comment_rv_profile_img);
            mName = itemView.findViewById(R.id.comment_rv_tv_name);
            mCommentText = itemView.findViewById(R.id.comment_rv_tv_comment_text);
            mCommentTimeStamp = itemView.findViewById(R.id.comment_rv_time_created);
            llExpandArea = itemView.findViewById(R.id.llExpandArea);
            rv_child = itemView.findViewById(R.id.comment_rv_childRecyclerView);
            mAddReplyBtn = itemView.findViewById(R.id.comment_rv_addReply_btn);
            mReplyText = itemView.findViewById(R.id.comment_rv_reply_editText);
        }
    }

    public PostParentCommentAdapter(Context ctx, ArrayList<Comment> commentArrayList) {
        this.ctx = ctx;
        mCommentList = commentArrayList;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_layout_parent_comment, parent, false);
        CommentViewHolder cvh = new CommentViewHolder(v);
        mUser = new User();
        getCurrentUserDetails();
        cvh.itemView.setOnClickListener(PostParentCommentAdapter.this);
        cvh.itemView.setTag(cvh);
        return cvh;
    }

    @Override
    public void onClick(View v) {
        CommentViewHolder holder = (CommentViewHolder) v.getTag();
        String commentID = mCommentList.get(holder.getAdapterPosition()).getmCommentText();

        if (holder.llExpandArea.getVisibility() == View.VISIBLE) {
            holder.llExpandArea.setVisibility(View.GONE);

//            notifyItemChanged(holder.getAdapterPosition());
        } else {
            holder.llExpandArea.setVisibility(View.VISIBLE);
        }
        Log.d(TAG, "onClick: " + commentID + holder.llExpandArea.getVisibility());
    }

    @Override
    public void onBindViewHolder(@NonNull final CommentViewHolder holder, int position) {
        final Comment currentItem = mCommentList.get(position);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Users").document(currentItem.getmUserId()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                User user = documentSnapshot.toObject(User.class);
                if (user.getUserProfilePicUrl() != null) {
                    Picasso.get()
                            .load(user.getUserProfilePicUrl())
                            .fit()
                            .centerCrop()
                            .into(holder.mImageView);
                    holder.mName.setText(user.getName());
                }
            }
        });

        holder.mCommentText.setText(currentItem.getmCommentText());

        Date date = currentItem.getmCommentTimeStamp();
        if (date != null) {
            DateFormat dateFormat = new SimpleDateFormat("HH:mm, MMM d", Locale.getDefault());
            String creationDate = dateFormat.format(date);
            long time = date.getTime();
            if (currentItem.getmCommentTimeStamp() != null && !currentItem.getmCommentTimeStamp().equals("")) {
                holder.mCommentTimeStamp.setText(getTimeAgo(time));
            }
        }


        holder.mAddReplyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String replyText = holder.mReplyText.getText().toString();
                FirebaseFirestore db = FirebaseFirestore.getInstance();

                if (!replyText.trim().equals("")) {
                    Comment comment = new Comment(currentItem.getmRecipeDocumentId(), mUser.getUser_id(), mUser.getUserProfilePicUrl()
                            , mUser.getName(), replyText, null);
                    db.collection("Posts").document(currentItem.getmRecipeDocumentId()).collection("Comments")
                            .document(currentItem.getDocumentId()).collection("ChildComments").add(comment)
                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                @Override
                                public void onSuccess(DocumentReference documentReference) {
                                    Toast.makeText(ctx, "Succesfully Added Reply", Toast.LENGTH_SHORT).show();
                                    holder.mReplyText.setText("");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(ctx, "Failed to add Reply", Toast.LENGTH_SHORT).show();
                                }
                            });
                }

            }
        });

        holder.mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(PostDetailedFragmentDirections.actionPostDetailedToUserProfileFragment2(currentItem.getmUserId()));
            }
        });

        //Initialize Child Comment RecyclerView
        initchildLayout(holder.rv_child, getChildComments(currentItem));
    }

    private void getCurrentUserDetails() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Users").document(FirebaseAuth.getInstance().getCurrentUser().getUid()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                mUser = documentSnapshot.toObject(User.class);
            }
        });

    }

    private ArrayList<Comment> getChildComments(Comment comment) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final ArrayList<Comment> childComments = new ArrayList<>();
        final ArrayList<String> childcommentID = new ArrayList<>();
        db.collection("Posts").document(comment.getmRecipeDocumentId()).collection("Comments")
                .document(comment.getDocumentId()).collection("ChildComments").orderBy("mCommentTimeStamp", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "onEvent: ", e);
                            return;
                        }
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            Comment commentToAdd = document.toObject(Comment.class);
                            if (!childcommentID.contains(document.getId())) {
                                childcommentID.add(document.getId());
                                childComments.add(0, commentToAdd);
                            }

                        }
                        childAdapter.notifyDataSetChanged();
                    }
                });
//        childAdapter.notifyDataSetChanged();
        return childComments;

    }


    private void initchildLayout(RecyclerView rv_child, ArrayList<Comment> childData) {
        rv_child.setLayoutManager(new LinearLayoutManager(ctx));
        childAdapter = new PostChildCommentAdapter(childData);
        rv_child.setAdapter(childAdapter);
        rv_child.setHasFixedSize(true);
    }


    public static String getTimeAgo(long time) {
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

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mCommentList.size();
    }
}
