package com.example.takumi.uimock;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Intent;
import android.widget.Toast;

import com.example.takumi.uimock.util.WebRTCHelper;


public class CalleeDetailActivity extends AppCompatActivity {
    private WebRTCHelper webRTCHelper;

    private boolean cover;
    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.activity_callee_detail);

        Intent intent = getIntent();

        final String selectedText = intent.getStringExtra("Text");
        TextView textView = findViewById(R.id.selected_txt);
        textView.setText(selectedText);

        int selectedPic = intent.getIntExtra("Pic", 0);
        ImageView imageView = findViewById(R.id.selected_pic);
        imageView.setImageResource(selectedPic);

        final String selectedNumber = intent.getStringExtra("Number");

        ImageButton btn = findViewById(R.id.call_button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplication(), com.google.firebase.samples.apps.mlkit.java.LivePreviewActivity.class);
                //Intent i = new Intent(getApplication(), VideoCallActivity.class);
                i.putExtra("Text", selectedText);
                i.putExtra("Number", selectedNumber);

                webRTCHelper.getPeerId(selectedText, i);

//                webRTCHelper.showPeerIDs();
//                webRTCHelper.onPeerSelected();
//
//                startActivity(i);
            }
        }
        );

        webRTCHelper = WebRTCHelper.getInstance(this);

        webRTCHelper.getAllPeers();
    }

    @Override
    protected void onResume() {
        if (webRTCHelper != null) {
            webRTCHelper.setContext(this);
        }
        super.onResume();
    }

}
