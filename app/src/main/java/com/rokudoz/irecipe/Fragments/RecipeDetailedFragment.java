package com.rokudoz.irecipe.Fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rokudoz.irecipe.R;
import com.squareup.picasso.Picasso;

public class RecipeDetailedFragment extends Fragment {
    private static final String TAG = "RecipeDetailedFragment";

    private static final String ARG_ID = "argId";
    private static final String ARG_TITLE = "argTitle";
    private static final String ARG_DESCRIPTION = "argDescription";
    private static final String ARG_IMAGEURL = "argImageurl";
    private static final String ARG_INGREDIENTS = "argIngredients";


    private TextView tvTitle, tvDescription, tvIngredients;
    private ImageView mImageView;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference recipesRef = db.collection("Recipes");

    public static RecipeDetailedFragment newInstance(String id, String title,String description, String ingredients,String imageUrl) {
        RecipeDetailedFragment fragment = new RecipeDetailedFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, id);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESCRIPTION, description);
        args.putString(ARG_IMAGEURL,imageUrl);
        args.putString(ARG_INGREDIENTS,ingredients);

        fragment.setArguments(args);
        return fragment;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recipe_detailed, container, false);

        tvTitle = view.findViewById(R.id.tvTitle);
        tvDescription = view.findViewById(R.id.tvDescription);
        tvIngredients = view.findViewById(R.id.tvIngredientsList);
        mImageView = view.findViewById(R.id.recipeDetailed_image);

        if (getArguments() != null) {


            String documentID = getArguments().getString(ARG_ID);
            String title = getArguments().getString(ARG_TITLE);
            String description = getArguments().getString(ARG_DESCRIPTION);
            String imageUrl = getArguments().getString(ARG_IMAGEURL);
            String ingredients = getArguments().getString(ARG_INGREDIENTS);



            tvTitle.setText(title);
            tvDescription.setText(description);
            tvIngredients.setText(ingredients);

            Picasso.get()
                    .load(imageUrl)
                    .fit()
                    .centerCrop()
                    .into(mImageView);
        }

        return view; // HAS TO BE THE LAST ONE ---------------------------------
    }
}
