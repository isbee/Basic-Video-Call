package io.agora.openvcall.ui;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;

import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import io.agora.openvcall.R;
import io.agora.openvcall.model.ConstantApp;

public class MainActivity extends BaseActivity {

    private final static Logger log = LoggerFactory.getLogger(MainActivity.class);

    static final String uri = "https://facechatoverlay.com";  // manifest의 intent filter에 정의한 host와 동일
    static final String uriPrefix = "https://facechatoverlay.page.link";  // firebase에서 제공하는 무료 도메인 사용

    private TextView appLinkView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            ab.setCustomView(R.layout.ard_agora_actionbar);
        }
    }

    @Override
    protected void initUIandEvent() {
        EditText v_channel = (EditText) findViewById(R.id.channel_name);
        v_channel.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                boolean isEmpty = TextUtils.isEmpty(s.toString());
                findViewById(R.id.button_join).setEnabled(!isEmpty);
                findViewById(R.id.button_invite).setEnabled(!isEmpty);
            }
        });

        Spinner encryptionSpinner = (Spinner) findViewById(R.id.encryption_mode);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.encryption_mode_values, R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        encryptionSpinner.setAdapter(adapter);

        encryptionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                vSettings().mEncryptionModeIndex = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        encryptionSpinner.setSelection(vSettings().mEncryptionModeIndex);

        String lastChannelName = vSettings().mChannelName;
        if (!TextUtils.isEmpty(lastChannelName)) {
            v_channel.setText(lastChannelName);
            v_channel.setSelection(lastChannelName.length());
        }

        EditText v_encryption_key = (EditText) findViewById(R.id.encryption_key);
        String lastEncryptionKey = vSettings().mEncryptionKey;
        if (!TextUtils.isEmpty(lastEncryptionKey)) {
            v_encryption_key.setText(lastEncryptionKey);
        }

        appLinkView = (TextView) findViewById(R.id.text_app_link);
        appLinkView.setClickable(true);
        appLinkView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    protected void deInitUIandEvent() {
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_settings:
                forwardToSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onClickJoin(View view) {
        forwardToRoom();
    }

    public void onClickInvite(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setData(ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, 10);
//        setDynamicLinkAndForwardToRoom();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 10 && resultCode == RESULT_OK) {
            String result = data.getDataString();

            String id = "";
            String name = "";
            String phone = "";
            int idx;
            Cursor cursor = getContentResolver().query(Uri.parse(result), null, null, null, null);
            if (cursor.moveToFirst()) {
                idx = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                id = cursor.getString(idx);

                idx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                name = cursor.getString(idx);
            }

            // Build the Entity URI.
            Uri.Builder b = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, id).buildUpon();
            b.appendPath(ContactsContract.Contacts.Entity.CONTENT_DIRECTORY);
            Uri contactUri = b.build();

// Create the projection (SQL fields) and sort order.
            String[] projection = {
                    ContactsContract.Contacts.Entity.RAW_CONTACT_ID,
                    ContactsContract.Contacts.Entity.DATA1,
                    ContactsContract.Contacts.Entity.MIMETYPE };
            String sortOrder = ContactsContract.Contacts.Entity.RAW_CONTACT_ID + " ASC";
            cursor = getContentResolver().query(contactUri, projection, null, null, sortOrder);

            String mime;
            int mimeIdx = cursor.getColumnIndex(ContactsContract.Contacts.Entity.MIMETYPE);
            int dataIdx = cursor.getColumnIndex(ContactsContract.Contacts.Entity.DATA1);
            if (cursor.moveToFirst()) {
                do {
                    mime = cursor.getString(mimeIdx);
                    if (mime.equalsIgnoreCase(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
                        phone = cursor.getString(dataIdx);
                    }
                    // ...etc.
                } while (cursor.moveToNext());
            }

            appLinkView.setText(phone);

            SmsManager smsmanage = SmsManager.getDefault();
            smsmanage.sendTextMessage(phone, null, "hello!!", null, null);
        }
    }

    public void setDynamicLinkAndForwardToRoom() {
        EditText v_channel = (EditText) findViewById(R.id.channel_name);
        String channel = v_channel.getText().toString();
        vSettings().mChannelName = channel;

        DynamicLink dynamicLink = buildDynamicLink(uri, uriPrefix);

        Uri dynamicLinkUriWithChannelId = getDynamicLinkUriWithChannelId(dynamicLink, channel);
        appLinkView.setText(Html.fromHtml(dynamicLinkUriWithChannelId.toString()));

        EditText v_encryption_key = (EditText) findViewById(R.id.encryption_key);
        String encryption = v_encryption_key.getText().toString();
        vSettings().mEncryptionKey = encryption;

        Intent i = new Intent(MainActivity.this, CallActivity.class);
        i.putExtra(ConstantApp.ACTION_KEY_CHANNEL_NAME, channel);
        i.putExtra(ConstantApp.ACTION_KEY_ENCRYPTION_KEY, encryption);
        i.putExtra(ConstantApp.ACTION_KEY_ENCRYPTION_MODE, getResources().getStringArray(R.array.encryption_mode_values)[vSettings().mEncryptionModeIndex]);

        startActivity(i);
    }

    public void forwardToRoom() {
        EditText v_channel = (EditText) findViewById(R.id.channel_name);
        String channel = v_channel.getText().toString();
        vSettings().mChannelName = channel;

        EditText v_encryption_key = (EditText) findViewById(R.id.encryption_key);
        String encryption = v_encryption_key.getText().toString();
        vSettings().mEncryptionKey = encryption;

        Intent i = new Intent(MainActivity.this, CallActivity.class);
        i.putExtra(ConstantApp.ACTION_KEY_CHANNEL_NAME, channel);
        i.putExtra(ConstantApp.ACTION_KEY_ENCRYPTION_KEY, encryption);
        i.putExtra(ConstantApp.ACTION_KEY_ENCRYPTION_MODE, getResources().getStringArray(R.array.encryption_mode_values)[vSettings().mEncryptionModeIndex]);

        startActivity(i);
    }

    public void forwardToSettings() {
        Intent i = new Intent(this, SettingsActivity.class);
        startActivity(i);
    }

    public void onClickDoNetworkTest(View view) {
        Intent i = new Intent(MainActivity.this, NetworkTestActivity.class);
        startActivity(i);
    }

    @Override
    public void permissionGranted() {

    }

    private DynamicLink buildDynamicLink(String uri, String uriPrefix) {
        DynamicLink dynamicLink = FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLink(Uri.parse(uri))
                .setDomainUriPrefix(uriPrefix)
                // Open links with this app on Android
                .setAndroidParameters(new DynamicLink.AndroidParameters.Builder().build())
                // Open links with com.example.ios on iOS
//                .setIosParameters(new DynamicLink.IosParameters.Builder("com.example.ios").build())
                .buildDynamicLink();
        return dynamicLink;
    }

    private Uri getDynamicLinkUriWithChannelId(DynamicLink dynamicLink, String channelId) {
        Uri.Builder uriBuilder = dynamicLink.getUri().buildUpon();
        uriBuilder.appendQueryParameter("channelid", channelId);
        Uri dynamicLinkUriWithChannelId = uriBuilder.build();

        return dynamicLinkUriWithChannelId;
    }
}
