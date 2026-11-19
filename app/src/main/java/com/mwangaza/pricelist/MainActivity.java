package com.mwangaza.pricelist;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import android.widget.ScrollView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int BLUE = 0xFF0B5FA8;
    private static final int DARK = 0xFF08457A;
    private static final int YELLOW = 0xFFFFC107;
    private static final int BG = 0xFFF2F5FA;
    private static final int RED = 0xFFD32F2F;
    private static final float W_NO = 0.8f, W_ITEM = 2.2f, W_BRAND = 1.6f,
            W_QTY = 1.5f, W_WS = 1.8f, W_RT = 1.6f;
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.##");

    private DbHelper db;
    private List<DbHelper.Item> data = new ArrayList<>();
    private ItemAdapter adapter;
    private ListView listView;
    private TextView emptyView, countView, deleteBannerText;
    private EditText searchBox;
    private View deleteBar, headerSpacer, fab;
    private boolean deleteMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DbHelper(this);
        setContentView(buildUi());
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        refresh();
    }

    // ---------------------------------------------------------------- UI

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable rounded(int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w);
    }

    private View buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        root.addView(col, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // ---- Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(14), dp(12), dp(6), dp(14));
        header.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{DARK, BLUE}));
        header.setElevation(dp(6));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Color.WHITE);
        logo.setBackground(circle);
        logo.setClipToOutline(true);
        logo.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        header.addView(logo, new LinearLayout.LayoutParams(dp(50), dp(50)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(12), 0, dp(8), 0);
        TextView title = text("MWANGAZA ELECTRONICS", 18, Color.WHITE, true);
        title.setLetterSpacing(0.06f);
        titles.addView(title);
        TextView sub = text("We Power, You Shine", 11, YELLOW, true);
        titles.addView(sub);
        header.addView(titles, weight(1f));

        ImageButton menuBtn = new ImageButton(this);
        menuBtn.setImageResource(R.drawable.ic_menu);
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, tv, true);
        menuBtn.setBackgroundResource(tv.resourceId);
        menuBtn.setPadding(dp(10), dp(10), dp(10), dp(10));
        menuBtn.setOnClickListener(this::showMenu);
        header.addView(menuBtn, new LinearLayout.LayoutParams(dp(48), dp(48)));
        col.addView(header);

        // ---- Delete mode banner
        deleteBar = new LinearLayout(this);
        LinearLayout db2 = (LinearLayout) deleteBar;
        db2.setOrientation(LinearLayout.HORIZONTAL);
        db2.setGravity(Gravity.CENTER_VERTICAL);
        db2.setBackgroundColor(RED);
        db2.setPadding(dp(16), dp(8), dp(8), dp(8));
        deleteBannerText = text("Delete mode: tap the bin beside an item", 13, Color.WHITE, true);
        db2.addView(deleteBannerText, weight(1f));
        TextView done = text("DONE", 13, RED, true);
        done.setBackground(rounded(Color.WHITE, 16));
        done.setPadding(dp(16), dp(6), dp(16), dp(6));
        done.setOnClickListener(v -> setDeleteMode(false));
        db2.addView(done);
        deleteBar.setVisibility(View.GONE);
        col.addView(deleteBar);

        // ---- Search box
        searchBox = new EditText(this);
        searchBox.setHint("Search item or brand...");
        searchBox.setSingleLine(true);
        searchBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        searchBox.setBackground(rounded(Color.WHITE, 24));
        searchBox.setElevation(dp(2));
        searchBox.setPadding(dp(18), dp(10), dp(18), dp(10));
        searchBox.setInputType(InputType.TYPE_CLASS_TEXT);
        searchBox.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable s) { refresh(); }
        });
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.setMargins(dp(12), dp(14), dp(12), dp(8));
        col.addView(searchBox, sp);

        // ---- Table card
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(rounded(Color.WHITE, 14));
        card.setElevation(dp(3));
        card.setClipToOutline(true);
        card.setOutlineProvider(ViewOutlineProvider.BACKGROUND);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setBackgroundColor(YELLOW);
        head.setPadding(dp(8), dp(10), dp(8), dp(10));
        head.addView(headCell("NO"), weight(W_NO));
        head.addView(headCell("ITEMS"), weight(W_ITEM));
        head.addView(headCell("BRAND"), weight(W_BRAND));
        head.addView(headCell("QUANTITY"), weight(W_QTY));
        head.addView(headCell("WHOLESALE\nPRICE"), weight(W_WS));
        head.addView(headCell("RETAIL\nPRICE"), weight(W_RT));
        headerSpacer = new View(this);
        headerSpacer.setVisibility(View.GONE);
        head.addView(headerSpacer, new LinearLayout.LayoutParams(dp(32), 1));
        card.addView(head);

        listView = new ListView(this);
        listView.setDivider(null);
        listView.setSelector(android.R.color.transparent);
        listView.setClipToPadding(false);
        listView.setPadding(0, 0, 0, dp(80));
        adapter = new ItemAdapter();
        listView.setAdapter(adapter);
        card.addView(listView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        emptyView = text("", 15, 0xFF6B7A8C, false);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(dp(24), dp(24), dp(24), dp(24));
        card.addView(emptyView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        cp.setMargins(dp(8), dp(4), dp(8), 0);
        col.addView(card, cp);

        countView = text("", 12, 0xFF6B7A8C, true);
        countView.setGravity(Gravity.CENTER);
        countView.setPadding(0, dp(8), 0, dp(10));
        col.addView(countView);

        // ---- Floating add button
        ImageButton f = new ImageButton(this);
        f.setImageResource(R.drawable.ic_add);
        GradientDrawable fg = new GradientDrawable();
        fg.setShape(GradientDrawable.OVAL);
        fg.setColor(YELLOW);
        f.setBackground(fg);
        f.setElevation(dp(8));
        f.setOnClickListener(v -> showItemDialog(null));
        fab = f;
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(dp(58), dp(58));
        fp.gravity = Gravity.BOTTOM | Gravity.END;
        fp.setMargins(0, 0, dp(22), dp(40));
        root.addView(f, fp);

        return root;
    }

    private TextView headCell(String s) {
        TextView t = text(s, 9.5f, DARK, true);
        return t;
    }

    // ---------------------------------------------------------------- Menu

    private void showMenu(View anchor) {
        PopupMenu pm = new PopupMenu(this, anchor, Gravity.END);
        pm.getMenu().add(0, 1, 0, "Add Item");
        pm.getMenu().add(0, 2, 1, deleteMode ? "Exit Delete Mode" : "Delete Items");
        pm.getMenu().add(0, 3, 2, "Delete All Items");
        pm.setOnMenuItemClickListener(mi -> {
            switch (mi.getItemId()) {
                case 1:
                    showItemDialog(null);
                    break;
                case 2:
                    setDeleteMode(!deleteMode);
                    break;
                case 3:
                    confirmDeleteAll();
                    break;
            }
            return true;
        });
        pm.show();
    }

    private void setDeleteMode(boolean on) {
        deleteMode = on;
        deleteBar.setVisibility(on ? View.VISIBLE : View.GONE);
        headerSpacer.setVisibility(on ? View.VISIBLE : View.GONE);
        fab.setVisibility(on ? View.GONE : View.VISIBLE);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        if (deleteMode) setDeleteMode(false);
        else super.onBackPressed();
    }

    // ---------------------------------------------------------------- Data

    private void refresh() {
        String q = searchBox == null ? "" : searchBox.getText().toString().trim();
        data = db.search(q);
        adapter.notifyDataSetChanged();
        boolean empty = data.isEmpty();
        listView.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            emptyView.setText(q.isEmpty()
                    ? "No items yet.\n\nTap the + button or the menu (top right) to add your first item."
                    : "No matching items found.");
        }
        countView.setText("Total items: " + data.size());
        if (empty && deleteMode) setDeleteMode(false);
    }

    private EditText field(LinearLayout box, String hint, int type, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(type);
        if (value != null) e.setText(value);
        box.addView(e);
        return e;
    }

    private void showItemDialog(final DbHelper.Item existing) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(12), dp(22), dp(4));
        ScrollView sv = new ScrollView(this);
        sv.addView(box);

        final int TXT = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS;
        final int NUM = InputType.TYPE_CLASS_NUMBER;
        final int DEC = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;
        final EditText itemEt = field(box, "Item (e.g. Microwave 20L)", TXT,
                existing == null ? null : existing.item);
        final EditText brandEt = field(box, "Brand (e.g. Hotpoint)", TXT,
                existing == null ? null : existing.brand);
        final EditText qtyEt = field(box, "Quantity", NUM,
                existing == null ? null : String.valueOf(existing.quantity));
        final EditText wsEt = field(box, "Wholesale price", DEC,
                existing == null ? null : plain(existing.wholesale));
        final EditText rtEt = field(box, "Retail price", DEC,
                existing == null ? null : plain(existing.retail));

        final AlertDialog d = new AlertDialog.Builder(this,
                android.R.style.Theme_Material_Light_Dialog_Alert)
                .setTitle(existing == null ? "Add Item" : "Edit Item")
                .setView(sv)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();
        d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        d.show();
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String it = itemEt.getText().toString().trim();
            String br = brandEt.getText().toString().trim();
            if (it.isEmpty()) {
                itemEt.setError("Enter the item name");
                return;
            }
            int qty = 0;
            try { qty = Integer.parseInt(qtyEt.getText().toString().trim()); } catch (Exception ignored) {}
            double ws = parseMoney(wsEt.getText().toString());
            double rt = parseMoney(rtEt.getText().toString());
            if (existing == null) db.add(it, br, qty, ws, rt);
            else db.update(existing.id, it, br, qty, ws, rt);
            d.dismiss();
            refresh();
        });
        itemEt.requestFocus();
    }

    private double parseMoney(String s) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0; }
    }

    private String plain(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    private void confirmDelete(final DbHelper.Item it) {
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert)
                .setTitle("Delete item?")
                .setMessage(it.item + (TextUtils.isEmpty(it.brand) ? "" : " (" + it.brand + ")"))
                .setPositiveButton("Delete", (d, w) -> {
                    db.delete(it.id);
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeleteAll() {
        if (data.isEmpty() && searchBox.getText().length() == 0) {
            Toast.makeText(this, "There are no items to delete", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert)
                .setTitle("Delete ALL items?")
                .setMessage("This will remove every item from the list and cannot be undone.")
                .setPositiveButton("Delete All", (d, w) -> {
                    db.deleteAll();
                    searchBox.setText("");
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ---------------------------------------------------------------- Adapter

    private class ItemAdapter extends BaseAdapter {
        @Override public int getCount() { return data.size(); }
        @Override public Object getItem(int p) { return data.get(p); }
        @Override public long getItemId(int p) { return data.get(p).id; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Row r;
            if (convertView == null) {
                r = new Row(MainActivity.this);
            } else {
                r = (Row) convertView;
            }
            final DbHelper.Item it = data.get(position);
            r.no.setText(String.valueOf(position + 1));
            r.item.setText(it.item);
            r.brand.setText(TextUtils.isEmpty(it.brand) ? "-" : it.brand);
            r.qty.setText(String.valueOf(it.quantity));
            r.ws.setText(MONEY.format(it.wholesale));
            r.rt.setText(MONEY.format(it.retail));
            r.setBackgroundColor(position % 2 == 0 ? Color.WHITE : 0xFFEEF4FB);
            r.bin.setVisibility(deleteMode ? View.VISIBLE : View.GONE);
            r.bin.setOnClickListener(v -> confirmDelete(it));
            r.setOnClickListener(v -> {
                if (!deleteMode) showItemDialog(it);
            });
            return r;
        }
    }

    private class Row extends LinearLayout {
        final TextView no, item, brand, qty, ws, rt;
        final ImageButton bin;

        Row(Context c) {
            super(c);
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            setPadding(dp(8), dp(11), dp(8), dp(11));
            no = text("", 12, 0xFF6B7A8C, true);
            item = text("", 13, 0xFF1B2733, true);
            brand = text("", 12, 0xFF3A4A5C, false);
            qty = text("", 12, 0xFF3A4A5C, false);
            ws = text("", 12, 0xFF3A4A5C, false);
            rt = text("", 12, 0xFF0B5FA8, true);
            addView(no, weight(W_NO));
            addView(item, weight(W_ITEM));
            addView(brand, weight(W_BRAND));
            addView(qty, weight(W_QTY));
            addView(ws, weight(W_WS));
            addView(rt, weight(W_RT));
            bin = new ImageButton(c);
            bin.setImageResource(R.drawable.ic_delete);
            bin.setBackground(null);
            bin.setPadding(0, 0, 0, 0);
            addView(bin, new LinearLayout.LayoutParams(dp(32), dp(32)));
        }
    }
}
