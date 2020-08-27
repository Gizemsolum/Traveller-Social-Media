package com.gizemsolum.travellerapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.FileProvider;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
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
import java.util.List;

public class ThereProfileActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    String myUid;

    ImageView avatarTv,coverIv;
    TextView nameTv,emailTv,phoneTv;

    RecyclerView postsRecyclerView, videosRecyclerView;

    String image, cover;
    List<ModelPost> postList;
    List<ModelVideo> videoList;
    AdapterPosts adapterPosts;
    String uid, videoName, videoUrl, vId;

    private DatabaseReference likesReference, postsReference;
    Boolean mProcessLike = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_there_profile);

        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Profile");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        avatarTv = findViewById(R.id.avatarTv);
        coverIv =  findViewById(R.id.coverIv);
        nameTv =   findViewById(R.id.nameTv);
        emailTv =  findViewById(R.id.emailTv);
        phoneTv =  findViewById(R.id.phoneTv);

        likesReference = FirebaseDatabase.getInstance().getReference().child("Likes");
        postsReference = FirebaseDatabase.getInstance().getReference().child("Posts");


        /*
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showEditProfileDialog();

            }
        });
        */

        postsRecyclerView = findViewById(R.id.recyclerview_posts);
        videosRecyclerView = findViewById(R.id.recyclerview_videos);
        firebaseAuth = FirebaseAuth.getInstance();

        //uid of clicked user to retrieve his posts
        Intent intent = getIntent();
        uid = intent.getStringExtra("uid");

        Query query = FirebaseDatabase.getInstance().getReference("Users").orderByChild("uid").equalTo(uid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for (DataSnapshot ds : snapshot.getChildren()){

                    String name = ""+ ds.child("name").getValue();
                    String email = ""+ ds.child("email").getValue();
                    String phone = ""+ ds.child("phone").getValue();
                    image = ""+ ds.child("image").getValue();
                    cover = ""+ds.child("cover").getValue();

                    nameTv.setText(name);
                    emailTv.setText(email);
                    phoneTv.setText(phone);

                    try {

                        Picasso.get().load(image).into(avatarTv);

                    }catch (Exception e){

                        Picasso.get().load(R.drawable.ic_default_img_white).into(avatarTv);

                    }

                    try {

                        Picasso.get().load(cover).into(coverIv);

                    }catch (Exception e){

                    }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        coverIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(ThereProfileActivity.this,FullScreenBannerActivity.class);
                intent.putExtra("cover",cover);
                startActivity(intent);

            }
        });

        avatarTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(ThereProfileActivity.this,FullScreenProfileActivity.class);
                intent.putExtra("image",image);
                startActivity(intent);

            }
        });

        postList = new ArrayList<>();
        videoList = new ArrayList<>();

        checkUserStatus();
        loadHistPosts();
        loadHistVideos();

    }

    private void loadHistPosts() {

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        postsRecyclerView.setLayoutManager(layoutManager);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");
        Query query = databaseReference.orderByChild("uid").equalTo(uid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                postList.clear();
                for (DataSnapshot ds:snapshot.getChildren()){

                    ModelPost myPosts = ds.getValue(ModelPost.class);

                    if (myPosts.getpTitle() != null || myPosts.getpDescription() != null) {

                        postList.add(myPosts);

                    }

                }

                adapterPosts = new AdapterPosts(ThereProfileActivity.this, postList);
                postsRecyclerView.setAdapter(adapterPosts);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                Toast.makeText(ThereProfileActivity.this, ""+error.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });


    }

    private void loadHistVideos() {

        LinearLayoutManager layoutManager2 = new LinearLayoutManager(this);
        layoutManager2.setStackFromEnd(true);
        layoutManager2.setReverseLayout(true);
        videosRecyclerView.setLayoutManager(layoutManager2);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");
        Query query = databaseReference.orderByChild("uid").equalTo(uid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                videoList.clear();
                for (DataSnapshot ds:snapshot.getChildren()) {

                    ModelVideo myVideo = ds.getValue(ModelVideo.class);

                    if (myVideo.getVideourl() != null || myVideo.getVideoname() != null){

                        videoList.add(myVideo);

                        FirebaseRecyclerOptions<ModelVideo> options =
                                new FirebaseRecyclerOptions.Builder<ModelVideo>()
                                        .setQuery(databaseReference, ModelVideo.class)
                                        .build();

                        FirebaseRecyclerAdapter<ModelVideo, AdapterVideo.MyHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<ModelVideo, AdapterVideo.MyHolder>(options) {
                            @Override
                            protected void onBindViewHolder(@NonNull AdapterVideo.MyHolder holder, int position, @NonNull ModelVideo model) {

                                vId = videoList.get(position).getVideoId();
                                videoName = videoList.get(position).getVideoname();
                                videoUrl = videoList.get(position).getVideourl();

                                myUid = firebaseAuth.getCurrentUser().getUid();
                                System.out.println("on HomeFragment line 177 myUid: " +myUid);

                                String pLikes = videoList.get(position).getvLikes();
                                holder.pLikesTv.setText(pLikes +" Likes");
                                holder.setLikes(holder,vId);

                                holder.setExoplayer(getApplication(), model.getVideoname(), model.getVideourl(), model.getuDp(), model.getuName(), model.getvTime());

                                holder.setOnClicklistener(new AdapterVideo.MyHolder.Clicklistener() {
                                    @Override
                                    public void onItemClick(View view, int position) {

                                        videoName = getItem(position).getVideoname();
                                        videoUrl = getItem(position).getVideourl();
                                        Intent intent = new Intent(ThereProfileActivity.this,FullScreenVideoActivity.class);
                                        intent.putExtra("Videoname",videoName);
                                        intent.putExtra("Videourl",videoUrl);
                                        startActivity(intent);

                                    }

                                    @Override
                                    public void onItemLongClick(View view, int position) {

                                    }
                                });

                                holder.likeButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {

                                        final int pLikes = Integer.parseInt(videoList.get(position).getvLikes());
                                        mProcessLike = true;
                                        //get id of the post clicked
                                        final String postId = videoList.get(position).getVideoId();
                                        likesReference.addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                                if (mProcessLike){

                                                    if (snapshot.child(postId).hasChild(myUid)){

                                                        System.out.println("HomeFragment line 227 myUid: "+myUid);
                                                        //already liked, so remove like
                                                        postsReference.child(postId).child("pLikes").setValue(""+(pLikes-1));
                                                        likesReference.child(postId).child(myUid).removeValue();
                                                        mProcessLike = false;

                                                    }else{

                                                        //not liked, like it
                                                        postsReference.child(postId).child("pLikes").setValue(""+(pLikes+1));
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
                                });

                                holder.commentButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {

                                        Intent intent = new Intent(ThereProfileActivity.this,VideoDetailActivity.class);
                                        intent.putExtra("postId",vId);
                                        intent.putExtra("videoUrl",videoUrl);
                                        startActivity(intent);

                                    }
                                });

                                holder.shareButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {

                                        holder.videoIv.buildDrawingCache();
                                        Bitmap bitmap = holder.videoIv.getDrawingCache();
                                        String shareBody = videoName;
                                        Uri uri = saveVideotoShare(bitmap);

                                        Intent sIntent = new Intent(Intent.ACTION_SEND);
                                        sIntent.putExtra(Intent.EXTRA_STREAM, uri);
                                        sIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here");
                                        sIntent.setType("videos/mp4");
                                        startActivity(Intent.createChooser(sIntent,"Share Via"));

                                    }
                                });

                            }

                            @NonNull
                            @Override
                            public AdapterVideo.MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_videos, parent, false);

                                return new AdapterVideo.MyHolder(view);

                            }
                        };

                        firebaseRecyclerAdapter.startListening();
                        videosRecyclerView.setAdapter(firebaseRecyclerAdapter);


                    }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                Toast.makeText(ThereProfileActivity.this, "Profilefragment_loadMyVideos onCancelled : "+error.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });

    }

    private void searchHistPosts(final String searchQuery) {

        LinearLayoutManager  layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        postsRecyclerView.setLayoutManager(layoutManager);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");
        Query query = databaseReference.orderByChild("uid").equalTo(uid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                postList.clear();
                for (DataSnapshot ds:snapshot.getChildren()){

                    ModelPost myPosts = ds.getValue(ModelPost.class);
                    ModelVideo modelVideo = ds.getValue(ModelVideo.class);
                    System.out.println("homefragment searcquerry lowecase öncesi searchQuery: " + searchQuery);

                    if (myPosts.getpTitle() != null || myPosts.getpDescription() != null) {
                        if (myPosts.getpTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                                myPosts.getpDescription().toLowerCase().contains(searchQuery.toLowerCase())) {

                            postList.add(myPosts);

                        }
                    }
                    if (modelVideo.getVideoname() != null && modelVideo.getSearch() != null){
                        System.out.println("İlk if'i geçtii ");
                        if (modelVideo.getSearch().toLowerCase().contains(searchQuery.toLowerCase()) || modelVideo.getVideoname().contains(searchQuery.toLowerCase())){

                            System.out.println("2. if'i geçtinn"+modelVideo.getSearch());
                            firebaseSearch(modelVideo.getSearch());

                        }

                    }

                    adapterPosts = new AdapterPosts(ThereProfileActivity.this, postList);
                    postsRecyclerView.setAdapter(adapterPosts);

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                Toast.makeText(ThereProfileActivity.this, ""+error.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });


    }

    private void firebaseSearch(final String searchQuery){

        LinearLayoutManager layoutManager2 = new LinearLayoutManager(this);
        layoutManager2.setStackFromEnd(true);
        layoutManager2.setReverseLayout(true);
        videosRecyclerView.setLayoutManager(layoutManager2);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");
        Query firebaseQuery = databaseReference.orderByChild("uid").equalTo(uid);
        firebaseQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                videoList.clear();
                for (DataSnapshot ds:snapshot.getChildren()){

                    ModelPost modelPost = ds.getValue(ModelPost.class);
                    ModelVideo modelVideo = ds.getValue(ModelVideo.class);

                   if (modelVideo.getVideoname() != null || modelVideo.getVideourl() != null){

                       if (modelVideo.getVideoname().toLowerCase().contains(searchQuery.toLowerCase()) || modelVideo.getSearch().toLowerCase().contains(searchQuery.toLowerCase())){

                           FirebaseRecyclerOptions<ModelVideo> options =
                                   new FirebaseRecyclerOptions.Builder<ModelVideo>()
                                           .setQuery(firebaseQuery,ModelVideo.class)
                                           .build();
                           FirebaseRecyclerAdapter<ModelVideo, AdapterVideo.MyHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<ModelVideo, AdapterVideo.MyHolder>(options) {
                               @Override
                               protected void onBindViewHolder(@NonNull AdapterVideo.MyHolder holder, int position, @NonNull ModelVideo model) {

                                   final String vId = videoList.get(position).getVideoId();
                                   final  String postkey = getRef(position).getKey();
                                   System.out.println("firebaseSearch for Likes postkey: "+postkey);
                                   videoName = getItem(position).getVideoname();
                                   videoUrl = getItem(position).getVideourl();

                                   myUid = firebaseAuth.getCurrentUser().getUid();
                                   System.out.println("on HomeFragment line 494 myUid: " +myUid);

                                   String pLikes = getItem(position).getvLikes();
                                   holder.pLikesTv.setText(pLikes +" Likes");
                                   holder.setLikes(holder,vId);

                                   holder.setExoplayer(getApplication(), model.getVideoname(), model.getVideourl(), model.getuDp(), model.getuName(), model.getvTime());

                                   holder.setOnClicklistener(new AdapterVideo.MyHolder.Clicklistener() {
                                       @Override
                                       public void onItemClick(View view, int position) {

                                           videoName = getItem(position).getVideoname();
                                           videoUrl = getItem(position).getVideourl();
                                           Intent intent = new Intent(ThereProfileActivity.this,FullScreenVideoActivity.class);
                                           intent.putExtra("Videoname",videoName);
                                           intent.putExtra("Videourl",videoUrl);
                                           startActivity(intent);

                                       }

                                       @Override
                                       public void onItemLongClick(View view, int position) {

                                       }
                                   });

                                   holder.likeButton.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View view) {

                                           final int pLikes = Integer.parseInt(getItem(position).getvLikes());
                                           mProcessLike = true;
                                           //get id of the post clicked
                                           final String postId = getItem(position).getVideoId();
                                           System.out.println("firebaseSearch for Likes postkey: "+postkey);
                                           likesReference.addValueEventListener(new ValueEventListener() {
                                               @Override
                                               public void onDataChange(@NonNull DataSnapshot snapshot) {

                                                   if (mProcessLike){

                                                       if (snapshot.child(postId).hasChild(myUid)){

                                                           System.out.println("firebaseSearch for Likes postkey: "+postkey);
                                                           System.out.println("HomeFragment line 227 myUid: "+myUid);
                                                           //already liked, so remove like
                                                           postsReference.child(postId).child("pLikes").setValue(""+(pLikes-1));
                                                           likesReference.child(postId).child(myUid).removeValue();
                                                           mProcessLike = false;

                                                       }else{

                                                           //not liked, like it
                                                           postsReference.child(postId).child("pLikes").setValue(""+(pLikes+1));
                                                           likesReference.child(postId).child(myUid).setValue("Liked");
                                                           mProcessLike = false;

                                                       }

                                                   }

                                               }

                                               @Override
                                               public void onCancelled(@NonNull DatabaseError error) {

                                               }
                                           });

                                           holder.shareButton.setOnClickListener(new View.OnClickListener() {
                                               @Override
                                               public void onClick(View view) {

                                                   holder.videoIv.buildDrawingCache();
                                                   Bitmap bitmap = holder.videoIv.getDrawingCache();
                                                   String shareBody = videoName;
                                                   Uri uri = saveVideotoShare(bitmap);

                                                   Intent sIntent = new Intent(Intent.ACTION_SEND);
                                                   sIntent.putExtra(Intent.EXTRA_STREAM, uri);
                                                   sIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here");
                                                   sIntent.setType("videos/mp4");
                                                   startActivity(Intent.createChooser(sIntent,"Share Via"));

                                               }
                                           });

                                       }
                                   });

                                   holder.commentButton.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View view) {

                                           Intent intent = new Intent(ThereProfileActivity.this,VideoDetailActivity.class);
                                           intent.putExtra("postId",vId);
                                           intent.putExtra("videoUrl",videoUrl);
                                           startActivity(intent);

                                       }
                                   });

                               }

                               @NonNull
                               @Override
                               public AdapterVideo.MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                                   View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_videos,parent,false);

                                   return new AdapterVideo.MyHolder(view);
                               }
                           };

                           firebaseRecyclerAdapter.startListening();
                           videosRecyclerView.setAdapter(firebaseRecyclerAdapter);


                       }//searchforVideos


                    }//not null variable for Videos
                    if (modelPost.getpTitle() != null || modelPost.getpDescription() != null) {
                        if (modelPost.getpTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                                modelPost.getpDescription().toLowerCase().contains(searchQuery.toLowerCase())) {

                                        searchHistPosts(searchQuery);

                        }
                    }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }

    private Uri saveVideotoShare(Bitmap bitmap) {

        File imageFolder = new File(getCacheDir(),"video");
        Uri uri = null;

        try {

            imageFolder.mkdirs();
            File file = new File(imageFolder, "shared_video.mp4");

            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.WEBP,100, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(this,"com.gizemsolum.travellerapplication.fileprovider",file);

        }catch (Exception e){

            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            System.out.println("Error ThereProfile at line 703 to shareFileError: "+e.getMessage());

        }

        return uri;

    }

    private void checkUserStatus(){

        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user != null){

            //mProfileTv.setText(user.getEmail());

        }else {

            startActivity(new Intent(ThereProfileActivity.this,MainActivity.class));
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

        MenuItem item = menu.findItem(R.id.action_search);
        //searchview : search user specific posts
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                if (!TextUtils.isEmpty(query)){

                    searchHistPosts(query);

                }else{

                    loadHistPosts();
                    loadHistVideos();

                }
                return false;

            }

            @Override
            public boolean onQueryTextChange(String newText) {

                if (!TextUtils.isEmpty(newText)){

                    searchHistPosts(newText);

                }else{

                    loadHistPosts();
                    loadHistVideos();

                }

                return false;
            }
        });

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
}