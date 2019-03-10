package com.example.android_review;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.xiaoma.autotracker.listener.XmTrackerOnTabSelectedListener;
import com.xiaoma.component.base.BaseFragment;
import com.xiaoma.launcher.R;
import com.xiaoma.launcher.common.constant.LauncherConstants;
import com.xiaoma.launcher.player.adapter.MainAudioAdapter;
import com.xiaoma.launcher.player.callback.LauncherConnectListener;
import com.xiaoma.launcher.player.callback.LauncherPlayListener;
import com.xiaoma.launcher.player.callback.OnPlayControlListener;
import com.xiaoma.launcher.player.callback.ResultCallBack;
import com.xiaoma.launcher.player.manager.PlayerConnectHelper;
import com.xiaoma.launcher.player.model.AudioInfoBean;
import com.xiaoma.launcher.player.model.LauncherAudioInfo;
import com.xiaoma.launcher.player.view.PlayerControlView;
import com.xiaoma.launcher.player.vm.AudioFragmentVM;
import com.xiaoma.model.ItemEvent;
import com.xiaoma.model.XmResource;
import com.xiaoma.player.AudioConstants;
import com.xiaoma.player.AudioInfo;
import com.xiaoma.player.ProgressInfo;
import com.xiaoma.thread.ThreadDispatcher;
import com.xiaoma.ui.dialog.XmDialog;
import com.xiaoma.ui.progress.loading.CustomProgressDialog;
import com.xiaoma.ui.toast.XMToast;
import com.xiaoma.utils.NetworkUtils;
import com.xiaoma.utils.tputils.TPUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AudioMainFragment extends BaseFragment implements LauncherConnectListener, LauncherPlayListener, ResultCallBack {

    private TabLayout mTabLayout;
    private RecyclerView mAudioRecyclerView;
    private PlayerControlView mPlayerControlView;

    private PlayerConnectHelper connectHelper;
    //tab标题栏
    private String[] titles;
    //tab ViewHolder
    private ViewHolder mHolder;

    //桌面列表info
    private List<LauncherAudioInfo> audioInfoList;
    //桌面列表adapter
    private MainAudioAdapter mainAudioAdapter;
    //桌面列表footer width
    private static final int FOOTER_VIEW_WIDTH = 1120;
    // LayoutManager
    private LinearLayoutManager mLayoutManager;
    //电台标题的位置,用于tab切换
    private int xTingTitlePosition;
    //我的收藏项位置,用于tab切换
    private int myFavoritePosition;

    //当前音频信息
    private AudioInfo mAudioInfo;
    //音频播放状态
    private int mPlayState;
    //音频数据来源
    private int mDataSource;

    //最后播放音频type
    private static final String LAST_AUDIO_TYPE = "last_audio_type";
    //最后播放数据来源
    private static final String LAST_SOURCE = "last_source";
    //最后播放的栏目id
    private static final String CATEGORY_ID = "category_id";

    //桌面音频item
    private LauncherAudioInfo mLauncherAudioInfo;

    //分类栏目切换loading
    private CustomProgressDialog progressDialog;

    //usb连接状态
    private int usbConnectState;
    //蓝牙连接状态
    private boolean btMusicConnected = false;
    //usb是否首次点击
    private boolean firstNoUsbClick = false;

    private Map<Integer, AudioInfo> historyAudioMap = new HashMap<>();
    private boolean isHistory = false;
    private AudioInfo lastAudioInfo;

    public static AudioMainFragment newInstance() {
        return new AudioMainFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateWrapView(inflater.inflate(R.layout.fragment_audio, container, false));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindView(view);
        initView();
        initData();
    }

    private void bindView(View view) {
        mTabLayout = view.findViewById(R.id.audio_tab);
        mAudioRecyclerView = view.findViewById(R.id.audio_rv);
        mPlayerControlView = view.findViewById(R.id.player_control_view);
    }

    private void initView() {
        //init Audio监听
        initAudioListener();

        //init tabLayout
        initTabLayout();

        //init RecyclerView
        initAudioRecyclerView();

        //set 播放器控制监听
        setPlayerControlListener();
    }

    /**
     * init 音频相关监听
     */
    private void initAudioListener() {
        connectHelper = PlayerConnectHelper.getInstance();
        //音频监听
        connectHelper.setPlayListener(this);
        //usb 与 蓝牙连接监听
        connectHelper.setConnectListener(this);
    }

    /**
     * init tabLayout
     */
    private void initTabLayout() {
        titles = getResources().getStringArray(R.array.home_audio_tab);
        mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        setupTabView(mTabLayout);
        mTabLayout.addOnTabSelectedListener(new XmTrackerOnTabSelectedListener() {
            private CharSequence mTabText;

            @Override
            public ItemEvent returnPositionEventMsg(View view) {
                return new ItemEvent(mTabText.toString(), "0");
            }

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mTabText = changeTab(tab);
                if (tab.getPosition() == 0) {
                    mLayoutManager.scrollToPositionWithOffset(0, 0);

                } else if (tab.getPosition() == 1) {
                    mLayoutManager.scrollToPositionWithOffset(xTingTitlePosition, 0);

                } else {
                    mLayoutManager.scrollToPositionWithOffset(myFavoritePosition, 0);
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                super.onTabReselected(tab);
                if (tab.getPosition() == 0) {
                    mLayoutManager.scrollToPositionWithOffset(0, 0);

                } else if (tab.getPosition() == 1) {
                    mLayoutManager.scrollToPositionWithOffset(xTingTitlePosition, 0);

                } else {
                    mLayoutManager.scrollToPositionWithOffset(myFavoritePosition, 0);
                }
                mTabText = changeTab(tab);
            }
        });
    }

    /**
     * init audio recycler
     */
    private void initAudioRecyclerView() {
        audioInfoList = new ArrayList<>();
        mainAudioAdapter = new MainAudioAdapter(audioInfoList);
        View view = new View(getActivity());
        view.setLayoutParams(new ViewGroup.LayoutParams(FOOTER_VIEW_WIDTH, mAudioRecyclerView.getHeight()));
        mainAudioAdapter.addFooterView(view, -1, LinearLayout.HORIZONTAL);
        mLayoutManager = new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false);
        mAudioRecyclerView.setLayoutManager(mLayoutManager);
        mAudioRecyclerView.setAdapter(mainAudioAdapter);
        mAudioRecyclerView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                int firstVisiblePos = mLayoutManager.findFirstVisibleItemPosition();
                int selectTab;
                if (firstVisiblePos < xTingTitlePosition) {
                    selectTab = 0;

                } else {
                    selectTab = 1;
                }
                if (firstVisiblePos == myFavoritePosition) {
                    selectTab = 2;
                }

                mTabLayout.setScrollPosition(selectTab, 0, true);
                changeTab(mTabLayout.getTabAt(selectTab));
            }
        });

        //列表项点击
        mainAudioAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                mainAudioAdapterOnItemClick(position);
            }
        });

        //收藏点击
        mainAudioAdapter.setMyFavoriteClickListener(new MainAudioAdapter.MyFavoriteClickListener() {
            @Override
            public void onFavoriteClick(int audioType) {
                toFavoriteFrame(audioType);
            }
        });
    }

    /**
     * 列表项点击
     *
     * @param position
     */
    private void mainAudioAdapterOnItemClick(int position) {
        LauncherAudioInfo item = mainAudioAdapter.getData().get(position);
        mLauncherAudioInfo = item;
        int itemType = item.getItemType();
        switch (itemType) {
            //title
            case MainAudioAdapter.ITEM_TYPE_XTING_TITLE:
            case MainAudioAdapter.ITEM_TYPE_MUSIC_TITLE:
            case MainAudioAdapter.ITEM_TYPE_MY_FAVORITE:
                return;

            //蓝牙音乐
            case AudioConstants.AudioTypes.MUSIC_LOCAL_BT:
                showToast("bt music is not connect");
                return;

            //USB音乐未连接
            case AudioConstants.AudioTypes.MUSIC_LOCAL_USB:
                if (usbConnectState != AudioConstants.ConnectStatus.USB_SCAN_FINISH) {
                    //如果没有usb挂载状态下点击
                    firstNoUsbClick = true;
                    mainAudioAdapter.setUsbConnectState(AudioConstants.ConnectStatus.USB_NOT_MOUNTED, true);
                    return;
                }
        }
        //音乐、电台未连接
        if (connectHelper.getSourceInfoByAudioType(itemType) == null) {
            showToast("audio client is not connect");
            return;
        }

        int playState = item.getPlayState();
        //当前栏目loading状态
        if (playState == LauncherConstants.LOADING_STATE) {
            return;
        }
        //当前栏目播放中
        if (playState == LauncherConstants.PLAY_STATE) {
            connectHelper.pauseAudio(item.getAudioType());

        } else {
            //桌面音频栏目id
            int mCategoryId = item.getId();
            //直接播放
            if (mCategoryId == TPUtils.get(getContext(), CATEGORY_ID, 0)) {
                connectHelper.playAudio(mLauncherAudioInfo.getAudioType(), AudioConstants.PlayAction.DEFAULT);

            } else {
                //切换栏目播放
                if (progressDialog == null) {
                    progressDialog = new CustomProgressDialog(getActivity());
                    progressDialog.setMessage(R.string.load_more_loading);
                }
                progressDialog.show();
                connectHelper.playAudioCategory(item.getAudioType(), mCategoryId, AudioMainFragment.this);
            }
        }
    }


    /**
     * 跳转收藏页面
     *
     * @param audioType 音乐、电台
     */
    private void toFavoriteFrame(int audioType) {
        if (connectHelper.getSourceInfoByAudioType(audioType) == null) {
            XMToast.showToast(mContext, "audio is not connect...");
            return;
        }

        if (mAudioInfo == null) {
            mAudioInfo = new AudioInfo();

        } else {
            mAudioInfo.setPlayState(mPlayState);
        }
        Intent intent = new Intent(mContext, AudioFavoriteActivity.class);
        intent.putExtra(AudioFavoriteActivity.FAVORITE_TYPE, audioType);
        intent.putExtra(AudioFavoriteActivity.AUDIO_INFO, mAudioInfo);
        mContext.startActivity(intent);
    }

    /**
     * 播放器控制监听
     */
    private void setPlayerControlListener() {
        mPlayerControlView.setOnPlayControlListener(new OnPlayControlListener() {
            /**
             * 上一曲
             * @param audioType 音源类型
             */
            @Override
            public void onPre(int audioType) {
                connectHelper.preNextAudio(AudioConstants.Action.Option.PREVIOUS, audioType);
            }

            /**
             * 下一曲
             * @param audioType 音源类型
             */
            @Override
            public void onNext(int audioType) {
                connectHelper.preNextAudio(AudioConstants.Action.Option.NEXT, audioType);
            }

            /**
             * 播放、暂停
             * @param audioType 音源类型
             * @param playState
             */
            @Override
            public void onPlayOrPause(int audioType, int playState) {
                if (playState == AudioConstants.Action.Option.PLAY) {
                    connectHelper.pauseAudio(audioType);

                } else if (playState == AudioConstants.Action.Option.PAUSE) {
                    connectHelper.playAudio(audioType, AudioConstants.PlayAction.DEFAULT);
                }
            }

            /**
             * 打开列表
             * @param dataSource 数据来源
             * @param playState
             */
            @Override
            public void onStartListActivity(int dataSource, int playState) {
                onPlayerStartListFrame(dataSource, playState);
            }

            /**
             * 收藏
             * @param audioType
             * @param favoriteState
             */
            @Override
            public void onFavorite(int audioType, boolean favoriteState) {
                onPlayerFavoriteClick(audioType, favoriteState);
            }
        });
    }

    /**
     * 播放器进入列表页面
     *
     * @param dataSource
     * @param playState
     */
    private void onPlayerStartListFrame(int dataSource, int playState) {
        //当前无音频播放
        if (mAudioInfo == null) {
            XMToast.showToast(getContext(), "current is not audio...");
            return;
        }
        Intent intent = new Intent(mContext, AudioListActivity.class);
        //设置当前音频播放状态
        mAudioInfo.setPlayState(playState);
        //如果是从桌面获取的音频数据,需要修改列表标题
        if (mLauncherAudioInfo != null) {
            if (dataSource == AudioConstants.OnlineInfoSource.LAUNCHER ||
                    mLauncherAudioInfo.getAudioType() == AudioConstants.AudioTypes.MUSIC_LOCAL_USB) {
                mAudioInfo.setTitle(mLauncherAudioInfo.getName());
                mAudioInfo.setLauncherCategoryId(TPUtils.get(getContext(), CATEGORY_ID, 0));
            }
        }
        intent.putExtra(AudioListActivity.AUDIO_INFO, mAudioInfo);
        startActivity(intent);
    }

    /**
     * 播放器收藏逻辑
     *
     * @param audioType
     * @param favoriteState
     */
    private void onPlayerFavoriteClick(int audioType, boolean favoriteState) {
        //当前无音频播放
        if (mAudioInfo == null) {
            XMToast.showToast(getContext(), "current is not audio...");
            return;
        }
        //网络异常
        if (!NetworkUtils.isConnected(getContext())) {
            XMToast.showToast(getContext(), getString(R.string.audio_no_network));
            return;
        }
        if (favoriteState) {
            //取消收藏弹框
            showCancelFavoriteDialog();

        } else {
            //收藏
            connectHelper.favoriteAudio();
            mPlayerControlView.setFavoriteState(true);
        }
    }

    /**
     * 取消收藏弹框
     */
    private void showCancelFavoriteDialog() {
        View view = View.inflate(getContext(), R.layout.dialog_cancel_favorite, null);
        final XmDialog builder = new XmDialog.Builder(getFragmentManager())
                .setView(view)
                .create();

        view.findViewById(R.id.tv_sure).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectHelper.favoriteAudio();
                mPlayerControlView.setFavoriteState(false);
                builder.dismiss();
            }
        });
        view.findViewById(R.id.tv_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                builder.dismiss();
            }
        });
        builder.show();
    }

    private void setupTabView(TabLayout tabLayout) {
        for (int i = 0; i < titles.length; i++) {
            mTabLayout.addTab(mTabLayout.newTab(), false);
        }

        for (int i = 0; i < titles.length; i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab != null) {
                tab.setCustomView(R.layout.service_tab_layout_item);
                View view = tab.getCustomView();
                if (view != null) {
                    mHolder = new ViewHolder(view);
                    mHolder.tabTv.setText(titles[i]);
                }
            }
            if (i == 0) {
                mHolder.tabTv.setSelected(true);
                mHolder.tabTv.setTextAppearance(mContext, R.style.text_view_light_blue_service);
            }
        }
    }

    private String changeTab(TabLayout.Tab tab) {
        for (int i = 0; i < mTabLayout.getTabCount(); i++) {
            TabLayout.Tab tab1 = mTabLayout.getTabAt(i);
            View customView = tab1.getCustomView();
            mHolder = new ViewHolder(customView);
            mHolder.tabTv.setTextAppearance(getActivity(), R.style.text_view_normal_service);
        }
        View customView = tab.getCustomView();
        if (customView == null) {
            return "";
        }
        mHolder = new ViewHolder(customView);
        mHolder.tabTv.setTextAppearance(getActivity(), R.style.text_view_light_blue_service);
        return mHolder.tabTv.getText().toString();
    }

    @Override
    public void connectState(final int connect) {
        ThreadDispatcher.getDispatcher().postOnMain(new Runnable() {
            @Override
            public void run() {
                switch (connect) {
                    case AudioConstants.ConnectStatus.BLUETOOTH_CONNECTED:
                        break;
                    case AudioConstants.ConnectStatus.BLUETOOTH_DISCONNECTED:
                        break;
                    //蓝牙已打开并且已经连接音乐
                    case AudioConstants.ConnectStatus.BLUETOOTH_SINK_CONNECTED:
                        btMusicConnected = true;
                        mainAudioAdapter.setBTConnectState(true);
                        break;
                    //已关闭蓝牙音乐
                    case AudioConstants.ConnectStatus.BLUETOOTH_SINK_DISCONNECTED:
                        btMusicConnected = false;
                        mainAudioAdapter.setBTConnectState(false);
                        break;

                    case AudioConstants.ConnectStatus.USB_SCAN_FINISH:
                    case AudioConstants.ConnectStatus.USB_REMOVE:
                    case AudioConstants.ConnectStatus.USB_NOT_MOUNTED:
                    case AudioConstants.ConnectStatus.USB_UNSUPPORT:
                        //刷新usb栏目状态
                        usbConnectState = connect;
                        mainAudioAdapter.setUsbConnectState(usbConnectState, firstNoUsbClick);
                        break;
                }
                //处理当前播放usb音乐时拔出
                handPlayingUsbRemove();
            }
        });
    }

    /**
     * 当前播放usb音乐时拔出
     */
    private void handPlayingUsbRemove() {
        if (mAudioInfo != null && mAudioInfo.getAudioType() == AudioConstants.AudioTypes.MUSIC_LOCAL_USB) {
            if (usbConnectState == AudioConstants.ConnectStatus.USB_REMOVE) {
                mAudioInfo = null;
                mPlayerControlView.setAudioInfo(null);
                updateLauncherPlayState(LauncherConstants.NULL_STATE);
            }
        }
    }

    /**
     * 刷新桌面栏目的播放状态
     *
     * @param playState
     */
    private void updateLauncherPlayState(int playState) {
        //重置所有栏目状态
        for (LauncherAudioInfo audioInfo : audioInfoList) {
            audioInfo.setPlayState(LauncherConstants.NULL_STATE);
        }
        if (mDataSource == AudioConstants.OnlineInfoSource.LAUNCHER) {
            //当前点击的栏目状态
            if (mLauncherAudioInfo != null) {
                mLauncherAudioInfo.setPlayState(playState);

            } else {
                //最后一次播放的栏目
                for (LauncherAudioInfo audioInfo : audioInfoList) {
                    if (audioInfo.getId() == TPUtils.get(getContext(), CATEGORY_ID, 0)) {
                        audioInfo.setPlayState(playState);
                        break;
                    }
                }
            }
        }
        //USB
        if (mLauncherAudioInfo != null && mLauncherAudioInfo.getAudioType() == AudioConstants.AudioTypes.MUSIC_LOCAL_USB) {
            mLauncherAudioInfo.setPlayState(playState);
        }
        mainAudioAdapter.notifyDataSetChanged();
    }

    @Override
    public void onProgress(final ProgressInfo progressInfo) {
        ThreadDispatcher.getDispatcher().postOnMain(new Runnable() {
            @Override
            public void run() {
                mPlayerControlView.setProgressInfo(progressInfo);
            }
        });
    }

    @Override
    public void onAudioInfo(final AudioInfo audioInfo) {
        this.mAudioInfo = audioInfo;

        recoverLastAudio(audioInfo);

        reSetDataSource(audioInfo);

        ThreadDispatcher.getDispatcher().postOnMain(new Runnable() {
            @Override
            public void run() {
                if (getActivity() != null && !getActivity().isDestroyed()) {
                    if (isHistory) {
                        mPlayerControlView.setAudioInfo(lastAudioInfo);

                    } else {
                        mPlayerControlView.setAudioInfo(audioInfo);
                    }
                }
            }
        });
    }

    /**
     * 根据音源类型重新赋值  mDataSource
     *
     * @param audioInfo
     */
    private void reSetDataSource(AudioInfo audioInfo) {
        //如果是usb、本地FM
        if (audioInfo.getAudioType() == AudioConstants.AudioTypes.MUSIC_LOCAL_USB ||
                audioInfo.getAudioType() == AudioConstants.AudioTypes.XTING_LOCAL_FM) {
            mDataSource = AudioConstants.OnlineInfoSource.LAUNCHER;
        }
    }

    /**
     * 获取最后一次播放的音频
     *
     * @param audioInfo
     */
    private void recoverLastAudio(AudioInfo audioInfo) {
        if (audioInfo.isHistory()) {
            historyAudioMap.put(audioInfo.getAudioType(), audioInfo);
            isHistory = true;
            lastAudioInfo = historyAudioMap.get(TPUtils.get(getContext(), LAST_AUDIO_TYPE, AudioConstants.AudioTypes.NONE));

        } else {
            //保存最后一次播放的audioInfo type
            TPUtils.put(getContext(), LAST_AUDIO_TYPE, audioInfo.getAudioType());
            isHistory = false;
        }
    }

    @Override
    public void onPlayState(final int playState) {
        this.mPlayState = playState;
        ThreadDispatcher.getDispatcher().postOnMain(new Runnable() {
            @Override
            public void run() {
                mPlayerControlView.setPlayState(playState);
                updateLauncherPlayState(playState);
            }
        });
    }

    @Override
    public void onDataSource(final int dataSource) {
        this.mDataSource = dataSource;
        ThreadDispatcher.getDispatcher().postOnMain(new Runnable() {
            @Override
            public void run() {
                mPlayerControlView.setDataSource(dataSource);
            }
        });
        //保存最后一次音频来源
        TPUtils.put(getContext(), LAST_SOURCE, dataSource);
    }

    @Override
    public void onAudioFavorite(final boolean favorite) {
        ThreadDispatcher.getDispatcher().postOnMain(new Runnable() {
            @Override
            public void run() {
                mPlayerControlView.setFavoriteState(favorite);
            }
        });
    }

    /**
     * 栏目切换播放result
     *
     * @param code
     * @param categoryId
     */
    @Override
    public void onSwitchCategoryResult(int code, int categoryId) {
        if (code == AudioConstants.AudioResponseCode.SUCCESS) {
            progressDialog.dismiss();
            //保存播放的栏目id
            TPUtils.put(getContext(), CATEGORY_ID, categoryId);

        } else {
            XMToast.showToast(getContext(), getString(R.string.audio_no_network));
        }
    }

    class ViewHolder {
        TextView tabTv;

        ViewHolder(View tabView) {
            tabTv = tabView.findViewById(R.id.view_tab_tv);
        }
    }

    /**
     * 获取桌面列表数据
     */
    private void initData() {
        AudioFragmentVM mAudioFragmentVM = ViewModelProviders.of(this).get(AudioFragmentVM.class);
        mAudioFragmentVM.getAudioList().observe(this, new Observer<XmResource<AudioInfoBean>>() {
            @Override
            public void onChanged(@Nullable XmResource<AudioInfoBean> listXmResource) {
                if (listXmResource == null) {
                    return;
                }
                listXmResource.handle(new OnCallback<AudioInfoBean>() {
                    @Override
                    public void onSuccess(AudioInfoBean data) {
                        wrapAudioInfoData(data);
                    }
                });
            }
        });
        mAudioFragmentVM.fetchLauncherAudioList();
    }

    /**
     * 封装返回的列表数据
     *
     * @param data
     */
    private void wrapAudioInfoData(AudioInfoBean data) {
        audioInfoList.clear();
        //添加音乐标题项
        LauncherAudioInfo musicTitle = new LauncherAudioInfo();
        musicTitle.setAudioType(MainAudioAdapter.ITEM_TYPE_MUSIC_TITLE);
        musicTitle.setName(getString(R.string.audio_fragment_audio_music));
        audioInfoList.add(musicTitle);
        //音乐
        audioInfoList.addAll(data.getMusic());
        //添加电台标题项
        LauncherAudioInfo xTingTitle = new LauncherAudioInfo();
        xTingTitle.setAudioType(MainAudioAdapter.ITEM_TYPE_XTING_TITLE);
        xTingTitle.setName(getString(R.string.audio_fragment_audio_xting));
        audioInfoList.add(xTingTitle);
        //电台
        audioInfoList.addAll(data.getRadio());
        //我的收藏
        LauncherAudioInfo favorite = new LauncherAudioInfo();
        favorite.setAudioType(MainAudioAdapter.ITEM_TYPE_MY_FAVORITE);
        favorite.setName(getString(R.string.audio_fragment_audio_favorite));
        audioInfoList.add(favorite);
        //电台标题item position
        xTingTitlePosition = data.getMusic().size() + 1;
        //收藏item position
        myFavoritePosition = audioInfoList.size() - 1;
        //通知数据刷新
        mainAudioAdapter.notifyDataSetChanged();
    }
}
