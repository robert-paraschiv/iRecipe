package com.rokudoz.irecipe.Utils.Adapters;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.rokudoz.irecipe.R;

import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;
import static com.rokudoz.irecipe.SearchRecipeActivity.FILTER_PREFS_NAME;

public class SearchFilterDialog extends AppCompatDialogFragment {
    private MaterialCheckBox breakfast, lunch, dinner, fixIngredients;
    private SearchFilterDialogListener searchFilterDialogListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(FILTER_PREFS_NAME, MODE_PRIVATE);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_search_filter, null);

        breakfast = view.findViewById(R.id.dialog_search_filter_breakfastCheckBox);
        lunch = view.findViewById(R.id.dialog_search_filter_lunchCheckBox);
        dinner = view.findViewById(R.id.dialog_search_filter_dinnerCheckBox);
        fixIngredients = view.findViewById(R.id.dialog_search_filter_fixIngredientsCheckBox);

        breakfast.setChecked(sharedPreferences.getBoolean("Breakfast", false));
        lunch.setChecked(sharedPreferences.getBoolean("Lunch", false));
        dinner.setChecked(sharedPreferences.getBoolean("Dinner", false));
        fixIngredients.setChecked(sharedPreferences.getBoolean("FixIngredients", false));

        builder.setView(view)
                .setTitle("Select your filters")
                .setPositiveButton("Apply", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Boolean breakfastBool = breakfast.isChecked();
                        Boolean lunchBool = lunch.isChecked();
                        Boolean dinnerBool = dinner.isChecked();
                        Boolean fixIngredientsBool = fixIngredients.isChecked();
                        searchFilterDialogListener.applyFilter(breakfastBool, lunchBool, dinnerBool, fixIngredientsBool);
                    }
                });


        return builder.create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            searchFilterDialogListener = (SearchFilterDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + "must implement SearchFilterDialogListener");
        }
    }

    public interface SearchFilterDialogListener {
        void applyFilter(Boolean breakfast, Boolean lunch, Boolean dinner, Boolean fixIngredients);
    }
}
