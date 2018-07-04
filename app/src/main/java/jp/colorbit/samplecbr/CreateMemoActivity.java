package jp.colorbit.samplecbr;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.UUID;

public class CreateMemoActivity extends AppCompatActivity {
    // MemoOpenHelperクラスを定義
    CustomOpenHelper helper = null;
    // 新規フラグ
    boolean newFlag = false;
    // id
    String id = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_memo);

        // データベースから値を取得する
        if(helper == null){
            helper = new CustomOpenHelper(CreateMemoActivity.this);
        }

        // ListActivityからインテントを取得
        Intent intent = this.getIntent();
        // 値を取得
        id = intent.getStringExtra("id");
        // 画面に表示
        if(id.equals("")){
            // 新規作成の場合
            newFlag = true;
        }else{
            // 編集の場合 データベースから値を取得して表示
            //データベースを取得
            SQLiteDatabase db = helper.getWritableDatabase();
            try {
                // rawQueryというSELECT専用メソッドを使用してデータを取得する
                Cursor c = db.rawQuery("select body from MEMO_TABLE where uuid = '"+ id +"'", null);
                // Cursorの先頭行があるかどうか確認
                boolean next = c.moveToFirst();
                // 取得した全ての行を取得
                while (next) {
                    // 取得したカラムの順番(0から始まる)と型を指定してデータを取得する
                    String dispBody = c.getString(0);
                    EditText body = (EditText)findViewById(R.id.body);
                    body.setText(dispBody, TextView.BufferType.NORMAL);
                    next = c.moveToNext();
                }
            } finally {
                // finallyは、tryの中で例外が発生した時でも必ず実行される
                // dbを開いたら確実にclose
                db.close();
            }
        }

        /**
         * 登録ボタン処理
         */
        // idがregisterのボタンを取得
        Button registerButton = (Button) findViewById(R.id.register);
        // clickイベント追加
        registerButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // 入力内容を取得する
                EditText body = (EditText)findViewById(R.id.body);
                String bodyStr = body.getText().toString();

                // データベースに保存する
                SQLiteDatabase db = helper.getWritableDatabase();
                try {
                    if(newFlag){
                        // 新規作成の場合
                        // 新しくuuidを発行する
                        id = UUID.randomUUID().toString();
                        // INSERT
                        db.execSQL("insert into MEMO_TABLE(uuid, body) VALUES('"+ id +"', '"+ bodyStr +"')");
                    }else{
                        // UPDATE
                        db.execSQL("update MEMO_TABLE set body = '"+ bodyStr +"' where uuid = '"+id+"'");
                    }
                } finally {
                    // finallyは、tryの中で例外が発生した時でも必ず実行される
                    // dbを開いたら確実にclose
                    db.close();
                }

                // 保存後に一覧へ戻る
                Intent intent = new Intent(CreateMemoActivity.this, jp.colorbit.samplecbr.ListActivity.class);
                startActivity(intent);
            }
        });


        /**
         * 戻るボタン処理
         */
        // idがbackのボタンを取得
        Button backButton = (Button) findViewById(R.id.back);
        // clickイベント追加
        backButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // 保存せずに一覧へ戻る
                Intent intent = new Intent(CreateMemoActivity.this, jp.colorbit.samplecbr.ListActivity.class);
                startActivity(intent);
            }
        });
    }
}
