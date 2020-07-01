package io.agora.openvcall.ui.layout;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.SurfaceView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import io.agora.propeller.UserStatusData;
import io.agora.propeller.VideoInfoData;
import io.agora.propeller.ui.RecyclerItemClickListener;

public class GridVideoViewContainer extends RecyclerView {

    private final static Logger log = LoggerFactory.getLogger(GridVideoViewContainer.class);

    private GridVideoViewContainerAdapter mGridVideoViewContainerAdapter;

    public GridVideoViewContainer(Context context) {
        super(context);
    }

    public GridVideoViewContainer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public GridVideoViewContainer(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setItemEventHandler(RecyclerItemClickListener.OnItemClickListener listener) {
        this.addOnItemTouchListener(new RecyclerItemClickListener(getContext(), listener));
    }

    private boolean initAdapter(LayoutInflater inflater, Context context, int localUid, HashMap<Integer, SurfaceView> uids) {
        if (mGridVideoViewContainerAdapter == null) {
            mGridVideoViewContainerAdapter = new GridVideoViewContainerAdapter(inflater, context, localUid, uids);
            mGridVideoViewContainerAdapter.setHasStableIds(true);
            return true;
        }
        return false;
    }

    public void initViewContainer(LayoutInflater inflater, Context context, int localUid, HashMap<Integer, SurfaceView> uids, boolean isLandscape) {
        boolean newCreated = initAdapter(inflater, context, localUid, uids);

        if (!newCreated) {
            mGridVideoViewContainerAdapter.setLocalUid(localUid);
            mGridVideoViewContainerAdapter.customizedInit(uids, true);
        }

        this.setAdapter(mGridVideoViewContainerAdapter);

        int orientation = isLandscape ? RecyclerView.HORIZONTAL : RecyclerView.VERTICAL;

        int count = uids.size();
        if (count <= 2) { // only local full view or or with one peer
            this.setLayoutManager(new LinearLayoutManager(context, orientation, false));
        } else if (count > 2) {
            int itemSpanCount = getNearestSqrt(count);
            this.setLayoutManager(new GridLayoutManager(context, itemSpanCount, orientation, false));
        }

        mGridVideoViewContainerAdapter.notifyDataSetChanged();
    }

    private int getNearestSqrt(int n) {
        return (int) Math.sqrt(n);
    }

    public void notifyUiChanged(HashMap<Integer, SurfaceView> uids, int localUid, HashMap<Integer, Integer> status, HashMap<Integer, Integer> volume) {
        if (mGridVideoViewContainerAdapter == null) {
            return;
        }
        mGridVideoViewContainerAdapter.notifyUiChanged(uids, localUid, status, volume);
    }

    public void addVideoInfo(int uid, VideoInfoData video) {
        if (mGridVideoViewContainerAdapter == null) {
            return;
        }
        mGridVideoViewContainerAdapter.addVideoInfo(uid, video);
    }

    public void cleanVideoInfo() {
        if (mGridVideoViewContainerAdapter == null) {
            return;
        }
        mGridVideoViewContainerAdapter.cleanVideoInfo();
    }

    public UserStatusData getItem(int position) {
        return mGridVideoViewContainerAdapter.getItem(position);
    }

}
