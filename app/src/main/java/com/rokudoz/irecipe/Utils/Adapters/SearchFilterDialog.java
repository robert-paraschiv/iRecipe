package com.rokudoz.irecipe.Utils.Adapters;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.rokudoz.irecipe.R;

import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;
import static com.rokudoz.irecipe.SearchRecipeActivity.FILTER_PREFS_NAME;

public class SearchFilterDialog {
    private MaterialCheckBox breakfast, lunch, dinner, fixIngredients;
    private MaterialButton applyButton;


    public void showDialog(final Activity activity) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.dialog_search_filter);

        SharedPreferences sharedPreferences = activity.getSharedPreferences(FILTER_PREFS_NAME, MODE_PRIVATE);
        breakfast = dialog.findViewById(R.id.dialog_search_filter_breakfastCheckBox);
        lunch = dialog.findViewById(R.id.dialog_search_filter_lunchCheckBox);
        dinner = dialog.findViewById(R.id.dialog_search_filter_dinnerCheckBox);
        fixIngredients = dialog.findViewById(R.id.dialog_search_filter_fixIngredientsCheckBox);
        applyButton = dialog.findViewById(R.id.dialog_search_filter_applyButton);

        breakfast.setChecked(sharedPreferences.getBoolean("Breakfast", false));
        lunch.setChecked(sharedPreferences.getBoolean("Lunch", false));
        dinner.setChecked(sharedPreferences.getBoolean("Dinner", false));
        fixIngredients.setChecked(sharedPreferences.getBoolean("FixIngredients", false));

        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final SharedPreferences.Editor sharedPrefsEditor = activity.getSharedPreferences(FILTER_PREFS_NAME, MODE_PRIVATE).edit();

                sharedPrefsEditor.putBoolean("Breakfast", breakfast.isChecked());
                sharedPrefsEditor.putBoolean("Lunch", lunch.isChecked());
                sharedPrefsEditor.putBoolean("Dinner", dinner.isChecked());
                sharedPrefsEditor.putBoolean("FixIngredients", fixIngredients.isChecked());
                sharedPrefsEditor.apply();
                dialog.cancel();
            }
        });

        dialog.show();

    }
}
