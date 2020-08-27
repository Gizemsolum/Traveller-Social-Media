package com.gizemsolum.travellerapplication;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
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
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

class AdapterVideo extends RecyclerView.Adapter<AdapterVideo.MyHolder> {

    Context context;
    List<ModelVideo> voiceList;

    public AdapterVideo(Context context, List<ModelVideo> voiceList) {
        this.context = context;
        this.voiceList = voiceList;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_videos, parent, false);

        return new AdapterVideo.MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {

        final String uid = voiceList.get(position).getUid();
        String uEmail = voiceList.get(position).getuEmail();
        String uName = voiceList.get(position).getuName();
        String UDp = voiceList.get(position).getuDp();
        final String vId = voiceList.get(position).getVideoId();
        String Videoname = voiceList.get(position).getVideoname();
        final String Videourl = voiceList.get(position).getVideourl();
        String vTimes = voiceList.get(position).getvTime();


        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(Long.parseLong(vTimes));
        String vTime = DateFormat.format("dd/MM/yyyy hh:mm aa",calendar).toString();

        //set data
        holder.uNameTv.setText(uName);
        holder.pTimeTv.setText(vTime);
        holder.Tvitem.setText(Videoname);

     /*  holder.likeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //will implement later
                Toast.makeText(context, "Like", Toast.LENGTH_SHORT).show();

            }
        });
        holder.commentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //will implement later
                Toast.makeText(context, "Comment", Toast.LENGTH_SHORT).show();

            }
        });
        holder.shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //will implement later
                Toast.makeText(context, "Share", Toast.LENGTH_SHORT).show();

            }
        });

        holder.profileLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(context, ThereProfileActivity.class);
                intent.putExtra("uid", uid);
                context.startActivity(intent);

            }
        }); */

    }

    @Override
    public int getItemCount() {
        return voiceList.size();
    }

    static class MyHolder extends RecyclerView.ViewHolder{

        TextView uNameTv, pTimeTv, pDescriptionTv, pLikesTv, Tvitem;
        ImageView uPictureIv;
        PlayerView videoIv;
        ImageButton moreButton;
        Button likeButton, commentButton, shareButton;
        LinearLayout profileLayout;

        SimpleExoPlayer exoPlayer;
        PlayerView playerView;

        //AdapterVideo.MyHolder holder;
        ModelVideo model;

        private DatabaseReference likesReference;

        public MyHolder(@NonNull View itemView) {

            super(itemView);

            videoIv = itemView.findViewById(R.id.exoplayer_item);
            Tvitem = itemView.findViewById(R.id.Tvitem);
            pDescriptionTv = itemView.findViewById(R.id.profileDescriptionTv);
            pLikesTv = itemView.findViewById(R.id.pLikesTv);
            moreButton = itemView.findViewById(R.id.moreButton);
            likeButton = itemView.findViewById(R.id.likeButton);
            commentButton = itemView.findViewById(R.id.commentButton);
            shareButton = itemView.findViewById(R.id.shareButton);
            profileLayout = itemView.findViewById(R.id.profileLayout);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    mClickListener.onItemClick(view,getAdapterPosition());

                }
            });
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {

                    mClickListener.onItemLongClick(view,getAdapterPosition());
                    return false;
                }
            });

        }

        public void setLikes(MyHolder holder, String postKey) {

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String tempuid="";
            try{
                tempuid = user.getUid();
            }catch (Exception e){
                System.out.println("AdapterVideo.MyHolder myUid of setLikes catch error: "+e.toString());
            }
            String myUid = tempuid;
            System.out.println("AdapterVideo.MyHolder myUid of setLikes: "+myUid);
            likesReference = FirebaseDatabase.getInstance().getReference().child("Likes");

            likesReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try{ if (snapshot.child(postKey).hasChild(myUid)){

                        System.out.println("HomeFragment line 290 myUid: "+myUid);
                        holder.likeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_liked,0,0,0);
                        holder.likeButton.setText("Liked");

                    }else{

                        holder.likeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_black,0,0,0);
                        holder.likeButton.setText("Like");

                    }
                    }catch (Exception e){
                        System.out.println("Error setLikes of AdapterView"+e.toString());
                    }


                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

        }

        public void setExoplayer(Application application , String name, String Videourl,String udp,String uname,String vTimes){

            System.out.println("!!!!!!!!!!!!!!!!" + application +"Videoname: "+ name +"Videourl: "+ Videourl +"uname: "+ uname +"vTimes: "+ vTimes);
            TextView textView = itemView.findViewById(R.id.Tvitem);
            uPictureIv = itemView.findViewById(R.id.uPictureIv);

            try{

                Picasso.get().load(udp).placeholder(R.drawable.ic_default_img).into(uPictureIv);

            }catch(Exception e){

                System.out.println("AdapterVideos Catch setExoplayer Error: "+e.toString());

            }

            uNameTv = itemView.findViewById(R.id.uNameTv);
            uNameTv.setText(uname);
            pTimeTv = itemView.findViewById(R.id.pTimeTv);
            System.out.println("pTimeTv: "+pTimeTv);
            System.out.println("uNameTv: "+uNameTv);
            System.out.println("uPictureIv: "+uPictureIv);
            Calendar calendar = Calendar.getInstance(Locale.getDefault());

            try {

                calendar.setTimeInMillis(Long.parseLong(vTimes));
               // System.out.println("!! Calendar to String:  "+calendar.toString());
                String vTime = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();
                pTimeTv.setText(vTime);

            }catch (Exception e){

                System.out.println("! Calendar catch error: "+e.getMessage());

            }

            playerView = itemView.findViewById(R.id.exoplayer_item);
            System.out.println("! playerView: "+playerView);
            textView.setText(name);

                try {

                    BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter.Builder(application).build();
                    TrackSelector trackSelector = new DefaultTrackSelector(new AdaptiveTrackSelection.Factory(bandwidthMeter));
                    exoPlayer = (SimpleExoPlayer) ExoPlayerFactory.newSimpleInstance(application);
                    Uri video = Uri.parse(Videourl);
                    DefaultHttpDataSourceFactory dataSourceFactory = new DefaultHttpDataSourceFactory("Posts");
                    ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
                    MediaSource mediaSource = new ExtractorMediaSource(video, dataSourceFactory, extractorsFactory, null, null);
                    playerView.setPlayer(exoPlayer);
                    exoPlayer.prepare(mediaSource);
                    exoPlayer.setPlayWhenReady(false);

                } catch (Exception e) {

                    Toast.makeText(application, "exp:" + e.toString(), Toast.LENGTH_LONG).show();

                }

        }//end of SetExoplayer

        private MyHolder.Clicklistener mClickListener;

        public interface Clicklistener{
            void onItemClick(View view,int position);
            void onItemLongClick(View view ,int position);

        }

        public void setOnClicklistener(MyHolder.Clicklistener clicklistener){

            mClickListener = clicklistener;

        }

    }//end of MyHolder

}//end of AdapterVideo
