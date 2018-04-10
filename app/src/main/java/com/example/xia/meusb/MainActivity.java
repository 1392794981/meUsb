package com.example.xia.meusb;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class MainActivity extends FragmentActivity {

    private ViewPager viewPager;  //对应的viewPager
    private View viewComplex, viewSimple;
    private List<View> viewList;//view数组

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
    Button btnSimple, btnComplex;
    TextView txtShowTextInSecond;
    Button btnShowTextInSecond, btnNextInSecond, btnAnotherNextInSecond, btnPreInSecond;

    ImageView imageViewProgress;

    SortedList pointList;


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
                            theActivity.txtPosition.setText(currentPointString + "->" + String.valueOf(m) + "分" + String.valueOf(s) + "秒" +
                                    " [" + "音量:" + String.valueOf(theActivity.getCurrentVolume()) + " 速度:" + String.valueOf(theActivity.playSpeed) + "]");

                            Calendar cal = Calendar.getInstance();
                            cal.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
                            String hour="";
                            if (cal.get(Calendar.AM_PM) == 0)
                                hour = String.valueOf(cal.get(Calendar.HOUR));
                            else
                                hour = String.valueOf(cal.get(Calendar.HOUR)+12);
                            String minute = String.valueOf(cal.get(Calendar.MINUTE));
                            String second = String.valueOf(cal.get(Calendar.SECOND));
                            theActivity.txtCurrentTime.setText("[时间："+hour+"时"+minute+"分]");//+second+"秒]");

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
                        }
                    } catch (Exception e) {
                        theActivity.txtPosition.setText(e.getMessage());
                    }
                    image.setImageBitmap(bitmap);
                    break;
            }
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


        //=====================================
        viewPager = findViewById(R.id.viewpager);
        LayoutInflater inflater = getLayoutInflater();
        viewComplex = inflater.inflate(R.layout.layout_complex, null);
        viewSimple = inflater.inflate(R.layout.layout_simple, null);

        viewList = new ArrayList<>();// 将要分页显示的View装入数组中
        viewList.add(viewComplex);
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

        txtFilePath = (TextView) viewComplex.findViewById(R.id.txtFilePath);
        txtPosition = (TextView) viewComplex.findViewById(R.id.txtPosition);
        txtCurrentTime=viewComplex.findViewById(R.id.txtCurrentTime);
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


        txtShowTextInSecond = viewSimple.findViewById(R.id.txtShowTextInSecond);
        btnNextInSecond = viewSimple.findViewById(R.id.btnNextInSecond);
        btnShowTextInSecond = viewSimple.findViewById(R.id.btnShowTextInSecond);
        btnPreInSecond = viewSimple.findViewById(R.id.btnPreInSecond);
        btnAnotherNextInSecond = viewSimple.findViewById(R.id.btnAnotherNextInSecond);

        initCustomSetting();

        String dir = "/storage/";
        if (strFilePath != null && strFilePath.lastIndexOf("/") > 0)
            dir = strFilePath.substring(0, strFilePath.lastIndexOf("/"));

        File dialogDir = new File(dir);
        if (!dialogDir.exists())
            dialogDir = new File("/storage/");
        Log.v("dir", dir);
        //txtTemp.setText(strFilePath+"\n"+ dir);

        fileDialog = new FileDialog(this, dialogDir, ".mp3");
        fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
            @Override
            public void fileSelected(File file) {
                strFilePath = file.getAbsolutePath();
                loadSound(strFilePath);
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
                toPlayOrPause();
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

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        txtTemp.setText(event.toString());
        //本函数处理onkeydown无法处理的几个键
        String characters = event.getCharacters();//返回全角字符
        if (characters != null) {
            switch (characters) {
                case "＋":
                    toPlayOrPause();
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
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                viewPager.setCurrentItem(0);
                break;
            case KeyEvent.KEYCODE_NUMPAD_4:
                viewPager.setCurrentItem(0);
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                viewPager.setCurrentItem(1);
                break;
            case KeyEvent.KEYCODE_NUMPAD_6:
                viewPager.setCurrentItem(1);
                break;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //txtTemp.setText(txtTemp.getText()+"\n&&&"+String.valueOf(keyCode) + "\n&&&" + event.toString());
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP: //音量键 上
                toRePlay();
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN: //音量键 下
                if ((!player.isPlaying()) && (pointList.getNextPoint() <= player.getCurrentPosition()))
                    toRePlay();
                else
                    toPlayOrPause();
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

            case KeyEvent.KEYCODE_NUMPAD_8:
                toSpeedUp();
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                toSpeedUp();
                break;
            case KeyEvent.KEYCODE_NUMPAD_2:
                toSpeedDown();
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                toSpeedDown();
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

            case KeyEvent.KEYCODE_BACK: //不屏蔽返回建
                super.onKeyDown(keyCode, event);
                break;
        }
        return true;
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
            txtTemp.setText(e.getMessage());
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
                    Double value = Double.valueOf(str.substring(1, 3)) * 60 * 1000 + Double.valueOf(str.substring(4, 10)) * 1000;
                    String string = str.substring(11, str.length());
                    pointList.insertByOrder(value, string);
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
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};


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
            e.printStackTrace();
        }
    }


}