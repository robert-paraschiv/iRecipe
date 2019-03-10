package com.rokudoz.irecipe;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.rokudoz.irecipe.Account.LoginActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SearchActivity extends AppCompatActivity {
    private static final String TAG = "SearchActivity";

    //Firebase
    private FirebaseAuth.AuthStateListener mAuthListener;

    private TextView textViewData;
    ArrayList<String> selectedIngredients;//-----

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = db.collection("Recipes");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        selectedIngredients = new ArrayList<String>();

        textViewData = findViewById(R.id.text_view_data);

        setupFirebaseAuth();

    }

    public void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener(mAuthListener);

        //create an instance of ListView
        ListView cbListView = findViewById(R.id.checkable_list);
        //set multiple selection mode
        cbListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        String[] items = {"potatoes", "cheese", "apples", "salt"};
        //supply data itmes to ListView
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.checkable_list_layout, R.id.txt_title, items);
        cbListView.setAdapter(arrayAdapter);
        //set OnItemClickListener
        cbListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // selected item
                String selectedItem = ((TextView) view).getText().toString();
                if (selectedIngredients.contains(selectedItem))
                    selectedIngredients.remove(selectedItem); //remove deselected item from the list of selected items
                else
                    selectedIngredients.add(selectedItem); //add selected item to the list of selected items

            }

        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(mAuthListener);
        }
    }

    public void signOut(View v){
        FirebaseAuth.getInstance().signOut();
    }

    public void searchRecipes(View v) {

        String ingredientsString = "";
        for (String ingredient : selectedIngredients) {
            if (ingredientsString == "") {
                ingredientsString = ingredient;
            } else {
                ingredientsString += "," + ingredient;
            }
        }

        String[] ingredientsArray = ingredientsString.split("\\s*,\\s*");
        Map<String, Boolean> ingredientsHashMap = new HashMap<>();

        for (String tag : ingredientsArray) {
            ingredientsHashMap.put(tag, true);
        }

        collectionReference.whereEqualTo("tags", ingredientsHashMap).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        String data = "";

                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            com.rokudoz.irecipe.Recipe recipe = documentSnapshot.toObject(com.rokudoz.irecipe.Recipe.class);
                            recipe.setDocumentId(documentSnapshot.getId());

                            String documentId = recipe.getDocumentId();
                            String title = recipe.getTitle();

                            data += "ID: " + documentId + "\nTitle: " + title;

                            for (String tag : recipe.getTags().keySet()) {
                                data += "\n-" + tag;
                            }

                            data += "\n\n";
                        }
                        textViewData.setText(data);
                    }
                });
    }

    public void navigateToAddRecipes(View v) {
        Intent intent = new Intent(this, AddRecipesActivity.class);
        startActivity(intent);
    }


    /*
        ----------------------------- Firebase setup ---------------------------------
     */
    private void setupFirebaseAuth(){
        Log.d(TAG, "setupFirebaseAuth: started");

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {

                    //check if email is verified
                    if(user.isEmailVerified()){
//                        Log.d(TAG, "onAuthStateChanged: signed_in: " + user.getUid());
//                        Toast.makeText(SearchActivity.this, "Authenticated with: " + user.getEmail(), Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(SearchActivity.this, "Email is not Verified\nCheck your Inbox", Toast.LENGTH_SHORT).show();
                        FirebaseAuth.getInstance().signOut();
                    }

                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged: signed_out");
                    Toast.makeText(SearchActivity.this, "Not logged in", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SearchActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                }
                // ...
            }
        };
    }
}