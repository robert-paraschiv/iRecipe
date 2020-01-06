package com.rokudoz.irecipe.Fragments;


import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.ArraySet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.rokudoz.irecipe.Account.LoginActivity;
import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.Activity.RESULT_OK;

public class ShoppingListFragment extends Fragment {

    private static final String TAG = "ShoppingListFragment";
    private ProgressBar pbLoading;
    private ListView cbListView;

    private String userDocumentID = "";

    private List<String> ingredientList;
    private String[] ingStringArray;
    private ArrayList<Ingredient> shoppingListIngredients = new ArrayList<>();
    private List<String> userIngredientList;

    Map<String, Boolean> ingredientsUserHas = new HashMap<>();

    //Firebase
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference usersReference = db.collection("Users");
    private CollectionReference ingredientsReference = db.collection("Ingredients");


    public static ShoppingListFragment newInstance() {
        ShoppingListFragment fragment = new ShoppingListFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shopping_list, container, false);
//        textViewData = view.findViewById(R.id.tv_data);
        cbListView = view.findViewById(R.id.checkable_list);


        userIngredientList = new ArrayList<>();

        setupFirebaseAuth();
        getUserInfo();

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

    private void getIngredientList() {
        ingredientsReference.document("ingredient_list")
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@javax.annotation.Nullable DocumentSnapshot documentSnapshot, @javax.annotation.Nullable FirebaseFirestoreException e) {
                        if (e == null) {
                            ingredientList = (List<String>) documentSnapshot.get("ingredient_list");
                            ingStringArray = ingredientList.toArray(new String[ingredientList.size()]);

                        }
                    }
                });
    }

    private void getUserInfo() {
        usersReference.whereEqualTo("user_id", FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e == null) {
                            String data = "";

                            User user = queryDocumentSnapshots.getDocuments().get(0).toObject(User.class);
                            userDocumentID = queryDocumentSnapshots.getDocuments().get(0).getId();
                            userIngredientList = user.getIngredient_array();

                            for (String tag : user.getTags().keySet()) {
                                data += "\n " + tag + " " + user.getTags().get(tag);
                                ingredientsUserHas.put(tag, Objects.requireNonNull(user.getTags().get(tag)));
                            }
                            Log.d(TAG, "onEvent: " + ingredientsUserHas.toString());


                            getUserShoppingList();
                            getIngredientList();
                        }
                    }
                });
    }

    private void getUserShoppingList() {
        usersReference.document(userDocumentID).collection("ShoppingList").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                if (queryDocumentSnapshots != null) {
                    for (QueryDocumentSnapshot querySnapshot : queryDocumentSnapshots) {
                        Ingredient ingredient = querySnapshot.toObject(Ingredient.class);
                        shoppingListIngredients.add(ingredient);
                    }
                    setupCheckList();
                }
            }
        });
    }


    private void setupCheckList() {
        //create an instance of ListView
        //set multiple selection mode
        if (getActivity() != null) {
            cbListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

            final List<String> checkBoxItemList = new ArrayList<>();

            for (Ingredient ingredient : shoppingListIngredients) {
                checkBoxItemList.add(ingredient.getName() + " " + ingredient.getQuantity() +" "+ ingredient.getQuantity_type());
            }

            //supply data items to ListView
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getContext(), R.layout.checkable_list_layout, R.id.txt_title, checkBoxItemList);
            cbListView.setAdapter(arrayAdapter);

            //When the Ingredients List has more elements than the UserTAGS(IngredientsUserHas), initialize elements as false to avoid crash
            for (String ing : ingStringArray) {
                if (!userIngredientList.contains(ing)) {
                    ingredientsUserHas.put(ing, false);
                }
            }

            // sets the initial checkbox values taken from database
            int index = 0;
            for (String item : checkBoxItemList) {
                cbListView.setItemChecked(index, shoppingListIngredients.get(index).getOwned());
                Log.d(TAG, item + " index " + index + " value " + shoppingListIngredients.get(index).getOwned());
                index++;
            }

            cbListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // gets checkBox value and updates database with it
                    Map<String, Boolean> selectedIngredientsMap = new HashMap<>();
                    if (userIngredientList == null) {
                        userIngredientList = new ArrayList<>();
                    }
                    int i = 0;
                    for (String tag : checkBoxItemList) {
                        selectedIngredientsMap.put(tag, cbListView.isItemChecked(i));
                        if (userIngredientList != null && userIngredientList.contains(tag) && !cbListView.isItemChecked(i)) {
                            userIngredientList.remove(tag);
                        } else if (userIngredientList != null && !userIngredientList.contains(tag) && cbListView.isItemChecked(i)) {
                            userIngredientList.add(tag);
                        }

                        i++;
                    }

                    db.collection("Users").document(userDocumentID)
                            .update("tags", selectedIngredientsMap);

                    db.collection("Users").document(userDocumentID)
                            .update("ingredient_array", userIngredientList);
                }

            });
        }
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
