package org.android.onvif;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

public class SimpleHolder extends RecyclerView.ViewHolder {

    private final RecyclerView recyclerView;
    private final TextView textView;
    private OnItemClickListener onItemClickListener;
    private int position;

    public SimpleHolder(@NonNull View itemView, RecyclerView parentView) {
        super(itemView);
        textView = itemView.findViewById(R.id.textView);
        itemView.setOnClickListener(onClickListener);
        recyclerView = parentView;
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (onItemClickListener != null)
                onItemClickListener.onItemClickListener(recyclerView, position);
        }
    };


    public void bindData(String text, int position) {
        textView.setText(text);
        this.position = position;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }
}
