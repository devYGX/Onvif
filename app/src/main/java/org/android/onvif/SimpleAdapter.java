package org.android.onvif;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.List;

public class SimpleAdapter<Text> extends RecyclerView.Adapter<SimpleHolder> {

    private final List<Text> textList;
    private final Fun<Text, String> function;
    private OnItemClickListener onItemClickListener;

    public SimpleAdapter(List<Text> list, Fun<Text, String> function) {
        this.textList = list;
        this.function = function;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener){
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public SimpleHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new SimpleHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_simple, viewGroup, false), (RecyclerView) viewGroup);
    }

    @Override
    public void onBindViewHolder(@NonNull SimpleHolder viewHolder, int i) {
        Text text = textList.get(i);
        viewHolder.bindData(function.apply(text), i);

        viewHolder.setOnItemClickListener(myItemClickListener);
    }

    private OnItemClickListener myItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClickListener(RecyclerView view, int position) {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClickListener(view,position);
            }
        }
    };

    @Override
    public int getItemCount() {
        return textList == null ? 0 : textList.size();
    }
}
