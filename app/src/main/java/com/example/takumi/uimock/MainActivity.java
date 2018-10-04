package com.example.takumi.uimock;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.view.View;
import android.content.Intent;

public class MainActivity extends AppCompatActivity
implements AdapterView.OnItemClickListener {

    private static Callee[] calleeList = {
            new Callee("てんちゃん", R.drawable.tenchan0, "foo"),
            new Callee("Duke mini", R.drawable.miniduke, "bar"),
            new Callee("通りすがりの覆面ねこさん", R.drawable.fuku, "frob"),
            new Callee("池猫1号2号", R.drawable.ikenekos, "frobboz"),
            new Callee("Duke XS Max(Summer 2018)",R.drawable.duke_summer, "baz")
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView contactList = findViewById(R.id.contact_list);
        BaseAdapter adapter =
                new ListViewAdapter(this.getApplicationContext(), R.layout.list, calleeList);
        contactList.setAdapter(adapter);

        contactList.setOnItemClickListener(this);

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int pos, long id){
        Intent intent = new Intent(this.getApplicationContext(), CalleeDetailActivity.class);

        String selectedText = calleeList[pos].getName();
        intent.putExtra("Text", selectedText);
        int selectedPic = calleeList[pos].getIcon();
        intent.putExtra("Pic", selectedPic);
        String selectedNumber = calleeList[pos].getNumber();
        intent.putExtra("Number", selectedNumber);

        startActivity(intent);
    }


}
