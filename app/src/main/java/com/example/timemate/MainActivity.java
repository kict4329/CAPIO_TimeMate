package com.example.timemate;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.timemate.Database.AppDatabase;
import com.example.timemate.Database.OneDay;
import com.example.timemate.Database.OneDayDao;
import com.example.timemate.Database.TotalData;
import com.example.timemate.Database.TotalDataDao;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private ImageView iv_open_otter, iv_closed_otter, iv_glasses, iv_dumbbell, iv_book, iv_setting, iv_chart, iv_stop, iv_resume, iv_save;
    private LinearLayout LLO_first, LLO_second, LLO_resume_stop, LLO_closed_txt, LLO_icons;
    private TextView tv_open_txt, tv_closed_txt, tv_time, tv_total, tv_iconTotal;
    private int state = 1; // 최초 상태 1, 눈 뜬 수달과 아이콘 - 2, 시간 기록 중 - 3, 일시정지 4, 최종 저장 페이지 5
    private int iconType = 0;
    private int OneDayDbSize = 0;
    private int TotalDatasDbSize = 0;
    private long myBaseTime, myPauseTime, outTime;
    private ProgressBar pb_total, pb_iconTotal;
    private int counter1 = -1, counter2 = -1;

    private long tempWDT, tempEDT, tempSDT; // 아이콘 별 "일일 총" 누적 시간, 얘네한테 outTime 더해서 db에 갱신
    private long tempWT, tempET, tempST; // 아이콘 별 "총" 누적 시간, 얘네한테 outTime 더해서 db에 갱신
    private long tempTT; // 전체 사용 시간 = 수달 lv에 이용될 녀석

    Animation fadein, fadeout, panelup, paneldown;

    // 연, 월, 일 추출
    /*Date curTime = Calendar.getInstance().getTime();
    SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());
    SimpleDateFormat monthFormat = new SimpleDateFormat("MM", Locale.getDefault());
    SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
    int year = Integer.parseInt(yearFormat.format(curTime));
    int month = Integer.parseInt(monthFormat.format(curTime));
    int day = Integer.parseInt(dayFormat.format(curTime));*/

    /*Calendar cal = Calendar.getInstance();
    int year = cal.get(Calendar.YEAR); // 연
    int month=cal.get(Calendar.MONTH)+1; // 월
    int day = cal.get(Calendar.DATE); // 일
    int date = cal.get(Calendar.DAY_OF_WEEK); // 요일*/
    Calendar cal = new GregorianCalendar();
    int year = cal.get(Calendar.YEAR); // 연
    int month = cal.get(Calendar.MONTH) + 1; // 월
    int day = cal.get(Calendar.DATE); // 일
    int date = cal.get(Calendar.DAY_OF_WEEK); // 요일

    AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // DB 초기화 및 생성
        db = AppDatabase.getAppDatabase(this);
        // 하루 항목 별 사용 시간 옵저버
        db.oneDayDao().getAll().observe(this, new Observer<List<OneDay>>() {
            @Override
            public void onChanged(List<OneDay> oneDays) {
                OneDayDbSize = oneDays.size();
                if (OneDayDbSize != 0) {
                    tempWDT = oneDays.get(OneDayDbSize - 1).getWork_dayTotal();
                    tempEDT = oneDays.get(OneDayDbSize - 1).getExercise_dayTotal();
                    tempSDT = oneDays.get(OneDayDbSize - 1).getStudy_dayTotal();
                }
            }
        });
        // 사용 총 시간 옵저버
        db.totalDataDao().getAll().observe(this, new Observer<List<TotalData>>() {
            @Override
            public void onChanged(List<TotalData> totalDatas) {
                TotalDatasDbSize = totalDatas.size();
                if (TotalDatasDbSize != 0) {
                    tempWT = totalDatas.get(TotalDatasDbSize - 1).getWork_Total();
                    tempET = totalDatas.get(TotalDatasDbSize - 1).getExercise_Total();
                    tempST = totalDatas.get(TotalDatasDbSize - 1).getStudy_Total();
                    tempTT = totalDatas.get(TotalDatasDbSize - 1).getTotalTime();

                    tv_total.setText("수달 Lv." + tempTT / 1000 / 60 / 60);
                    if (iconType == 1) {
                        tv_iconTotal.setText("업무 Lv." + tempWT / 1000 / 60 / 60);
                    } else if (iconType == 2) {
                        tv_iconTotal.setText("운동 Lv." + tempET / 1000 / 60 / 60);
                    } else if (iconType == 3) {
                        tv_iconTotal.setText("공부 Lv." + tempST / 1000 / 60 / 60);
                    }
                }
            }
        });

        // 이미지뷰들 버튼 클릭 이벤트 처리
        ImageView.OnClickListener onClickListener = new ImageView.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.iv_closed_otter: // 맨처음 눈 감은 수달 누르면
                        if (state == 1) {
                            iv_closed_otter.setVisibility(View.INVISIBLE);
                            iv_open_otter.setVisibility(View.VISIBLE);

                            tv_closed_txt.startAnimation(fadeout);
                            tv_closed_txt.setVisibility(View.INVISIBLE);

                            new Timer().schedule(new TimerTask() {
                                public void run() {
                                    tv_open_txt.startAnimation(fadein);
                                    LLO_icons.startAnimation(panelup);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            tv_open_txt.setVisibility(View.VISIBLE);
                                        }
                                    });
                                }
                            }, 1000);
                            state = 2; // 상태를 2단계로 변경

                            break;
                        }

                        // 안경 선택시
                    case R.id.iv_glasses:
                        tv_open_txt.startAnimation(fadeout);
                        tv_open_txt.setVisibility(View.INVISIBLE);
                        LLO_icons.startAnimation(paneldown);

                        new Timer().schedule(new TimerTask() {
                            public void run() {
                                tv_time.startAnimation(fadein);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        tv_time.setVisibility(View.VISIBLE);
                                    }
                                });
                            }
                        }, 1000);

                        myBaseTime = SystemClock.elapsedRealtime();
                        System.out.println(myBaseTime);
                        myTimer.sendEmptyMessage(0);
                        state = 3;
                        iconType = 1; // 안경(업무)을 선택한 경우, 아이콘 타입 1

                        break;
                    // 덤벨 선택시
                    case R.id.iv_dumbbell:
                        tv_open_txt.startAnimation(fadeout);
                        tv_open_txt.setVisibility(View.INVISIBLE);
                        LLO_icons.startAnimation(paneldown);

                        new Timer().schedule(new TimerTask() {
                            public void run() {
                                tv_time.startAnimation(fadein);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        tv_time.setVisibility(View.VISIBLE);
                                    }
                                });
                            }
                        }, 1000);

                        myBaseTime = SystemClock.elapsedRealtime();
                        System.out.println(myBaseTime);
                        myTimer.sendEmptyMessage(0);
                        state = 3;
                        iconType = 2; // 덤벨(운동)을 선택한 경우, 아이콘 타입 2

                        break;
                    // 책 선택시
                    case R.id.iv_book:
                        tv_open_txt.startAnimation(fadeout);
                        tv_open_txt.setVisibility(View.INVISIBLE);
                        LLO_icons.startAnimation(paneldown);

                        new Timer().schedule(new TimerTask() {
                            public void run() {
                                tv_time.startAnimation(fadein);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        tv_time.setVisibility(View.VISIBLE);
                                    }
                                });
                            }
                        }, 1000);

                        myBaseTime = SystemClock.elapsedRealtime();
                        System.out.println(myBaseTime);
                        myTimer.sendEmptyMessage(0);
                        state = 3;
                        iconType = 3; // 책(공부)을 선택한 경우, 아이콘 타입 3

                        break;

                    case R.id.iv_open_otter: // 시간 기록 중에 눈 뜬 수달 누르면 일시정지
                        if (state == 3) {
                            myTimer.removeMessages(0);
                            myPauseTime = SystemClock.elapsedRealtime();
                            LLO_resume_stop.startAnimation(panelup);
                            myTimer.removeMessages(0);
                            myPauseTime = SystemClock.elapsedRealtime();
                        }

                        state = 4; // 일시정지
                        break;

                    case R.id.iv_resume:
                        long now = SystemClock.elapsedRealtime();
                        myTimer.sendEmptyMessage(0);
                        myBaseTime += now - myPauseTime;

                        LLO_resume_stop.startAnimation(paneldown);
                        LLO_resume_stop.setVisibility(View.VISIBLE);

                        state = 3; // 다시 시간 기록 중
                        break;

                    case R.id.iv_stop:
                        LLO_first.setVisibility(View.INVISIBLE);
                        LLO_second.setVisibility(View.VISIBLE);

                        if (outTime >= 60000) { // 기록 시간이 1분이 넘으면 데이터 베이스에 저장
                            if (iconType == 1) { // 안경 선택
                                new OneDayInsertAsyncTask(db.oneDayDao()).execute(new OneDay(year, month, day, date, iconType, outTime, tempWDT + outTime, tempEDT, tempSDT));
                                new TotalDataInsertAsyncTask(db.totalDataDao()).execute(new TotalData(tempWT + outTime, tempET, tempST));
                            } else if (iconType == 2) { // 덤벨 선택
                                new OneDayInsertAsyncTask(db.oneDayDao()).execute(new OneDay(year, month, day, date, iconType, outTime, tempWDT, tempEDT + outTime, tempSDT));
                                new TotalDataInsertAsyncTask(db.totalDataDao()).execute(new TotalData(tempWT, tempET + outTime, tempST));
                            } else { // 책 선택
                                new OneDayInsertAsyncTask(db.oneDayDao()).execute(new OneDay(year, month, day, date, iconType, outTime, tempWDT, tempEDT, tempSDT + outTime));
                                new TotalDataInsertAsyncTask(db.totalDataDao()).execute(new TotalData(tempWT, tempET, tempST + outTime));
                            }
                        } else { // 기록 시간이 1분이 넘지 않으면
                            tv_total.setText("수달 Lv." + tempTT / 1000 / 60 / 60);
                            if (iconType == 1) {
                                tv_iconTotal.setText("업무 Lv." + tempWT / 1000 / 60 / 60);
                            } else if (iconType == 2) {
                                tv_iconTotal.setText("운동 Lv." + tempET / 1000 / 60 / 60);
                            } else if (iconType == 3) {
                                tv_iconTotal.setText("공부 Lv." + tempST / 1000 / 60 / 60);
                            }
                        }

                        // 0.5초 이후 프로그레스바 실행
                        new Timer().schedule(new TimerTask() {
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        pb1();
                                        pb2();
                                    }
                                });
                            }
                        }, 500);
                        state = 5;

                        break;

                    case R.id.iv_save:
                        // 눈뜬 수달이랑 아이콘 있는 페이지로 이동
                        LLO_second.setVisibility(View.INVISIBLE);
                        LLO_first.setVisibility(View.VISIBLE);

                        new Timer().schedule(new TimerTask() {
                            public void run() {
                                LLO_resume_stop.startAnimation(paneldown);
                                tv_time.startAnimation(fadeout);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        tv_time.setVisibility(View.INVISIBLE);
                                    }
                                });
                            }
                        }, 200);

                        new Timer().schedule(new TimerTask() {
                            public void run() {
                                LLO_icons.startAnimation(panelup);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        tv_open_txt.setVisibility(View.VISIBLE);
                                    }
                                });
                            }
                        }, 800);

                        // save버튼 누름과 동시에 프로그레스바도 초기화해놓는다.
                        pb_iconTotal.setProgress(0);
                        pb_total.setProgress(0);

                        state = 2;

                        break;

                    case R.id.iv_setting:
                        Intent intentSetting = new Intent(MainActivity.this, SettingActivity.class);
                        startActivity(intentSetting);

                        break;

                    case R.id.iv_chart:
                        if (OneDayDbSize == 0) { // 데이터베이스가 비어 있다면
                            Toast.makeText(getApplicationContext(), "먼저 시간을 기록해주세요", Toast.LENGTH_SHORT).show();
                        } else { // 데이터 베이스가 비어있지 않다면 통계창으로 이동하여 recyclerview를 띄운다
                            Intent intentChart = new Intent(MainActivity.this, ChartActivity.class);
                            startActivity(intentChart);
                            break;
                        }
                }
            }
        };

        // 애니메이션
        fadein = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fadein);
        fadeout = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fadeout);
        panelup = AnimationUtils.loadAnimation(MainActivity.this, R.anim.panelup);
        paneldown = AnimationUtils.loadAnimation(MainActivity.this, R.anim.paneldown);
        // 이미지뷰, 버튼
        iv_closed_otter = findViewById(R.id.iv_closed_otter);
        iv_open_otter = findViewById(R.id.iv_open_otter);
        iv_glasses = findViewById(R.id.iv_glasses);
        iv_dumbbell = findViewById(R.id.iv_dumbbell);
        iv_book = findViewById(R.id.iv_book);
        iv_setting = findViewById(R.id.iv_setting);
        iv_chart = findViewById(R.id.iv_chart);
        iv_stop = findViewById(R.id.iv_stop);
        iv_resume = findViewById(R.id.iv_resume);
        iv_save = findViewById(R.id.iv_save);
        //텍스트뷰
        tv_open_txt = findViewById(R.id.tv_open_txt);
        tv_closed_txt = findViewById(R.id.tv_closed_txt);
        tv_time = findViewById(R.id.tv_time);
        // 레이아웃
        LLO_first = findViewById(R.id.LLO_first);
        LLO_second = findViewById(R.id.LLO_second);
        LLO_resume_stop = findViewById(R.id.LLO_resume_stop);
        LLO_icons = findViewById(R.id.LLO_icons);
        //프로그레스바
        pb_iconTotal = findViewById(R.id.pb_iconTotal);
        pb_total = findViewById(R.id.pb_total);
        tv_total = findViewById(R.id.tv_total);
        tv_iconTotal = findViewById(R.id.tv_iconTotal);

        iv_closed_otter.setOnClickListener(onClickListener);
        iv_open_otter.setOnClickListener(onClickListener);
        iv_glasses.setOnClickListener(onClickListener);
        iv_dumbbell.setOnClickListener(onClickListener);
        iv_book.setOnClickListener(onClickListener);
        iv_stop.setOnClickListener(onClickListener);
        iv_resume.setOnClickListener(onClickListener);
        iv_setting.setOnClickListener(onClickListener);
        iv_save.setOnClickListener(onClickListener);
        iv_chart.setOnClickListener(onClickListener);

        // 패널업 애니메이션 리스너
        panelup.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (state == 1) {
                    LLO_icons.setVisibility(View.VISIBLE);
                    LLO_icons.clearAnimation();
                } else if (state == 3) {
                    LLO_resume_stop.setVisibility(View.VISIBLE);
                    LLO_resume_stop.clearAnimation();
                } else if (state == 5) {
                    LLO_icons.setVisibility(View.VISIBLE);
                    LLO_icons.clearAnimation();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        // 패널다운 애니메이션 리스너
        paneldown.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                LLO_icons.setVisibility(View.INVISIBLE);
                LLO_icons.clearAnimation();
                LLO_resume_stop.setVisibility(View.INVISIBLE);
                LLO_resume_stop.clearAnimation();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // 이 아래로는 타이머 관련 함수
    Handler myTimer = new Handler() {
        public void handleMessage(Message msg) {
            tv_time.setText(getTimeOut());
            myTimer.sendEmptyMessage(0);
        }
    };

    String getTimeOut() {
        long now = SystemClock.elapsedRealtime();
        outTime = now - myBaseTime;
        String easy_outTime = String.format("%02d:%02d:%02d",
                outTime / 1000 / 60 / 60,
                outTime / 1000 / 60,
                (outTime / 1000) % 60);
        return easy_outTime;
    }

    public static class OneDayInsertAsyncTask extends AsyncTask<OneDay, Void, Void> {
        private OneDayDao mOneDayDao;

        public OneDayInsertAsyncTask(OneDayDao oneDayDao) {
            this.mOneDayDao = oneDayDao;
        }

        @Override // 백그라운드 작업
        protected Void doInBackground(OneDay... oneDays) {
            /*List<OneDay> temp = mOneDayDao.getAllItems();
            OneDay last = temp.get(temp.size() - 1); // 가장 최근에 저장된 정보 객체
            // insert하기 전에 오늘이 가장 첫 일요일이면 데이터초기화
            if (oneDays[0].getDate() == 1) { // 일요일이면서
                if (mOneDayDao.getAllItems().size() != 0) { // 최초 등록이 아니고
                    if (oneDays[0].getYear() != last.getYear() || oneDays[0].getMonth() != last.getMonth() || oneDays[0].getDay() != last.getDay()) {
                        //요일 정보가 다르다면
                        mOneDayDao.clear(); // 정보 저장에 앞서서 데이터를 비워준다.
                        // 왜냐, 우리는 일~토 까지의 한 주 단위의 데이터를 보여줄 수 있어야하니까
                    }
                }
            }*/

            if (mOneDayDao.getAllItems().size() != 0) {
                List<OneDay> temp = mOneDayDao.getAllItems();
                OneDay last = temp.get(temp.size() - 1); // 가장 최근에 저장된 정보 객체
                if (temp.size() != 0 && (oneDays[0].getDay() != last.getDay())) {
                    // 최초 등록이 아니고
                    // 하루가 바뀌었음을, 즉 날짜에 변경을 인지하면
                    // 업무 별 하루 총 시간을 0부터 시작하도록 한다.
                    int tempY, tempM, tempD, tempDate, tempIT;
                    long tempOT;
                    tempY = oneDays[0].getYear();
                    tempM = oneDays[0].getMonth();
                    tempD = oneDays[0].getDay();
                    tempDate = oneDays[0].getDate();
                    tempIT = oneDays[0].getCategory();
                    tempOT = oneDays[0].getTime();
                    if (tempIT == 1)
                        mOneDayDao.insert(new OneDay(tempY, tempM, tempD, tempDate, tempIT, tempOT, tempOT, 0, 0));
                    else if (tempIT == 2)
                        mOneDayDao.insert(new OneDay(tempY, tempM, tempD, tempDate, tempIT, tempOT, 0, tempOT, 0));
                    else
                        mOneDayDao.insert(new OneDay(tempY, tempM, tempD, tempDate, tempIT, tempOT, 0, 0, tempOT));
                } else {
                    mOneDayDao.insert(oneDays[0]);
                }

            } else {
                mOneDayDao.insert(oneDays[0]);
            }
            return null;
        }
    }

    public static class TotalDataInsertAsyncTask extends AsyncTask<TotalData, Void, Void> {
        private TotalDataDao mTotalDataDao;

        public TotalDataInsertAsyncTask(TotalDataDao totalDataDao) {
            this.mTotalDataDao = totalDataDao;
        }

        @Override // 백그라운드 작업
        protected Void doInBackground(TotalData... totalDatas) {
            mTotalDataDao.clear(); // 이녀석은 데이터베이스를 매번 쌓을 필요가 없으니 기존의 녀석은 삭제
            mTotalDataDao.insert(totalDatas[0]);
            return null;
        }

    }

    public void pb1() {
        final Timer t = new Timer();
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                // tempTT는 ms단위임
                counter1++;
                Log.d("dddd", "" + counter1);
                pb_total.setProgress(counter1);

                if (counter1 == (tempTT / 1000 / 60 % 60)) t.cancel();
            }
        };
        t.schedule(tt, 0, 20);
        counter1 = -1;
    }

    public void pb2() {
        final Timer t = new Timer();
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                // tempTT는 ms단위임
                counter2++;
                pb_iconTotal.setProgress(counter2);

                if (iconType == 1) {
                    if (counter2 == (tempWT / 1000 / 60 % 60)) t.cancel();
                } else if (iconType == 2) {
                    if (counter2 == (tempET / 1000 / 60 % 60)) t.cancel();
                } else if (iconType == 3) {
                    if (counter2 == (tempST / 1000 / 60 % 60)) t.cancel();
                }
            }
        };
        t.schedule(tt, 0, 20);
        counter2 = -1;
    }

    @Override
    public void onBackPressed() { // state가 3 또는 4일때는 뒤로가기 버튼이 작동안하도록 설계,
        if (state == 1) finish();
        else if (state == 2) finish();
        else if (state == 5) { // state가 5일 때, 뒤로가기 버튼은 save버튼과 동일한 기능을 한다.
            LLO_second.setVisibility(View.INVISIBLE);
            LLO_first.setVisibility(View.VISIBLE);

            new Timer().schedule(new TimerTask() {
                public void run() {
                    LLO_resume_stop.startAnimation(paneldown);
                    tv_time.startAnimation(fadeout);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv_time.setVisibility(View.INVISIBLE);
                        }
                    });
                }
            }, 200);

            new Timer().schedule(new TimerTask() {
                public void run() {
                    LLO_icons.startAnimation(panelup);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv_open_txt.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }, 800);

            // save버튼 누름과 동시에 프로그레스바도 초기화해놓는다.
            pb_iconTotal.setProgress(0);
            pb_total.setProgress(0);

            state = 2;
        }
    }
}
