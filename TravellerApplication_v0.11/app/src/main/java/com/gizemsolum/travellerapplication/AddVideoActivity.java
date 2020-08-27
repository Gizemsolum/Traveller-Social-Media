package com.gizemsolum.travellerapplication;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

public class AddVideoActivity extends AppCompatActivity {

    private static final int PICK_VIDEO = 1;
    private  static final int PERMISSION_STORAGE_CODE = 1000;

    VideoView videoview;
    Button button;
    ProgressBar progressBar;
    ProgressDialog pd;
    EditText editText;
    ActionBar actionBar;

    private Uri videoUri;
    MediaController mediaController;
    String name, email, uid, dp;
    Uri image_uri = null;

    FirebaseAuth firebaseAuth;
    StorageReference storageReference;
    DatabaseReference databaseReference;

    ModelVideo modelVideo;

    String editVideoId,editVideoName,editVideoUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_video);

        actionBar = getSupportActionBar();
        actionBar.setTitle("Add New Video");
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        modelVideo = new ModelVideo();

        videoview = findViewById(R.id.videoview_main);
        button = findViewById(R.id.Uploadbutton);
        progressBar = findViewById(R.id.progressBarVideo);
        editText = findViewById(R.id.et_video_name);
        mediaController = new MediaController(this);
        videoview.setMediaController(mediaController);
        videoview.start();

        pd = new ProgressDialog(this);

        firebaseAuth = FirebaseAuth.getInstance();
        checkUserStatus();

        Intent intent = getIntent();
        String isUpdatedKey = ""+intent.getStringExtra("key");
        String editVideoUrl = ""+intent.getStringExtra("editVideoUrl");
        if (isUpdatedKey.equals("editVideo")){

            actionBar.setTitle("Update Video");
            button.setText("Update");
            loadVideoData(editVideoId);

        }else{

            actionBar.setTitle("Add New Video");
            button.setText("Upload");

        }

        actionBar.setSubtitle(email);

        databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        Query query = databaseReference.orderByChild("email").equalTo(email);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for (DataSnapshot ds : snapshot.getChildren()) {

                    name = "" + ds.child("name").getValue();
                    email = "" + ds.child("email").getValue();
                    dp = "" + ds.child("image").getValue();

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (isUpdatedKey.equals("editVideo")){

                    beginUpdate(editVideoId);

                }else{

                    UploadVideo();

                }

            }
        });

    }

    private void beginUpdate(String editVideoId) {

        String videoName = editText.getText().toString();
        pd.setMessage("Updating Post...");
        pd.show();

        StorageReference mVideoReference = FirebaseStorage.getInstance().getReferenceFromUrl(editVideoUrl);
        mVideoReference.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        String timestamplate = String.valueOf(System.currentTimeMillis());
                        String filePathAndName = "Posts/" + "post_" + timestamplate + "." + getExt(videoUri);

                        //getVideo from VideoView
                        //https://stackoverflow.com/questions/15203491/get-bitmap-of-videoview
                        videoview.buildDrawingCache();

                        Bitmap bitmap = videoview.getDrawingCache();
                        ByteArrayOutputStream stream=new ByteArrayOutputStream();

                        bitmap.compress(Bitmap.CompressFormat.WEBP, 100, stream);

                        byte[] videoByteArray = stream.toByteArray();
                        //String video_str = Base64.encodeToString(videoByteArray, 0);

                        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(filePathAndName);
                        storageReference.putBytes(videoByteArray)
                                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                                        while (!uriTask.isSuccessful());
                                        String downloadUri = uriTask.getResult().toString();
                                        if (uriTask.isSuccessful()){

                                            HashMap<String, Object> hashMap = new HashMap<>();
                                            hashMap.put("uid", uid);
                                            hashMap.put("uName", name);
                                            hashMap.put("uEmail", email);
                                            hashMap.put("uDp", dp);
                                            hashMap.put("Videoname", videoName);
                                            hashMap.put("Videourl", downloadUri);

                                            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");
                                            databaseReference.child(editVideoId)
                                                    .updateChildren(hashMap)
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {

                                                            pd.dismiss();
                                                            Toast.makeText(AddVideoActivity.this, "Updated...", Toast.LENGTH_SHORT).show();

                                                        }
                                                    }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {

                                                    pd.dismiss();
                                                    Toast.makeText(AddVideoActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    System.out.println("Error addOnFailureListener of beginUpdate at AddVideoActivity " +e.getMessage());

                                                }
                                            });

                                        }

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                                pd.dismiss();
                                Toast.makeText(AddVideoActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                System.out.println("Second Error addOnFailureListener of beginUpdate at AddVideoActivity " +e.getMessage());

                            }
                        });

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                pd.dismiss();
                Toast.makeText(AddVideoActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                System.out.println("Third Error addOnFailureListener of beginUpdate at AddVideoActivity " +e.getMessage());

            }
        });


    }

    private void loadVideoData(String editVideoId) {

        progressBar.setVisibility(View.VISIBLE);
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");
        Query fquery = databaseReference.orderByChild("VideoId").equalTo(editVideoId);
        fquery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for (DataSnapshot ds : snapshot.getChildren()) {

                            editVideoName = "" + ds.child("Videoname").getValue();
                            editVideoUrl = "" + ds.child("Videourl").getValue();

                            editText.setText(editVideoName);
                            videoview.setVideoPath(editVideoUrl);

                        }

                }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    public void ChooseVideo(View view) {

        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent,PICK_VIDEO);

    }

    @Override
    protected void onStart() {
        super.onStart();
        checkUserStatus();
    }

    private String getExt(Uri uri){
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return  mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void UploadVideo() {

        String videoName = editText.getText().toString();
        String search = editText.getText().toString().toLowerCase();
        if (videoUri != null && !TextUtils.isEmpty(videoName)) {

            progressBar.setVisibility(View.VISIBLE);
            final String timestamplate = String.valueOf(System.currentTimeMillis());

            String filePathAndName = "Posts/" + "post_" + timestamplate + "." + getExt(videoUri);

            //getVideo from VideoView
            //https://stackoverflow.com/questions/15203491/get-bitmap-of-videoview
            videoview.buildDrawingCache();

            if(videoview.getDrawingCache() != null) {

                Bitmap bitmap = videoview.getDrawingCache();
                ByteArrayOutputStream stream=new ByteArrayOutputStream();

                bitmap.compress(Bitmap.CompressFormat.WEBP, 100, stream);

                byte[] videoByteArray = stream.toByteArray();
                //String video_str = Base64.encodeToString(videoByteArray, 0);

                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(filePathAndName);
                storageReference.putBytes(videoByteArray)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                                while (!uriTask.isSuccessful()) ;

                                String downloadUri = uriTask.getResult().toString();
                                if (uriTask.isSuccessful()) {

                                    HashMap<Object, String> hashMap = new HashMap<>();
                                    hashMap.put("uid", uid);
                                    hashMap.put("uName", name);
                                    hashMap.put("uEmail", email);
                                    hashMap.put("uDp", dp);
                                    hashMap.put("VideoId",timestamplate);
                                    hashMap.put("Videoname", videoName);
                                    hashMap.put("vLikes","0");
                                    hashMap.put("vComments","0");
                                    hashMap.put("search",search);
                                    hashMap.put("Videourl", downloadUri);
                                    hashMap.put("vTime", timestamplate);

                                    //path to store post data
                                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");
                                    databaseReference.child(timestamplate).setValue(hashMap)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {

                                                    Toast.makeText(AddVideoActivity.this, "Data saved", Toast.LENGTH_SHORT).show();
                                                    //reset views
                                                    editText.setText("");
                                                    videoview.setVideoPath(null);
                                                    videoUri = null;

                                                }

                                            }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {

                                            Toast.makeText(AddVideoActivity.this, "Failed", Toast.LENGTH_SHORT).show();

                                        }
                                    });

                                }
                            }

                        });

                }

            }else {

                Toast.makeText(AddVideoActivity.this, "All Fields are required", Toast.LENGTH_SHORT).show();

            }
    }

    private void checkUserStatus(){

        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user != null){

            email = user.getEmail();
            uid = user.getUid();

        }else {

            startActivity(new Intent(AddVideoActivity.this,MainActivity.class));
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

            firebaseAuth.signOut();
            checkUserStatus();

        }

        return super.onOptionsItemSelected(item);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_VIDEO || resultCode == RESULT_OK ||
                data != null || data.getData() != null ){
            videoUri = data.getData();
            videoview.setVideoURI(videoUri);
        }

    }

}


