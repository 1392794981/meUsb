package com.example.xia.meusb;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.nio.ByteOrder;
import java.nio.channels.Selector;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends FragmentActivity {

    private ViewPager viewPager;  //对应的viewPager
    private View viewComplex, viewSimple;
    private View viewPlayer;
    private List<View> viewList;//view数组

    private MediaPlayer recordPlayer;
    private MediaRecorder mediaRecorder;
    private ProgressDialog recordDialog;

    String strRecordPath;
    boolean isRecording = false;
    //------------------------
    private FFmpeg fFmpeg;//=FFmpeg.getInstance(this);

    ///--------------------------------
    private int bufferSizeInShort = 0;//缓冲区大小
    //音频获取来源
    private int audioSource = MediaRecorder.AudioSource.MIC;
    //设置音频的采样率，44100是目前的标准，但是某些设备仍然支持22050,16000,11025
    private static int sampleRateInHz = 44100;
    //设置音频的录制声道，CHANNEL_IN_STEREO为双声道，CHANNEL_CONFIGURATION_MONO为单声道
    private static int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
    //设置音频数据格式:PCM 16位每个样本，保证设备支持。PCM 8位每个样本，不一定能得到设备的支持。
    private static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    //    private static int audioFormat=AudioFormat.ENCODING_PCM_8BIT;
    //AudioName裸音频数据文件
    private String AudioName;
    //NewAudioName可播放的音频文件
    private String NewAudioName;

    private AudioRecord audioRecord;
    //播放音频
    private MediaPlayer mediaPlayer;
    //====================================
    String strFilePath;
    boolean isShowText = false;
    float playSpeed = 1.0f;
    final int REQUEST_CODE_OPEN_FILE = 6;

    static MediaPlayer player = new MediaPlayer();
    AudioManager audioManager;
    FileDialog fileDialog;

    TextView txtFilePath, txtPosition, txtText, txtTemp, txtCurrentTime;//txtVolume,
    Button btnOpenFile, btnLRC, btnClear, btnForward, btnBack, btnRePlay, btnPlayOrPause, btnPre, btnNext, btnInsertPoint, btnDelPoint, btnShowText, btnVolumeUp, btnVolumeDown;
    Button btnSpeedUp, btnSpeedDown;
    TextView txtShowTextInSecond;
    Button btnShowTextInSecond, btnNextInSecond, btnAnotherNextInSecond, btnPreInSecond;
    Button btnRecord, btnRecordPlay, btnRecordPlayStop;
    Button btnForwardLesson, btnNextLesson;

    Button play_pause_button, backword_button, farward_button, repeat_button, next_button, pre_button, farnext_button, farpre_button;
    Button lrc_button, clear_button,lrcShow_button;
    TextView lrc_text;

    ImageView imageViewProgress;

    SortedList pointList;

    //========================================
    private void creatAudioRecord() {
        //根据AudioRecord的音频采样率、音频录制声道、音频数据格式获取缓冲区大小
        bufferSizeInShort = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);

        //根据音频获取来源、音频采用率、音频录制声道、音频数据格式和缓冲区大小来创建AudioRecord对象
        audioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInShort);

        //创建播放实例

    }

    /**
     * 播放录制的音频
     */
    private void playMusic() {
        File file = new File(NewAudioName);
        if (file.exists()) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {
                txtTemp.setText(e.getMessage());
            }
            try {
//                mediaPlayer.reset();
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(NewAudioName);
                mediaPlayer.prepare();//进行数据缓冲
                mediaPlayer.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 开始录制音频
     */
    private void startAudioRecord() {
        audioRecord.startRecording();//开始录制

        isRecording = true;
        btnRecord.setText("停止录音");
        btnRecord.setBackgroundColor(Color.rgb(0x44, 0x44, 0x44));

        new AudioRecordThread().start();//开启线程来把录制的音频数据保留下来
    }

    /**
     * 停止录制音频
     */
    private void stopAudioRecord() {
        close();
    }

    private void close() {
        if (audioRecord != null) {
            System.out.println("stopRecord");

            isRecording = false;
            txtTemp.setText(strRecordPath);
            btnRecord.setText("开始录音");
            btnRecord.setBackgroundResource(R.drawable.buttonstyle);

            audioRecord.stop();
            audioRecord.release();//释放资源
            audioRecord = null;

            creatAudioRecord();
        }
    }

    /**
     * 音频数据写入线程
     *
     * @author Administrator
     */
    class AudioRecordThread extends Thread {
        @Override
        public void run() {
            super.run();
            writeDataToFile();//把录制的音频裸数据写入到文件中去
            copyWaveFile(AudioName, NewAudioName);//给裸数据加上头文件
        }
    }

    /**
     * 把录制的音频裸数据写入到文件中去
     * 这里将数据写入文件，但是并不能播放，因为AudioRecord获得的音频是原始的裸音频，
     * 如果需要播放就必须加入一些格式或者编码的头信息。但是这样的好处就是你可以对音频的 裸数据进行处理，比如你要做一个爱说话的TOM
     * 猫在这里就进行音频的处理，然后重新封装 所以说这样得到的音频比较容易做一些音频的处理。
     */
    private void writeDataToFile() {
        // new一个byte数组用来存一些字节数据，大小为缓冲区大小
        short[] audioData = new short[bufferSizeInShort];
        int readSize = 0;
        FileOutputStream fos = null;
        File file = new File(AudioName);
        if (file.exists())
            file.delete();
        try {
            fos = new FileOutputStream(file);//获取一个文件的输出流
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        while (isRecording == true) {
            readSize = audioRecord.read(audioData, 0, bufferSizeInShort);
            if (AudioRecord.ERROR_INVALID_OPERATION != readSize) {
                try {
                    long temp;
                    for (int i = 0; i < audioData.length; i++) {
                        temp = audioData[i];

//                        if (-0xf < temp && temp < 0xf)
//                            temp = 0;

                        temp = temp * 2;

                        if (temp < -0x7fff) {
                            temp = -0x7fff;
                        } else if (temp > 0x7fff) {
                            temp = 0x7fff;
                        }
                        audioData[i] = (short) (temp);
                    }

                    fos.write(Shorts2Bytes(audioData));
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        try {
            fos.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public byte[] Shorts2Bytes(short[] s) {
        byte bLength = 2;
        byte[] buf = new byte[s.length * bLength];

        for (int iLoop = 0; iLoop < s.length; iLoop++) {
            byte[] temp = getBytes(s[iLoop]);

            for (int jLoop = 0; jLoop < bLength; jLoop++) {
                buf[iLoop * bLength + jLoop] = temp[jLoop];
            }
        }

        return buf;
    }

    public byte[] getBytes(short s) {
        return getBytes(s, this.testCPU());
    }

    public byte[] getBytes(short s, boolean bBigEnding) {
        byte[] buf = new byte[2];

        if (bBigEnding) {
            for (int i = buf.length - 1; i >= 0; i--) {
                buf[i] = (byte) (s & 0x00ff);
                s >>= 8;
            }
        } else {
            for (int i = 0; i < buf.length; i++) {
                buf[i] = (byte) (s & 0x00ff);
                s >>= 8;
            }
        }

        return buf;
    }

    public boolean testCPU() {
        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
            // System.out.println("is big ending");
            return true;
        } else {
            // System.out.println("is little ending");
            return false;
        }
    }

    void calc1(short[] lin, int off, int len) {
        int i, j;
        for (i = 0; i < len; i++) {
            j = lin[i + off];
            lin[i + off] = (short) (j >> 2);
        }
    }

    private void copyWaveFile(String inFileName, String outFileName) {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = sampleRateInHz;
        int channels = 2;
        long byteRate = 16 * sampleRateInHz * channels / 8;

        byte[] data = new byte[bufferSizeInShort * 2];

        try {
            in = new FileInputStream(inFileName);
            out = new FileOutputStream(outFileName);

            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            WriteWaveFileHeader(out, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 这里提供一个头信息。插入这些信息就可以得到可以播放的文件。
     * 为我为啥插入这44个字节，这个还真没深入研究，不过你随便打开一个wav
     * 音频的文件，可以发现前面的头文件可以说基本一样哦。每种格式的文件都有
     * 自己特有的头文件。
     */
    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, long byteRate)
            throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }
    //==============================================================

    static class HandlerProgress extends Handler {
        WeakReference<MainActivity> mActivity;

        HandlerProgress(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            MainActivity theActivity = mActivity.get();
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    ImageView image = theActivity.imageViewProgress;
                    progressShow(theActivity, image);
                    image = theActivity.findViewById(R.id.surfaceViewProgress_player);
                    progressShow(theActivity, image);
                    break;
            }
        }

        private void progressShow(MainActivity theActivity, ImageView image) {
            MediaPlayer player = theActivity.player;
            SortedList list = theActivity.pointList;

            int width = image.getWidth();
            int height = image.getHeight();

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            paint.setColor(Color.argb(0xff, 0x11, 0x11, 0x11));
            canvas.drawRect(0, 0, width, height, paint);
            paint.setColor(Color.argb(0xff, 0x88, 0x88, 0x88));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1);
            canvas.drawRect(40, 30, 1040, 90, paint);
            //canvas.drawRect(40, 40, 1040, 80, paint);
            try {
                if (player.getDuration() > 0) {
                    int duration = player.getDuration();
                    int position = player.getCurrentPosition();
                    int value = (position * 1000) / duration;
                    int startValue = (int) ((Math.round(list.getValueByPosition(list.position) * 1000) / duration) + 40);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawRect(startValue, 46, value + 40, 74, paint);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(3);
                    for (int i = 0; i < list.getSize(); i++) {
                        value = (int) ((Math.round(list.getValueByPosition(i)) * 1000) / duration + 40);
                        canvas.drawLine(value, 30, value, 90, paint);
                    }
                    value = (int) ((Math.round(list.getValueByPosition(list.position) * 1000) / duration) + 40);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(value, 82, 6, paint);
                    if (player.getCurrentPosition() >= list.getNextPoint())
                        player.pause();
                    String str = "#";
                    for (int i = 0; i < theActivity.pointList.getSize(); i++)
                        str = str + " " + theActivity.pointList.getValueByPosition(i);
                    int m = (position / 1000) / 60;
                    int s = (position / 1000) % 60;
                    long currentPoint = (long) list.getCurrentPoint();
                    String currentPointString = String.valueOf((currentPoint / 1000) / 60) + "分" + String.valueOf((currentPoint / 1000) % 60) + "秒";
                    long lastPoint = (long) list.getLastPoint();
                    String lastPointString = String.valueOf((lastPoint / 1000) / 60) + "分" + String.valueOf((lastPoint / 1000) % 60) + "秒";
                    theActivity.txtPosition.setText(currentPointString + "->" + String.valueOf(m) + "分" + String.valueOf(s) + "秒/" + lastPointString +
                            " [" + "音量:" + String.valueOf(theActivity.getCurrentVolume()) + " 速度:" + String.valueOf(theActivity.playSpeed) + "]");

                    Calendar cal = Calendar.getInstance();
                    cal.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
                    String hour = "";
                    if (cal.get(Calendar.AM_PM) == 0)
                        hour = String.valueOf(cal.get(Calendar.HOUR));
                    else
                        hour = String.valueOf(cal.get(Calendar.HOUR) + 12);
                    String minute = String.valueOf(cal.get(Calendar.MINUTE));
                    String second = String.valueOf(cal.get(Calendar.SECOND));
                    theActivity.txtCurrentTime.setText("[时间：" + hour + "时" + minute + "分]");//+second+"秒]");

                    if (theActivity.isShowText) {
                        theActivity.btnShowText.setText("隐藏");
                        theActivity.btnShowTextInSecond.setText("隐");
                        str = list.getCurrentDataString();
                        if (str != null) {
                            theActivity.txtText.setText(str);
                            theActivity.txtShowTextInSecond.setText(str);
                        } else {
                            theActivity.txtText.setText("无");
                            theActivity.txtShowTextInSecond.setText("");
                        }
                    } else {
                        theActivity.btnShowText.setText("显示");
                        theActivity.btnShowTextInSecond.setText("显");
                        theActivity.txtText.setText("");
                        theActivity.txtShowTextInSecond.setText("");
                    }

                    theActivity.txtFilePath.setText(theActivity.strFilePath);

                    theActivity.showVolume();

                    if (theActivity.player.isPlaying()) {
                        theActivity.play_pause_button.setBackgroundResource(R.drawable.ic_pause_unpressed);
                    } else
                        theActivity.play_pause_button.setBackgroundResource(R.drawable.ic_play_unpressed);

                    if (theActivity.blPointShow_second) {
                        theActivity.lrc_text.setText("No");
                        theActivity.loadLRC_Second();
                        for (int i = 0; i < theActivity.pointsList_second.size(); i++) {
                            if (i > 0 && (theActivity.pointsList_second.get(i).value+66) > theActivity.player.getCurrentPosition()) {
                                theActivity.lrc_text.setText(theActivity.pointsList_second.get(i - 1).str);
                                break;
                            }
                        }
                    } else {
                        theActivity.lrc_text.setText("");
                    }
                }
            } catch (Exception e) {
                theActivity.txtPosition.setText(e.getMessage());
            }
            image.setImageBitmap(bitmap);
        }
    }

    private final HandlerProgress handlerProgress = new HandlerProgress(this);


    class ThreadProgress extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    txtPosition.setText(e.getMessage());
                }
                Message message = new Message();
                message.what = 1;
                handlerProgress.sendMessage(message);
            }
        }
    }

    ThreadProgress threadProgress = new ThreadProgress();

    @Override
    protected void onResume() {
        super.onResume();

        threadProgress.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.layout_main);

        strRecordPath = getApplicationContext().getFilesDir() + "/record.mp3";
        mediaRecorder = new MediaRecorder();
        recordDialog = new ProgressDialog(MainActivity.this);
        //=====================================
        AudioName = getApplicationContext().getFilesDir() + "/record..raw";
        //NewAudioName可播放的音频文件
        NewAudioName = getApplicationContext().getFilesDir() + "/record..wav";
        creatAudioRecord();
        //========================================
        viewPager = findViewById(R.id.viewpager);
        LayoutInflater inflater = getLayoutInflater();
        viewComplex = inflater.inflate(R.layout.layout_complex, null);
        viewPlayer = inflater.inflate(R.layout.layout_simple_player, null);
        viewSimple = inflater.inflate(R.layout.layout_simple, null);

        viewList = new ArrayList<>();// 将要分页显示的View装入数组中
        viewList.add(viewComplex);
        viewList.add(viewPlayer);
        viewList.add(viewSimple);


        PagerAdapter pagerAdapter = new PagerAdapter() {

            @Override
            public boolean isViewFromObject(View arg0, Object arg1) {
                // TODO Auto-generated method stub
                return arg0 == arg1;
            }

            @Override
            public int getCount() {
                // TODO Auto-generated method stub
                return viewList.size();
            }

            @Override
            public void destroyItem(ViewGroup container, int position,
                                    Object object) {
                // TODO Auto-generated method stub
                container.removeView(viewList.get(position));
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                // TODO Auto-generated method stub
                container.addView(viewList.get(position));


                return viewList.get(position);
            }
        };


        viewPager.setAdapter(pagerAdapter);
        ///===================================
        verifyStoragePermissions(this);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        imageViewProgress = viewComplex.findViewById(R.id.surfaceViewProgress);

//        try {
//            keyBroadcastReceiver = new KeyBroadcastReceiver();
//            keyIntentFilter = new IntentFilter();
//            keyIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
//            registerReceiver(keyBroadcastReceiver, keyIntentFilter);
//        } catch (Exception e) {
//            txtTemp.setText(e.getMessage());
//        }
        //FFmpeg
        try {
            fFmpeg = FFmpeg.getInstance(this.getApplicationContext());
            fFmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onStart() {
                    txtTemp.setText("\n开始" + txtTemp.getText());
                }

                @Override
                public void onFailure() {
                    txtTemp.setText("\n失败" + txtTemp.getText());
                }

                @Override
                public void onSuccess() {
                    txtTemp.setText("\n成功" + txtTemp.getText());
                }

                @Override
                public void onFinish() {
                    txtTemp.setText("\n结束" + txtTemp.getText());
                }

            });

        } catch (Exception e) {
            txtTemp.setText(e.getMessage() + txtTemp.getText());
        }

        txtFilePath = (TextView) viewComplex.findViewById(R.id.txtFilePath);
        txtPosition = (TextView) viewComplex.findViewById(R.id.txtPosition);
        txtCurrentTime = viewComplex.findViewById(R.id.txtCurrentTime);
        txtText = viewComplex.findViewById(R.id.txtText);
//        txtVolume = complexView.findViewById(R.id.txtVolume);
        txtTemp = viewComplex.findViewById(R.id.textView2);

        btnOpenFile = (Button) viewComplex.findViewById(R.id.btnOpenFile);
        btnLRC = (Button) viewComplex.findViewById(R.id.btnLRC);
        btnClear = (Button) viewComplex.findViewById(R.id.btnClear);
        btnForward = (Button) viewComplex.findViewById(R.id.btnForward);
        btnBack = (Button) viewComplex.findViewById(R.id.btnBack);
        btnRePlay = (Button) viewComplex.findViewById(R.id.btnRePlay);
        btnPlayOrPause = (Button) viewComplex.findViewById(R.id.btnPlayOrPause);
        btnNext = (Button) viewComplex.findViewById(R.id.btnNext);
        btnPre = (Button) viewComplex.findViewById(R.id.btnPre);
        btnDelPoint = (Button) viewComplex.findViewById(R.id.btnDelPoint);
        btnInsertPoint = (Button) viewComplex.findViewById(R.id.btnInsertPoint);
        btnShowText = viewComplex.findViewById(R.id.btnShowText);
        btnVolumeUp = viewComplex.findViewById(R.id.btnVolumeUp);
        btnVolumeDown = viewComplex.findViewById(R.id.btnVolumeDown);
        btnSpeedDown = viewComplex.findViewById(R.id.btnSpeedDown);
        btnSpeedUp = viewComplex.findViewById(R.id.btnSpeedUp);
        btnRecord = viewComplex.findViewById(R.id.btnRecord);
        btnRecordPlay = viewComplex.findViewById(R.id.btnRecordPlay);
        btnRecordPlayStop = viewComplex.findViewById(R.id.btnRecordPlayStop);
        btnForwardLesson = viewComplex.findViewById(R.id.btnForwardLesson);
        btnNextLesson = viewComplex.findViewById(R.id.btnNextLesson);

        txtShowTextInSecond = viewSimple.findViewById(R.id.txtShowTextInSecond);
        btnNextInSecond = viewSimple.findViewById(R.id.btnNextInSecond);
        btnShowTextInSecond = viewSimple.findViewById(R.id.btnShowTextInSecond);
        btnPreInSecond = viewSimple.findViewById(R.id.btnPreInSecond);
        btnAnotherNextInSecond = viewSimple.findViewById(R.id.btnAnotherNextInSecond);


        play_pause_button = viewPlayer.findViewById(R.id.play_button);
        backword_button = viewPlayer.findViewById(R.id.backforward_button);
        repeat_button = viewPlayer.findViewById(R.id.repeat_button);
        next_button = viewPlayer.findViewById(R.id.next_button);
        pre_button = viewPlayer.findViewById(R.id.pre_button);
        farnext_button = viewPlayer.findViewById(R.id.farnext_butoon);
        farpre_button = viewPlayer.findViewById(R.id.farpre_button);
        lrc_button = viewPlayer.findViewById(R.id.LRC_button);
        clear_button = viewPlayer.findViewById(R.id.clear_button);
        lrc_text = viewPlayer.findViewById(R.id.lrc_text);
        lrcShow_button=viewPlayer.findViewById(R.id.show_button);

        initCustomSetting();

        String dir = "/storage/";
        if (strFilePath != null && strFilePath.lastIndexOf("/") > 0)
            dir = strFilePath.substring(0, strFilePath.lastIndexOf("/"));

        File dialogDir = new File(dir);
        if (!dialogDir.exists())
            dialogDir = new File("/storage/");
        Log.v("dir", dir);
        //txtTemp.setText(strFilePath+"\n"+ dir);

        fileDialog = new FileDialog(this, dialogDir, "");
        fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
            @Override
            public void fileSelected(File file) {
                strFilePath = file.getAbsolutePath();
                loadSound(strFilePath);
            }
        });

        lrcShow_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                blPointShow_second=!blPointShow_second;
            }
        });

        play_pause_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toAdvancePlayOrPause();
            }
        });

        lrc_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toLRC();
            }
        });

        clear_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toClear();
            }
        });

        backword_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toBack(6000);
            }
        });

        repeat_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toRePlay();
            }
        });

        next_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toNext();
            }
        });

        pre_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toPre();
            }
        });

        farnext_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toNextLesson();
            }
        });

        farpre_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toForwardLesson();
            }
        });

        btnOpenFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toOpenFile();
            }
        });

        btnRePlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toRePlay();
            }
        });

        btnPlayOrPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toAdvancePlayOrPause();
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toNext();
            }
        });

        btnPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toPre();
            }
        });

        btnInsertPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toInsertPoint();
            }
        });

        btnDelPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toDelPoint();
            }
        });

        btnLRC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toLRC();
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toClear();
            }
        });

        btnForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toForward();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toBack();
            }
        });

        btnShowText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toShowText();
            }
        });

        btnVolumeDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toDownVolume();
            }
        });

        btnVolumeUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toUpVolume();
            }
        });

        btnShowTextInSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toShowText();
            }
        });

        btnNextInSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toNext();
            }
        });

        btnAnotherNextInSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toNext();
            }
        });

        btnPreInSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toPre();
            }
        });

        btnSpeedUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toSpeedUp();
            }
        });

        btnSpeedDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toSpeedDown();
            }
        });

        btnRecordPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                toPlayRecord();
                File file = new File(strFilePath + ".txt");
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    fileOutputStream.write("I am a chinese".getBytes());
                    fileOutputStream.close();
                    txtTemp.setText("写入成功！");
                } catch (Exception e) {
                    txtTemp.setText(e.getMessage());
                }

            }
        });

        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //========================
                try {

                    String wavFileName = Environment.getExternalStorageDirectory() + "/create.wav";
                    String cmd = "-i " + strFilePath + " " + wavFileName;
                    File file = new File(wavFileName);
                    if (file.exists()) {
                        file.delete();
                    }
                    txtTemp.setText(wavFileName + "\n" + txtTemp.getText());
                    String[] command = cmd.split(" ");
                    fFmpeg.execute(command, new ExecuteBinaryResponseHandler() {
                        @Override
                        public void onFailure(String s) {
                            txtTemp.setText("\nFAILED with output : " + s + txtTemp.getText());
                        }

                        @Override
                        public void onSuccess(String s) {
                            txtTemp.setText("\nSUCCESS with output : " + s + txtTemp.getText());
                        }

                        @Override
                        public void onProgress(String s) {
                            txtTemp.setText("\nprogress : " + s + txtTemp.getText());
                        }


                        @Override
                        public void onStart() {
                            txtTemp.setText("\nProcessing..." + txtTemp.getText());
                        }

                        @Override
                        public void onFinish() {
                            txtTemp.setText("\nfinished" + txtTemp.getText());
                        }
                    });
                } catch (Exception e) {
                    // do nothing for now
                    txtTemp.setText(e.getMessage() + "\n" + txtTemp.getText());
                }
//                toRecord();
            }
        });

        btnRecordPlayStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                toRecordPlayStop();
                txtTemp.setVisibility(txtTemp.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
            }
        });

        btnForwardLesson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toForwardLesson();
            }
        });

        btnNextLesson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toNextLesson();
            }
        });
    }

    private void toRecordPlayStop() {
        try {
            mediaPlayer.stop();
            mediaPlayer.release();
//            recordPlayer.stop();
//            recordPlayer.release();
        } catch (Exception e) {
            //txtTemp.setText(e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadSound(String soundFilePath) {
        player.reset();
        try {
            player.setDataSource(soundFilePath);
            player.prepare();
            initPoint();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    boolean isAction_Multiple = false;

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        //txtTemp.setText(event.toString());
        //本函数处理onkeydown无法处理的几个键
        if (event.getAction() == KeyEvent.ACTION_MULTIPLE) {
            isAction_Multiple = true;
            String characters = event.getCharacters();//返回全角字符
            if (characters != null) {
                switch (characters) {
                    case "＋":
                        toAdvancePlayOrPause();
                        break;
                    case "－":
                        toRePlay();
                        break;
                    case "＊":
                        toForward();
                        break;
                    case "／":
                        toBack();
                        break;
                }
            }
        }
        return super.dispatchKeyEvent(event);

    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (isAction_Multiple == false && isKeyDown == false) {
            isKeyDown = false;
            switch (keyCode) {
                case KeyEvent.KEYCODE_NUMPAD_DIVIDE:
                    toBack();
                    break;
                case KeyEvent.KEYCODE_NUMPAD_MULTIPLY:
                    toForward();
                    break;
                case KeyEvent.KEYCODE_NUMPAD_ADD:
                    toAdvancePlayOrPause();
                    break;
                case KeyEvent.KEYCODE_NUMPAD_SUBTRACT:
                    toRePlay();
                    break;
            }
        }

        isAction_Multiple = false;
        isKeyDown = false;
        return super.onKeyUp(keyCode, event);
    }

    boolean isKeyDown = false;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //txtTemp.setText(txtTemp.getText()+"\n&&&"+String.valueOf(keyCode) + "\n&&&" + event.toString());
        switch (keyCode) {

            case KeyEvent.KEYCODE_VOLUME_UP: //音量键 上
//                toRePlay();
                toBack(6000);
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN: //音量键 下
                toAdvancePlayOrPause();
                break;

//            case KeyEvent.KEYCODE_DPAD_LEFT:
//                toForwardLesson();
//                break;
//            case KeyEvent.KEYCODE_NUMPAD_4:
//                toForwardLesson();
//                break;
//            case KeyEvent.KEYCODE_DPAD_RIGHT:
//                toNextLesson();
//                break;
//            case KeyEvent.KEYCODE_NUMPAD_6:
//                toNextLesson();
//                break;


            case KeyEvent.KEYCODE_NUMPAD_DIVIDE:
                isKeyDown = true;
                toBack();
                break;
            case KeyEvent.KEYCODE_NUMPAD_MULTIPLY:
                isKeyDown = true;
                toForward();
                break;
            case KeyEvent.KEYCODE_NUMPAD_ADD:
                isKeyDown = true;
                toAdvancePlayOrPause();
                break;
            case KeyEvent.KEYCODE_NUMPAD_SUBTRACT:
                isKeyDown = true;
                toRePlay();
                break;

            case KeyEvent.KEYCODE_NUMPAD_9:
                toPre();
                break;
            case KeyEvent.KEYCODE_PAGE_UP:
                toPre();
                break;
            case KeyEvent.KEYCODE_NUMPAD_3:
                toNext();
                break;
            case KeyEvent.KEYCODE_PAGE_DOWN:
                toNext();
                break;

            case KeyEvent.KEYCODE_NUMPAD_0:
                toInsertPoint();
                break;
            case KeyEvent.KEYCODE_INSERT:
                toInsertPoint();
                break;
            case KeyEvent.KEYCODE_NUMPAD_DOT:
                toDelPoint();
                break;
            case KeyEvent.KEYCODE_FORWARD_DEL:
                toDelPoint();
                break;


            case KeyEvent.KEYCODE_NUMPAD_1:
                toSpeedNormal();
                break;
            case KeyEvent.KEYCODE_MOVE_END:
                toSpeedNormal();
                break;

            case KeyEvent.KEYCODE_DEL:
                toLongBack();
                break;

            case KeyEvent.KEYCODE_MOVE_HOME:
                toLRC();
                break;
            case KeyEvent.KEYCODE_NUMPAD_7:
                toLRC();
                break;

            case KeyEvent.KEYCODE_NUMPAD_5:
                txtTemp.setVisibility(txtTemp.getVisibility() == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE);
                break;

//            case KeyEvent.KEYCODE_NUMPAD_ENTER:
//                toRecord();
//                break;
            case KeyEvent.KEYCODE_NUMPAD_2:
                toNextLesson();
                //toRecord();
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                toNextLesson();
//                toRecord();
                break;
            case KeyEvent.KEYCODE_NUMPAD_8:
//                toPlayRecord();
                toForwardLesson();
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                toForwardLesson();
//                toPlayRecord();
                break;

            case KeyEvent.KEYCODE_BACK: //不屏蔽返回建
                super.onKeyDown(keyCode, event);
                break;
        }
        return true;
    }

    private void toAdvancePlayOrPause() {
        if ((!player.isPlaying()) && (pointList.getNextPoint() <= player.getCurrentPosition()))
            toRePlay();
        else
            toPlayOrPause();
    }

    private void toNextLesson() {
        strFilePath = FileDialog.getNextLession(strFilePath);
        loadSound(strFilePath);
        toRePlay();
    }

    private void toForwardLesson() {
        strFilePath = FileDialog.getForwardLession(strFilePath);
        loadSound(strFilePath);
        toRePlay();
    }

    private void toRecord() {
        if (!isRecording)
            toStartRecord();
        else
            toStopRecord();
    }

    private void toPlayRecord() {
        playMusic();

//        try {
//            recordPlayer.stop();
//            recordPlayer.release();
//        } catch (Exception e) {
//            txtTemp.setText(e.getMessage());
//            e.printStackTrace();
//        }
//        try {
//            recordPlayer = new MediaPlayer();
//            recordPlayer.setDataSource(strRecordPath);
//            recordPlayer.prepare();
//            recordPlayer.start();
//            recordPlayer.setVolume(1f, 1f);
//        } catch (Exception e) {
//            txtTemp.setText(e.getMessage());
//            e.printStackTrace();
//        }
    }

    private void toStopRecord() {
        stopAudioRecord();
//        try {
//            mediaRecorder.stop();
//            mediaRecorder.reset();
//            isRecording = false;
//            txtTemp.setText(strRecordPath);
//            btnRecord.setText("开始录音");
//            btnRecord.setBackgroundColor(Color.rgb(0x11, 0x11, 0x11));
//            btnRecord.setBackgroundResource(R.drawable.buttonstyle);
//        } catch (Exception e) {
//            txtTemp.setText(e.getMessage());
//        }
    }

    private void toStartRecord() {
        startAudioRecord();
//        if (checkPermission()) {
//            try {
//                mediaRecorder.setOutputFile(strRecordPath);
//                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//                mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
//                mediaRecorder.prepare();
//                mediaRecorder.start();
//                isRecording = true;
//                btnRecord.setText("停止录音");
//                btnRecord.setBackgroundColor(Color.rgb(0x44, 0x44, 0x44));
//                txtTemp.setText("正在录音....");
//            } catch (Exception e) {
//                txtTemp.setText(e.getMessage());
//                e.printStackTrace();
//            }
//        } else {
//            requestPermission();
//        }
    }

    private void toSpeedNormal() {
        playSpeed = 1;
        setPlaySpeed();
    }

    private void toSpeedDown() {
        if (playSpeed > 0.11f)
            playSpeed -= 0.1f;
        setPlaySpeed();
    }

    private void setPlaySpeed() {
        playSpeed = new BigDecimal(playSpeed).setScale(1, BigDecimal.ROUND_HALF_UP).floatValue();//四舍五入，保留一位小数
        player.setPlaybackParams(player.getPlaybackParams().setSpeed(playSpeed));
    }

    private void toSpeedUp() {
        if (playSpeed < 2.91)
            playSpeed += 0.1f;
        setPlaySpeed();
    }

    private void showVolume() {
        //txtVolume.setText(String.valueOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));
    }

    private int getCurrentVolume() {
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    private void toUpVolume() {
        int oldIndex = getCurrentVolume();
        int maxIndex = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (oldIndex + 1 < maxIndex)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, oldIndex + 1, AudioManager.FLAG_PLAY_SOUND);
        showVolume();
    }

    private void toDownVolume() {
        int oldIndex = getCurrentVolume();
        if (oldIndex - 1 >= 0)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, oldIndex - 1, AudioManager.FLAG_PLAY_SOUND);
        showVolume();
    }

    private void toClear() {
        pointList.clearPoint();
    }

    private void toLRC() {
        loadLRC();
    }

    private void toShowText() {
        isShowText = !isShowText;
    }


    private void toBack() {
        toBack(2000);
    }

    private void toLongBack() {
        toBack(5000);
    }

    private void toBack(int span) {
        int pos = player.getCurrentPosition();
        if (pos - span > 0)
            player.seekTo(pos - span);
        else
            player.seekTo(0);
        player.start();
    }

    private void toForward() {
        toForward(2000);
    }

    private void toLongForward() {
        toForward(5000);
    }

    private void toForward(int span) {
        int pos = player.getCurrentPosition();
        if (pos + span < player.getDuration())
            player.seekTo(pos + span);
        player.start();
    }

    private void toDelPoint() {
        pointList.deleteCurrentPoint();
        toRePlay();
    }

    private void toInsertPoint() {
        pointList.position = pointList.insertByOrder(player.getCurrentPosition());
        toRePlay();
    }

    private void toPre() {
        if (pointList.position > 0) {
            pointList.position--;
        }
        toRePlay();
    }

    private void toNext() {
        if (pointList.position < pointList.getSize() - 2) {
            pointList.position++;
        }
        toRePlay();
    }

    private void toPlayOrPause() {
        if (player.isPlaying()) player.pause();
        else player.start();
    }

    private void toRePlay() {
        player.seekTo((int) Math.round(pointList.getCurrentPoint()));
        player.start();
    }

    private void toOpenFile() {
        fileDialog.showDialog();
//        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//        intent.setType("audio/MP3");//设置类型，我这里是任意类型，任意后缀的可以这样写。
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        startActivityForResult(intent, REQUEST_CODE_OPEN_FILE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        stopCustomSettings();
//        unregisterReceiver(keyBroadcastReceiver);
    }

    private void stopCustomSettings() {
        String settingFileName = getApplicationContext().getFilesDir() + "/setting.txt";
        File file = new File(settingFileName);
        if (file.exists())
            file.delete();

        try {
            file.createNewFile();
            OutputStream outStream = new FileOutputStream(file);//设置输出流
            OutputStreamWriter out = new OutputStreamWriter(outStream);//设置内容输出方式
            out.write(strFilePath + "\n");//输出内容到文件中
            out.close();
        } catch (java.io.IOException e) {
            android.util.Log.i("stop", e.getMessage());
        }
    }

    private void initCustomSetting() {
        String settingFileName = getApplicationContext().getFilesDir() + "/setting.txt";
        File file = new File(settingFileName);

        try {
//            float speed= (float) 0.5;
//            player.setPlaybackParams(player.getPlaybackParams().setSpeed(speed));

            if (file.exists()) {
                InputStream inputStream = new FileInputStream(file);//读取输入流
                InputStreamReader inputReader = new InputStreamReader(inputStream);//设置流读取方式
                BufferedReader bufferedReader = new BufferedReader(inputReader);
                String line = bufferedReader.readLine();
                strFilePath = line;
                player.reset();
                player.setDataSource(line);
                player.prepare();
                initPoint();
            }
        } catch (Exception e) {
            //txtTemp.setText(e.getMessage());
        }
    }

    protected void initPoint() {
        pointList = new SortedList();
        pointList.insertByOrder(0);
        pointList.insertByOrder(player.getDuration());
    }

    public static String encoder(String filePath) throws Exception {
        BufferedInputStream bin = new BufferedInputStream(
                new FileInputStream(filePath));
        int p = (bin.read() << 8) + bin.read();
        String code = null;
        switch (p) {
            case 0xefbb:
                code = "UTF-8";
                break;
            case 0xfffe:
                code = "Unicode";
                break;
            case 0xfeff:
                code = "UTF-16BE";
                break;
            default:
                code = "GBK";
        }

        return code;
    }

    ArrayList<LRCShow> pointsList_second = new ArrayList<>();
    boolean blPointShow_second = false;

    protected void loadLRC_Second() {
        pointsList_second.clear();
        try {
            String fileName = strFilePath.substring(0, strFilePath.length() - 4) + ".lrc";
            File file = new File(fileName);
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(fileName);
                InputStreamReader isr = new InputStreamReader(fis, encoder(fileName));
                BufferedReader br = new BufferedReader(isr);
                String str;
                while ((str = br.readLine()) != null) {
                    Pattern pattern = Pattern.compile("\\[[0-9]+:[0-9]+\\.[0-9]+\\]");
                    Matcher matcher = pattern.matcher(str);
                    if (matcher.find()) {
                        String strValue = matcher.group();
                        Double value = Double.valueOf(strValue.substring(1, 3)) * 60 * 1000 + Double.valueOf(str.substring(4, strValue.length() - 1)) * 1000;
                        if (value > 1000) {
                            String string = str.substring(strValue.length(), str.length());
                            pointsList_second.add(new LRCShow(value, string));
                        }
                    }

//                    Double value = Double.valueOf(str.substring(1, 3)) * 60 * 1000 + Double.valueOf(str.substring(4, 10)) * 1000;
//                    String string = str.substring(11, str.length());
//                    pointList.insertByOrder(value, string);
                }
                br.close();
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG);
        }
    }

    protected void loadLRC() {
        try {
            String fileName = strFilePath.substring(0, strFilePath.length() - 4) + ".lrc";
            File file = new File(fileName);
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(fileName);
                InputStreamReader isr = new InputStreamReader(fis, encoder(fileName));
                BufferedReader br = new BufferedReader(isr);
                String str;
                while ((str = br.readLine()) != null) {
                    Pattern pattern = Pattern.compile("\\[[0-9]+:[0-9]+\\.[0-9]+\\]");
                    Matcher matcher = pattern.matcher(str);
                    if (matcher.find()) {
                        String strValue = matcher.group();
                        Double value = Double.valueOf(strValue.substring(1, 3)) * 60 * 1000 + Double.valueOf(str.substring(4, strValue.length() - 1)) * 1000;
                        if (value > 1000) {
                            String string = str.substring(strValue.length(), str.length());
                            pointList.insertByOrder(value, string);
                        }
                    }

//                    Double value = Double.valueOf(str.substring(1, 3)) * 60 * 1000 + Double.valueOf(str.substring(4, 10)) * 1000;
//                    String string = str.substring(11, str.length());
//                    pointList.insertByOrder(value, string);
                }
                br.close();
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_OPEN_FILE) {
                strFilePath = getRealPath(data.getData());
                txtFilePath.setText(strFilePath);
                try {
                    player.reset();
                    player.setDataSource(strFilePath);
                    player.prepare();
                    initPoint();
                } catch (IOException e) {
                    txtFilePath.setText(e.getMessage());
                }
            }
        }
    }

    protected String getRealPath(Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {"_data"};
            Cursor cursor = this.getContentResolver().query(uri, projection, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow("_data");
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        } else {
            return null;
        }
    }


    //

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final int REQ_PERMISSION_AUDIO = 0x02;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.MOUNT_UNMOUNT_FILESYSTEMS"};

    private boolean checkPermission() {
        int result = ActivityCompat.checkSelfPermission(getApplicationContext(),
                WRITE_EXTERNAL_STORAGE);
        int result1 = ActivityCompat.checkSelfPermission(getApplicationContext(),
                RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED &&
                result1 == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new
                String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, REQ_PERMISSION_AUDIO);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQ_PERMISSION_AUDIO:
                if (grantResults.length > 0) {
                    boolean StoragePermission = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;
                    boolean RecordPermission = grantResults[1] ==
                            PackageManager.PERMISSION_GRANTED;

                    if (StoragePermission && RecordPermission) {
                        showToast("Permission Granted");
                    } else {
                        showToast("Permission  Denied");
                    }
                }
                break;
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    public static void verifyStoragePermissions(Activity activity) {

        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }

        } catch (Exception e) {
            Toast.makeText(null, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


}