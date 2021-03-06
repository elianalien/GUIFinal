package com.nantonelli.guifinal.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.nantonelli.guifinal.Events.ShowSongDialogEvent;
import com.nantonelli.guifinal.FinalApplication;
import com.nantonelli.guifinal.Model.Favorite;
import com.nantonelli.guifinal.Model.Song;
import com.nantonelli.guifinal.Model.SongsRepo;
import com.nantonelli.guifinal.R;
import com.squareup.otto.Bus;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by ndantonelli on 11/19/15.
 * this adapter does the majority of the work for this project
 * it handles playback of the music and making sure that 2 songs dont play at once
 */
public class GridAdapter extends BaseAdapter {
    private static class ViewHolder{
        ImageView image;
        TextView text;
        ViewFlipper flipper;
        ProgressBar progress;
        ImageView pause;
        ImageView play;
        MediaRunnable runnable;
        ImageView star;
    }
    @Inject Picasso picasso;
    @Inject Typeface typeface;
    @Inject Bus eventBus;
    @Inject SongsRepo repo;

    private Context mContext;
    private List<Song> songs;
    private Handler mHandler;
    private MediaPlayer player;

    int playingPos = -1;

    public GridAdapter(Context context, List<Song> events){
        this.songs = events;
        mContext = context;
        FinalApplication.getInstance().getObjectGraph().inject(this);
        mHandler = new Handler();
        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    public void refresh(List<Song> songs){

        this.songs = songs;
        notifyDataSetChanged();
    }
    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int position) {
        return songs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder vHold;
        final Song temp = songs.get(position);
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            vHold = new ViewHolder();
            convertView = inflater.inflate(R.layout.component_grid_item, parent, false);
            vHold.text= (TextView) convertView.findViewById(R.id.title);
            vHold.text.setTypeface(typeface);
            vHold.image= (ImageView)convertView.findViewById(R.id.back_img);
            vHold.flipper = (ViewFlipper) convertView.findViewById(R.id.play_pause_flipper);
            vHold.progress = (ProgressBar) convertView.findViewById(R.id.progressBar);
            vHold.pause = (ImageView) convertView.findViewById(R.id.pause_button);
            vHold.play = (ImageView) convertView.findViewById(R.id.play_button);
            vHold.runnable = new MediaRunnable(vHold);
            vHold.star = (ImageView) convertView.findViewById(R.id.favorite_star);
            convertView.setTag(vHold);
        } else {
            vHold = (ViewHolder)convertView.getTag();
            vHold.runnable = new MediaRunnable(vHold);
            vHold.runnable.progress = 0;
        }

        if (temp.isExplicit()) vHold.text.setTextColor(Color.RED);
        vHold.text.setTextColor(Color.WHITE);
        vHold.text.setText(temp.getCensorTitle());
        vHold.text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eventBus.post(new ShowSongDialogEvent(temp));
            }
        });
        picasso.load(temp.getArtUrl()).fit().priority(Picasso.Priority.HIGH).into(vHold.image);
        if(repo.isFavorite(temp)){
            vHold.star.setAlpha(1.0f);
            vHold.star.setOnClickListener(null);
        }
        else{
            vHold.star.setAlpha(.5f);
            vHold.star.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    vHold.star.setAlpha(1.0f);
                    Favorite fave = new Favorite(temp);
                    repo.addFavorite(fave);
                    fave.save();
                    vHold.star.setOnClickListener(null);
                }
            });
        }

        //determines the view for a song that is already playing
        if(position == playingPos) {
            vHold.flipper.setDisplayedChild(1);
            mHandler.post(vHold.runnable);
            vHold.pause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mHandler.removeCallbacksAndMessages(null);
                    player.stop();
                    player.reset();
                    vHold.flipper.setDisplayedChild(0);
                    vHold.progress.setProgress(0);
                    vHold.runnable.progress = 0;
                    playingPos = -1;
                    vHold.pause.setOnClickListener(null);
                    notifyDataSetChanged();
                }
            });
        }
        else {
            vHold.flipper.setDisplayedChild(0);
            vHold.runnable.progress = 0;
            vHold.play.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(playingPos != -1) {
                        mHandler.removeCallbacksAndMessages(null);
                        player.stop();
                        player.reset();
                        notifyDataSetChanged();
                    }
                    playingPos = position;
                    vHold.flipper.setDisplayedChild(1);
                    vHold.pause.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mHandler.removeCallbacksAndMessages(null);
                            player.stop();
                            player.reset();
                            vHold.flipper.setDisplayedChild(0);
                            vHold.progress.setProgress(0);
                            vHold.runnable.progress = 0;
                            playingPos = -1;
                            vHold.pause.setOnClickListener(null);
                            notifyDataSetChanged();
                        }
                    });
                    try {
                        player.setDataSource(temp.getPreviewUrl());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mp.start();
                            mHandler.removeCallbacksAndMessages(null);
                            mHandler.postDelayed(vHold.runnable, 1000);
                        }
                    });
                    player.prepareAsync();
                }
            });
        }
        return convertView;
    }

    //handles updating the progress bar for the playing song
    private class MediaRunnable implements Runnable {

        private ViewHolder vHold;
        int progress;
        public MediaRunnable(ViewHolder vHold){
            this.vHold = vHold;
            progress = 0;
        }

        @Override
        public void run(){
            progress = player.getCurrentPosition();
            vHold.progress.setProgress(progress);
            if(progress < 28477)
                mHandler.postDelayed(this, 1000);
            else{
                player.stop();
                player.reset();
                vHold.flipper.setDisplayedChild(0);
                progress = 0;
                playingPos = -1;
            }

        }
    }
}