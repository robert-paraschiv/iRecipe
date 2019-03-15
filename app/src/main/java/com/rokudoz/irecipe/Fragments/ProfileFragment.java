package com.rokudoz.irecipe.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.rokudoz.irecipe.Account.LoginActivity;
import com.rokudoz.irecipe.Models.PossibleIngredients;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";

    private TextView textViewData, tvHelloUserName;
    private ProgressBar pbLoading;
    private ListView cbListView;
    private Button signOutBtn;
    private CircleImageView mProfileImage;

    private Boolean querrySucceeded = false;

    private String documentID = "";

    Map<String, Boolean> ingredientsUserHas = new HashMap<>();

    //Firebase
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference usersReference = db.collection("Users");


    public static ProfileFragment newInstance() {
        ProfileFragment fragment = new ProfileFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        textViewData = view.findViewById(R.id.tv_data);
        cbListView = view.findViewById(R.id.checkable_list);
        pbLoading = view.findViewById(R.id.pbLoading);
        pbLoading.setVisibility(View.VISIBLE);
        signOutBtn = view.findViewById(R.id.profileFragment_signOut);
        tvHelloUserName = view.findViewById(R.id.profileFragment_helloUser_textview);
        mProfileImage = view.findViewById(R.id.profileFragment_profileImage);

        getDocumentId();
        setupFirebaseAuth();
        retrieveSavedIngredients();

        Picasso.get()
                .load("https://firebasestorage.googleapis.com/v0/b/irecipe.appspot.com/o/RecipePhotos%2F1552491481106.jpg?alt=media&token=873c295f-e2be-4080-8835-f9e0144a2666")
                .fit()
                .centerCrop()
                .into(mProfileImage);

        signOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });

        return view;
    }


    @Override
    public void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(mAuthListener);
        }
    }

    private void getDocumentId() {
        usersReference.whereEqualTo("user_id", FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e == null) {
                            documentID = queryDocumentSnapshots.getDocuments().get(0).getId();
                        }
                    }
                });
    }

    private void retrieveSavedIngredients() {

        usersReference.whereEqualTo("user_id", FirebaseAuth.getInstance().getCurrentUser().getUid()).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        String data = "";

                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            User user = documentSnapshot.toObject(User.class);
                            for (String tag : user.getTags().keySet()) {
                                data += "\n " + tag + " " + user.getTags().get(tag);
                                ingredientsUserHas.put(tag, Objects.requireNonNull(user.getTags().get(tag)));
                            }

                            tvHelloUserName.setText(String.format("Hello, %s", user.getName()));
                        }
                        textViewData.setText(data);

                        setupCheckList();
                        pbLoading.setVisibility(View.INVISIBLE);

                    }
                });


    }


    private void setupCheckList() {
        //create an instance of ListView
        //set multiple selection mode
        cbListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        final String[] items = PossibleIngredients.getIngredientsNames();
        //supply data items to ListView
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getContext(), R.layout.checkable_list_layout, R.id.txt_title, items);
        cbListView.setAdapter(arrayAdapter);

        // sets the initial checkbox values taken from database
        int index = 0;
        for (String item : items) {
            cbListView.setItemChecked(index, Objects.requireNonNull(ingredientsUserHas.get(item)));
            Log.d(TAG, item + " index " + index + " value " + ingredientsUserHas.get(item));
            index++;
        }

        cbListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // gets checkBox value and updates database with it
                Map<String, Boolean> selectedIngredientsMap = new HashMap<>();
                int i = 0;
                for (String tag : items) {
                    selectedIngredientsMap.put(tag, cbListView.isItemChecked(i));
                    i++;
                }

                db.collection("Users").document(documentID)
                        .update("tags", selectedIngredientsMap);
            }

        });

    }

    public void signOut() {
        FirebaseAuth.getInstance().signOut();
    }

    /*
    ----------------------------- Firebase setup ---------------------------------
 */
    private void setupFirebaseAuth() {
        Log.d(TAG, "setupFirebaseAuth: started");

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {

                    //check if email is verified
                    if (user.isEmailVerified()) {
//                        Log.d(TAG, "onAuthStateChanged: signed_in: " + user.getUid());
//                        Toast.makeText(MainActivity.this, "Authenticated with: " + user.getEmail(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Email is not Verified\nCheck your Inbox", Toast.LENGTH_SHORT).show();
                        FirebaseAuth.getInstance().signOut();
                    }

                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged: signed_out");
                    Toast.makeText(getContext(), "Not logged in", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    getActivity().finish();
                }
                // ...
            }
        };
    }
}
