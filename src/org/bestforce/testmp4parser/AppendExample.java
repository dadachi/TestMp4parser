package org.bestforce.testmp4parser;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import org.bestforce.utils.SimpleThreadFactory;

import com.coremedia.iso.IsoFile;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.builder.FragmentedMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bestforce.utils.Ut;

public class AppendExample {
	
	private static final String TAG = "AppendExample";
    private final Context mCxt;
    private ExecutorService mThreadExecutor = null;
	private SimpleInvalidationHandler mHandler;

	private class SimpleInvalidationHandler extends Handler {

		@Override
		public void handleMessage(final Message msg) {
			switch (msg.what) {
			case R.id.shorten:
//				if(msg.arg1 == 0)
//					Toast.makeText(DriveRecorderActivity.this, R.string.trackwriter_nothing, Toast.LENGTH_LONG).show();
//				else
//					Toast.makeText(DriveRecorderActivity.this, R.string.trackwriter_saved, Toast.LENGTH_LONG).show();

				break;
			case R.id.append:

				if (msg.arg1 == 0)
					Toast.makeText(mCxt,
							mCxt.getString(R.string.message_error) + " " + (String) msg.obj,
							Toast.LENGTH_LONG).show();
				else
					Toast.makeText(mCxt,
							mCxt.getString(R.string.message_appended) + " " + (String) msg.obj,
							Toast.LENGTH_LONG).show();
				break;
			}
		}
	}

	public AppendExample(Context context) {
        mCxt = context;
        mHandler = new SimpleInvalidationHandler();
   }
    
    public void append() {
    	doAppend();
    }

	private void doAppend() {
		if(mThreadExecutor == null)
			mThreadExecutor = Executors.newSingleThreadExecutor(new SimpleThreadFactory("DoAppend"));

		this.mThreadExecutor.execute(new Runnable() {
			public void run() {
				try {
			        File folder = Ut.getTestMp4ParserVideosDir(mCxt);

			        if (!folder.exists()) {
			            Log.d(TAG, "failed to create directory");
			        }

//			        Log.d(TAG, "sample1.mp4 path:" + folder.getPath() + File.separator + "sample1" + ".mp4");
//			        File file1 = new File(folder.getPath() + File.separator + "sample1" + ".mp4");
//			        if (!file1.exists()) {
//			            Log.d(TAG, "file is not exist");
//			        }
//			        File file2 = new File(folder.getPath() + File.separator + "sample2" + ".mp4");
//			        
//			        FileChannel fci1 = new FileInputStream(file1).getChannel();
//			        FileChannel fci2 = new FileInputStream(file2).getChannel();

			        InputStream is1 = mCxt.getResources().openRawResource(R.raw.sample1);
			        InputStream is2 = mCxt.getResources().openRawResource(R.raw.sample2);
			        
//			        FileInputStream is1 = new FileInputStream(file1);

//			        FileInputStream is2 = new FileInputStream(file2);
			        
			        
//			        Movie[] inMovies = new Movie[]{MovieCreator.build(Channels.newChannel(AppendExample.class.getResourceAsStream("/count-deutsch-audio.mp4"))),
//	                MovieCreator.build(Channels.newChannel(AppendExample.class.getResourceAsStream("/count-english-audio.mp4")))};

			        Movie[] inMovies = new Movie[]{MovieCreator.build(Channels.newChannel(is1)), MovieCreator.build(Channels.newChannel(is2))};
//			        Movie[] inMovies = new Movie[]{MovieCreator.build(fci1), MovieCreator.build(fci2)};


			        List<Track> videoTracks = new LinkedList<Track>();
			        List<Track> audioTracks = new LinkedList<Track>();

			        for (Movie m : inMovies) {
			            for (Track t : m.getTracks()) {
			                if (t.getHandler().equals("soun")) {
			                    audioTracks.add(t);
			                }
			                if (t.getHandler().equals("vide")) {
			                    videoTracks.add(t);
			                }
			            }
			        }

			        Movie result = new Movie();

			        if (audioTracks.size() > 0) {
			            result.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
			        }
			        if (videoTracks.size() > 0) {
			            result.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
			        }

			        IsoFile out = new DefaultMp4Builder().build(result);
			        
			        // Create a media file name
			        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
			        String filename = folder.getPath() + File.separator + "TMP4_APP_OUT_" + timeStamp + ".mp4";
			        
			        File fo = new File(filename);
			        
//			        FileChannel fc = new RandomAccessFile(String.format("output.mp4"), "rw").getChannel();
			        FileChannel fco = new FileOutputStream(fo).getChannel();

			        fco.position(0);
			        out.getBox(fco);
			        fco.close();

					Message.obtain(mHandler, R.id.append, 1, 0, filename).sendToTarget();
				} catch (FileNotFoundException e) {
					Message.obtain(mHandler, R.id.append, 0, 0, e.getMessage()).sendToTarget();
					e.printStackTrace();
				} catch (IOException e) {
					Message.obtain(mHandler, R.id.append, 0, 0, e.getMessage()).sendToTarget();
					e.printStackTrace();
				}

			}
		});
	}
    
}
