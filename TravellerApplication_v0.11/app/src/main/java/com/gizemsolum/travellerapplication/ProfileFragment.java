package com.gizemsolum.travellerapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.app.Activity.RESULT_OK;

/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment {

    FirebaseAuth firebaseAuth;
    FirebaseUser user;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    StorageReference storageReference;
    String storagePath = "Users_Profile_Cover_Imgs/";

    ImageView avatarTv,coverIv;
    TextView nameTv,emailTv,phoneTv;
    FloatingActionButton fab;
    RecyclerView postsRecyclerView,videosRecyclerView;

    ProgressDialog pd;

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_GALLERY_CODE = 300;
    private static final int IMAGE_PICK_CAMERA_CODE = 400;
    String cameraPermissons[];
    String storagePermissons[];
    Uri image_uri;

    String image, cover;
    List<ModelPost> postList;
    List<ModelVideo> videoList;
    AdapterPosts adapterPosts;
    String uid;

    String profileOrcoverPhoto;
    String videoName, videoUrl, myUid, vId;

    private DatabaseReference likesReference, postsReference;
    Boolean mProcessLike = false;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        // Inflate the layout for this fragment
        View view =inflater.inflate(R.layout.fragment_profile, container, false);

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("Users");
        storageReference =  FirebaseStorage.getInstance().getReference();

        likesReference = FirebaseDatabase.getInstance().getReference().child("Likes");
        postsReference = FirebaseDatabase.getInstance().getReference().child("Posts");

        cameraPermissons = new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissons = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        avatarTv = view.findViewById(R.id.avatarTv);
        coverIv = view.findViewById(R.id.coverIv);
        nameTv = view.findViewById(R.id.nameTv);
        emailTv = view.findViewById(R.id.emailTv);
        phoneTv = view.findViewById(R.id.phoneTv);
        fab = view.findViewById(R.id.fab);
        postsRecyclerView = view.findViewById(R.id.recyclerview_posts);
        videosRecyclerView = view.findViewById(R.id.recyclerview_videos);

        pd = new ProgressDialog(getActivity());

        Query query = databaseReference.orderByChild("email").equalTo(user.getEmail());
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

                        System.out.println("Catch inside profile fragment onDataChange: "+e.toString());

                    }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showEditProfileDialog();

            }
        });

        coverIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(getActivity(),FullScreenBannerActivity.class);
                intent.putExtra("cover",cover);
                startActivity(intent);

            }
        });

        avatarTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(getActivity(),FullScreenProfileActivity.class);
                intent.putExtra("image",image);
                startActivity(intent);

            }
        });

        postList = new ArrayList<>();
        videoList = new ArrayList<>();
        checkUserStatus();

        loadMyPosts();
        loadMyVideos();
        return view;

    }

    private void loadMyPosts() {

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
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

                adapterPosts = new AdapterPosts(getActivity(), postList);
                postsRecyclerView.setAdapter(adapterPosts);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                Toast.makeText(getActivity(), ""+error.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });

    }

    private void loadMyVideos() {

        System.out.println("! A Profilefragment_loadMyVideos");
        LinearLayoutManager  layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        videosRecyclerView.setLayoutManager(layoutManager);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");
        Query query = databaseReference.orderByChild("uid").equalTo(uid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                videoList.clear();
                for (DataSnapshot ds:snapshot.getChildren()) {

                    ModelVideo myVideo = ds.getValue(ModelVideo.class);

                    if(myVideo.getVideoname() != null || myVideo.getVideourl() != null){

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

                                holder.setExoplayer(getActivity().getApplication(), model.getVideoname(), model.getVideourl(), model.getuDp(), model.getuName(), model.getvTime());

                                holder.setOnClicklistener(new AdapterVideo.MyHolder.Clicklistener() {
                                    @Override
                                    public void onItemClick(View view, int position) {

                                        videoName = getItem(position).getVideoname();
                                        videoUrl = getItem(position).getVideourl();
                                        Intent intent = new Intent(getActivity(),FullScreenVideoActivity.class);
                                        intent.putExtra("Videoname",videoName);
                                        intent.putExtra("Videourl",videoUrl);
                                        getActivity().startActivity(intent);

                                    }

                                    @Override
                                    public void onItemLongClick(View view, int position) {

                                    }
                                });

                                holder.moreButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        videoName = getItem(position).getVideoname();
                                        videoUrl = getItem(position).getVideourl();
                                        showMoreOptionsforVideo(uid,myUid,videoName,videoUrl);
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

                                        Intent intent = new Intent(getActivity(),VideoDetailActivity.class);
                                        intent.putExtra("postId",vId);
                                        intent.putExtra("videoUrl",videoUrl);
                                        getActivity().startActivity(intent);

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
                                        getActivity().startActivity(Intent.createChooser(sIntent,"Share Via"));

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

                Toast.makeText(getActivity(), "Profilefragment_loadMyVideos onCancelled : "+error.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });

    }

    private void showMoreOptionsforVideo(String uid, String myUid, final String videoName, final String videoUrl) {

        final String options []= {"Delete Post", "Edit Post","View Detail"};

        if (uid.equals(myUid)){

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Choose Action");
            builder.setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                   if (i == 0){

                        deleteWithVideo(videoUrl);

                    }else if (i == 1){

                       Intent intent = new Intent(getActivity(),AddVideoActivity.class);;
                       intent.putExtra("key","editVideo");
                       intent.putExtra("editVideoUrl",videoUrl);
                       getActivity().startActivity(intent);

                   }else if (i == 2){

                       Intent intent = new Intent(getActivity(),VideoDetailActivity.class);
                       intent.putExtra("postId",vId);
                       System.out.println("HomeFragrement vId line 320"+ vId);
                       getActivity().startActivity(intent);

                   }

                }

            });

            builder.create().show();

        }

    }

    private void deleteWithVideo(final String videoUrl) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Delete");
        builder.setMessage("Are you Sure to Delete this data");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                Query query = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("Videourl").equalTo(videoUrl);
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()){
                            dataSnapshot1.getRef().removeValue();
                        }
                        Toast.makeText(getActivity(), "Deleted succesfully..", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        ///
                    }
                });

            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                dialogInterface.dismiss();
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }

    private void searchMyPosts(final String searchQuery) {

        LinearLayoutManager  layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        postsRecyclerView.setLayoutManager(layoutManager);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");
        Query query = databaseReference.orderByChild("uid").equalTo(uid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                postList.clear();
                for(DataSnapshot ds:snapshot.getChildren()) {

                    ModelPost modelPost = ds.getValue(ModelPost.class);
                    ModelVideo modelVideo = ds.getValue(ModelVideo.class);
                    System.out.println("homefragment searcquerry lowecase öncesi searchQuery: " + searchQuery);

                    if (modelPost.getpTitle() != null && modelPost.getpDescription() != null){

                        if (modelPost.getpTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                                modelPost.getpDescription().toLowerCase().contains(searchQuery.toLowerCase())) {

                            postList.add(modelPost);

                        }
                    }
                    if (modelVideo.getVideoname() != null && modelVideo.getSearch() != null){

                        System.out.println("İlk if'i geçtii ");

                        if (modelVideo.getSearch().toLowerCase().contains(searchQuery.toLowerCase()) || modelVideo.getVideoname().contains(searchQuery.toLowerCase())){

                            System.out.println("2. if'i geçtinn"+modelVideo.getSearch());
                            firebaseSearch(modelVideo.getSearch());

                        }

                    }

                    adapterPosts = new AdapterPosts(getActivity(), postList);
                    postsRecyclerView.setAdapter(adapterPosts);
                    System.out.println("! A Homefragment setAdapter line 211");

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                // Toast.makeText(getActivity(), ""+error.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });

    }

   private void firebaseSearch(final String searchQuery){

        LinearLayoutManager layoutManager2 = new LinearLayoutManager(getActivity());
        layoutManager2.setStackFromEnd(true);
        layoutManager2.setReverseLayout(true);
        videosRecyclerView.setLayoutManager(layoutManager2);

        String query = searchQuery.toLowerCase();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");
        Query firebaseQuery = databaseReference.orderByChild("uid").equalTo(uid);//.startAt(query).endAt(query+ "\uf8ff")
        firebaseQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                videoList.clear();
                for (DataSnapshot ds:snapshot.getChildren()){

                    ModelVideo modelVideo = ds.getValue(ModelVideo.class);
                    ModelPost modelPost = ds.getValue(ModelPost.class);

                    if(modelVideo.getVideoname() != null || modelVideo.getVideourl() != null ){

                        if (modelVideo.getVideoname().toLowerCase().contains(searchQuery.toLowerCase()) || modelVideo.getSearch().toLowerCase().contains(searchQuery.toLowerCase())){

                            FirebaseRecyclerOptions<ModelVideo> options =
                                    new FirebaseRecyclerOptions.Builder<ModelVideo>()
                                            .setQuery(firebaseQuery,ModelVideo.class)
                                            .build();
                            FirebaseRecyclerAdapter<ModelVideo, AdapterVideo.MyHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<ModelVideo, AdapterVideo.MyHolder>(options) {
                                @Override
                                protected void onBindViewHolder(@NonNull AdapterVideo.MyHolder holder, int position, @NonNull ModelVideo model) {
                                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                    String uid = user.getUid();

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

                                    holder.setExoplayer(getActivity().getApplication(), model.getVideoname(), model.getVideourl(), model.getuDp(), model.getuName(), model.getvTime());

                                    holder.setOnClicklistener(new AdapterVideo.MyHolder.Clicklistener() {
                                        @Override
                                        public void onItemClick(View view, int position) {

                                            videoName = getItem(position).getVideoname();
                                            videoUrl = getItem(position).getVideourl();
                                            Intent intent = new Intent(getActivity(),FullScreenVideoActivity.class);
                                            intent.putExtra("Videoname",videoName);
                                            intent.putExtra("Videourl",videoUrl);
                                            getActivity().startActivity(intent);

                                        }

                                        @Override
                                        public void onItemLongClick(View view, int position) {
                                        }
                                    });

                                    holder.moreButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            videoName = getItem(position).getVideoname();
                                            videoUrl = getItem(position).getVideourl();
                                            showMoreOptionsforVideo(uid,myUid,videoName,videoUrl);
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

                                        }
                                    });

                                    holder.commentButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {

                                            Intent intent = new Intent(getActivity(),VideoDetailActivity.class);
                                            intent.putExtra("postId",vId);
                                            intent.putExtra("videoUrl",videoUrl);
                                            getActivity().startActivity(intent);

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
                                            getActivity().startActivity(Intent.createChooser(sIntent,"Share Via"));

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

                        }//edn of searchforVideo


                    }//not null variable for Videos
                    if (modelPost.getpTitle() != null && modelPost.getpDescription() != null){

                        if (modelPost.getpTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                                modelPost.getpDescription().toLowerCase().contains(searchQuery.toLowerCase())) {

                                    searchMyPosts(searchQuery);

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

        File imageFolder = new File(getActivity().getCacheDir(),"video");
        Uri uri = null;

        try {

            imageFolder.mkdirs();
            File file = new File(imageFolder, "shared_video.mp4");

            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.WEBP,100, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(getActivity(),"com.gizemsolum.travellerapplication.fileprovider",file);

        }catch (Exception e){

            Toast.makeText(getActivity(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            System.out.println("Error ProfileFragrement at line 803 to shareFileError: "+e.getMessage());

        }

        return uri;

    }

    private boolean checkStoragePermission(){

        boolean result = ContextCompat.checkSelfPermission(getActivity(),Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result;

    }

    private void  requestStoragePermission(){

        requestPermissions(storagePermissons, STORAGE_REQUEST_CODE);

    }

    private boolean checkCameraPermission(){

        boolean result = ContextCompat.checkSelfPermission(getActivity(),Manifest.permission.CAMERA)
                == (PackageManager.PERMISSION_GRANTED);

        boolean result1 = ContextCompat.checkSelfPermission(getActivity(),Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);

        return result && result1;

    }

    private void  requestCameraPermission(){

        requestPermissions(cameraPermissons, CAMERA_REQUEST_CODE);

    }

    private void showEditProfileDialog() {

        final String options []= {"Edit Profile Picture", "Edit Cover Photo", "Edit Name", "Edit Phone"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Choose Action");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                if (i == 0){

                    pd.setMessage("Updating Profile Picture...");
                    profileOrcoverPhoto = "image";
                    showImagePictureDialog();

                }else if (i == 1){

                    pd.setMessage("Updating Cover Photo...");
                    profileOrcoverPhoto = "cover";
                    showImagePictureDialog();

                }else if (i == 2){

                    pd.setMessage("Updating Name...");
                    showNamePhoneUpdateDialog("name");

                }else if (i == 3){

                    pd.setMessage("Updating Phone...");
                    showNamePhoneUpdateDialog("phone");

                }

            }
        });
        builder.create().show();

    }

    private void showNamePhoneUpdateDialog(final String key) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Update "+ key);
        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(10,10,10,10);
        final EditText editText = new EditText(getActivity());
        editText.setHint("Enter "+ key);
        linearLayout.addView(editText);
        builder.setView(linearLayout);

        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                final String value = editText.getText().toString().trim();
                if (!TextUtils.isEmpty(value)){

                    pd.show();
                    HashMap<String, Object> result = new HashMap<>();
                    result.put(key,value);
                    databaseReference.child(user.getUid()).updateChildren(result)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    pd.dismiss();
                                    Toast.makeText(getActivity(), "Updated...", Toast.LENGTH_SHORT).show();

                                }
                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                            pd.dismiss();
                            Toast.makeText(getActivity(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                        }
                    });

                    //user edit his name and posts
                    if (key.equals("name")){

                        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");
                        Query query = databaseReference.orderByChild("uid").equalTo(uid);
                        query.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                for (DataSnapshot ds: snapshot.getChildren()){

                                    String child = ds.getKey();
                                    snapshot.getRef().child(child).child("uName").setValue(value);

                                }

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                        //update name in current users comments on posts
                        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                for (DataSnapshot ds:snapshot.getChildren()){

                                    String child = ds.getKey();
                                    if (snapshot.child(child).hasChild("Comments")){

                                        String childl = ""+snapshot.child(child).getKey();
                                        Query child2 = FirebaseDatabase.getInstance().getReference("Posts")
                                                .child(childl).child("Comments").orderByChild("uid").equalTo(uid);
                                        child2.addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                                for (DataSnapshot ds:snapshot.getChildren()){

                                                    String child = ds.getKey();
                                                    snapshot.getRef().child(child).child("uName").setValue(value);

                                                }

                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });



                                    }

                                }

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                    }

                }else {

                    Toast.makeText(getActivity(), "Please enter "+key, Toast.LENGTH_SHORT).show();

                }

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        builder.create().show();

    }

    private void showImagePictureDialog() {

        final String options []= {"Camera", "Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Pick Image Form");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                if (i == 0){

                    if (!checkCameraPermission()){

                        requestCameraPermission();

                    }else{

                        pickFromCamera();

                    }

                }else if (i == 1){

                    if (!checkStoragePermission()){

                        requestStoragePermission();

                    }else{

                        pickFromGallery();

                    }

                }

            }
        });
        builder.create().show();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode){

            case CAMERA_REQUEST_CODE:{

                if (grantResults.length > 0){

                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAccepted){

                        pickFromCamera();

                    }else{

                        Toast.makeText(getActivity(),"Please enable camera & storage permission",Toast.LENGTH_SHORT).show();

                    }

                }

            }
            break;
            case STORAGE_REQUEST_CODE:{

                if (grantResults.length > 0){

                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted){

                        pickFromGallery();

                    }else{

                        Toast.makeText(getActivity(),"Please enable storage permission",Toast.LENGTH_SHORT).show();

                    }

                }

            }
            break;

        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (resultCode ==RESULT_OK){

            if(requestCode == IMAGE_PICK_GALLERY_CODE){

                image_uri = data.getData();

                uploadProfileCoverPhoto(image_uri);

            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE){

                uploadProfileCoverPhoto(image_uri);

            }

        }
        super.onActivityResult(requestCode, resultCode, data);

    }

    private void uploadProfileCoverPhoto(Uri uri) {

        pd.show();
        String filePathandName = storagePath+  ""+ profileOrcoverPhoto +"_"+ user.getUid();
        StorageReference storageReference2nd = storageReference.child(filePathandName);
        storageReference2nd.putFile(uri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        final Uri downloadUri = uriTask.getResult();

                        if(uriTask.isSuccessful()){

                            HashMap<String, Object> results = new HashMap<>();
                            results.put(profileOrcoverPhoto,downloadUri.toString());
                            databaseReference.child(user.getUid()).updateChildren(results)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {

                                            pd.dismiss();
                                            Toast.makeText(getActivity(),"Image Updated...",Toast.LENGTH_SHORT).show();

                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {

                                    pd.dismiss();
                                    Toast.makeText(getActivity(),"Error Updating Image...",Toast.LENGTH_SHORT).show();

                                }
                            });

                            //user edit his name and posts
                            if (profileOrcoverPhoto.equals("image")){

                                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");
                                Query query = databaseReference.orderByChild("uid").equalTo(uid);
                                query.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                                        for (DataSnapshot ds: snapshot.getChildren()){

                                            String child = ds.getKey();
                                            snapshot.getRef().child(child).child("uDp").setValue(downloadUri.toString());

                                        }

                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });

                                //update user image in current users comments on posts
                                databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                                        for (DataSnapshot ds:snapshot.getChildren()){

                                            String child = ds.getKey();
                                            if (snapshot.child(child).hasChild("Comments")){

                                                String childl = ""+snapshot.child(child).getKey();
                                                Query child2 = FirebaseDatabase.getInstance().getReference("Posts")
                                                        .child(childl).child("Comments").orderByChild("uid").equalTo(uid);
                                                child2.addValueEventListener(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                                                        for (DataSnapshot ds:snapshot.getChildren()){

                                                            String child = ds.getKey();
                                                            snapshot.getRef().child(child).child("uDp").setValue(downloadUri.toString());

                                                        }

                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {

                                                    }
                                                });



                                            }

                                        }

                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });

                            }

                        }else{

                            pd.dismiss();
                            Toast.makeText(getActivity(),"Some error occured",Toast.LENGTH_SHORT).show();

                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                pd.dismiss();
                Toast.makeText(getActivity(),""+e.getMessage(),Toast.LENGTH_SHORT).show();

            }
        });

    }

    private void pickFromCamera() {

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Temp Pic");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");
        image_uri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);

    }

    private void pickFromGallery() {

        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent,IMAGE_PICK_GALLERY_CODE);

    }

    private void checkUserStatus(){

        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user != null){

            //mProfileTv.setText(user.getEmail());
            uid = user.getUid();

        }else {

            startActivity(new Intent(getActivity(),MainActivity.class));
            getActivity().finish();

        }

    }

    private void showAddingDialog() {

        final String options []= {"Add Post", "Add Video"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Choose Action");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                if (i == 0){

                    pd.setMessage("Displaying Add Post Page...");
                    startActivity(new Intent(getActivity(),AddPostActivity.class));

                }else if (i == 1){

                    pd.setMessage("Displaying Add Video Page...");
                    startActivity(new Intent(getActivity(),AddVideoActivity.class));

                }
            }
        });
        builder.create().show();

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        setHasOptionsMenu(true);

        super.onCreate(savedInstanceState);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.menu_main, menu);

        MenuItem item = menu.findItem(R.id.action_search);
        //searchview : search user specific posts
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {

                    if (!TextUtils.isEmpty(query)) {

                        searchMyPosts(query);

                    } else {

                        loadMyPosts();
                        loadMyVideos();

                    }
                    return false;

                }

                @Override
                public boolean onQueryTextChange(String newText) {

                    if (!TextUtils.isEmpty(newText)) {

                        searchMyPosts(newText);

                    } else {

                        loadMyPosts();
                        loadMyVideos();

                    }

                    return false;
                }
            });

        super.onCreateOptionsMenu(menu,inflater);

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_logout){

            firebaseAuth.signOut();
            checkUserStatus();

        }if (id == R.id.action_add_post){

            //startActivity(new Intent(getActivity(),AddPostActivity.class));
            showAddingDialog();

        }

        return super.onOptionsItemSelected(item);

    }

}