package cn.ycbjie.ycaudioplayer.ui.music.onLine.view;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.blankj.utilcode.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import org.yczbj.ycrefreshviewlib.YCRefreshView;
import org.yczbj.ycrefreshviewlib.adapter.RecyclerArrayAdapter;

import java.io.File;

import butterknife.Bind;
import cn.ycbjie.ycaudioplayer.R;
import cn.ycbjie.ycaudioplayer.base.BaseActivity;
import cn.ycbjie.ycaudioplayer.inter.OnMoreClickListener;
import cn.ycbjie.ycaudioplayer.ui.music.onLine.model.api.OnLineMusicModel;
import cn.ycbjie.ycaudioplayer.ui.music.onLine.model.bean.OnLineSongListInfo;
import cn.ycbjie.ycaudioplayer.ui.music.onLine.model.bean.OnlineMusicList;
import cn.ycbjie.ycaudioplayer.util.musicUtils.FileMusicUtils;
import cn.ycbjie.ycaudioplayer.util.musicUtils.ImageUtils;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by yc on 2018/1/29.
 */

public class OnlineMusicActivity extends BaseActivity {


    @Bind(R.id.recyclerView)
    YCRefreshView recyclerView;
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    private View view;
    private TextView tv_comment;
    private TextView tv_update_date;
    private TextView tv_title;
    private ImageView iv_cover;
    private ImageView iv_header_bg;

    private OnLineSongListInfo mListInfo;
    private LineMusicAdapter adapter;
    private int mOffset = 0;
    private static final int MUSIC_LIST_SIZE = 20;


    @Override
    public int getContentView() {
        return R.layout.base_bar_easy_recycle;
    }


    @Override
    public void initView() {
        initToolBar();
        initIntentData();
        initRecyclerView();
    }


    private void initToolBar() {
        setSupportActionBar(toolbar);
        toolbar.setTitle("音乐热播榜");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void initIntentData() {
        mListInfo = (OnLineSongListInfo) getIntent().getSerializableExtra("music_list_type");
        setTitle(mListInfo.getTitle());
    }


    @Override
    public void initListener() {
        adapter.setOnItemClickListener(new RecyclerArrayAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {

            }
        });
        adapter.setOnMoreClickListener(new OnMoreClickListener() {
            @Override
            public void onMoreClick(int position) {
                showMoreDialog(position);
            }
        });
    }

    @Override
    public void initData() {
        recyclerView.showProgress();
        getData(mOffset);
    }


    private void initRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LineMusicAdapter(this);
        recyclerView.setAdapter(adapter);
        addHeader();
    }


    private void addHeader() {
        adapter.addHeader(new RecyclerArrayAdapter.ItemView() {
            @Override
            public View onCreateView(ViewGroup parent) {
                view = getLayoutInflater().inflate(R.layout.header_online_music, parent, false);
                return view;
            }

            @Override
            public void onBindView(View view) {
                iv_header_bg = (ImageView) view.findViewById(R.id.iv_header_bg);
                iv_cover = (ImageView) view.findViewById(R.id.iv_cover);
                tv_title = (TextView) view.findViewById(R.id.tv_title);
                tv_update_date = (TextView) view.findViewById(R.id.tv_update_date);
                tv_comment = (TextView) view.findViewById(R.id.tv_comment);
            }
        });
    }


    private void getData(final int offset) {
        OnLineMusicModel model = OnLineMusicModel.getInstance();
        model.getSongListInfo(OnLineMusicModel.METHOD_GET_MUSIC_LIST,mListInfo.getType(),"20",String.valueOf(offset))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<OnlineMusicList>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof RuntimeException) {
                            // 歌曲全部加载完成
                            recyclerView.showError();
                            return;
                        }
                        if (offset == 0) {
                            recyclerView.showError();
                        } else {
                            ToastUtils.showShort(R.string.load_fail);
                        }
                    }

                    @Override
                    public void onNext(OnlineMusicList onlineMusicList) {
                        if (offset == 0 && onlineMusicList == null) {
                            recyclerView.showEmpty();
                            return;
                        } else if (offset == 0) {
                            initHeader(onlineMusicList);
                            recyclerView.showRecycler();
                        }
                        if (onlineMusicList == null || onlineMusicList.getSong_list() == null || onlineMusicList.getSong_list().size() == 0) {
                            return;
                        }
                        mOffset += MUSIC_LIST_SIZE;
                        adapter.addAll(onlineMusicList.getSong_list());
                        adapter.notifyDataSetChanged();
                    }
                });
    }


    private void initHeader(OnlineMusicList response) {
        if (view != null) {
            tv_title.setText(response.getBillboard().getName());
            tv_update_date.setText(getString(R.string.recent_update, response.getBillboard().getUpdate_date()));
            tv_comment.setText(response.getBillboard().getComment());
            Glide.with(this)
                    .load(response.getBillboard().getPic_s640())
                    .asBitmap()
                    .placeholder(R.drawable.default_cover)
                    .error(R.drawable.default_cover)
                    .override(200, 200)
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                            iv_cover.setImageBitmap(resource);
                            iv_header_bg.setImageBitmap(ImageUtils.blur(resource));
                        }
                    });
        }
    }


    private void showMoreDialog(int position) {
        final OnlineMusicList.OnlineMusic onlineMusic = adapter.getAllData().get(position);
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(adapter.getAllData().get(position).getTitle());
        String path = FileMusicUtils.getMusicDir() + FileMusicUtils.getMp3FileName(
                onlineMusic.getArtist_name(), onlineMusic.getTitle());
        File file = new File(path);
        int itemsId = file.exists() ? R.array.online_music_dialog_without_download : R.array.online_music_dialog;
        dialog.setItems(itemsId, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    // 分享
                    case 0:
                        //share(onlineMusic);
                        break;
                    // 查看歌手信息
                    case 1:
                        lookArtistInfo(onlineMusic);
                        break;
                    // 下载
                    case 2:
                        //download(onlineMusic);
                        break;
                    default:
                        break;
                }
            }
        });
        dialog.show();
    }


    private void lookArtistInfo(OnlineMusicList.OnlineMusic onlineMusic) {
        Intent intent = new Intent(this, ArtistInfoActivity.class);
        intent.putExtra("artist_id", onlineMusic.getTing_uid());
        startActivity(intent);
    }


}
