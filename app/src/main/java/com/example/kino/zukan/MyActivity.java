package com.example.kino.zukan;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.client.CookieStore;
import org.w3c.dom.Text;

import java.util.Objects;


public class MyActivity extends Activity {

    private DataBaseHelper mDbHelper;
    private SQLiteDatabase db;
    private Cursor cursor;

    /*
    アプリケーションの状態をステート制御。
    起動直後 = 0
    長さ計測中 = 1
    検索結果一覧 = 2
    詳細描画 = 3
     */
    private int appState = 0;
    private int mValue = 0; // 計測結果の長さを格納
    DefaultHttpClient httpClient;

    Handler guiThreadHandler;
    LinearLayout resultImages;
    private PopupWindow mPopupWindow;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        mDbHelper = new DataBaseHelper(this);
        db = mDbHelper.getWritableDatabase();

        guiThreadHandler = new Handler();



        Button startButton = (Button)findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appState = 1;
                setContentView(R.layout.mesurement_layout);

                SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
                mValue = seekBar.getProgress();
                TextView textView = (TextView) findViewById(R.id.value_text);
                textView.setText(mValue+"cm");


                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @TargetApi(Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                        if(Math.abs(mValue - seekBar.getProgress()) >= 1) {

                            final String query = "select id, name,img, detail from test3 where size = \"" + mValue + ".0cm\"";

                            AsyncTask asyncTask = new AsyncTask() {
                                @Override
                                protected Object doInBackground(Object[] params) {
                                    Cursor cursor = db.rawQuery(query, null);

                                    cursor.moveToFirst();


                                    for (int i = 0; i < cursor.getCount(); i++) {
                                        Resources r = getResources();
                                        BitmapFactory.Options options = new BitmapFactory.Options();
                                        options.inMutable = true;
                                        //Bitmap bitmap = BitmapFactory.decodeResource(r,R.drawable.suzuki,options);

                        /*
                        画像データをcursorより取得。
                        画像はbase64でエンコードされているので、デコード。
                         */
                                        byte[] img_base64 = cursor.getBlob(2);
                                        byte[] img_binary = Base64.decode(img_base64, Base64.DEFAULT);

                                        //バイト配列をbitmapにデコード。
                                        Bitmap bitmap = BitmapFactory.decodeByteArray(img_binary, 0, img_binary.length);

                                        BlurMaskFilter blurFilter = new BlurMaskFilter(8, BlurMaskFilter.Blur.OUTER);
                                        Paint shadowPaint = new Paint();

                                        //影の色変えたかったらこのへんのフィルターを弄る
                                        ColorFilter filter = new LightingColorFilter(Color.BLACK, 1);
                                        shadowPaint.setColorFilter(filter);
                                        shadowPaint.setMaskFilter(blurFilter);

                                        int[] offsetXY = new int[2];
                                        Bitmap shadowImage = bitmap.extractAlpha(shadowPaint, offsetXY);
                                        Bitmap shadowImage32 = shadowImage.copy(Bitmap.Config.ARGB_8888, true);

                                        // Fix the non pre-multiplied exception for API 19+.
                                        if (android.os.Build.VERSION.SDK_INT >= 19 && !shadowImage32.isPremultiplied()) {
                                            shadowImage32.setPremultiplied(true);
                                        }
                                        shadowImage.recycle();
                                        Canvas c = new Canvas(shadowImage32);

                                        c.drawBitmap(bitmap, -offsetXY[0], -offsetXY[1], null);


                                        setResultAsync(shadowImage32,cursor.getString(1),cursor.getString(3));
                                        cursor.moveToNext();
                                        bitmap.recycle();
                                    }

                                    return null;
                                }
                            };
                            resultImages = (LinearLayout) findViewById(R.id.result_images);
                            resultImages.removeAllViews();
                            asyncTask.execute();



                        }

                        mValue = seekBar.getProgress();
                        TextView textView = (TextView) findViewById(R.id.value_text);
                        textView.setText(mValue + "cm");
                    }



                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });

//                Button searchButton = (Button) findViewById(R.id.search_button);
//                Button resetButton = (Button) findViewById(R.id.restart_button);
//
//
//                searchButton.setVisibility(View.INVISIBLE);
//                resetButton.setVisibility(View.INVISIBLE);
//
//                searchButton.setOnClickListener(new View.OnClickListener() {
//                    @TargetApi(Build.VERSION_CODES.KITKAT)
//                    @Override
//                    public void onClick(View v) {
//                        appState = 2;
//                        setContentView(R.layout.result_layout);
//
//                        httpClient = new DefaultHttpClient();
//
//                        WebView webView = null;
//                        webView = (WebView) findViewById(R.id.webView);
//                        webView.getSettings().setJavaScriptEnabled(true);
//                        webView.getSettings().setBuiltInZoomControls(false);
//                        webView.setHorizontalScrollBarEnabled(false);
//                        webView.setVisibility(View.INVISIBLE);
//                        webView.setWebViewClient(new WebViewClient(){
//
//                            @Override
//                            public void onPageStarted(WebView view, String url, Bitmap favicon){
//                                // URLがログアウトの時SharedPreferenceに保存されているCookieを削除する
////                                if(url.indexOf(LOGOUT) > -1){
////                                    SharedPreferences.Editor editor = getSharedPreferences(PREF_KEY, 0).edit();
////                                    editor.putString(LOGIN_KEY, "");
////                                    editor.commit();
////                                }
//                            }
//
//                            @Override
//                            public void onPageFinished(WebView view, String url){
//                                // 自ドメインの時CookieをCheckする
//                                if(url.indexOf("http://zukan.com/") > -1){
//                                    String cookie = CookieManager.getInstance().getCookie(url);// 文字列でCookieを取得
//                                    String[] oneCookie = cookie.split(";");
//                                    for(String pair : oneCookie){
//                                        pair = pair.trim();
//                                        String[] set = pair.split("=");
//                                        set[0] = set[0].trim();
//
//                                        // Cookieを作成
//                                        BasicClientCookie bCookie = new BasicClientCookie(set[0], set[1]);
//                                        bCookie.setDomain("http://zukan.com/");
//                                        bCookie.setPath("/");
//
//
//                                        // CookieStoreを取得
//                                        CookieStore store;
//                                        store = httpClient.getCookieStore();
//                                        // Cookieを追加
//                                        store.addCookie(bCookie);
//                                    }
//                                }
//                            }
//                        });
//                        synchronized (httpClient) {
//                            webView.loadUrl("http://zukan.com/");
//                        }
//
//
//                        TextView textView = (TextView) findViewById(R.id.value_text);
//                        textView.setText(mValue+"cm");
//                        synchronized (httpClient) {
//                            PostMessageTask postMessageTask = new PostMessageTask(httpClient);
//
//                            String[] contents = {mValue + "cm", "all"};
//
//                            postMessageTask.execute(contents);
//                        }
//
//
//                        /*
//                        DBから要素を検索
//                         */
//                        String query = "select id, name,img, detail from test3 where size = \""+mValue+".0cm\"";
//                        final Cursor cursor = db.rawQuery(query,null);
//                        cursor.moveToFirst();
//
//
//
//
//
//
//                        LinearLayout resultImages = (LinearLayout)findViewById(R.id.result_images);
//
//                        /*
//                        以下に、長さで検索→LinearLayoutに配置の処理を書く。
//                        検索→レイアウトに配置の処理はAsyncTask使って
//                        非同期処理にしとくと色々と捗るかも。
//                        今は適当なデータを仮配置。
//                         */
//
//                        for(int i = 0; i < cursor.getCount(); i++){
//                            Resources r = getResources();
//                            BitmapFactory.Options options = new  BitmapFactory.Options();
//                            options.inMutable = true;
//                            //Bitmap bitmap = BitmapFactory.decodeResource(r,R.drawable.suzuki,options);
//
//                            /*
//                            画像データをcursorより取得。
//                            画像はbase64でエンコードされているので、デコード。
//                             */
//                            byte[] img_base64 = cursor.getBlob(2);
//                            byte[]img_binary = Base64.decode(img_base64,Base64.DEFAULT);
//
//                            //バイト配列をbitmapにデコード。
//                            Bitmap bitmap = BitmapFactory.decodeByteArray(img_binary, 0,img_binary.length);
//
//                            BlurMaskFilter blurFilter = new BlurMaskFilter(8, BlurMaskFilter.Blur.OUTER);
//                            Paint shadowPaint = new Paint();
//
//                            //影の色変えたかったらこのへんのフィルターを弄る
//                            ColorFilter filter = new LightingColorFilter(Color.BLACK,1);
//                            shadowPaint.setColorFilter(filter);
//                            shadowPaint.setMaskFilter(blurFilter);
//
//                            int[] offsetXY = new int[2];
//                            Bitmap shadowImage = bitmap.extractAlpha(shadowPaint, offsetXY);
//                            Bitmap shadowImage32 = shadowImage.copy(Bitmap.Config.ARGB_8888, true);
//
//                            // Fix the non pre-multiplied exception for API 19+.
//                            if ( android.os.Build.VERSION.SDK_INT >= 19 && !shadowImage32.isPremultiplied() )
//                            {
//                                shadowImage32.setPremultiplied( true );
//                            }
//                            shadowImage.recycle();
//                            Canvas c = new Canvas(shadowImage32);
//
//                            c.drawBitmap(bitmap,-offsetXY[0], -offsetXY[1], null);
//
//
//
//
//
//                            ImageView image = new ImageView(getApplicationContext());
//
//
//                            image.setImageBitmap(shadowImage32);
//                            image.setPadding(5,0,5,0);
//                            image.setClickable(true);
//                            image.setOnClickListener(new View.OnClickListener() {
//                                @Override
//                                public void onClick(View v) {
//                                    /*
//                                    この辺に詳細Viewへ移行する処理を書く。
//                                     */
//                                }
//                            });
//
//                            resultImages.addView(image);
//                            cursor.moveToNext();
//                        }
//                    }
//                });

            }


        });






    }


    public void setResultAsync(final Bitmap shadowImage32,final String name,final String detail) {
        guiThreadHandler.post(new Runnable() {
            @TargetApi(Build.VERSION_CODES.KITKAT)
            @Override
            public void run() {


                ImageView image = new ImageView(getApplicationContext());

                image.setImageBitmap(shadowImage32);
                image.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER_VERTICAL));

                image.setScaleType(ImageView.ScaleType.FIT_CENTER);

                image.setPadding(5, 0, 5, 0);
                image.setHorizontalFadingEdgeEnabled(true);

                image.setClickable(true);
                image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {


                        mPopupWindow = new PopupWindow(MyActivity.this);

                        // レイアウト設定
                        View popupView = getLayoutInflater().inflate(R.layout.popup_layout, null);
                        popupView.findViewById(R.id.close_button).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (mPopupWindow.isShowing()) {
                                    mPopupWindow.dismiss();
                                }
                            }
                        });
                        popupView.findViewById(R.id.close_button).setVisibility(View.INVISIBLE);
                        mPopupWindow.setContentView(popupView);

                        TextView nameText = (TextView)mPopupWindow.getContentView().findViewById(R.id.nameText);
                        TextView detailText = (TextView)mPopupWindow.getContentView().findViewById(R.id.detailText);
                        nameText.setText(name);
                        detailText.setText(detail);

                        // 背景設定
                        mPopupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.popup_background));

                        // タップ時に他のViewでキャッチされないための設定
                        mPopupWindow.setOutsideTouchable(true);
                        mPopupWindow.setFocusable(true);

                        // 表示サイズの設定 今回は幅300dp
                        float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 300, getResources().getDisplayMetrics());
                        mPopupWindow.setWindowLayoutMode((int) width, WindowManager.LayoutParams.WRAP_CONTENT);
                        mPopupWindow.setWidth((int) width);
                        mPopupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);

                        // 画面中央に表示
                        mPopupWindow.showAtLocation(findViewById(R.id.result_images), Gravity.CENTER, 0, 0);




                    }
                });

                resultImages.addView(image);
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





