package jp.colorbit.samplecbr;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.TwoLineListItem;

import java.util.ArrayList;
import java.util.HashMap;


public class ListActivity extends AppCompatActivity {
    // MemoOpenHelperクラスを定義
    CustomOpenHelper helper = null;
    static final String BR = System.getProperty("line.separator");/*改行コード*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        // データベースから値を取得する
        if(helper == null){
            helper = new CustomOpenHelper(ListActivity.this);
        }

        // メモリストデータを格納する変数
        final ArrayList<HashMap<String, String>> memoList = new ArrayList<>();
        // データベースを取得する
        SQLiteDatabase db = helper.getWritableDatabase();
        try {
            // rawQueryというSELECT専用メソッドを使用してデータを取得する
            Cursor c = db.rawQuery("select uuid, body from MEMO_TABLE where status = 0 ORDER BY uuid", null);
            // Cursorの先頭行があるかどうか確認
            boolean next = c.moveToFirst();

            // 取得した全ての行を取得
            while (next) {
                HashMap<String,String> data = new HashMap<>();
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
                data.put("body",body);
                data.put("id",uuid);
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
                new String[]{"body","id"}, // どの項目を
                new int[]{android.R.id.text1, android.R.id.text2} // どのidの項目に入れるか
        );
        // idがmemoListのListViewを取得
        ListView listView = (ListView) findViewById(R.id.memoList);
        listView.setAdapter(simpleAdapter);

        // リスト項目を長押しクリックした時の処理
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
            /**
             * @param parent ListView
             * @param view 選択した項目
             * @param position 選択した項目の添え字
             * @param id 選択した項目のID
             */
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // インテント作成  第二引数にはパッケージ名からの指定で、遷移先クラスを指定
                Intent intent = new Intent(ListActivity.this, jp.colorbit.samplecbr.CreateMemoActivity.class);

                // 選択されたビューを取得 TwoLineListItemを取得した後、text2の値を取得する
                TwoLineListItem two = (TwoLineListItem)view;
                TextView idTextView = (TextView)two.getText2();
                String isStr = (String) idTextView.getText();
                // 値を引き渡す (識別名, 値)の順番で指定します
                intent.putExtra("id", isStr);
                // Activity起動
                startActivity(intent);

                return true;
            }
        });
        // リスト項目をクリックした時の処理
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            /**
             * @param parent ListView
             * @param view 選択した項目
             * @param position 選択した項目の添え字
             * @param id 選択した項目のID
             */
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                // 選択されたビューを取得 TwoLineListItemを取得した後、text2の値を取得する
                TwoLineListItem two = (TwoLineListItem) view;
                TextView idTextView = (TextView) two.getText2();
                final String idStr = (String) idTextView.getText();

                /*クリックで確認ダイアログ*/
                AlertDialog.Builder alertD = new AlertDialog.Builder(ListActivity.this);
                // alertD.setTitle("確認");
                alertD.setMessage("削除します");
                alertD.setPositiveButton("削除", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // クリック項目をデータベースから削除
                        SQLiteDatabase db = helper.getWritableDatabase();
                        try {
                            //db.execSQL("DELETE FROM MEMO_TABLE WHERE uuid = '" + idStr + "'");
                            db.execSQL("update MEMO_TABLE set status  =1 where uuid ='"+idStr+"'" );
                        } finally {
                            db.close();
                        }

                        // クリック項目を画面から削除
                        memoList.remove(position);
                        simpleAdapter.notifyDataSetChanged();
                    }
                });
                alertD.setNeutralButton("完全削除", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // クリック項目をデータベースから削除
                        SQLiteDatabase db = helper.getWritableDatabase();
                        try {
                            //db.execSQL("DELETE FROM MEMO_TABLE WHERE uuid = '" + idStr + "'");
                            db.execSQL("DELETE FROM MEMO_TABLE WHERE uuid ='"+idStr+"'" );
                        } finally {
                            db.close();
                        }

                        // クリック項目を画面から削除
                        memoList.remove(position);
                        simpleAdapter.notifyDataSetChanged();
                    }
                });
                alertD.setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {}
                });
                alertD.create().show();

                // trueにすることで通常のクリックイベントを発生させない
                //return true;
            }
        });


        /**
         * 新規作成するボタン処理
         */
        // idがnewButtonのボタンを取得
        Button newButton = (Button) findViewById(R.id.newButton);
        // clickイベント追加
        newButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // CreateMemoActivityへ遷移
                Intent intent = new Intent(ListActivity.this, jp.colorbit.samplecbr.CreateMemoActivity.class);
                intent.putExtra("id", "");
                startActivity(intent);
            }
        });
        /**
         * 削除履歴
         * 画面遷移 DeleteHistory
         */
        Button deleteHistory = (Button)findViewById(R.id.delete_history);
        deleteHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ListActivity.this,jp.colorbit.samplecbr.DeleteHistoryActivity.class);
                startActivity(intent);
            }
        });
        /**
         * カメラボタン処理
         */
        Button cameraButton = (Button) findViewById(R.id.camera);
        // clickイベント追加
        cameraButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //EmptyActivityへ遷移
                Intent intent = new Intent(ListActivity.this, jp.colorbit.samplecbr.MainActivity.class);
                startActivity(intent);
            }
        });
    }
}
