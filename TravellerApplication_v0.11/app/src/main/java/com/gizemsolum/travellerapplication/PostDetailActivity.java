package com.gizemsolum.travellerapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.blogspot.atifsoftwares.circularimageview.CircularImageView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class PostDetailActivity extends AppCompatActivity {

    String myUid, myEmail, myName, myDp, postId, pLikes, pImage, hisDp, hisName, hisUid;

    boolean mProcessComment = false;
    boolean mProcessLike = false;

    ProgressDialog pd;
    ImageView uPictureIv, pImageIv;
    TextView uNameTv, pTimeTv, pTtileTv, pDescriptionTv, pLikesTv, pCommentsTv;
    CircularImageView commentAvatarIv;
    EditText commentEt;
    ImageButton sendButton, moreButton;
    Button likeButton, shareButton;
    LinearLayout profileLayout;
    RecyclerView recyclerView_comments;

    List<ModelComment> commentList;
    AdapterComments adapterComments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Post Detail");
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");

        uPictureIv = findViewById(R.id.uPictureIv);
        pImageIv = findViewById(R.id.pImageIv);
        uNameTv = findViewById(R.id.uNameTv);
        pTimeTv = findViewById(R.id.pTimeTv);
        pTtileTv = findViewById(R.id.pTitleTv);
        pDescriptionTv = findViewById(R.id.profileDescriptionTv);
        pLikesTv = findViewById(R.id.pLikesTv);
        pCommentsTv = findViewById(R.id.pCommentsTv);
        moreButton = findViewById(R.id.moreButton);
        likeButton = findViewById(R.id.likeButton);
        shareButton = findViewById(R.id.shareButton);
        profileLayout = findViewById(R.id.profileLayout);
        recyclerView_comments = findViewById(R.id.recyclerview_comments);

        commentAvatarIv = findViewById(R.id.commentAvatarIv);
        commentEt = findViewById(R.id.commentEt);
        sendButton = findViewById(R.id.sendButton);

        loadPostInfo();
        checkUserStatus();
        loadUserInfo();
        setLikes();

        actionBar.setSubtitle("SignedIn as: "+myEmail);

        loadComments();

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                postComment();

            }
        });

        likeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                likePost();

            }
        });

        moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showMoreOptions();

            }
        });

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String pTitle = pTtileTv.getText().toString().trim();
                String pDescription = pDescriptionTv.getText().toString().trim();

                BitmapDrawable bitmapDrawable = (BitmapDrawable) pImageIv.getDrawable();
                if (bitmapDrawable == null){

                    shareTextOnly(pTitle, pDescription);

                }else{

                    Bitmap bitmap = bitmapDrawable.getBitmap();
                    shareImageandText(pTitle, pDescription, bitmap);

                }

            }
        });

    }

    private void shareTextOnly(String pTitle, String pDescription) {

        String shareBody = pTitle +"\n"+ pDescription;

        Intent sIntent = new Intent(Intent.ACTION_SEND);
        sIntent.setType("text/plain");
        sIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here");
        sIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sIntent, "Share Via"));

    }

    private void shareImageandText(String pTitle, String pDescription, Bitmap bitmap) {

        String shareBody = pTitle +"\n"+ pDescription;

        Uri uri = saveImagetoShare(bitmap);

        Intent sIntent = new Intent(Intent.ACTION_SEND);
        sIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here");
        sIntent.setType("image/png");
        startActivity(Intent.createChooser(sIntent,"Share Via"));

    }

    private Uri saveImagetoShare(Bitmap bitmap) {

        File imageFolder = new File(getCacheDir(),"images");
        Uri uri = null;

        try {

            imageFolder.mkdirs();
            File file = new File(imageFolder, "shared_image.png");

            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG,90, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(this,"com.gizemsolum.travellerapplication.fileprovider",file);

        }catch (Exception e){

            Toast.makeText(PostDetailActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            System.out.println("Error PostDetailActivity at line 198 to shareFileError: "+e.getMessage());

        }

        return uri;

    }

    private void loadComments() {

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView_comments.setLayoutManager(layoutManager);
        commentList = new ArrayList<>();

        DatabaseReference firebaseDatabase = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments");
        firebaseDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                commentList.clear();
                for (DataSnapshot ds:snapshot.getChildren()){

                    ModelComment modelComment = ds.getValue(ModelComment.class);
                    commentList.add(modelComment);



                }
                adapterComments = new AdapterComments(getApplicationContext(), commentList, myUid, postId);
                recyclerView_comments.setAdapter(adapterComments);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void showMoreOptions() {

        PopupMenu popupMenu = new PopupMenu(this, moreButton, Gravity.END);

        if (hisUid.equals(myUid)){

            popupMenu.getMenu().add(Menu.NONE, 0, 0, "Delete");
            popupMenu.getMenu().add(Menu.NONE, 1, 0, "Edit");

        }

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {

                int id = menuItem.getItemId();
                if (id == 0){

                    beginDelete();

                }else if (id == 1){

                    Intent intent = new Intent(PostDetailActivity.this,AddPostActivity.class);;
                    intent.putExtra("key","editPost");
                    intent.putExtra("editPostId",postId);
                    startActivity(intent);

                }

                return false;
            }
        });

        popupMenu.show();

    }

    private void beginDelete() {

        if (pImage.equals("noImage")){

            deleteWithoutImage();

        }else{

            deleteWithImage();

        }

    }

    private void deleteWithImage() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete");
        builder.setMessage("Are you Sure to Delete this data");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                Query fquery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(postId);
                fquery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()){

                            dataSnapshot1.getRef().removeValue();

                        }

                        Toast.makeText(PostDetailActivity.this, "Deleted succesfully..", Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        ///
                    }
                });

            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                dialogInterface.dismiss();
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }

    private void deleteWithoutImage() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete");
        builder.setMessage("Are you Sure to Delete this data");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                Query fquery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(postId);
                fquery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()){

                            dataSnapshot1.getRef().removeValue();

                        }

                        Toast.makeText(PostDetailActivity.this, "Deleted succesfully..", Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        ///
                    }
                });

            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                dialogInterface.dismiss();
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }

    private void setLikes() {

        final DatabaseReference likesReference = FirebaseDatabase.getInstance().getReference().child("Likes");

        likesReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.child(postId).hasChild(myUid)){

                    likeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_liked,0,0,0);
                    likeButton.setText("Liked");

                }else{

                    likeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_black,0,0,0);
                    likeButton.setText("Like");

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }

    private void likePost() {

        mProcessLike = true;
        //get id of the post clicked
        final DatabaseReference likesReference = FirebaseDatabase.getInstance().getReference().child("Likes");
        final DatabaseReference postsReference = FirebaseDatabase.getInstance().getReference().child("Posts");
        likesReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (mProcessLike){

                    if (snapshot.child(postId).hasChild(myUid)){

                        //already liked, so remove like
                        postsReference.child(postId).child("pLikes").setValue(""+(Integer.parseInt(pLikes)-1));
                        likesReference.child(postId).child(myUid).removeValue();
                        mProcessLike = false;

                    }else{

                        //not liked, like it
                        postsReference.child(postId).child("pLikes").setValue(""+(Integer.parseInt(pLikes)+1));
                        likesReference.child(postId).child(myUid).setValue("Liked");
                        mProcessLike = false;

                    }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }

    private void postComment() {

        pd = new ProgressDialog(this);
        pd.setMessage("Adding Comment...");

        String comment = commentEt.getText().toString().trim();

        if (TextUtils.isEmpty(comment)){

            Toast.makeText(this, "Comment is empty...", Toast.LENGTH_SHORT).show();
            return;

        }
        String timeStamplate = String.valueOf(System.currentTimeMillis());

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments");

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("cId",timeStamplate);
        hashMap.put("comment",comment);
        hashMap.put("timestamplate",timeStamplate);
        hashMap.put("uid",myUid);
        hashMap.put("uEmail",myEmail);
        hashMap.put("uDp",myDp);
        hashMap.put("uName",myName);

        databaseReference.child(timeStamplate).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        pd.dismiss();
                        Toast.makeText(PostDetailActivity.this, "Comment Added...", Toast.LENGTH_SHORT).show();
                        commentEt.setText("");
                        updateCommentCount();

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                pd.dismiss();
                Toast.makeText(PostDetailActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                System.out.println("Error onFailure at line 141 of PostDetailActivity" +e.getMessage());

            }
        });

    }

    private void updateCommentCount() {

        mProcessComment = true;
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts").child(postId);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (mProcessComment){

                    String comments = ""+snapshot.child("pComments").getValue();
                    int newCommentValue = Integer.parseInt(comments) + 1;
                    databaseReference.child("pComments").setValue(""+newCommentValue);
                    mProcessComment = false;

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void loadUserInfo() {

        Query query = FirebaseDatabase.getInstance().getReference("Users");
        query.orderByChild("uid").equalTo(myUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for (DataSnapshot ds : snapshot.getChildren()){

                    myName = ""+ds.child("name").getValue();
                    myDp = ""+ds.child("image").getValue();

                    try{
                        if (!myDp.equals("noImage") && !myDp.equals("")){
                        Picasso.get().load(myDp).placeholder(R.drawable.ic_default_img).into(commentAvatarIv);}

                    }catch (Exception e){

                        Picasso.get().load(R.drawable.ic_default_img).into(commentAvatarIv);
                        System.out.println("Error PostDetailActivity at line 99" +e.getMessage());

                    }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void loadPostInfo() {

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");
        Query query = databaseReference.orderByChild("pId").equalTo(postId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                //keep checking the posts until get the required post
                for (DataSnapshot ds : snapshot.getChildren()){

                    String pTitle = ""+ds.child("pTitle").getValue().toString();
                    String pDescription = ""+ds.child("pDescription").getValue().toString();
                    pLikes = ""+ds.child("pLikes").getValue().toString();
                    String pTimeStamplate = ""+ds.child("pTime").getValue().toString();
                    pImage = ""+ds.child("pImage").getValue().toString();
                    hisDp = ""+ds.child("uDp").getValue().toString();
                    hisUid = ""+ds.child("uid").getValue().toString();
                    String uEmail = ""+ds.child("uEmail").getValue().toString();
                    hisName = ""+ds.child("uName").getValue().toString();
                    String commentCount = ""+ds.child("pComments").getValue().toString();

                    Calendar calendar = Calendar.getInstance(Locale.getDefault());
                    calendar.setTimeInMillis(Long.parseLong(pTimeStamplate));
                    String pTime = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();

                    pTtileTv.setText(pTitle);
                    System.out.println("PostDetailActivity at 482 line pTitle" + pTitle);
                    pDescriptionTv.setText(pDescription);
                    System.out.println("PostDetailActivity at 484 line pDescription" + pDescription);
                    pLikesTv.setText(pLikes +" Likes");
                    System.out.println("PostDetailActivity at 486 line pLikes" + pLikes);
                    pTimeTv.setText(pTime);
                    pCommentsTv.setText(commentCount+" Comments");

                    uNameTv.setText(hisName);

                    if (pImage.equals("noImage") || pImage.equals("")){

                        pImageIv.setVisibility(View.GONE);

                    }else{

                        pImageIv.setVisibility(View.VISIBLE);

                        try{

                            Picasso.get().load(pImage).into(pImageIv);

                        }catch(Exception e){

                            System.out.println("error PostDetailActivity at line 121 " +e.getMessage());

                        }
                    }

                        try{
                            if (!hisDp.equals("noImage") && !hisDp.equals("")){
                            Picasso.get().load(hisDp).placeholder(R.drawable.ic_default_img).into(uPictureIv);}

                        }catch(Exception e){

                            Picasso.get().load(R.drawable.ic_default_img).into(uPictureIv);
                            System.out.println("error PostDetailActivity at line 131 " +e.getMessage());

                        }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {



            }
        });

    }

    private void checkUserStatus(){

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null){

            myEmail = user.getEmail();
            myUid = user.getUid();

        }else {

            startActivity(new Intent(PostDetailActivity.this,MainActivity.class));
            finish();

        }

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main,menu);
        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);

        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_logout){

            FirebaseAuth.getInstance().signOut();
            checkUserStatus();

        }
        return super.onOptionsItemSelected(item);

    }

}