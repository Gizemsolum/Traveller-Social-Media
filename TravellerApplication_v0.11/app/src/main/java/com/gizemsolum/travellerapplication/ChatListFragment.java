package com.gizemsolum.travellerapplication;

import android.content.Intent;
import android.graphics.ColorSpace;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class ChatListFragment extends Fragment {

    FirebaseAuth firebaseAuth;
    RecyclerView recyclerview_chatList;
    List<ModelChatList> chatlistList;
    List<ModelUsers> usersList;
    DatabaseReference databaseReference;
    FirebaseUser curentUser;
    AdapterChatList adapterChatList;

    public ChatListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);

        firebaseAuth = FirebaseAuth.getInstance();
        curentUser =  FirebaseAuth.getInstance().getCurrentUser();

        recyclerview_chatList = view.findViewById(R.id.recyclerview_chatList);
        chatlistList = new ArrayList<>();

        databaseReference = FirebaseDatabase.getInstance().getReference("ChatList").child(curentUser.getUid());
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                chatlistList.clear();
                for (DataSnapshot ds:snapshot.getChildren()){

                    ModelChatList modelChatList = ds.getValue(ModelChatList.class);
                    chatlistList.add(modelChatList);

                }

                loadChats();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        return view;

    }

    private void loadChats() {

        usersList = new ArrayList<>();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                usersList.clear();
                for (DataSnapshot ds:snapshot.getChildren()){

                   ModelUsers modelUsers = ds.getValue(ModelUsers.class);
                   for (ModelChatList chatList: chatlistList){

                       if (modelUsers.getUid() != null && modelUsers.getUid().equals(chatList.getId())){

                           usersList.add(modelUsers);
                           break;

                       }

                   }

                }

                adapterChatList = new AdapterChatList(getContext(),usersList);
                recyclerview_chatList.setAdapter(adapterChatList);

                for (int i = 0; i < usersList.size(); i++){

                    lastMessage(usersList.get(i).getUid());

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void lastMessage(String userId) {

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Chats");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                String theLastMessage = "default";
                for (DataSnapshot ds:snapshot.getChildren()){

                    ModelChat modelChat = ds.getValue(ModelChat.class);
                    if (modelChat == null){

                        continue;

                    }

                    String sender = modelChat.getSender();
                    String receiver = modelChat.getReceiver();
                    if (sender == null || receiver == null){

                        continue;

                    }

                    if (modelChat.getReceiver().equals(curentUser.getUid()) && modelChat.getSender().equals(userId)
                            || modelChat.getReceiver().equals(userId) && modelChat.getSender().equals(curentUser.getUid())){

                        theLastMessage = modelChat.getMessage();

                    }

                }

                adapterChatList.setLastMessageMap(userId, theLastMessage);
                adapterChatList.notifyDataSetChanged();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void checkUserStatus(){

        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user != null){



        }else {

            startActivity(new Intent(getActivity(),MainActivity.class));
            getActivity().finish();

        }

    }

}