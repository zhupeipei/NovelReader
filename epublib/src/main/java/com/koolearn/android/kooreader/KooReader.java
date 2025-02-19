package com.koolearn.android.kooreader;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.githang.statusbar.StatusBarCompat;
import com.koolearn.android.kooreader.api.KooReaderIntents;
import com.koolearn.android.kooreader.httpd.DataService;
import com.koolearn.android.kooreader.libraryService.BookCollectionShadow;
import com.koolearn.android.kooreader.popup.NavigationPopup;
import com.koolearn.android.kooreader.popup.PopupPanel;
import com.koolearn.android.kooreader.popup.ProgressPopup;
import com.koolearn.android.kooreader.popup.SelectionPopup;
import com.koolearn.android.kooreader.popup.SettingPopup;
import com.koolearn.android.kooreader.popup.ShowNavigationAction;
import com.koolearn.android.util.UIMessageUtil;
import com.koolearn.android.util.UIUtil;
import com.koolearn.klibrary.core.application.ZLApplicationWindow;
import com.koolearn.klibrary.core.filesystem.ZLFile;
import com.koolearn.klibrary.core.options.Config;
import com.koolearn.klibrary.core.util.ZLColor;
import com.koolearn.klibrary.core.view.ZLViewWidget;
import com.koolearn.klibrary.text.view.ZLTextView;
import com.koolearn.klibrary.ui.android.curl.CurlView;
import com.koolearn.klibrary.ui.android.error.ErrorKeys;
import com.koolearn.klibrary.ui.android.view.AndroidFontUtil;
import com.koolearn.klibrary.ui.android.view.ZLAndroidCurlWidget;
import com.koolearn.klibrary.ui.android.view.ZLAndroidPaintContext;
import com.koolearn.klibrary.ui.android.view.ZLAndroidWidget;
import com.koolearn.kooreader.Paths;
import com.koolearn.kooreader.book.Book;
import com.koolearn.kooreader.book.BookUtil;
import com.koolearn.kooreader.book.Bookmark;
import com.koolearn.kooreader.bookmodel.BookModel;
import com.koolearn.kooreader.kooreader.ActionCode;
import com.koolearn.kooreader.kooreader.KooReaderApp;
import com.kooreader.util.NavigationUtil;
import com.kooreader.util.StatusBarManager;
import com.ninestars.DialogActivity;
import com.ninestars.ReaderEvent;
import com.ninestars.android.R;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;

/**
 * 阅读器主页面
 */
public final class KooReader extends KooReaderMainActivity implements ZLApplicationWindow {
    public static final int RESULT_DO_NOTHING = RESULT_FIRST_USER;

    public static void openBookActivity(Context context, Book book, Bookmark bookmark) {
        final Intent intent = new Intent(context, KooReader.class);
        intent.setAction(KooReaderIntents.Action.VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        KooReaderIntents.putBookExtra(intent, book);
        KooReaderIntents.putBookmarkExtra(intent, bookmark);
        context.startActivity(intent);
    }

    private KooReaderApp myKooReaderApp;
    private volatile Book myBook;
    private Bookmark myBookmark;
    private RelativeLayout myRootView;
    private ZLAndroidWidget myMainView;
    private ZLAndroidCurlWidget myCurlView;

    final DataService.Connection DataConnection = new DataService.Connection();

    volatile boolean IsPaused = false;
//    private volatile long myResumeTimestamp; // 数据同步

    private Intent myOpenBookIntent = null;

    private synchronized void openBook(Intent intent, final Runnable action, boolean force) {
        if (!force && myBook != null) {
            return;
        }
        myBook = KooReaderIntents.getBookExtra(intent, myKooReaderApp.Collection);

        if (myBook == null) {
            final Uri data = intent.getData();
            if (data != null) {
                myBook = createBookForFile(ZLFile.createFileByPath(data.getPath()));
            }
        }
        if (myBook != null) {
            ZLFile file = BookUtil.fileByBook(myBook);
            if (!file.exists()) {
                if (file.getPhysicalFile() != null) {
                    file = file.getPhysicalFile();
                }
                UIMessageUtil.showErrorMessage(this, "fileNotFound", file.getPath());
                myBook = null;
            } else {

            }
        }
        Config.Instance().runOnConnect(new Runnable() {
            public void run() {
                myKooReaderApp.openBook(myBook, myBookmark, action); // UIUtil
                AndroidFontUtil.clearFontCache();
            }
        });
    }

    private Book createBookForFile(ZLFile file) {
        if (file == null) {
            return null;
        }
        Book book = myKooReaderApp.Collection.getBookByFile(file.getPath());
        if (book != null) {
            return book;
        }
        if (file.isArchive()) {
            for (ZLFile child : file.children()) {
                book = myKooReaderApp.Collection.getBookByFile(child.getPath());
                if (book != null) {
                    return book;
                }
            }
        }
        return null;
    }

    /**
     * 登陆提示框
     * @param readerEvent
     */
    @Subscribe
    public void showDialog(ReaderEvent readerEvent){
        startActivity(new Intent(KooReader.this, DialogActivity.class));
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        StatusBarCompat.setStatusBarColor(this, getResources().getColor(R.color.white), true);
        if (!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }
//        EventBus.getDefault().post(new FinishEvent());
        myRootView = (RelativeLayout) findViewById(R.id.root_view);
        myMainView = (ZLAndroidWidget) findViewById(R.id.main_view);
        myCurlView = (ZLAndroidCurlWidget) findViewById(R.id.curl_view);
        myCurlView.setMargins(0, 0, 0, 0);
        myCurlView.setSizeChangedObserver(new CurlView.SizeChangedObserver() {
            @Override
            public void onSizeChanged(int width, int height) {
                myCurlView.setMargins(0, 0, 0, 0);
                myCurlView.setViewMode(CurlView.SHOW_ONE_PAGE);
            }
        });
        myKooReaderApp = (KooReaderApp) KooReaderApp.Instance();
        if (myKooReaderApp == null) {
            myKooReaderApp = new KooReaderApp(Paths.systemInfo(this), new BookCollectionShadow());
        }
        getCollection().bindToService(this, null); // 绑定libraryService

        myBook = null;

        myKooReaderApp.setWindow(this);
        myKooReaderApp.initWindow();

//        getWindow().setFlags( //y 全屏
//                WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN // 设置窗体全屏
//        );

        NavigationUtil.hideNavigationBar(getWindow(), true);
        fitStatusBar(true, false);


        if (myKooReaderApp.getPopupById(NavigationPopup.ID) == null) {
            new NavigationPopup(myKooReaderApp);
        }
        if (myKooReaderApp.getPopupById(SettingPopup.ID) == null) {
            new SettingPopup(myKooReaderApp);
        }
        if (myKooReaderApp.getPopupById(SelectionPopup.ID) == null) {
            new SelectionPopup(myKooReaderApp);
        }
        if (myKooReaderApp.getPopupById(ProgressPopup.ID) == null) {
            new ProgressPopup(myKooReaderApp);
        }
        myKooReaderApp.addAction(ActionCode.SHOW_NAVIGATION, new ShowNavigationAction(this, myKooReaderApp)); //y 页面跳转
        myKooReaderApp.addAction(ActionCode.PROCESS_HYPERLINK, new ProcessHyperlinkAction(this, myKooReaderApp)); //y 打开超链接、图片等
        myKooReaderApp.addAction(ActionCode.OPEN_VIDEO, new OpenVideoAction(this, myKooReaderApp));
        myKooReaderApp.addAction(ActionCode.HIDE_TOAST, new HideToastAction(this, myKooReaderApp));

        myKooReaderApp.addAction(ActionCode.SELECTION_BOOKMARK, new SelectionBookmarkAction(this, myKooReaderApp));
        myKooReaderApp.addAction(ActionCode.SELECTION_SHOW_PANEL, new SelectionShowPanelAction(this, myKooReaderApp));
        myKooReaderApp.addAction(ActionCode.SELECTION_HIDE_PANEL, new SelectionHidePanelAction(this, myKooReaderApp));
        myKooReaderApp.addAction(ActionCode.SELECTION_COPY_TO_CLIPBOARD, new SelectionCopyAction(this, myKooReaderApp));
        myKooReaderApp.addAction(ActionCode.SELECTION_SHARE, new SelectionShareAction(this, myKooReaderApp));
        new OpenPhotoAction(this, myKooReaderApp, myRootView);
//        final Intent intent = getIntent();
//        myOpenBookIntent = intent;
        ZLAndroidPaintContext.myReader = myKooReaderApp;
    }


    @Override
    protected void onStart() {
        super.onStart();
        //初始化
        ((ProgressPopup) myKooReaderApp.getPopupById(ProgressPopup.ID)).setPanelInfo(this, myRootView);
        ((NavigationPopup) myKooReaderApp.getPopupById(NavigationPopup.ID)).setPanelInfo(this, myRootView);
        ((SettingPopup) myKooReaderApp.getPopupById(SettingPopup.ID)).setPanelInfo(this, myRootView);
        ((PopupPanel) myKooReaderApp.getPopupById(SelectionPopup.ID)).setPanelInfo(this, myRootView);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        switchWakeLock(hasFocus && getZLibrary().BatteryLevelToTurnScreenOffOption.getValue() < myKooReaderApp.getBatteryLevel());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (myCurlView != null) {   // && 显示
            myCurlView.onResume();
        }
        myStartTimer = true;
        Config.Instance().runOnConnect(new Runnable() {
            public void run() {
                final int brightnessLevel = getZLibrary().ScreenBrightnessLevelOption.getValue(); // 亮度设置
                if (brightnessLevel != 0) {
                    getViewWidget().setScreenBrightness(brightnessLevel);
                } else {
                    setScreenBrightnessAuto();
                }
                getCollection().bindToService(KooReader.this, new Runnable() { // 字体类型等设置改变时更新界面 clearTextCaches(); getViewWidget().repaint();
                    public void run() {
                        final BookModel model = myKooReaderApp.Model;
                        if (model == null || model.Book == null) {
                            return; // 首次调用会进入一次
                        }
                        onPreferencesUpdate(myKooReaderApp.Collection.getBookById(model.Book.getId()));
                    }
                });
            }
        });

        registerReceiver(myBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        IsPaused = false;
//        myResumeTimestamp = System.currentTimeMillis();
        myOpenBookIntent = getIntent();
//        Bookmark mark = KooReaderIntents.getBookmarkExtra(getIntent());
        if (myOpenBookIntent != null) {
//            myOpenBookIntent = null;
            getCollection().bindToService(this, new Runnable() {
                public void run() {
                    System.out.println(KooReaderIntents.getBookmarkExtra(myOpenBookIntent));
                    openBook(myOpenBookIntent, null, true);
                }
            });
        }

        NavigationUtil.hideNavigationBar(getWindow(), true);
        fitStatusBar(true, false);
    }

    private void fitStatusBar(boolean hideStatusBar, boolean darkStatusBar) {
        Window window = getWindow();
        if (window == null)
            return;
        StatusBarManager.canChangeColor(window);
        StatusBarManager.transparencyBar(this);
        if (hideStatusBar) {
            StatusBarManager.hideStatusBar(window, true);
        } else {
            StatusBarManager.hideStatusBar(window, false);
            if (darkStatusBar) {
                StatusBarManager.setStatusBarColor(window, true);
            } else {
                StatusBarManager.setStatusBarColor(window, false);
            }
        }
    }

    @Override
    protected void onPause() {
        IsPaused = true;

        if (myCurlView != null && myCurlView.getVisibility() == View.VISIBLE) {
            myCurlView.onPause();
        }
        try {
            unregisterReceiver(myBatteryInfoReceiver);
        } catch (IllegalArgumentException e) {
        }

        myKooReaderApp.stopTimer();
        myKooReaderApp.onWindowClosing();
        super.onPause();
    }


    @Override
    protected void onStop() {
        OpenPhotoAction.isOpen = false; // 防止未关闭图片直接按按返回键
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        getCollection().unbind();
        super.onDestroy();
        if (EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().unregister(this);
        }
    }

    @Override
    public void onLowMemory() {
        myKooReaderApp.onWindowClosing();
        super.onLowMemory();
    }

//    @Override
//    public boolean onSearchRequested() {
//        final KooReaderApp.PopupPanel popup = myKooReaderApp.getActivePopup();
//        myKooReaderApp.hideActivePopup();
//        if (DeviceType.Instance().hasStandardSearchDialog()) {
//            final SearchManager manager = (SearchManager)getSystemService(SEARCH_SERVICE);
//            manager.setOnCancelListener(new SearchManager.OnCancelListener() {
//                public void onCancel() {
//                    if (popup != null) {
//                        myKooReaderApp.showPopup(popup.getId());
//                    }
//                    manager.setOnCancelListener(null);
//                }
//            });
//            startSearch(myKooReaderApp.MiscOptions.TextSearchPattern.getValue(), true, null, false);
//        } else {
//            SearchDialogUtil.showDialog(
//                    this, KooReader.class, myKooReaderApp.MiscOptions.TextSearchPattern.getValue(), new DialogInterface.OnCancelListener() {
//                        @Override
//                        public void onCancel(DialogInterface di) {
//                            if (popup != null) {
//                                myKooReaderApp.showPopup(popup.getId());
//                            }
//                        }
//                    }
//            );
//        }
//        return true;
//    }

    //选中字体之后，进行的提示
    public void showSelectionPanel() {
        final ZLTextView view = myKooReaderApp.getTextView();
        ((SelectionPopup) myKooReaderApp.getPopupById(SelectionPopup.ID))
                .move(view.getSelectionStartY(), view.getSelectionEndY());
        myKooReaderApp.showPopup(SelectionPopup.ID);
    }

    //清楚选中的字体
    public void hideSelectionPanel() {
        final KooReaderApp.PopupPanel popup = myKooReaderApp.getActivePopup();
        if (popup != null && popup.getId() == SelectionPopup.ID) {
            myKooReaderApp.hideActivePopup();
        }
    }

    private void onPreferencesUpdate(Book book) {
        AndroidFontUtil.clearFontCache();
        myKooReaderApp.onBookUpdated(book);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
            case REQUEST_PREFERENCES:
                if (resultCode != RESULT_DO_NOTHING && data != null) {
                    final Book book = KooReaderIntents.getBookExtra(data, myKooReaderApp.Collection);
                    if (book != null) {
                        getCollection().bindToService(this, new Runnable() {
                            public void run() {
                                onPreferencesUpdate(book);
                            }
                        });
                    }
                }
                break;
            case REQUEST_EDITBOOKMARK:
                if (resultCode == RESULT_SELECTCOLOR) {
                    int selectColor = data.getIntExtra("selectColor", 9846973);
                    myKooReaderApp.ViewOptions.getColorProfile().SelectionBackgroundOption.setValue(new ZLColor(selectColor));
                }
                break;
        }
    }

    @Override
    public void hideDictionarySelection() {
        myKooReaderApp.getTextView().hideOutline();
//y        myKooReaderApp.getTextView().removeHighlightings(DictionaryHighlighting.class);
        myKooReaderApp.getViewWidget().reset();
        myKooReaderApp.getViewWidget().repaint();
    }

    //弹出一级菜单
    public void navigate() {
        ((NavigationPopup) myKooReaderApp.getPopupById(NavigationPopup.ID)).runNavigation();
//        ((MainMenuPopup)myKooReaderApp.getPopupById(MainMenuPopup.ID)).runNavigation();
    }

    //点击设置，弹出二级菜单
    public void setting() {
        ((SettingPopup) myKooReaderApp.getPopupById(SettingPopup.ID)).runNavigation();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return (myMainView != null && myMainView.onKeyDown(keyCode, event)) || super.onKeyDown(keyCode, event);
//        return (myCurlView != null && myCurlView.onKeyDown(keyCode, event)) || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
//        return (myCurlView != null && myCurlView.onKeyUp(keyCode, event)) || super.onKeyUp(keyCode, event);
        return (myMainView != null && myMainView.onKeyUp(keyCode, event)) || super.onKeyUp(keyCode, event);
    }

    private PowerManager.WakeLock myWakeLock;
    private boolean myWakeLockToCreate;
    private boolean myStartTimer;

    public final void createWakeLock() {
        // 滑动过程不停调用
        if (myWakeLockToCreate) { // 首次调用1次
            synchronized (this) {
                if (myWakeLockToCreate) {
                    myWakeLockToCreate = false;
                    myWakeLock =
                            ((PowerManager) getSystemService(POWER_SERVICE))
                                    .newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "KooReader");
                    myWakeLock.acquire();
                }
            }
        }
        if (myStartTimer) { // 首次调用1次
            myKooReaderApp.startTimer();
            myStartTimer = false;
        }
    }

    private final void switchWakeLock(boolean on) {
        if (on) {
            if (myWakeLock == null) {
                myWakeLockToCreate = true;
            }
        } else {
            if (myWakeLock != null) {
                synchronized (this) {
                    if (myWakeLock != null) {
                        myWakeLock.release();
                        myWakeLock = null;
                    }
                }
            }
        }
    }

    private BroadcastReceiver myBatteryInfoReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final int level = intent.getIntExtra("level", 100);
            setBatteryLevel(level);
            switchWakeLock(
                    hasWindowFocus() &&
                            getZLibrary().BatteryLevelToTurnScreenOffOption.getValue() < level
            );
        }
    };

    private BookCollectionShadow getCollection() {
        return (BookCollectionShadow) myKooReaderApp.Collection;
    }

    @Override
    public void showErrorMessage(String key) {
        UIMessageUtil.showErrorMessage(this, key);
    }

    @Override
    public void showErrorMessage(String key, String parameter) {
        UIMessageUtil.showErrorMessage(this, key, parameter);
    }

    @Override
    public KooReaderApp.SynchronousExecutor createExecutor(String key) {
        return UIUtil.createExecutor(this, key);
    }

    private int myBatteryLevel;

    @Override
    public int getBatteryLevel() {
        return myBatteryLevel;
    }

    private void setBatteryLevel(int percent) {
        myBatteryLevel = percent;
    }

    @Override
    public void close() {
        finish();
    }

    @Override
    public ZLViewWidget getViewWidget() {
        return myMainView;
//        return myCurlView;
    }

    @Override
    public void hideViewWidget(boolean flag) {
        if (myCurlView != null && myMainView != null) {
            if (flag) {
                myCurlView.setVisibility(View.VISIBLE);
                myMainView.setVisibility(View.GONE);
            } else {
                myCurlView.setVisibility(View.GONE);
                myMainView.setVisibility(View.VISIBLE);
            }
        } else {
            Toast.makeText(this, "view 切换错误", Toast.LENGTH_SHORT).show();
        }
    }

    private final HashMap<MenuItem, String> myMenuItemMap = new HashMap<MenuItem, String>();

    private final MenuItem.OnMenuItemClickListener myMenuListener =
            new MenuItem.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    myKooReaderApp.runAction(myMenuItemMap.get(item));
                    return true;
                }
            };

    @Override
    public void refresh() {
    }

    @Override
    public void processException(Exception exception) {
        exception.printStackTrace();
        final Intent intent = new Intent(
                KooReaderIntents.Action.ERROR,
                new Uri.Builder().scheme(exception.getClass().getSimpleName()).build()
        );
        intent.setPackage(KooReaderIntents.DEFAULT_PACKAGE);
        intent.putExtra(ErrorKeys.MESSAGE, exception.getMessage());
        final StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));
        intent.putExtra(ErrorKeys.STACKTRACE, stackTrace.toString());
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    //弹出进度菜单
    public void progress() {
        ((ProgressPopup) myKooReaderApp.getPopupById(ProgressPopup.ID)).runNavigation();
    }
}