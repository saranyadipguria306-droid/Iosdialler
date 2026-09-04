package com.example.mydialer;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    LinearLayout root, content;
    TextView title, numberDisplay;
    final ArrayList<Contact> contacts = new ArrayList<>();
    final ArrayList<Contact> favorites = new ArrayList<>();
    final ArrayList<Recent> recents = new ArrayList<>();
    int green=Color.rgb(52,199,89), bg=Color.rgb(248,248,250), text=Color.rgb(20,20,22), muted=Color.rgb(110,110,115);

    int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    TextView tv(String s,float size){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(text); return t; }
    Button nav(String s){ Button b=new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(13); b.setTextColor(muted); b.setBackgroundColor(Color.TRANSPARENT); return b; }
    Button key(String s){ Button b=new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(25); b.setTextColor(text); b.setBackgroundColor(Color.WHITE); b.setPadding(0,0,0,0); return b; }

    @Override public void onCreate(Bundle b){
        super.onCreate(b); getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg);
        requestNeeded(); loadContacts(); loadRecents(); showDialer();
    }

    void requestNeeded(){
        ArrayList<String> p=new ArrayList<>();
        if(checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED) p.add(Manifest.permission.READ_CONTACTS);
        if(checkSelfPermission(Manifest.permission.READ_CALL_LOG)!=PackageManager.PERMISSION_GRANTED) p.add(Manifest.permission.READ_CALL_LOG);
        if(!p.isEmpty()) requestPermissions(p.toArray(new String[0]),50);
    }

    void shell(String heading){
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(bg);
        LinearLayout head=new LinearLayout(this); head.setPadding(dp(22),dp(15),dp(18),dp(5)); head.setGravity(Gravity.CENTER_VERTICAL);
        title=tv(heading,30); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); head.addView(title,new LinearLayout.LayoutParams(0,dp(55),1));
        TextView plus=tv("＋",32); plus.setGravity(Gravity.CENTER); plus.setTextColor(green); plus.setOnClickListener(v->showDialer());
        head.addView(plus,new LinearLayout.LayoutParams(dp(50),dp(55)));
        root.addView(head);
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(16),0,dp(16),0);
        root.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(this); nav.setBackgroundColor(Color.WHITE); nav.setGravity(Gravity.CENTER);
        Button rec=nav("🕘\nRecents"), fav=nav("★\nFavorites"), dial=nav("⌨\nKeypad"), con=nav("👥\nContacts");
        rec.setOnClickListener(v->showRecents()); fav.setOnClickListener(v->showFavorites()); dial.setOnClickListener(v->showDialer()); con.setOnClickListener(v->showContacts());
        nav.addView(rec,new LinearLayout.LayoutParams(0,dp(65),1)); nav.addView(fav,new LinearLayout.LayoutParams(0,dp(65),1)); nav.addView(dial,new LinearLayout.LayoutParams(0,dp(65),1)); nav.addView(con,new LinearLayout.LayoutParams(0,dp(65),1));
        root.addView(nav); setContentView(root);
    }

    void showDialer(){
        shell("Keypad");
        numberDisplay=tv("",34); numberDisplay.setGravity(Gravity.CENTER); numberDisplay.setTypeface(Typeface.DEFAULT,Typeface.NORMAL);
        content.addView(numberDisplay,new LinearLayout.LayoutParams(-1,dp(75)));
        String[][] k={{"1",""},{"2","ABC"},{"3","DEF"},{"4","GHI"},{"5","JKL"},{"6","MNO"},{"7","PQRS"},{"8","TUV"},{"9","WXYZ"},{"*",""},{"0","+"},{"#",""}};
        LinearLayout grid=new LinearLayout(this); grid.setOrientation(LinearLayout.VERTICAL);
        for(int r=0;r<4;r++){ LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER);
            for(int c=0;c<3;c++){ int i=r*3+c; Button b=key(k[i][0]+"\n"+k[i][1]); b.setOnClickListener(v->{String s=((Button)v).getText().toString().substring(0,1); numberDisplay.setText(numberDisplay.getText()+s);}); row.addView(b,new LinearLayout.LayoutParams(0,dp(62),1));}
            grid.addView(row,new LinearLayout.LayoutParams(-1,0,1));
        }
        content.addView(grid,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout actions=new LinearLayout(this); actions.setGravity(Gravity.CENTER);
        Button call=key("●"); call.setTextColor(Color.WHITE); call.setTextSize(28); call.setBackgroundColor(green); call.setOnClickListener(v->callNumber());
        Button del=key("⌫"); del.setTextSize(23); del.setOnClickListener(v->{String s=numberDisplay.getText().toString(); if(!s.isEmpty()) numberDisplay.setText(s.substring(0,s.length()-1));});
        actions.addView(call,new LinearLayout.LayoutParams(dp(76),dp(68))); actions.addView(del,new LinearLayout.LayoutParams(dp(76),dp(68)));
        content.addView(actions);
    }

    void callNumber(){
        String n=numberDisplay==null?"":numberDisplay.getText().toString().trim();
        if(n.isEmpty()){Toast.makeText(this,"Enter a number",Toast.LENGTH_SHORT).show();return;}
        if(checkSelfPermission(Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CALL_PHONE},51);return;}
        startActivity(new Intent(Intent.ACTION_CALL,Uri.parse("tel:"+Uri.encode(n))));
    }

    void showContacts(){
        shell("Contacts");
        EditText search=new EditText(this); search.setHint("Search"); search.setSingleLine(); search.setTextSize(17);
        content.addView(search,new LinearLayout.LayoutParams(-1,dp(52)));
        LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll=new ScrollView(this); scroll.addView(list); content.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        Runnable refresh=()->{list.removeAllViews(); String q=search.getText().toString().toLowerCase();
            for(Contact c:contacts) if(c.name.toLowerCase().contains(q)||c.phone.contains(q)) addContactRow(list,c);
        };
        refresh.run(); search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){} public void onTextChanged(CharSequence s,int a,int b,int c){refresh.run();} public void afterTextChanged(android.text.Editable e){}});
    }

    void addContactRow(LinearLayout list, Contact c){
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(5),dp(7),dp(5),dp(7));
        TextView avatar=tv(initials(c.name),18); avatar.setGravity(Gravity.CENTER); avatar.setTextColor(Color.WHITE); avatar.setBackgroundColor(Color.rgb(130,130,135));
        row.addView(avatar,new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout info=new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL); info.setPadding(dp(12),0,0,0);
        TextView n=tv(c.name,17); n.setTypeface(Typeface.DEFAULT,Typeface.BOLD); TextView p=tv(c.phone,14); p.setTextColor(muted);
        info.addView(n); info.addView(p); row.addView(info,new LinearLayout.LayoutParams(0,dp(64),1));
        TextView fav=tv(isFav(c)?"★":"☆",28); fav.setTextColor(green); fav.setGravity(Gravity.CENTER); fav.setOnClickListener(v->{toggleFav(c); showContacts();});
        row.addView(fav,new LinearLayout.LayoutParams(dp(45),dp(55)));
        row.setOnClickListener(v->showContact(c)); list.addView(row); View line=new View(this); line.setBackgroundColor(0xFFE5E5EA); list.addView(line,new LinearLayout.LayoutParams(-1,1));
    }

    void showContact(Contact c){
        shell("Contact");
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setGravity(Gravity.CENTER); box.setPadding(0,dp(15),0,dp(20));
        TextView av=tv(initials(c.name),42); av.setTextColor(Color.WHITE); av.setGravity(Gravity.CENTER); av.setBackgroundColor(Color.rgb(130,130,135));
        box.addView(av,new LinearLayout.LayoutParams(dp(100),dp(100)));
        TextView n=tv(c.name,27); n.setGravity(Gravity.CENTER); n.setTypeface(Typeface.DEFAULT,Typeface.BOLD); box.addView(n);
        TextView p=tv(c.phone,17); p.setGravity(Gravity.CENTER); p.setTextColor(muted); box.addView(p);
        content.addView(box);
        LinearLayout actions=new LinearLayout(this); actions.setGravity(Gravity.CENTER);
        Button call=key("📞\nCall"); call.setTextSize(16); call.setOnClickListener(v->{numberDisplay=tv(c.phone,1); callNumber();});
        Button msg=key("💬\nMessage"); msg.setTextSize(16); msg.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:"+Uri.encode(c.phone)))));
        Button f=key((isFav(c)?"★":"☆")+"\nFavorite"); f.setTextSize(16); f.setOnClickListener(v->{toggleFav(c);showContact(c);});
        actions.addView(call,new LinearLayout.LayoutParams(0,dp(78),1)); actions.addView(msg,new LinearLayout.LayoutParams(0,dp(78),1)); actions.addView(f,new LinearLayout.LayoutParams(0,dp(78),1)); content.addView(actions);
    }

    void showFavorites(){shell("Favorites"); if(favorites.isEmpty()){TextView e=tv("No favorites yet",18); e.setGravity(Gravity.CENTER); content.addView(e,new LinearLayout.LayoutParams(-1,-1));} else for(Contact c:favorites)addContactRow(content,c);}
    void showRecents(){shell("Recents"); if(recents.isEmpty()){TextView e=tv("No recent calls",18);e.setGravity(Gravity.CENTER);content.addView(e,new LinearLayout.LayoutParams(-1,-1));} else for(Recent r:recents){Contact c=find(r.number); String name=c==null?r.number:c.name; LinearLayout row=new LinearLayout(this);row.setPadding(dp(8),dp(10),dp(8),dp(10));row.setGravity(Gravity.CENTER_VERTICAL);TextView icon=tv(r.type.equals("OUTGOING")?"↗":"↙",22);icon.setTextColor(green);row.addView(icon,new LinearLayout.LayoutParams(dp(45),dp(50)));LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);TextView n=tv(name,17);n.setTypeface(Typeface.DEFAULT,Typeface.BOLD);TextView p=tv(r.number+"  •  "+r.time,13);p.setTextColor(muted);info.addView(n);info.addView(p);row.addView(info,new LinearLayout.LayoutParams(0,dp(60),1));TextView ph=tv("📞",22);ph.setOnClickListener(v->{numberDisplay=tv(r.number,1);callNumber();});row.addView(ph,new LinearLayout.LayoutParams(dp(45),dp(50)));content.addView(row);}}

    String initials(String n){String[] x=n.trim().split("\\s+");return x.length==1?x[0].substring(0,1).toUpperCase():(""+x[0].charAt(0)+x[x.length-1].charAt(0)).toUpperCase();}
    Contact find(String phone){for(Contact c:contacts)if(c.phone.replaceAll("\\D","").endsWith(phone.replaceAll("\\D","")))return c;return null;}
    boolean isFav(Contact c){for(Contact x:favorites)if(x.phone.equals(c.phone))return true;return false;}
    void toggleFav(Contact c){if(isFav(c)){for(int i=0;i<favorites.size();i++)if(favorites.get(i).phone.equals(c.phone)){favorites.remove(i);break;}}else favorites.add(c);}
    void loadContacts(){contacts.clear();Cursor c=getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER},null,null,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC");if(c!=null){while(c.moveToNext()){String n=c.getString(0),p=c.getString(1);if(n==null)n="Unknown";if(p==null)p="";contacts.add(new Contact(n,p));}c.close();}}
    void loadRecents(){recents.clear();if(checkSelfPermission(Manifest.permission.READ_CALL_LOG)!=PackageManager.PERMISSION_GRANTED)return;Cursor c=getContentResolver().query(CallLog.Calls.CONTENT_URI,new String[]{CallLog.Calls.NUMBER,CallLog.Calls.TYPE,CallLog.Calls.DATE},null,null,CallLog.Calls.DATE+" DESC");if(c!=null){int count=0;while(c.moveToNext()&&count++<50){String n=c.getString(0);int type=c.getInt(1);long date=c.getLong(2);String t=android.text.format.DateFormat.format("dd MMM, hh:mm a",new Date(date)).toString();recents.add(new Recent(n,type==CallLog.Calls.OUTGOING_TYPE?"OUTGOING":"INCOMING",t));}c.close();}}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==50){loadContacts();loadRecents();}if(r==51&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)callNumber();}
    static class Contact{String name,phone;Contact(String n,String p){name=n;phone=p;}}
    static class Recent{String number,type,time;Recent(String n,String t,String d){number=n;type=t;time=d;}}
}
