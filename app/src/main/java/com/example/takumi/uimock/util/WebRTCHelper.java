package com.example.takumi.uimock.util;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.example.takumi.uimock.MainActivity;
import com.example.takumi.uimock.VideoCallActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.skyway.Peer.Browser.MediaConstraints;
import io.skyway.Peer.Browser.MediaStream;
import io.skyway.Peer.Browser.Navigator;
import io.skyway.Peer.CallOption;
import io.skyway.Peer.DataConnection;
import io.skyway.Peer.MediaConnection;
import io.skyway.Peer.OnCallback;
import io.skyway.Peer.Peer;
import io.skyway.Peer.PeerError;
import io.skyway.Peer.PeerOption;

public class WebRTCHelper {
    private static final String TAG = WebRTCHelper.class.getSimpleName();

    private static final String API_KEY = "85d0baee-f528-4da1-8c4d-c1942b72fa8a";
    private static final String DOMAIN = "localhost";

    private boolean logined = false;
//	private static final String API_KEY = "yourAPIKEY";
//	private static final String DOMAIN = "yourDomain";

    private Peer peer;
    private MediaStream localStream;
    private MediaStream remoteStream;
    private MediaConnection mediaConnection;
    private DataConnection signalingChannel;
    private static WebRTCHelper instance;

    private String ownId;
    private String peerId;

    private FirebaseDatabase database;

    //
    // Update actionButton title
    //
    public void updateActivity() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (callState == CallState.ESTABLISHED && !(context instanceof VideoCallActivity)) {
                    Intent i = new Intent(((Activity)context).getApplication(), VideoCallActivity.class);
                    //TODO: 誰から呼び出されたかをどうやって取得する？
//                    i.putExtra("Text", selectedText);
//                    i.putExtra("Number", selectedNumber);

                    ((Activity)context).startActivity(i);

                } else if (callState == CallState.TERMINATED) {
                    ((Activity)context).finish();
                }
//                Button btnAction = (Button) findViewById(R.id.btnAction);
//                if (null != btnAction) {
//                    if (CallState.TERMINATED == callState) {
//                        btnAction.setText("Make Call");
//                    }
//                    else if (CallState.CALLING == callState) {
//                        btnAction.setText("Cancel");
//                    }
//                    else {
//                        btnAction.setText("Hang up");
//                    }
//                }
            }
        });
    }

    public enum	CallState {
        TERMINATED,
        CALLING,
        ESTABLISHED
    }

    Context context;
    private CallState callState;

    private Handler handler;

    public String getOwnId() {
        return ownId;
    }

    public String getPeerId() {
        return peerId;
    }

    public static WebRTCHelper getInstance(final Context context) {
        if (instance == null) {
            instance = new WebRTCHelper(context);
        }
        return instance;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void login(String name) {
        if (ownId == null) {
            return;
        }
        Map<String, Object> map = new HashMap<>();
        Log.d(TAG, "USER: " + name + ", ownId: " + ownId);
        map.put(ownId, (Object)name);
        database.getReference().child("users").updateChildren(map);

        Toast.makeText((Activity)context, name + "でログインしました", Toast.LENGTH_LONG);
        Log.d(TAG, "User: " + name + " login");
    }

    public void logout() {
        database.getReference().child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Object> peers = (Map) dataSnapshot.getValue();
                Log.d(TAG, "peers: " + peers);
                peers.remove(ownId);
                database.getReference().child("users").updateChildren(peers);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }
    public void getAllPeers() {
        database.getReference().child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Object> peers = (Map) dataSnapshot.getValue();
                Log.d(TAG, "peers: " + peers);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    public void getPeerId(final String name, final Intent intent) {
        if (ownId == null) {
            return;
        }
        database.getReference().child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Object> peers = (Map) dataSnapshot.getValue();
                Log.d(TAG, "peers: " + peers);
                for (String key: peers.keySet()) {
                    if (!key.equals(ownId)) {
                        peerId = key;
                        Log.d(TAG, "User: " + name + "PeerId: " + peerId);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                onPeerSelected();
                                ((Activity)context).startActivity(intent);
                            }
                        });
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private WebRTCHelper(final Context context) {
        this.context = context;
        FirebaseApp.initializeApp(context);
//
//        FirebaseOptions.Builder builder = new FirebaseOptions.Builder()
//                .setApplicationId("1:302939623826:android:a806387762327d53")
//                .setApiKey("AIzaSyBimnfffrARDetI2BtcKr6UkowfVLaBduw")
//                .setDatabaseUrl("https://myfirstfirebase-d4390.firebaseio.com")
//                .setStorageBucket("myfirstfirebase-d4390.appspot.com");
//
//        FirebaseApp.initializeApp(context, builder.build());

        final FirebaseAuth auth = FirebaseAuth.getInstance();
        final FirebaseUser user = auth.getCurrentUser();
        Log.d(TAG, "USER: " + user);
        auth.signInAnonymously().addOnCompleteListener((Activity) context, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "signInAnonymously:success: USER: " + user);
                } else {
                    Log.w(TAG, "signInAnonymously:failure", task.getException());
                    Toast.makeText((Activity)context, "Authentication failed.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        database = FirebaseDatabase.getInstance();

        handler = new Handler(Looper.getMainLooper());
        callState = CallState.TERMINATED;
        PeerOption option = new PeerOption();
        option.key = API_KEY;
        option.domain = DOMAIN;
        peer = new Peer(context, option);

        // OPEN
        peer.on(Peer.PeerEventEnum.OPEN, new OnCallback() {
            @Override
            public void onCallback(Object object) {

                // Show my ID
                ownId = (String) object;
                Toast.makeText(context, "OWN ID: " + ownId, Toast.LENGTH_LONG);
                Log.d(TAG, "OWN ID: " + ownId);

                login(MainActivity.LOGIN_NAME);

                // Request permissions
                if (ContextCompat.checkSelfPermission(context,
                        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(context,
                        Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},0);
                } else {
                    // Get a local MediaStream & show it
                    startLocalStream();
                }

            }
        });

        // CALL (Incoming call)
        peer.on(Peer.PeerEventEnum.CALL, new OnCallback() {
            @Override
            public void onCallback(Object object) {
                if (!(object instanceof MediaConnection)) {
                    return;
                }

                mediaConnection = (MediaConnection) object;
                callState = CallState.CALLING;
                showIncomingCallDialog();

            }
        });

        // CONNECT (Custom Signaling Channel for a call)
        peer.on(Peer.PeerEventEnum.CONNECTION, new OnCallback() {
            @Override
            public void onCallback(Object object) {
                if (!(object instanceof  DataConnection)) {
                    return;
                }

                signalingChannel = (DataConnection) object;
                setSignalingCallbacks();

            }
        });

        peer.on(Peer.PeerEventEnum.CLOSE, new OnCallback()	{
            @Override
            public void onCallback(Object object) {
                Log.d(TAG, "[On/Close]");
            }
        });

        peer.on(Peer.PeerEventEnum.DISCONNECTED, new OnCallback() {
            @Override
            public void onCallback(Object object) {
                Log.d(TAG, "[On/Disconnected]");
            }
        });

        peer.on(Peer.PeerEventEnum.ERROR, new OnCallback() {
            @Override
            public void onCallback(Object object) {
                PeerError error = (PeerError) object;
                Log.d(TAG, "[On/Error]" + error);
            }
        });
    }

    //
    // Get a local MediaStream & show it
    //
    public void startLocalStream() {
        Navigator.initialize(peer);
        MediaConstraints constraints = new MediaConstraints();
        // 音声のみの通信とする
        constraints.videoFlag = false;
        constraints.audioFlag = true;
        localStream = Navigator.getUserMedia(constraints);
    }

    //
    // Set callbacks for MediaConnection.MediaEvents
    //
    void setMediaCallbacks() {
        mediaConnection.on(MediaConnection.MediaEventEnum.STREAM, new OnCallback() {
            @Override
            public void onCallback(Object object) {
                remoteStream = (MediaStream) object;
                callState = CallState.ESTABLISHED;
                instance.updateActivity();
            }
        });

        mediaConnection.on(MediaConnection.MediaEventEnum.CLOSE, new OnCallback()	{
            @Override
            public void onCallback(Object object) {
                closeRemoteStream();
                signalingChannel.close();
                callState = CallState.TERMINATED;
                instance.updateActivity();
            }
        });

        mediaConnection.on(MediaConnection.MediaEventEnum.ERROR, new OnCallback()	{
            @Override
            public void onCallback(Object object) {
                PeerError error = (PeerError) object;
                Log.d(TAG, "[On/MediaError]" + error);
            }
        });
    }

    //
    // Set callbacks for DataConnection.DataEvents
    //
    void setSignalingCallbacks() {
        signalingChannel.on(DataConnection.DataEventEnum.OPEN, new OnCallback() {
            @Override
            public void onCallback(Object object) {

            }
        });

        signalingChannel.on(DataConnection.DataEventEnum.CLOSE, new OnCallback() {
            @Override
            public void onCallback(Object object) {

            }
        });

        signalingChannel.on(DataConnection.DataEventEnum.ERROR, new OnCallback() {
            @Override
            public void onCallback(Object object) {
                PeerError error = (PeerError) object;
                Log.d(TAG, "[On/DataError]" + error);
            }
        });

        signalingChannel.on(DataConnection.DataEventEnum.DATA, new OnCallback() {
            @Override
            public void onCallback(Object object) {
                String message = (String) object;
                Log.d(TAG, "[On/Data]" + message);

                switch(message) {
                    case "reject":
                        closeMediaConnection();
                        signalingChannel.close();
                        callState = CallState.TERMINATED;
                        instance.updateActivity();
                        break;
                    case "cancel":
                        closeMediaConnection();
                        signalingChannel.close();
                        callState = CallState.TERMINATED;
                        instance.updateActivity();
                        dismissIncomingCallDialog();
                        break;
                }
            }
        });

    }

    //
    // Clean up objects
    //
    private void destroyPeer() {
        closeRemoteStream();

        if (null != localStream) {
            localStream.close();
        }

        closeMediaConnection();

        Navigator.terminate();

        if (null != peer) {
            unsetPeerCallback(peer);
            if (!peer.isDisconnected()) {
                peer.disconnect();
            }

            if (!peer.isDestroyed()) {
                peer.destroy();
            }

            peer = null;
        }
    }

    //
    // Unset callbacks for PeerEvents
    //
    void unsetPeerCallback(Peer peer) {
        if(null == peer){
            return;
        }

        peer.on(Peer.PeerEventEnum.OPEN, null);
        peer.on(Peer.PeerEventEnum.CONNECTION, null);
        peer.on(Peer.PeerEventEnum.CALL, null);
        peer.on(Peer.PeerEventEnum.CLOSE, null);
        peer.on(Peer.PeerEventEnum.DISCONNECTED, null);
        peer.on(Peer.PeerEventEnum.ERROR, null);
    }

    //
    // Unset callbacks for MediaConnection.MediaEvents
    //
    void unsetMediaCallbacks() {
        if(null == mediaConnection){
            return;
        }

        mediaConnection.on(MediaConnection.MediaEventEnum.STREAM, null);
        mediaConnection.on(MediaConnection.MediaEventEnum.CLOSE, null);
        mediaConnection.on(MediaConnection.MediaEventEnum.ERROR, null);
    }

    //
    // Close a MediaConnection
    //
    void closeMediaConnection() {
        if (null != mediaConnection)	{
            if (mediaConnection.isOpen()) {
                mediaConnection.close();
            }
            unsetMediaCallbacks();
        }
    }

    //
    // Close a remote MediaStream
    //
    void closeRemoteStream(){
        if (null == remoteStream) {
            return;
        }

        remoteStream.close();
    }

    //
    // Create a MediaConnection
    //
    public void onPeerSelected() {
        if (null == peer) {
            return;
        }

        if (null != mediaConnection) {
            mediaConnection.close();
        }

        showPeerIDs();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
        if (peerId == null) {
            return;
        }

        CallOption option = new CallOption();
        mediaConnection = peer.call(peerId, localStream, option);
        if (null != mediaConnection) {
            setMediaCallbacks();
            callState = CallState.CALLING;
        }

        // custom P2P signaling channel to reject call attempt
        signalingChannel = peer.connect(peerId);
        if (null != signalingChannel) {
            setSignalingCallbacks();
        }

        instance.updateActivity();
    }

    public void hugup() {
        // Hang up a call
        closeRemoteStream();
        mediaConnection.close();
        signalingChannel.close();
        callState = CallState.TERMINATED;
        instance.updateActivity();
    }

    //
    // Listing all peers
    //
    public void showPeerIDs() {
        if ((null == peer) || (null == ownId) || (0 == ownId.length())) {
            Toast.makeText(context, "Your PeerID is null or invalid.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get all IDs connected to the server
        final Context fContext = context;
        peer.listAllPeers(new OnCallback() {
            @Override
            public void onCallback(Object object) {
                if (!(object instanceof JSONArray)) {
                    return;
                }

                JSONArray peers = (JSONArray) object;
                ArrayList<String> _listPeerIds = new ArrayList<>();
                String peerId;

                // Exclude my own ID
                for (int i = 0; peers.length() > i; i++) {
                    try {
                        peerId = peers.getString(i);
                        if (!ownId.equals(peerId)) {
                            WebRTCHelper.instance.peerId = peerId;
                            break;
                        }
                    } catch(Exception e){
                        e.printStackTrace();
                    }
                }

                if (WebRTCHelper.instance.peerId == null && WebRTCHelper.instance.peerId.equals("")) {
                    Toast.makeText(fContext, "PeerID list (other than your ID) is empty.", Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "PeerID list (other than your ID) is empty.");
                }
            }
        });

    }


    //
    // Show alert dialog on an incoming call
    //
    AlertDialog incomingCallDialog;
    void showIncomingCallDialog(){
        incomingCallDialog = new AlertDialog.Builder(context)
                .setTitle("Incoming call")
                .setMessage("from : " + mediaConnection.peer())
                .setPositiveButton("Answer", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mediaConnection.answer(localStream);
                        setMediaCallbacks();
                        callState = CallState.ESTABLISHED;
                        instance.updateActivity();
                    }
                })
                .setNegativeButton("Reject", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(null != signalingChannel){
                            signalingChannel.send("reject");
                            callState = CallState.TERMINATED;
                        }
                    }
                })
                .show();
    }

    //
    // Dismiss alert dialog for an incoming call
    //
    void dismissIncomingCallDialog(){
        if( null != incomingCallDialog ) {
            incomingCallDialog.cancel();
            incomingCallDialog = null;
        }
    }
}