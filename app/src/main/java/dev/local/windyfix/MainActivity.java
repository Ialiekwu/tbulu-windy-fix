package dev.local.windyfix;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding * 2, padding, padding);
        scroll.addView(layout);
        addText(layout, "两步路 Windy 修复", 26, Color.rgb(20, 108, 84));
        addText(layout, "1.0.0 · 仅作用于两步路户外助手", 15, Color.DKGRAY);
        addText(layout, "启用方法\n\n1. 在 LSPosed 中启用本模块，作用域只勾选“两步路户外助手”。\n\n2. 完全退出并重新打开两步路。\n\n3. 选择已有 windy 图层，继续原来的离线下载任务。", 18, Color.BLACK);
        addText(layout, "模块是否启用，请以 LSPosed 中的开关和作用域为准。切换图层后若有空白，可轻微拖动或缩放刷新。\n\n停用：在 LSPosed 中关闭本模块，然后重启两步路。已下载的地图仍然保留。", 16, Color.DKGRAY);
        Button open = new Button(this);
        open.setText("打开两步路");
        open.setOnClickListener(view -> {
            Intent intent = getPackageManager().getLaunchIntentForPackage(TilePolicy.TARGET_PACKAGE);
            if (intent == null) Toast.makeText(this, "未找到两步路户外助手", Toast.LENGTH_SHORT).show();
            else startActivity(intent);
        });
        layout.addView(open);
        setContentView(scroll);
    }

    private void addText(LinearLayout layout, String text, int size, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setPadding(0, 0, 0, (int) (24 * getResources().getDisplayMetrics().density));
        layout.addView(view);
    }
}
