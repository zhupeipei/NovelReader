package com.koolearn.android.kooreader.popup;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.koolearn.android.kooreader.KooReader;
import com.koolearn.android.kooreader.TOCActivity;
import com.koolearn.android.kooreader.api.KooReaderIntents;
import com.koolearn.android.util.OrientationUtil;
import com.koolearn.klibrary.core.application.ZLApplication;
import com.koolearn.klibrary.text.view.ZLTextView;
import com.koolearn.klibrary.text.view.ZLTextWordCursor;
import com.koolearn.klibrary.ui.android.library.ZLAndroidApplication;
import com.koolearn.kooreader.kooreader.ActionCode;
import com.koolearn.kooreader.kooreader.KooReaderApp;
import com.kooreader.util.BaseUtil;
import com.kooreader.util.NavigationUtil;
import com.kooreader.util.StatusBarManager;
import com.ninestars.android.R;
public final class NavigationPopup extends ZLApplication.PopupPanel implements View.OnClickListener {
    public final static String ID = "NavigationPopup";

    private volatile NavigationWindow myWindow;
    private volatile MainMenuWindow myWindowTop;
    private volatile KooReader myActivity;
    private volatile RelativeLayout myRoot;
    private ZLTextWordCursor myStartPosition;
    private final KooReaderApp myKooReader;
    private volatile boolean myIsInProgress;
    private ZLTextView.PagePosition pagePosition;
    private TextView mTextViewToc,mTextViewProgress,mTextViewFonts;
    private ImageButton mButtonBack,mButtonBookMark;
    public NavigationPopup(KooReaderApp kooReader) {
        super(kooReader);
        myKooReader = kooReader;
    }

    public void setPanelInfo(KooReader activity, RelativeLayout root) {
        myActivity = activity;
        myRoot = root;
    }

    public void runNavigation() {
        if (myWindow == null || myWindow.getVisibility() == View.GONE) {
            myIsInProgress = false;
            Application.showPopup(ID);
        }
    }

    @Override
    protected void show_() {
        setStatusBarVisibility(true);
        if (myActivity != null) {
            createPanel(myActivity, myRoot);
        }
        if (myWindow != null) {
            myWindow.show();
        }
        if (myWindowTop != null) {
            myWindowTop.show();
        }
    }

    @Override
    protected void hide_() {
        setStatusBarVisibility(false);
        if (myWindow != null) {
            myWindow.hide();
        }
        if (myWindowTop != null) {
            myWindowTop.hide();
        }
    }

    private void setStatusBarVisibility(boolean visible) {
        if (visible) {
            NavigationUtil.hideNavigationBar(myActivity.getWindow(), false);
            fitStatusBar(false, true);
        } else {
            NavigationUtil.hideNavigationBar(myActivity.getWindow(), true);
            fitStatusBar(true, false);
        }
    }

    private void fitStatusBar(boolean hideStatusBar, boolean darkStatusBar) {
        Window window = myActivity.getWindow();
        if (window == null)
            return;
        StatusBarManager.canChangeColor(window);
        StatusBarManager.transparencyBar(myActivity);
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
    public String getId() {
        return ID;
    }

    @Override
    protected void update() {

    }
    private void createPanel(KooReader activity, RelativeLayout root) {
        if (myWindow != null && activity == myWindow.getContext() && myWindowTop != null) {
            return;
        }
        activity.getLayoutInflater().inflate(R.layout.navigation_panel, root);
        activity.getLayoutInflater().inflate(R.layout.mainmenu, root);
        myWindow = (NavigationWindow) root.findViewById(R.id.navigation_panel);
        myWindowTop = (MainMenuWindow) root.findViewById(R.id.mainmenuwindow);
        RelativeLayout rl = root.findViewById(R.id.relative1);
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) rl.getLayoutParams();
        lp.topMargin = BaseUtil.getStatusBarHeight(ZLAndroidApplication.getContext());

        init();
        initClick();
    }
    private void initClick() {
        mTextViewToc.setOnClickListener(this);
        mTextViewProgress.setOnClickListener(this);
        mTextViewFonts.setOnClickListener(this);
        mButtonBack.setOnClickListener(this);
        mButtonBookMark.setOnClickListener(this);
    }
    private void init() {
        mTextViewToc = (TextView) myWindow.findViewById(R.id.navigation_toc);//目录
        mTextViewFonts = (TextView) myWindow.findViewById(R.id.navigation_fonts);//设置
        mTextViewProgress = (TextView) myWindow.findViewById(R.id.navigation_progress);
        mButtonBookMark = (ImageButton) myWindowTop.findViewById(R.id.mark);
        mButtonBack = (ImageButton) myWindowTop.findViewById(R.id.back);
    }

    final void removeWindow(Activity activity) {

        if (myWindow != null && activity == myWindow.getContext()) {
            final ViewGroup root = (ViewGroup) myWindow.getParent();
            myWindow.hide();
            root.removeView(myWindow);
            myWindow = null;
        }
        if (myWindowTop != null && activity == myWindowTop.getContext()) {
            final ViewGroup root = (ViewGroup) myWindowTop.getParent();
            myWindowTop.hide();
            root.removeView(myWindowTop);
            myWindowTop = null;
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.navigation_toc) {
            toc();

        } else if (i == R.id.navigation_progress) {
            progress();

        } else if (i == R.id.navigation_fonts) {
            fonts();

        } else if (i == R.id.mark) {
            myKooReader.addBookMark();
            Toast.makeText(myActivity, "书签添加成功", Toast.LENGTH_SHORT).show();

        } else if (i == R.id.back) {
            myActivity.finish();

        }
    }

    private void toc(){
        Application.hideActivePopup();
        final Intent intent =
                new Intent(myActivity.getApplicationContext(), TOCActivity.class);
        KooReaderIntents.putBookExtra(intent, myKooReader.getCurrentBook());
        KooReaderIntents.putBookmarkExtra(intent, myKooReader.createBookmark(100, true));
        OrientationUtil.startActivity(myActivity, intent);
    }
    private void progress(){
        //隐藏其他的pop
        Application.hideActivePopup();
        //显示我们需要的进度pop
        ((ProgressPopup) myKooReader.getPopupById(ProgressPopup.ID)).runNavigation();
    }
    private void fonts(){
        Application.hideActivePopup();
        ((SettingPopup) myKooReader.getPopupById(SettingPopup.ID)).runNavigation();
    }
    private void mark(){
        Application.hideActivePopup();
        Application.runAction(ActionCode.SELECTION_BOOKMARK);
    }
}