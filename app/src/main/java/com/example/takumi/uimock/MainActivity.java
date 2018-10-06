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
            new Callee("じゅんこ", R.drawable.ashu, "foo"),
            new Callee("emiko", R.drawable.nikukyu, "bar"),
            new Callee("住田さん", R.drawable.nekochan, "frob"),
            new Callee("鰈崎さん", R.drawable.karei, "frobboz"),
            new Callee("土井さん",R.drawable.duke_summer, "baz")
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
