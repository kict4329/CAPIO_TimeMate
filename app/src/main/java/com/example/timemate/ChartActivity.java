package com.example.timemate;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.AsyncTask;
import android.os.Bundle;

import com.example.timemate.Database.AppDatabase;
import com.example.timemate.Database.OneDay;
import com.example.timemate.Database.OneDayDao;

import java.util.ArrayList;
import java.util.List;

public class ChartActivity extends AppCompatActivity {

    private RecyclerView rv_days;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;
    private static ArrayList<ODData> arrayList;
    private static List<OneDay> rvODItems;

    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        db = AppDatabase.getAppDatabase(this); // 데이터베이스 가져오기

        rv_days = findViewById(R.id.rv_days);
        rv_days.setHasFixedSize(true); // 얘는 그냥 달아주긴하는데 뭔지 정확히 모름
        layoutManager = new LinearLayoutManager(this);
        rv_days.setLayoutManager(layoutManager);
        rv_days.addItemDecoration(new Re);
        arrayList = new ArrayList<>(); // OOData 객체를 담을 어레이 리스트(어댑터로 쏴줄것)
        rvODItems=new ArrayList<>();

        // List<OneDay> temp = db.oneDayDao().getAllItems();

        /*for (int i = 0; i < temp.size(); i++) {
            ODData tempODD = new ODData(temp.get(i).getCategory(), temp.get(i).getTime());
            arrayList.add(tempODD);
        }*/

        new OneDaySearchAsyncTask(db.oneDayDao()).execute();
        /*for (int i = 0; i < rvODItems.size(); i++) {
            ODData tempODD = new ODData(rvODItems.get(i).getCategory(), rvODItems.get(i).getTime());
            arrayList.add(tempODD);
        }*/

        adapter = new OneDayAdapater(arrayList, this);
        rv_days.setAdapter(adapter); // 리사이클러 뷰에 어댑터 연결
    }

    // 데이터 가져오기
    public static class OneDaySearchAsyncTask extends AsyncTask<OneDay, Void, Void> {
        private OneDayDao mOneDayDao;

        public OneDaySearchAsyncTask(OneDayDao oneDayDao) {
            this.mOneDayDao = oneDayDao;
        }

        @Override // 백그라운드 작업
        protected Void doInBackground(OneDay... oneDays) {

            if(!arrayList.isEmpty()) arrayList.clear();
            if (!rvODItems.isEmpty()) rvODItems.clear();

            rvODItems = mOneDayDao.getAllItems();

            for(OneDay oneDay : rvODItems) {
                ODData temp = new ODData(oneDay.getCategory(), oneDay.getTime());
                arrayList.add(temp);
            }
            return null;
        }
    }
}
