package pl.edu.agh.mindmap.gdrive;

import android.app.Activity;
import android.content.res.Resources;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.drive.model.File;

public class Utils {
    public static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";

    public static void logAndShow(Activity activity, String tag, Throwable t) {
        Log.e(tag, "Error", t);
        String message = t.getMessage();
        if (t instanceof GoogleJsonResponseException) {
            GoogleJsonError details = ((GoogleJsonResponseException) t).getDetails();
            if (details != null) {
                message = details.getMessage();
            }
        } else if (t.getCause() instanceof GoogleAuthException) {
            message = ((GoogleAuthException) t.getCause()).getMessage();
        }
        showError(activity, message);
    }

    public static void showError(Activity activity, String message) {
        String errorMessage = getErrorMessage(activity, message);
        showErrorInternal(activity, errorMessage);
    }

    private static void showErrorInternal(final Activity activity, final String errorMessage) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private static String getErrorMessage(Activity activity, String message) {
        Resources resources = activity.getResources();
        if (message == null) {
            return "Unknown error occured";
        }
        return "[Error] " + message;
    }

    public static boolean isFolder(File file){
        return file.getMimeType().equals(FOLDER_MIME_TYPE);
    }
}
