package com.example.kino.zukan;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;


public class MyActivity extends Activity {

    /*
    アプリケーションの状態をステート制御。
    起動直後 = 0
    長さ計測中 = 1
    検索結果一覧 = 2
    詳細描画 = 3
     */
    private int appState = 0;
    private int mValue = 0; // 計測結果の長さを格納

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);



        Button startButton = (Button)findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appState = 1;
                setContentView(R.layout.mesurement_layout);

                Button searchButton = (Button) findViewById(R.id.search_button);

                searchButton.setOnClickListener(new View.OnClickListener() {
                    @TargetApi(Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onClick(View v) {
                        appState = 2;
                        setContentView(R.layout.result_layout);

                        LinearLayout resultImages = (LinearLayout)findViewById(R.id.result_images);

                        /*
                        以下に、長さで検索→LinearLayoutに配置の処理を書く。
                        検索→レイアウトに配置の処理はAsyncTask使って
                        非同期処理にしとくと色々と捗るかも。
                        今は適当なデータを仮配置。
                         */

                        Resources r = getResources();
                        BitmapFactory.Options options = new  BitmapFactory.Options();
                        options.inMutable = true;
                        Bitmap bitmap = BitmapFactory.decodeResource(r,R.drawable.suzuki,options);

                        BlurMaskFilter blurFilter = new BlurMaskFilter(8, BlurMaskFilter.Blur.OUTER);
                        Paint shadowPaint = new Paint();

                        //影の色変えたかったらこのへんのフィルターを弄る
                        ColorFilter filter = new LightingColorFilter(Color.BLACK,1);
                        shadowPaint.setColorFilter(filter);
                        shadowPaint.setMaskFilter(blurFilter);

                        int[] offsetXY = new int[2];
                        Bitmap shadowImage = bitmap.extractAlpha(shadowPaint, offsetXY);
                        Bitmap shadowImage32 = shadowImage.copy(Bitmap.Config.ARGB_8888, true);

                        // Fix the non pre-multiplied exception for API 19+.
                        if ( android.os.Build.VERSION.SDK_INT >= 19 && !shadowImage32.isPremultiplied() )
                        {
                            shadowImage32.setPremultiplied( true );
                        }
                        shadowImage.recycle();
                        Canvas c = new Canvas(shadowImage32);

                        c.drawBitmap(bitmap,-offsetXY[0], -offsetXY[1], null);




                        for(int i = 0; i < 5;i++) {
                            ImageView image = new ImageView(getApplicationContext());


                            image.setImageBitmap(shadowImage32);
                            image.setPadding(5,0,5,0);
                            image.setClickable(true);
                            image.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    /*
                                    この辺に詳細Viewへ移行する処理を書く。
                                     */
                                }
                            });

                            resultImages.addView(image);
                        }
                    }
                });

            }


        });






    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}





