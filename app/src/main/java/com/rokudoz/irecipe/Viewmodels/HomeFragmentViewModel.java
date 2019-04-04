package com.rokudoz.irecipe.Viewmodels;

import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Repositories.RecipeRepository;

import java.util.ArrayList;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeFragmentViewModel extends ViewModel {
    private MutableLiveData<ArrayList<Recipe>> mRecipes;
    private RecipeRepository mRepo;

    public void init(){
        if (mRecipes!=null){
            return;
        }
        mRepo = RecipeRepository.getInstance();
        mRecipes = mRepo.getRecipes();
    }

    public LiveData<ArrayList<Recipe>> getRecipes(){
        return mRecipes;
    }
}

