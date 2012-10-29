package org.bestforce.testmp4parser;

import java.io.IOException;

import org.bestforce.utils.Ut;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Ut.getTestMp4ParserVideosDir(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
	public void onClickAppendExample(View v) {
		AppendExample appendExample = new AppendExample(this);
		appendExample.append();
	}

	public void onClickShortenExample(View v) throws IOException {
		ShortenExample shortenExample = new ShortenExample(this);
		shortenExample.shorten();
	}

}
