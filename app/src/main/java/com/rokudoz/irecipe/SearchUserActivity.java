package com.rokudoz.irecipe;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.rokudoz.irecipe.Fragments.ProfileFragmentDirections;
import com.rokudoz.irecipe.Models.Friend;
import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.Utils.Adapters.FriendAdapter;
import com.rokudoz.irecipe.Utils.Adapters.SearchRecipeAdapter;
import com.rokudoz.irecipe.Utils.Adapters.SearchUserAdapter;

import java.util.ArrayList;
import java.util.List;

public class SearchUserActivity extends AppCompatActivity implements SearchUserAdapter.OnItemClickListener {
    private static final String TAG = "SearchUserActivity";

    //FireBase
    private FirebaseAuth.AuthStateListener mAuthListener;
    private User mUser;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference recipeRef = db.collection("Recipes");
    private CollectionReference usersReference = db.collection("Users");
    private ListenerRegistration userDetailsListener, currentSubCollectionListener, recipesListener;
    private DocumentSnapshot mLastQueriedDocument;

    private List<String> userFavRecipesList = new ArrayList<>();
    private List<String> friends_userID_list = new ArrayList<>();
    private List<Friend> friendList = new ArrayList<>();
    private String loggedInUserDocumentId = "";
    private String userFavDocId = "";

    private List<User> usersList = new ArrayList<>();

    private RecyclerView mRecyclerView;
    private SearchUserAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_user);

        MaterialToolbar myToolbar = (MaterialToolbar) findViewById(R.id.searchUserActivity_my_toolbar);
        myToolbar.setTitle("Search User");
        setSupportActionBar(myToolbar);


        getCurrentUserDetails();
    }


    private void getCurrentUserDetails() {
        usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                List<Ingredient> userIngredient_list = new ArrayList<>();
                User user = documentSnapshot.toObject(User.class);

                mUser = documentSnapshot.toObject(User.class);
                loggedInUserDocumentId = documentSnapshot.getId();
                if (!friends_userID_list.contains(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                    friends_userID_list.add(FirebaseAuth.getInstance().getCurrentUser().getUid());
                }

                usersReference.document(user.getUser_id()).collection("FriendList").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots) {
                            Friend friend = queryDocumentSnapshot.toObject(Friend.class);
                            if (!friendList.contains(friend)) {
                                if (friend.getFriend_status().equals("friends") || friend.getFriend_status().equals("friend_request_accepted"))
                                    friendList.add(friend);
                            }
                        }
                        for (Friend friend : friendList) {
                            if (!friends_userID_list.contains(friend.getFriend_user_id()))
                                friends_userID_list.add(friend.getFriend_user_id());
                        }
                        performQuery();

                    }
                });
            }
        });
    }


    private void performQuery() {

        usersReference.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "onEvent: ", e);
                    return;
                }
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    User user = document.toObject(User.class);
                    if (!usersList.contains(user)) {
                        usersList.add(user);
                    }
                }
                if (queryDocumentSnapshots.getDocuments().size() != 0) {
                    mLastQueriedDocument = queryDocumentSnapshots.getDocuments()
                            .get(queryDocumentSnapshots.getDocuments().size() - 1);
                }
                buildRecyclerView();
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    private void buildRecyclerView() {
        mRecyclerView = findViewById(R.id.searchUserActivity_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);

        mAdapter = new SearchUserAdapter(usersList);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(this);

        initializeRecyclerViewAdapterOnClicks();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.search_recipe_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mAdapter.getFilter().filter(newText);
                return false;
            }

        });

        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showInputMethod(v.findFocus());
                }
            }
        });

        return true;
    }

    private void showInputMethod(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, 0);
        }
    }

    private void initializeRecyclerViewAdapterOnClicks() {
        mAdapter.setOnItemClickListener(new SearchUserAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                String id = usersList.get(position).getUser_id();
                Log.d(TAG, "onItemClick: CLICKED " + " id " + id);

                Intent intent = new Intent(SearchUserActivity.this,MainActivity.class);
                intent.putExtra("user_id",id);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onItemClick(int position) {

    }
}
