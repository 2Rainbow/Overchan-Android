package nya.miku.wishmaster.ui.settings;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import nya.miku.wishmaster.R;
import nya.miku.wishmaster.api.interfaces.CancellableTask;
import nya.miku.wishmaster.common.Async;
import nya.miku.wishmaster.common.Logger;
import nya.miku.wishmaster.common.MainApplication;
import nya.miku.wishmaster.lib.org_json.JSONArray;
import nya.miku.wishmaster.lib.org_json.JSONObject;
import nya.miku.wishmaster.ui.Database;
import nya.miku.wishmaster.ui.presentation.Subscriptions;

public class SettingsImporter {
    private static final String TAG = "SettingsImporter";

    public static void Import(final File filename, final boolean overwrite, final Activity activity) {
        // TODO: implement merge import
        final CancellableTask task = new CancellableTask.BaseCancellableTask();
        final ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.setMessage(activity.getString(R.string.app_settings_importing));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                task.cancel();
            }
        });
        progressDialog.show();
        Async.runAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    Import();
                } catch (Exception e) {
                    showMessage(e.getMessage());
                    Logger.e(TAG, e);
                    return;
                }
                showMessage(activity.getString(R.string.app_settings_import_completed));
            }

            private void Import() throws Exception {
                FileInputStream inputStream = null;
                inputStream = new FileInputStream(filename);
                StringBuffer json_string = new StringBuffer("");
                byte[] buffer = new byte[1024];
                int d;
                while ((d = inputStream.read(buffer)) != -1) {
                    json_string.append(new String(buffer, 0, d));
                }
                inputStream.close();
                if (task.isCancelled()) throw new Exception("Interrupted");
                JSONObject json = new JSONObject(json_string.toString());
                // TODO Check version
                int version = json.getInt("version");
                JSONArray history = json.getJSONArray("history");
                for (int i = 0; i < history.length(); i++) {
                    MainApplication.getInstance().database.addHistory(new Database.HistoryEntry(history.getJSONObject(i)));
                }
                if (task.isCancelled()) throw new Exception("Interrupted");
                JSONArray favorites = json.getJSONArray("favorites");
                for (int i = 0; i < favorites.length(); i++) {
                    MainApplication.getInstance().database.addFavorite(new Database.FavoritesEntry(favorites.getJSONObject(i)));
                }
                if (task.isCancelled()) throw new Exception("Interrupted");
                JSONArray hidden = json.getJSONArray("hidden");
                for (int i = 0; i < hidden.length(); i++) {
                    MainApplication.getInstance().database.addHidden(new Database.HiddenEntry(hidden.getJSONObject(i)));
                }
                if (task.isCancelled()) throw new Exception("Interrupted");
                JSONArray subscriptions = json.getJSONArray("subscriptions");
                for (int i = 0; i < subscriptions.length(); i++) {
                    MainApplication.getInstance().subscriptions.addSubscription(new Subscriptions.SubscriptionEntry(subscriptions.getJSONObject(i)));
                }
                if (task.isCancelled()) throw new Exception("Interrupted");
                JSONObject preferences = json.getJSONObject("preferences");
                MainApplication.getInstance().settings.setSharedPreferences(preferences);
            }

            private void showMessage(final String message) {
                if (task.isCancelled()) return;
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (task.isCancelled()) return;
                        try {
                            progressDialog.dismiss();
                        } catch (Exception e) {
                            Logger.e(TAG, e);
                            return ;
                        }
                        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}