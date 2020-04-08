package com.rokudoz.irecipe.Utils;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.rokudoz.irecipe.R;

public class ConversationViewDialog {

    public void showDialog(Activity activity, String userName, final String userId, String userPicUrl, final View view) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.dialog_conversation);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        TextView text = dialog.findViewById(R.id.conversationDialog_user_name);
        text.setText(userName);

        ImageView imageView = dialog.findViewById(R.id.conversationDialog_user_picture);
        Glide.with(activity).load(userPicUrl).centerInside().into(imageView);

        Button dialogButton = dialog.findViewById(R.id.conversationDialog_viewProfileBtn);
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle args = new Bundle();
                args.putString("documentID", userId);
                Navigation.findNavController(view).navigate(R.id.userProfileFragment2, args);
                dialog.dismiss();
            }
        });

        dialog.show();

    }
}
