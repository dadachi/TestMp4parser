package org.bestforce.testmp4parser;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
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
	private ProgressDialog mProgressDialog;

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
				mProgressDialog.dismiss();

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
    	mProgressDialog = Ut.ShowWaitDialog(mCxt, 0);

    	if(mThreadExecutor == null)
			mThreadExecutor = Executors.newSingleThreadExecutor(new SimpleThreadFactory("DoAppend"));

		this.mThreadExecutor.execute(new Runnable() {
			public void run() {
				try {
			        File folder = Ut.getTestMp4ParserVideosDir(mCxt);

			        if (!folder.exists()) {
			            Log.d(TAG, "failed to create directory");
			        }
			        
//			        AssetFileDescriptor afd1 = mCxt.getResources().openRawResourceFd(R.raw.sample1);
//			        AssetFileDescriptor afd2 = mCxt.getResources().openRawResourceFd(R.raw.sample2);
//			        FileDescriptor fd1 = afd1.getFileDescriptor();
//			        FileDescriptor fd2 = afd2.getFileDescriptor();
//			        FileChannel fc1 = new FileInputStream(fd1).getChannel();
//			        FileChannel fc2 = new FileInputStream(fd2).getChannel();
//			        
//			        Movie[] inMovies = new Movie[]{MovieCreator.build(fc1),
//			                MovieCreator.build(fc2)};
//
//			        afd1.close();
//			        afd2.close();
			        
//			        InputStream is1 = getResources().openRawResource(R.raw.sample1);
//			        InputStream is2 = getResources().openRawResource(R.raw.sample2);
			        
			        FileInputStream fis1 = new FileInputStream(folder.getPath() + File.separator + "sample1" + ".mp4");
			        FileInputStream fis2 = new FileInputStream(folder.getPath() + File.separator + "sample2" + ".mp4");
			        
//			        Movie[] inMovies = new Movie[]{MovieCreator.build(new FileInputStream(folder.getPath() + File.separator + "sample1" + ".mp4").getChannel()),
//			                MovieCreator.build(new FileInputStream(folder.getPath() + File.separator + "sample2" + ".mp4").getChannel())};
			        Movie[] inMovies = new Movie[]{MovieCreator.build(fis1.getChannel()),
	                MovieCreator.build(fis2.getChannel())};
			        
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
			        
//			        File fo = new File(filename);
			        
//			        FileChannel fc = new RandomAccessFile(String.format("output.mp4"), "rw").getChannel();
//			        FileChannel fco = new FileOutputStream(filename).getChannel();
			        FileOutputStream fos = new FileOutputStream(filename);
			        FileChannel fco = fos.getChannel();

			        fco.position(0);
			        out.getBox(fco);
			        fco.close();
			        fos.close();
			        fis1.close();
			        fis2.close();

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
