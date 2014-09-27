
package me.caiying.sample;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import me.caiying.asiv.SmartImageView;

import java.util.List;

public class IgListAdapter extends BaseAdapter {
    private Context mContext;
    private List<String> imageUrls;

    public IgListAdapter(Context paramContext) {
        this.mContext = paramContext;
        this.imageUrls = Images.getImageNetUrls();
    }

    @Override
    public int getCount() {
        return imageUrls.size();
    }

    public String getUrlForRow(int paramInt) {
        return imageUrls.get(paramInt);
    }

    @Override
    public View getView(int paramInt, View paramView, ViewGroup paramViewGroup) {
        Holder localHolder;
        if (paramView == null) {
            paramView = LayoutInflater.from(this.mContext).inflate(R.layout.row_image, null);
            localHolder = new Holder();
            localHolder.igImageView = ((SmartImageView) paramView
                    .findViewById(R.id.row_image_igimageview));
            paramView.setTag(localHolder);
        } else {
            localHolder = (Holder) paramView.getTag();
        }
        localHolder.igImageView.setUrl(getUrlForRow(paramInt));
        return paramView;
    }

    static class Holder {
        SmartImageView igImageView;
    }

    @Override
    public Object getItem(int position) {
        return imageUrls.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
}
