package com.gizemsolum.travellerapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

public class FullScreenBannerActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    String myUid;

    ImageView coverIv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_banner);

        firebaseAuth = FirebaseAuth.getInstance();
        checkUserStatus();

        coverIv = findViewById(R.id.coverIv);

        Intent intent = getIntent();
        String cover = intent.getStringExtra("cover");

        try {

            Picasso.get().load(cover).into(coverIv);

        }catch (Exception e){

            System.out.println("Catch inside FullScreenBannerActivity onCreate: "+e.toString());

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