package com.rokudoz.irecipe.Fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rokudoz.irecipe.Account.LoginActivity;
import com.rokudoz.irecipe.R;

import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;
import static com.rokudoz.irecipe.App.SETTINGS_PREFS_NAME;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends Fragment {
    private static final String TAG = "SettingsFragment";

    //Firebase
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference usersReference = db.collection("Users");


    private View view;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_settings, container, false);

        //NIGHT MODE SETTINGS
        final SharedPreferences.Editor sharedPrefsEditor = Objects.requireNonNull(this.getActivity()).getSharedPreferences(SETTINGS_PREFS_NAME, MODE_PRIVATE).edit();

        RadioGroup appThemeRadioGroup = view.findViewById(R.id.settings_appTheme_radioGroup);
        MaterialButton signOutBtn = view.findViewById(R.id.settings_logOut);

        SharedPreferences sharedPreferences = this.getActivity().getSharedPreferences(SETTINGS_PREFS_NAME, MODE_PRIVATE);
        int darkMode = sharedPreferences.getInt("NightMode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        switch (darkMode) {
            case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:
                appThemeRadioGroup.check(R.id.dark_mode_follow_system);
                break;
            case AppCompatDelegate.MODE_NIGHT_NO:
                appThemeRadioGroup.check(R.id.dark_mode_light);
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                appThemeRadioGroup.check(R.id.dark_mode_dark);
                break;
        }

        appThemeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.dark_mode_follow_system:
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                        sharedPrefsEditor.putInt("NightMode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                        sharedPrefsEditor.apply();
                        break;
                    case R.id.dark_mode_light:
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        sharedPrefsEditor.putInt("NightMode", AppCompatDelegate.MODE_NIGHT_NO);
                        sharedPrefsEditor.apply();
                        break;
                    case R.id.dark_mode_dark:
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        sharedPrefsEditor.putInt("NightMode", AppCompatDelegate.MODE_NIGHT_YES);
                        sharedPrefsEditor.apply();
                        break;

                }
            }
        });

        signOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });


        ///////////
        return view;
    }

    private void signOut() {
        usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).update("user_tokenID", "")
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "onSuccess: updated token to null");
                        FirebaseAuth.getInstance().signOut();
                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        startActivity(intent);
                        getActivity().finish();
                    }
                });
    }
}

