package com.rokudoz.irecipe.Utils.Adapters;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.formats.MediaView;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.R;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;


public class RecipeWithAdsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final int RECIPE_ITEM_VIEW_TYPE = 0;
    private final int UNIFIED_NATIVE_AD_VIEW_TYPE = 1;
    private List<Object> itemList;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(int position);

        void onFavoriteClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public class RecipeWithAdsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView tvTitle, tvDescription, tvNrOfFaves, tvNumMissingIngredients, tvNumComments, tvCreatorName;
        ImageView mImageView, imgFavorited, imgPrivacy;
        CircleImageView imgCreatorPic;

        public RecipeWithAdsViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.text_view_title);
            tvDescription = itemView.findViewById(R.id.text_view_description);
            mImageView = itemView.findViewById(R.id.recipeItem_image);
            imgFavorited = itemView.findViewById(R.id.recyclerview_favorite);
            tvNrOfFaves = itemView.findViewById(R.id.recyclerview_nrOfFaves_textView);
            imgPrivacy = itemView.findViewById(R.id.recycler_view_privacy);
            tvNumMissingIngredients = itemView.findViewById(R.id.recycler_view_recipeItem_missingIngredients);
            tvNumComments = itemView.findViewById(R.id.recycler_view_recipeItem_nrOfComments_textView);
            tvCreatorName = itemView.findViewById(R.id.recipeItem_creator_name_textView);
            imgCreatorPic = itemView.findViewById(R.id.recipeItem_creator_image);

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

    public RecipeWithAdsAdapter(List<Object> itemList) {
        this.itemList = itemList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case UNIFIED_NATIVE_AD_VIEW_TYPE:
                View unifiedNativeLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.ad_unified, parent, false);
                return new UnifiedNativeAdViewHolder(unifiedNativeLayoutView);
            case RECIPE_ITEM_VIEW_TYPE:

            default:
                View postItemLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_layout_recipe_item, parent, false);
                return new RecipeWithAdsViewHolder(postItemLayoutView);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (itemList.get(position) instanceof Recipe) {
            return RECIPE_ITEM_VIEW_TYPE;
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
            case RECIPE_ITEM_VIEW_TYPE:
                RecipeWithAdsViewHolder recipeViewHolder = (RecipeWithAdsViewHolder) holder;
                Recipe currentItem = (Recipe) itemList.get(position);

                recipeViewHolder.tvTitle.setText(currentItem.getTitle());
                recipeViewHolder.tvDescription.setText(currentItem.getDescription());

                if (currentItem.getCreator_name() != null) {
                    recipeViewHolder.tvCreatorName.setText(currentItem.getCreator_name());
                }
                if (currentItem.getCreator_imageUrl() != null && !currentItem.getCreator_imageUrl().equals("")) {
                    Glide.with(recipeViewHolder.imgCreatorPic).load(currentItem.getCreator_imageUrl()).centerCrop().into(recipeViewHolder.imgCreatorPic);
                }

                Glide.with(recipeViewHolder.mImageView).load(currentItem.getImageUrls_list().get(0)).centerCrop().into(recipeViewHolder.mImageView);

                if (currentItem.getNumber_of_likes() != null) {
                    recipeViewHolder.tvNrOfFaves.setText("" + currentItem.getNumber_of_likes());
                }
                if (currentItem.getMissingIngredients() != null) {
                    if (currentItem.getNrOfMissingIngredients() == 0) {
                        recipeViewHolder.tvNumMissingIngredients.setVisibility(View.GONE);
                    } else {
                        StringBuilder missingIngredients = new StringBuilder("Missing ingredients: ");
                        for (int i = 0; i < currentItem.getMissingIngredients().size(); i++) {
                            if (i == currentItem.getMissingIngredients().size() - 1) {
                                missingIngredients.append(currentItem.getMissingIngredients().get(i));
                            } else
                                missingIngredients.append(currentItem.getMissingIngredients().get(i)).append(", ");
                        }
                        recipeViewHolder.tvNumMissingIngredients.setVisibility(View.VISIBLE);
                        recipeViewHolder.tvNumMissingIngredients.setText(missingIngredients.toString());
                    }
                } else
                    recipeViewHolder.tvNumMissingIngredients.setVisibility(View.GONE);

                if (currentItem.getPrivacy().equals("Everyone")) {
                    recipeViewHolder.imgPrivacy.setVisibility(View.GONE);
                }

                recipeViewHolder.imgFavorited.setImageResource(R.drawable.ic_favorite_border_black_24dp);
                if (currentItem.getFavorite() != null && currentItem.getFavorite()) {
                    recipeViewHolder.imgFavorited.setImageResource(R.drawable.ic_favorite_red_24dp);
                }
                if (currentItem.getNumber_of_comments() != null) {
                    recipeViewHolder.tvNumComments.setText("" + currentItem.getNumber_of_comments());
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
            ((CircleImageView) adView.getIconView()).setImageDrawable(icon.getDrawable());
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

    @Override
    public int getItemCount() {
        return itemList.size();
    }
}