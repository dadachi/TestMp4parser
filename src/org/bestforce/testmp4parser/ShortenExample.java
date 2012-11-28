package org.bestforce.testmp4parser;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bestforce.utils.SimpleThreadFactory;
import org.bestforce.utils.Ut;

/**
 * Shortens/Crops a track
 */
public class ShortenExample {

	private static final String TAG = "ShortenExample";
    private final Context mCxt;
    private ExecutorService mThreadExecutor = null;
	private SimpleInvalidationHandler mHandler;
	private ProgressDialog mProgressDialog;

	private class SimpleInvalidationHandler extends Handler {

		@Override
		public void handleMessage(final Message msg) {
			switch (msg.what) {
			case R.id.shorten:
				mProgressDialog.dismiss();

				if (msg.arg1 == 0)
					Toast.makeText(mCxt,
							mCxt.getString(R.string.message_error) + " " + (String) msg.obj,
							Toast.LENGTH_LONG).show();
				else
					Toast.makeText(mCxt,
							mCxt.getString(R.string.message_shortened) + " " + (String) msg.obj,
							Toast.LENGTH_LONG).show();
				break;
			}
		}
	}
	
    public ShortenExample(Context context) {
        mCxt = context;
        mHandler = new SimpleInvalidationHandler();
    }

    public void shorten() {
    	doShorten();
    }

    private void doShorten() {
    	mProgressDialog = Ut.ShowWaitDialog(mCxt, 0);
        
		if(mThreadExecutor == null)
			mThreadExecutor = Executors.newSingleThreadExecutor(new SimpleThreadFactory("doShorten"));

		this.mThreadExecutor.execute(new Runnable() {
			public void run() {
				try {
			        File folder = Ut.getTestMp4ParserVideosDir(mCxt);

			        if (!folder.exists()) {
			            Log.d(TAG, "failed to create directory");
			        }

			        //Movie movie = new MovieCreator().build(new RandomAccessFile("/home/sannies/suckerpunch-distantplanet_h1080p/suckerpunch-distantplanet_h1080p.mov", "r").getChannel());
//			        Movie movie = MovieCreator.build(new FileInputStream("/home/sannies/CSI.S13E02.HDTV.x264-LOL.mp4").getChannel());
			        Movie movie = MovieCreator.build(new FileInputStream(new File(folder.getPath() + File.separator + "sample1" + ".mp4")).getChannel());

			        List<Track> tracks = movie.getTracks();
			        movie.setTracks(new LinkedList<Track>());
			        // remove all tracks we will create new tracks from the old

			        double startTime = 0;
			        double endTime = (double) getDuration(tracks.get(0)) / tracks.get(0).getTrackMetaData().getTimescale();

			        boolean timeCorrected = false;

			        // Here we try to find a track that has sync samples. Since we can only start decoding
			        // at such a sample we SHOULD make sure that the start of the new fragment is exactly
			        // such a frame
			        for (Track track : tracks) {
			            if (track.getSyncSamples() != null && track.getSyncSamples().length > 0) {
			                if (timeCorrected) {
			                    // This exception here could be a false positive in case we have multiple tracks
			                    // with sync samples at exactly the same positions. E.g. a single movie containing
			                    // multiple qualities of the same video (Microsoft Smooth Streaming file)

			                    throw new RuntimeException("The startTime has already been corrected by another track with SyncSample. Not Supported.");
			                }
			                startTime = correctTimeToSyncSample(track, startTime, false);
			                endTime = correctTimeToSyncSample(track, endTime, true);
			                timeCorrected = true;
			            }
			        }

			        for (Track track : tracks) {
			            long currentSample = 0;
			            double currentTime = 0;
			            long startSample = -1;
			            long endSample = -1;

			            for (int i = 0; i < track.getDecodingTimeEntries().size(); i++) {
			                TimeToSampleBox.Entry entry = track.getDecodingTimeEntries().get(i);
			                for (int j = 0; j < entry.getCount(); j++) {
			                    // entry.getDelta() is the amount of time the current sample covers.

			                    if (currentTime <= startTime) {
			                        // current sample is still before the new starttime
			                        startSample = currentSample;
			                    }
			                    if (currentTime <= endTime) {
			                        // current sample is after the new start time and still before the new endtime
			                        endSample = currentSample;
			                    } else {
			                        // current sample is after the end of the cropped video
			                        break;
			                    }
			                    currentTime += (double) entry.getDelta() / (double) track.getTrackMetaData().getTimescale();
			                    currentSample++;
			                }
			            }
			            movie.addTrack(new CroppedTrack(track, startSample, endSample));
			        }
			        long start1 = System.currentTimeMillis();
			        IsoFile out = new DefaultMp4Builder().build(movie);
			        long start2 = System.currentTimeMillis();
			        
//			        FileOutputStream fos = new FileOutputStream(String.format("output-%f-%f.mp4", startTime, endTime));
			        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
			        String filename = folder.getPath() + File.separator + String.format("TMP4_APP_OUT-%f-%f", startTime, endTime) + "_" + timeStamp + ".mp4";
			        FileOutputStream fos = new FileOutputStream(filename);
			        
			        FileChannel fc = fos.getChannel();
			        out.getBox(fc);
			        fc.close();
			        fos.close();
			        long start3 = System.currentTimeMillis();
			        System.err.println("Building IsoFile took : " + (start2 - start1) + "ms");
			        System.err.println("Writing IsoFile took  : " + (start3 - start2) + "ms");
			        System.err.println("Writing IsoFile speed : " + (new File(String.format("TMP4_APP_OUT-%f-%f", startTime, endTime)).length() / (start3 - start2) / 1000) + "MB/s");

					Message.obtain(mHandler, R.id.shorten, 1, 0, filename).sendToTarget();
				} catch (FileNotFoundException e) {
					Message.obtain(mHandler, R.id.shorten, 0, 0, e.getMessage()).sendToTarget();
					e.printStackTrace();
				} catch (IOException e) {
					Message.obtain(mHandler, R.id.shorten, 0, 0, e.getMessage()).sendToTarget();
					e.printStackTrace();
				}

			}
		});

    }

    protected static long getDuration(Track track) {
        long duration = 0;
        for (TimeToSampleBox.Entry entry : track.getDecodingTimeEntries()) {
            duration += entry.getCount() * entry.getDelta();
        }
        return duration;
    }

    private static double correctTimeToSyncSample(Track track, double cutHere, boolean next) {
        double[] timeOfSyncSamples = new double[track.getSyncSamples().length];
        long currentSample = 0;
        double currentTime = 0;
        for (int i = 0; i < track.getDecodingTimeEntries().size(); i++) {
            TimeToSampleBox.Entry entry = track.getDecodingTimeEntries().get(i);
            for (int j = 0; j < entry.getCount(); j++) {
                if (Arrays.binarySearch(track.getSyncSamples(), currentSample + 1) >= 0) {
                    // samples always start with 1 but we start with zero therefore +1
                    timeOfSyncSamples[Arrays.binarySearch(track.getSyncSamples(), currentSample + 1)] = currentTime;
                }
                currentTime += (double) entry.getDelta() / (double) track.getTrackMetaData().getTimescale();
                currentSample++;
            }
        }
        double previous = 0;
        for (double timeOfSyncSample : timeOfSyncSamples) {
            if (timeOfSyncSample > cutHere) {
                if (next) {
                    return timeOfSyncSample;
                } else {
                    return previous;
                }
            }
            previous = timeOfSyncSample;
        }
        return timeOfSyncSamples[timeOfSyncSamples.length - 1];
    }


}
