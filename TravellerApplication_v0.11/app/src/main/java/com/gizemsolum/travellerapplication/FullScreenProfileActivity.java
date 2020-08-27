package com.gizemsolum.travellerapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

public class FullScreenProfileActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    String myUid;

    ImageView avatarIv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_profile);

        firebaseAuth = FirebaseAuth.getInstance();
        checkUserStatus();

        avatarIv = findViewById(R.id.avatarIv);

        Intent intent = getIntent();
        String image = intent.getStringExtra("image");

        try {

            Picasso.get().load(image).into(avatarIv);

        }catch (Exception e){

            Picasso.get().load(R.drawable.ic_default_img_white).into(avatarIv);
            System.out.println("Catch inside FullScreenProfileActivity onCreate: "+e.toString());

        }

    }

    private void checkUserStatus(){

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null){

            //myEmail = user.getEmail();
            myUid = user.getUid();

        }else {

            startActivity(new Intent(this,MainActivity.class));
            finish();

        }

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

}