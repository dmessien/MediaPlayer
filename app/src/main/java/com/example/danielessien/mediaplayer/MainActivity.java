package com.example.danielessien.mediaplayer;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity {

    MediaPlayer mediaPlayer = new MediaPlayer();

    private static int volumeScale = 0;

    private static int current = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mediaPlayer = MediaPlayer.create(this, R.raw.musette);

        PositionerTask positionerTask = new PositionerTask();
        positionerTask.execute();
        //mediaPlayer.start();

        int milliseconds = mediaPlayer.getDuration();
        int hours = milliseconds / (1000 * 60 * 60);
        milliseconds -= 1000 * 60 * 60 * hours;
        int minutes = milliseconds / (1000 * 60);
        milliseconds -= 1000 * 60 * minutes;
        float seconds = milliseconds / 1000f;

        String s = String.format("Duration: %02d:%02d:%06.3f",
                hours, minutes, seconds);
        final TextView textView = (TextView)findViewById(R.id.textView);

        textView.append("\n" + s);

        Button button = (Button)findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button button = (Button) view;
                int id;
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    id = R.string.play;
                } else {
                    mediaPlayer.start(); //or resume from where we left off
                    id = R.string.pause;
                }
                button.setText(id);
            }
        });

        int duration = mediaPlayer.getDuration();

        float fraction = (float)milliseconds/duration;

        SeekBar seekBar = (SeekBar)MainActivity.this.findViewById(R.id.seekBar);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                float fraction = (float)progress / seekBar.getMax();
                int duration = mediaPlayer.getDuration();
                mediaPlayer.seekTo((int)(fraction*duration));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mediaPlayer.getCurrentPosition();

        AudioManager am =
                (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        current = am.getStreamVolume(am.STREAM_MUSIC);
        volumeScale = current*10;

        Toast.makeText(MainActivity.this, "Current Volume = " + am.getStreamVolume(am.STREAM_MUSIC), Toast.LENGTH_LONG).show();

        final RelativeLayout relativeLayout = (RelativeLayout)findViewById(R.id.relativeLayout);

        final ScaleGestureDetector scaleGestureDetector =
                new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {

                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        TextView textView2 = (TextView)findViewById(R.id.textView2);
                        final float scaleFactor = detector.getScaleFactor();
                        String verdict = null;
                        AudioManager audioManager =
                                (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                        int max = audioManager.getStreamMaxVolume(audioManager.STREAM_MUSIC);


                        if (scaleFactor < 1) {
                            verdict = "pinch";

                            current = Math.round((float)volumeScale/10);

                            if(current > 0){
                                volumeScale--;
                                textView2.setText("Volume: "+volumeScale);
                                Toast.makeText(MainActivity.this, "Volume: " + current, Toast.LENGTH_SHORT).show();
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, current, 0);
                                /*audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                                        AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);*/
                            }

                        } else if (scaleFactor > 1) {
                            verdict = "spread";

                            current = Math.round((float)volumeScale/10);

                            if(current < max){
                                volumeScale++;
                                textView2.setText(""+volumeScale);
                                Toast.makeText(MainActivity.this, "Volume: " + current, Toast.LENGTH_SHORT).show();
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, current, 0);
                                /*audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                                        AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);*/

                            }
                        } else {
                            verdict = "neither";
                        }

                        float rounded = Math.round(1000 * scaleFactor) / 1000f;	//to nearest thousandth
                        //textView2.setTextSize(TypedValue.COMPLEX_UNIT_PX, scaleFactor * textView.getTextSize());
                        //textView2.setText((int)detector.getCurrentSpan() + "\n" + verdict + "\n" + rounded);

                        return true;
                    }
                });

        relativeLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                scaleGestureDetector.onTouchEvent(event);
                return true;
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null){
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mediaPlayer != null){
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class PositionerTask extends AsyncTask<Void, Integer, Void> {

        //This method is executed by the second thread.
        //It gets its arguments from the execute method.

        @Override
        protected Void doInBackground(Void... v) {
            for (;;) {
                int position = mediaPlayer.getCurrentPosition();
                publishProgress(position);
                try {
                    Thread.sleep(100L);   //milliseconds
                } catch (InterruptedException interruptedException) {
                }
            }
        }

        //This method is executed by the UI thread.
        //It gets its arguments from the publishProgress method.

        @Override
        protected void onProgressUpdate(Integer... position) {
            int milliseconds = position[0].intValue();

            int duration = mediaPlayer.getDuration();
            float fraction = (float)milliseconds / duration;
            SeekBar seekBar = (SeekBar)MainActivity.this.findViewById(R.id.seekBar);
            seekBar.setProgress((int) (fraction * seekBar.getMax()));
            //Display milliseconds in the second TextView;

            TextView textView2 = (TextView)MainActivity.this.findViewById(R.id.textView2);
            textView2.setText(""+milliseconds);


        }

        //This method is executed by the UI thread when doInBackground has finished.
        //Its argument is the return value ofDoInBackground.
        //But our doInBackground never finishes, so onPostExecure is never called.

        @Override
        protected void onPostExecute(Void v) {
        }
    }
}
