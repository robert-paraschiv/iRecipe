package com.rokudoz.irecipe.Utils.Adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.formats.MediaView;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;
import com.rokudoz.irecipe.Models.Post;
import com.rokudoz.irecipe.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class FeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private final int POST_ITEM_VIEW_TYPE = 0;
    private final int UNIFIED_NATIVE_AD_VIEW_TYPE = 1;
    private OnItemClickListener mListener;


    public interface OnItemClickListener {
        void onItemClick(int position);

        void onFavoriteClick(int position);

        void onCommentClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }


    //get time ago
    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;


    private List<Object> itemList;

    public FeedAdapter(Context context, List<Object> itemList) {
        this.context = context;
        this.itemList = itemList;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case UNIFIED_NATIVE_AD_VIEW_TYPE:
                View unifiedNativeLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.ad_unified, parent, false);
                return new UnifiedNativeAdViewHolder(unifiedNativeLayoutView);
            case POST_ITEM_VIEW_TYPE:

            default:
                View postItemLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_layout_post_item, parent, false);
                return new PostItemViewHolder(postItemLayoutView);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (itemList.get(position) instanceof Post) {
            return POST_ITEM_VIEW_TYPE;
        } else {
            return UNIFIED_NATIVE_AD_VIEW_TYPE;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        switch (viewType) {
            case UNIFIED_NATIVE_AD_VIEW_TYPE:
                UnifiedNativeAd unifiedNativeAd = (UnifiedNativeAd) itemList.get(position);
                populateNativeAdView(unifiedNativeAd, ((UnifiedNativeAdViewHolder) holder).getAdView());
                break;
            case POST_ITEM_VIEW_TYPE:
                PostItemViewHolder postItemViewHolder = (PostItemViewHolder) holder;
                Post currentItem = (Post) itemList.get(position);


                postItemViewHolder.tvDescription.setText(currentItem.getText());
                if (!currentItem.getImageUrl().equals("")) {

                    Glide.with(postItemViewHolder.mImageView.getContext()).load(currentItem.getImageUrl()).centerCrop().into(postItemViewHolder.mImageView);
                }

                if (currentItem.getCreation_date() != null) {
                    Date date = currentItem.getCreation_date();
                    if (date != null) {
                        DateFormat dateFormat = new SimpleDateFormat("HH:mm, d MMM", Locale.getDefault());
                        String creationDate = dateFormat.format(date);
                        long time = date.getTime();
                        if (currentItem.getCreation_date() != null && !currentItem.getCreation_date().equals("")) {
                            postItemViewHolder.creationDate.setText(getTimeAgo(time));
                        }
                    }
                }

                if (currentItem.getFavorite() != null) {
                    boolean fav = currentItem.getFavorite();
                    if (fav)
                        postItemViewHolder.imgFavorited.setImageResource(R.drawable.ic_favorite_red_24dp);
                    else
                        postItemViewHolder.imgFavorited.setImageResource(R.drawable.ic_favorite_border_black_24dp);
                }
                if (currentItem.getCreator_name() != null) {
                    postItemViewHolder.creatorName.setText(currentItem.getCreator_name());
                }
                if (currentItem.getCreator_imageUrl() != null && !currentItem.getCreator_imageUrl().equals("")) {
                    Glide.with(postItemViewHolder.creatorImage).load(currentItem.getCreator_imageUrl()).centerCrop().into(postItemViewHolder.creatorImage);
                }
                if (currentItem.getNumber_of_likes() != null) {
                    postItemViewHolder.tvNrOfFaves.setText("" + currentItem.getNumber_of_likes());
                }
                if (currentItem.getNumber_of_comments() != null) {
                    postItemViewHolder.tvNumberOfComments.setText("" + currentItem.getNumber_of_comments());
                }
                if (currentItem.getRecipe_name() != null && !currentItem.getRecipe_name().equals("")) {
                    postItemViewHolder.recipeNameTv.setText("Recipe: " + currentItem.getRecipe_name());
                }
                if (currentItem.getRecipe_imageUrl() != null && !currentItem.getRecipe_imageUrl().equals("")) {
//            Glide.with(holder.recipeImage).load(currentItem.getRecipe_imageUrl()).centerCrop().into(holder.recipeImage);
                }

        }

    }

    private void populateNativeAdView(UnifiedNativeAd unifiedNativeAd, UnifiedNativeAdView adView) {
        ((TextView) adView.getHeadlineView()).setText(unifiedNativeAd.getHeadline());
        ((TextView) adView.getBodyView()).setText(unifiedNativeAd.getBody());
        ((TextView) adView.getCallToActionView()).setText(unifiedNativeAd.getCallToAction());

        NativeAd.Image icon = unifiedNativeAd.getIcon();
        if (icon == null) {
            adView.getIconView().setVisibility(View.GONE);
        } else {
            ((ImageView) adView.getIconView()).setImageDrawable(icon.getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }
        if (unifiedNativeAd.getPrice() == null) {
            adView.getPriceView().setVisibility(View.GONE);
        } else {
            ((TextView) adView.getPriceView()).setText(unifiedNativeAd.getPrice());
            adView.getPriceView().setVisibility(View.VISIBLE);
        }
        if (unifiedNativeAd.getStore() == null) {
            adView.getStoreView().setVisibility(View.GONE);
        } else {
            ((TextView) adView.getStoreView()).setText(unifiedNativeAd.getStore());
            adView.getStoreView().setVisibility(View.VISIBLE);
        }
        if (unifiedNativeAd.getStarRating() == null) {
            adView.getStarRatingView().setVisibility(View.GONE);
        } else {
            ((RatingBar) adView.getStarRatingView()).setRating(unifiedNativeAd.getStarRating().floatValue());
            adView.getStarRatingView().setVisibility(View.VISIBLE);
        }
        if (unifiedNativeAd.getAdvertiser() == null) {
            adView.getAdvertiserView().setVisibility(View.GONE);
        } else {
            ((TextView) adView.getAdvertiserView()).setText(unifiedNativeAd.getAdvertiser());
            adView.getAdvertiserView().setVisibility(View.VISIBLE);
        }

        adView.setNativeAd(unifiedNativeAd);

    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public class PostItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView tvDescription, tvNrOfFaves, creatorName, creationDate, tvNumberOfComments, recipeNameTv;
        ImageView mImageView, imgFavorited, imgComment;
        CircleImageView creatorImage, recipeImage;

        public PostItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDescription = itemView.findViewById(R.id.postItem_description_text_view);
            mImageView = itemView.findViewById(R.id.postItem_image);
            imgFavorited = itemView.findViewById(R.id.recycler_view_postItem_favorite);
            tvNrOfFaves = itemView.findViewById(R.id.recycler_view_postItem_nrOfFaves_textView);
            tvNumberOfComments = itemView.findViewById(R.id.recycler_view_postItem_nrOfComments_textView);
            creatorName = itemView.findViewById(R.id.postItem_creator_name_textView);
            creatorImage = itemView.findViewById(R.id.postItem_creator_image);
            creationDate = itemView.findViewById(R.id.postItem_creationDate_text_view);
            imgComment = itemView.findViewById(R.id.postItem_comment);
            recipeNameTv = itemView.findViewById(R.id.recycler_view_postItem_recipeName);
            recipeImage = itemView.findViewById(R.id.recycler_view_postItem_recipeImage);

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
            imgComment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mListener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            mListener.onCommentClick(position);
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

    private class UnifiedNativeAdViewHolder extends RecyclerView.ViewHolder {
        private UnifiedNativeAdView adView;

        public UnifiedNativeAdView getAdView() {
            return adView;
        }

        public UnifiedNativeAdViewHolder(@NonNull View itemView) {
            super(itemView);
            adView = itemView.findViewById(R.id.ad_native);

            adView.setMediaView((MediaView) adView.findViewById(R.id.ad_media));
            adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
            adView.setBodyView(adView.findViewById(R.id.ad_body));
            adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
            adView.setIconView(adView.findViewById(R.id.ad_icon));
            adView.setPriceView(adView.findViewById(R.id.ad_price));
            adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
            adView.setStoreView(adView.findViewById(R.id.ad_store));
            adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));

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
}
