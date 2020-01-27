package com.rokudoz.irecipe.Utils.Adapters;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.rokudoz.irecipe.Models.Post;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;


public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    private static final String TAG = "PostAdapter";
    private List<Post> post_List;
    private OnItemClickListener mListener;

    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;


    public interface OnItemClickListener {
        void onItemClick(int position);

        void onFavoriteClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public class PostViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView tvDescription, tvNrOfFaves, creatorName, creationDate;
        ImageView mImageView, imgFavorited;
        CircleImageView creatorImage;

        public PostViewHolder(View itemView) {
            super(itemView);
            tvDescription = itemView.findViewById(R.id.postItem_description_text_view);
            mImageView = itemView.findViewById(R.id.postItem_image);
            imgFavorited = itemView.findViewById(R.id.recycler_view_postItem_favorite);
            tvNrOfFaves = itemView.findViewById(R.id.recycler_view_postItem_nrOfFaves_textView);
            creatorName = itemView.findViewById(R.id.postItem_creator_name_textView);
            creatorImage = itemView.findViewById(R.id.postItem_creator_image);
            creationDate = itemView.findViewById(R.id.postItem_creationDate_text_view);

            itemView.setOnClickListener(this);

            imgFavorited.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            mListener.onFavoriteClick(position);
                        }
                    }
                }
            });
        }

        @Override
        public void onClick(View v) {
            if (mListener != null) {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    mListener.onItemClick(position);
                }
            }
        }
    }

    public PostAdapter(List<Post> postList) {
        post_List = postList;
    }

    @Override
    public PostViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_layout_post_item, parent, false);
        return new PostViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final PostViewHolder holder, int position) {
        final Post currentItem = post_List.get(position);

        holder.tvDescription.setText(currentItem.getText());
        if (!currentItem.getImageUrl().equals("")) {

            Glide.with(holder.mImageView.getContext()).load(currentItem.getImageUrl()).centerCrop().into(holder.mImageView);
        }

        if (currentItem.getCreation_date() != null) {
            Date date = currentItem.getCreation_date();
            if (date != null) {
                DateFormat dateFormat = new SimpleDateFormat("HH:mm, d MMM", Locale.getDefault());
                String creationDate = dateFormat.format(date);
                long time = date.getTime();
                if (currentItem.getCreation_date() != null && !currentItem.getCreation_date().equals("")) {
                    holder.creationDate.setText(getTimeAgo(time));
                }
            }
        }

        if (currentItem.getDocumentId() != null && !currentItem.getDocumentId().equals("")) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            CollectionReference currentRecipeSubCollection = db.collection("Posts").document(currentItem.getDocumentId())
                    .collection("UsersWhoFaved");

            db.collection("Users").document(currentItem.getCreatorId()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                    if (e != null) {
                        Log.w(TAG, "onEvent: ", e);
                        return;
                    }
                    User user = documentSnapshot.toObject(User.class);
                    holder.creatorName.setText(user.getName());
                    Glide.with(holder.creatorImage).load(user.getUserProfilePicUrl()).centerCrop().into(holder.creatorImage);
                }
            });

            currentRecipeSubCollection.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                    if (queryDocumentSnapshots != null)
                        holder.tvNrOfFaves.setText("" + queryDocumentSnapshots.size());

                }
            });

            if (currentItem.getFavorite() != null) {
                boolean fav = currentItem.getFavorite();
                if (fav)
                    holder.imgFavorited.setImageResource(R.drawable.ic_favorite_red_24dp);
                else
                    holder.imgFavorited.setImageResource(R.drawable.ic_favorite_border_black_24dp);
            }
        }

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
    public int getItemCount() {
        return post_List.size();
    }
}