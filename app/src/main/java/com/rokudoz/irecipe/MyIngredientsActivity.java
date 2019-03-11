package com.rokudoz.irecipe;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.rokudoz.irecipe.Models.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MyIngredientsActivity extends AppCompatActivity {

    private TextView textViewData;

    private Boolean hasPotatoes=false;
    private Boolean hasCheese=false;
    private Boolean hasApples=false;
    private Boolean hasSalt=false;

    private String documentID = "";
    ArrayList<String> selectedIngredients;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference usersReference = db.collection("Users");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_ingredients);

        selectedIngredients = new ArrayList<String>();
        textViewData = findViewById(R.id.tv_data);

        getDocumentId();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //setupCheckList();
        retrieveSavedIngredients();

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

    public void saveMyIngredients() {

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
        if (!selectedIngredients.isEmpty()) {
            db.collection("Users").document(documentID)
                    .update("tags", ingredientsHashMap);
        } else {
            Map<String, Boolean>emptyIngredientsHashMap=new HashMap<>();
            emptyIngredientsHashMap.put("noTags",true);
            db.collection("Users").document(documentID)
                    .update("tags", emptyIngredientsHashMap);
        }

    }

    private void retrieveSavedIngredients() {
        final Map<String, Boolean> ingredientsHashMap = new HashMap<>();

        usersReference.whereEqualTo("user_id", FirebaseAuth.getInstance().getCurrentUser().getUid()).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        String data = "";

                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            User user = documentSnapshot.toObject(User.class);
                            for (String tag : user.getTags().keySet()) {
                                data = ""+ tag;
                                if (tag.equals("potatoes")) {
                                    hasPotatoes=true;
                                    data+="yah ";
                                }
                                if (tag.equals("cheese")) {
                                    hasCheese=true;
                                }
                                if (tag.equals("apples")) {
                                    hasApples=true;
                                }
                                if (tag.equals("salt")) {
                                    hasSalt=true;
                                }

                            }
                        }
                        textViewData.setText(data);

                        setupCheckList();
                    }
                });


    }


    private void setupCheckList() {
        //create an instance of ListView
        ListView cbListView = findViewById(R.id.checkable_list);
        //set multiple selection mode
        cbListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        String[] items = {"potatoes", "cheese", "apples", "salt"};
        //supply data items to ListView
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.checkable_list_layout, R.id.txt_title, items);
        cbListView.setAdapter(arrayAdapter);
        //set OnItemClickListener
        cbListView.setItemChecked(0, hasPotatoes);
        cbListView.setItemChecked(1, hasCheese);
        cbListView.setItemChecked(2, hasApples);
        cbListView.setItemChecked(3, hasSalt);

        cbListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // selected item
                String selectedItem = ((TextView) view).getText().toString();
                if (selectedIngredients.contains(selectedItem)) {
                    selectedIngredients.remove(selectedItem); //remove deselected item from the list of selected items
                } else {
                    selectedIngredients.add(selectedItem); //add selected item to the list of selected items
                }
                saveMyIngredients();
            }

        });

    }

    public void backToSearch(View v) {
        Intent intent = new Intent(this, SearchActivity.class);
        startActivity(intent);
    }
}