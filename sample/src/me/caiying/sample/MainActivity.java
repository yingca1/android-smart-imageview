package me.caiying.sample;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

public class MainActivity extends Activity {
    private ListView mListView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.mListView = (ListView) findViewById(R.id.listview);
        this.mListView.setAdapter(new IgListAdapter(this));
    }

}
