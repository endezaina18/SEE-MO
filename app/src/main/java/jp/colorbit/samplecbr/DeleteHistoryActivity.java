package jp.colorbit.samplecbr;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.TwoLineListItem;

import java.util.ArrayList;
import java.util.HashMap;

public class DeleteHistoryActivity extends AppCompatActivity {
    // MemoOpenHelperクラスを定義
    CustomOpenHelper helper = null;
    static final String BR = System.getProperty("line.separator");/*改行コード*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deletehistory);

        // データベースから値を取得する
        if (helper == null) {
            helper = new CustomOpenHelper(DeleteHistoryActivity.this);
        }
        // メモリストデータを格納する変数
        final ArrayList<HashMap<String, String>> memoList = new ArrayList<>();
        // データベースを取得する
        SQLiteDatabase db = helper.getWritableDatabase();
        try {
            // rawQueryというSELECT専用メソッドを使用してデータを取得する
            Cursor c = db.rawQuery("select uuid, body from MEMO_TABLE WHERE status=1", null);
            // Cursorの先頭行があるかどうか確認
            boolean next = c.moveToFirst();

            // 取得した全ての行を取得
            while (next) {
                HashMap<String, String> data = new HashMap<>();
                // 取得したカラムの順番(0から始まる)と型を指定してデータを取得する
                String uuid = c.getString(0);
                String body = c.getString(1);
                if(body.length() > 1){
                    // リストに表示するのは10文字まで
                    //body = body.substring(0, 11) + "...";
                    String sbr,cont=(body.length()>=10?"...":"");
                    int i;
                    for(i=0;i < body.length();i++){
                        sbr = String.valueOf(body.charAt(i));
                        if(sbr.equals(BR)) {
                            cont="...";
                            break;
                        }else if(sbr.equals(null))
                            i=11;
                    }
                    if(i>=11)i=11;
                    body = body.substring(0, i) + cont;
                }
                // 引数には、(名前,実際の値)という組合せで指定します　名前はSimpleAdapterの引数で使用します
                data.put("body", body);
                data.put("id", uuid);
                memoList.add(data);
                // 次の行が存在するか確認
                next = c.moveToNext();
            }
        } finally {
            // finallyは、tryの中で例外が発生した時でも必ず実行される
            // dbを開いたら確実にclose
            db.close();
        }

        // Adapter生成
        final SimpleAdapter simpleAdapter = new SimpleAdapter(this,
                memoList, // 使用するデータ
                android.R.layout.simple_list_item_2, // 使用するレイアウト
                new String[]{"body", "id"}, // どの項目を
                new int[]{android.R.id.text1, android.R.id.text2} // どのidの項目に入れるか
        );
        // idがmemoListのListViewを取得
        final ListView listView = (ListView) findViewById(R.id.memoList2);
        listView.setAdapter(simpleAdapter);

        // リスト項目をクリックした時の処理
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            /**
             * @param parent   ListView
             * @param view     選択した項目
             * @param position 選択した項目の添え字
             * @param id       選択した項目のID
             */
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                // 選択されたビューを取得 TwoLineListItemを取得した後、text2の値を取得する
                TwoLineListItem two = (TwoLineListItem) view;
                TextView idTextView = (TextView) two.getText2();
                final String idStr = (String) idTextView.getText();

                //クリックで確認ダイアログ
                AlertDialog.Builder alertD = new AlertDialog.Builder(DeleteHistoryActivity.this);
                // alertD.setTitle("確認");
                alertD.setMessage("操作を選んでください");
                alertD.setPositiveButton("完全削除", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // クリックした項目をデータベースから削除
                        SQLiteDatabase db = helper.getWritableDatabase();
                        try {
                            //db.execSQL("DELETE FROM MEMO_TABLE WHERE uuid = '" + idStr + "'");
                            db.execSQL("DELETE FROM MEMO_TABLE WHERE uuid = '" + idStr + "'");
                        } finally {
                            db.close();
                        }

                        // クリックした項目を画面から削除
                        memoList.remove(position);
                        simpleAdapter.notifyDataSetChanged();
                    }
                });
                alertD.setNegativeButton("復元", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SQLiteDatabase db = helper.getWritableDatabase();
                        try {
                            //db.execSQL("DELETE FROM MEMO_TABLE WHERE uuid = '" + idStr + "'");
                            db.execSQL("update MEMO_TABLE set status  =0 where uuid ='" + idStr + "'");
                        } finally {
                            db.close();
                        }
                        // 長押しした項目を画面から削除
                        memoList.remove(position);
                        simpleAdapter.notifyDataSetChanged();
                    }
                });
                alertD.create().show();

                // trueにすることで通常のクリックイベントを発生させない
                //return true;
            }
        });
        /**
         * ALL CLEARボタンの処理
         */
        // idがallButtonのボタンを取得
        Button allButton = (Button) findViewById(R.id.all_clear);
        // clickイベント追加
        allButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                SQLiteDatabase db = helper.getWritableDatabase();
               try {
                   // rawQueryというSELECT専用メソッドを使用してデータを取得する
                   Cursor c = db.rawQuery("select uuid from MEMO_TABLE WHERE status=1", null);
                   // Cursorの先頭行があるかどうか確認
                   boolean next = c.moveToFirst();

                   // 取得した全ての行を取得
                   while (next) {
                       // 取得したカラムの順番(0から始まる)と型を指定してデータを取得する
                       String did = c.getString(0);
                       db.execSQL("DELETE FROM MEMO_TABLE WHERE uuid = '" + did + "'");

                       next = c.moveToNext();
                   }
               }finally {
                   db.close();
                   // ListActivityへ遷移
                   Intent intent = new Intent(DeleteHistoryActivity.this, jp.colorbit.samplecbr.ListActivity.class);
                   //intent.putExtra("id", "");
                   startActivity(intent);
               }
            }
        });
        /**
         * 戻るボタン処理
         */
        // idがbackButtonのボタンを取得
        Button backButton = (Button) findViewById(R.id.back);
        // clickイベント追加
        backButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // ListActivityへ遷移
                Intent intent = new Intent(DeleteHistoryActivity.this, jp.colorbit.samplecbr.ListActivity.class);
                //intent.putExtra("id", "");
                startActivity(intent);
            }
        });
    }
}
