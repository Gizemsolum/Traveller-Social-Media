package com.gizemsolum.travellerapplication;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.FileProvider;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class
HomeFragment extends Fragment {

    FirebaseAuth firebaseAuth;

    RecyclerView recyclerView, recyclerView_videos;
    List<ModelPost> postList;
    List<ModelVideo> videoList;
    AdapterPosts adapterPosts;

    private DatabaseReference likesReference, postsReference;
    Boolean mProcessLike = false;

    ProgressDialog pd;
    String videoName, videoUrl, myUid, vId;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        //firebaseAuth = FirebaseAuth.getInstance();
        recyclerView = view.findViewById(R.id.recyclerview_posts);
        recyclerView_videos = view.findViewById(R.id.recyclerview_videos);

        firebaseAuth = FirebaseAuth.getInstance();

        pd = new ProgressDialog(getActivity());
        postList = new ArrayList<>();
        videoList = new ArrayList<>();

        likesReference = FirebaseDatabase.getInstance().getReference().child("Likes");
        postsReference = FirebaseDatabase.getInstance().getReference().child("Posts");

        prepare_post();
        prepare_video();
        return view;

    }

    public void prepare_post(){

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(layoutManager);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                postList.clear();
                for(DataSnapshot ds:snapshot.getChildren()){

                    ModelPost modelPost = ds.getValue(ModelPost.class);

                    if (modelPost.getpTitle() != null || modelPost.getpDescription() != null) {

                        postList.add(modelPost);

                    }

                }

                System.out.println("! A HomeFragment onDataChange post arraysize: "+postList.size());
                adapterPosts = new AdapterPosts(getActivity(), postList);
                recyclerView.setAdapter(adapterPosts);
                System.out.println("! A Homefragment setAdapter line 111");

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                try {

                    Toast.makeText(getActivity(), "" + error.getMessage(), Toast.LENGTH_SHORT).show();

                }catch(NullPointerException err){

                    System.out.println("! A HomeFragment onCancelled  catch: "+ err.toString());

                }

            }

        });

    }

    public void prepare_video(){

        LinearLayoutManager layoutManager2 = new LinearLayoutManager(getActivity());
        layoutManager2.setStackFromEnd(true);
        layoutManager2.setReverseLayout(true);
        recyclerView_videos.setLayoutManager(layoutManager2);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                videoList.clear();
                for (DataSnapshot ds:snapshot.getChildren()) {

                    ModelVideo myVideo = ds.getValue(ModelVideo.class);
                    System.out.println("ModelVideo at 152 line of HomeFragment " + myVideo);
                    videoList.add(myVideo);

                    FirebaseRecyclerOptions<ModelVideo> options =
                                new FirebaseRecyclerOptions.Builder<ModelVideo>()
                                        .setQuery(databaseReference, ModelVideo.class)
                                        .build();
                        FirebaseRecyclerAdapter<ModelVideo, AdapterVideo.MyHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<ModelVideo, AdapterVideo.MyHolder>(options) {
                            @Override
                            protected void onBindViewHolder(@NonNull AdapterVideo.MyHolder holder, int position, @NonNull ModelVideo model) {

                                final String uid = videoList.get(position).getUid();
                                vId = videoList.get(position).getVideoId();
                                videoName = videoList.get(position).getVideoname();
                                videoUrl = videoList.get(position).getVideourl();

                                myUid = firebaseAuth.getCurrentUser().getUid();
                                System.out.println("on HomeFragment line 177 myUid: " +myUid);

                                String pLikes = videoList.get(position).getvLikes();
                                holder.pLikesTv.setText(pLikes +" Likes");
                                holder.setLikes(holder,vId);

                                if (model.getVideourl() != null) {

                                    holder.setExoplayer(getActivity().getApplication(), model.getVideoname(), model.getVideourl(), model.getuDp(), model.getuName(), model.getvTime());

                                    holder.setOnClicklistener(new AdapterVideo.MyHolder.Clicklistener() {
                                        @Override
                                        public void onItemClick(View view, int position) {
                                            showOptionsforVideo(uid,videoName,videoUrl);
                                        }

                                        @Override
                                        public void onItemLongClick(View view, int position) {

                                        }
                                    });

                                    holder.moreButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            System.out.println("HomeFragment line 206 myUid: "+myUid);
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
                            }

                            @NonNull
                            @Override
                            public AdapterVideo.MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_videos, parent, false);

                                return new AdapterVideo.MyHolder(view);
                            }
                        };
                        firebaseRecyclerAdapter.startListening();

                        recyclerView_videos.setAdapter(firebaseRecyclerAdapter);
                        System.out.println("! A Homefragment setAdapter line 206");

                    }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

              // Toast.makeText(getActivity(), "Homefragment prepare_video on Cancelled :  "+error.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });

    }

    private void showMoreOptionsforVideo(String uid, String myUid, final String videoName, final String videoUrl) {

        final String options []= {"Delete", "Edit","View Detail"};

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

    private void showOptionsforVideo(String uid, final String videoName, final String videoUrl) {

        final String options []= {"Profile","FullScreen"};

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Choose Action");
            builder.setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                    if (i == 0){

                        Intent intent = new Intent(getActivity(), ThereProfileActivity.class);
                        intent.putExtra("uid", uid);
                        getActivity().startActivity(intent);

                    }else if (i == 1){

                        Intent intent = new Intent(getActivity(),FullScreenVideoActivity.class);
                        intent.putExtra("Videoname",videoName);
                        intent.putExtra("Videourl",videoUrl);
                        getActivity().startActivity(intent);

                    }

                }
            });

            builder.create().show();

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

    private void searchPosts(final String searchQuery){

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(layoutManager);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");
        databaseReference.addValueEventListener(new ValueEventListener() {
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
                   recyclerView.setAdapter(adapterPosts);
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
        recyclerView_videos.setLayoutManager(layoutManager2);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");
        Query firebaseQuery = databaseReference.orderByChild("search");
        firebaseQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                videoList.clear();
                for (DataSnapshot ds:snapshot.getChildren()){

                    ModelVideo modelVideo = ds.getValue(ModelVideo.class);
                    ModelPost modelPost = ds.getValue(ModelPost.class);
                    if (modelVideo.getVideoname() != null || modelVideo.getVideourl() != null){

                        if(modelVideo.getVideoname().toLowerCase().contains(searchQuery.toLowerCase()) || modelVideo.getSearch().toLowerCase().contains(searchQuery.toLowerCase())){

                            FirebaseRecyclerOptions<ModelVideo> options =
                                    new FirebaseRecyclerOptions.Builder<ModelVideo>()
                                            .setQuery(firebaseQuery,ModelVideo.class)
                                            .build();
                            System.out.println("!!!!!:  opt" + options+" fireba: "+firebaseQuery);
                            FirebaseRecyclerAdapter<ModelVideo, AdapterVideo.MyHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<ModelVideo, AdapterVideo.MyHolder>(options) {
                                @Override
                                protected void onBindViewHolder(@NonNull AdapterVideo.MyHolder holder, int position, @NonNull ModelVideo model) {
                                    System.out.println("!!!!model: "+model.toString());
                                    String tempuid="";
                                    try {
                                        tempuid = videoList.get(position).getUid();
                                    }catch (Exception e){
                                        System.out.println("Error searchFirebaseforVideos tempuid line 490: "+e.toString());
                                    }
                                    final String uid = tempuid;

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

                                    if(model.getVideourl()!=null){

                                        holder.setExoplayer(getActivity().getApplication(),model.getVideoname(),model.getVideourl(),model.getuDp(),model.getuName(),model.getvTime());}

                                    holder.setOnClicklistener(new AdapterVideo.MyHolder.Clicklistener() {
                                        @Override
                                        public void onItemClick(View view, int position) {
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
                            recyclerView_videos.setAdapter(firebaseRecyclerAdapter);
                            System.out.println("! A Homefragment setAdapter line 265");

                        }

                    }//end of searchVideo
                    if (modelPost.getpTitle() != null && modelPost.getpDescription() != null){

                        if (modelPost.getpTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                                modelPost.getpDescription().toLowerCase().contains(searchQuery.toLowerCase())) {

                            searchPosts(searchQuery);

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
            System.out.println("Error HomeFragrement at line 703 to shareFileError: "+e.getMessage());

        }

        return uri;

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);

    }

    private void checkUserStatus(){

        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user != null){

            //mProfileTv.setText(user.getEmail());

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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.menu_main,menu);

        //searchview to search posts by post title/description
        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        //search listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {

                if (!TextUtils.isEmpty(s)){

                    searchPosts(s);

                }else{

                    prepare_post();
                    prepare_video();

                }

                return false;

            }

            @Override
            public boolean onQueryTextChange(String s) {

                if (!TextUtils.isEmpty(s)){

                    searchPosts(s);

                }else{

                    prepare_post();
                    prepare_video();

                }

                return false;

            }
        });

        super.onCreateOptionsMenu(menu,inflater);

    }

}