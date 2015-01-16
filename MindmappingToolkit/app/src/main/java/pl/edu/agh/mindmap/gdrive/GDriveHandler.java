package pl.edu.agh.mindmap.gdrive;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import pl.edu.agh.mindmap.ResultListener;

public class GDriveHandler {
    public static final String GOOGLE_DRIVE_PREF = "GoogleDrivePreferences";
    public static final String PREF_ACCOUNT_NAME = "accountName";


    private final Drive drive;
    private final SharedPreferences prefs;
    private GoogleAccountCredential credential;
    private int requestCodeForGooglePlayServices;
    private int requestCodeForAuthorization;
    private int requestCodeForAccountPicker;

    public GDriveHandler(Context applicationContext, int requestCodeForGooglePlayServices,
                         int requestCodeForAuthorization, int requestCodeForAccountPicker) {
        this.requestCodeForAccountPicker = requestCodeForAccountPicker;
        this.requestCodeForAuthorization = requestCodeForAuthorization;
        this.requestCodeForGooglePlayServices = requestCodeForGooglePlayServices;

        credential = GoogleAccountCredential.usingOAuth2(applicationContext, Collections.singleton(DriveScopes.DRIVE));
        prefs = applicationContext.getSharedPreferences(GOOGLE_DRIVE_PREF, Context.MODE_PRIVATE);
        credential.setSelectedAccountName(prefs.getString(PREF_ACCOUNT_NAME, null));
        drive = new Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                GsonFactory.getDefaultInstance(),
                credential
        ).setApplicationName("MindMapDroid").build();

    }

    public void chooseAccount(Activity activity) {
        if (checkGooglePlayServicesAvailable(activity)) {
            if (credential.getSelectedAccountName() == null) {
                activity.startActivityForResult(credential.newChooseAccountIntent(), requestCodeForAccountPicker);
            }
        }
    }

    public boolean hasAccountLinked() {
        return credential.getSelectedAccountName() != null;
    }

    private boolean checkGooglePlayServicesAvailable(final Activity activity) {
        final int connectionStatusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(activity, connectionStatusCode);
            return false;
        }
        return true;
    }

    private void showGooglePlayServicesAvailabilityErrorDialog(final Activity activity, final int connectionStatusCode) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(connectionStatusCode, activity, requestCodeForGooglePlayServices);
                dialog.show();
            }
        });
    }

    public void storePickedAccountData(Intent data) throws AccountNotPickedException {
        if (data == null || data.getExtras() == null)
            throw new AccountNotPickedException();
        String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
        if (accountName != null) {
            credential.setSelectedAccountName(accountName);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PREF_ACCOUNT_NAME, accountName);
            editor.apply();
        }
    }

    public void fetchFileMetadata(String fileId, boolean listFolder, Activity activity, ResultListener<GDriveFile, IOException> resultListener) {
        new FetchFileInfoTask(fileId, listFolder, activity, resultListener).execute();
    }

    public void downloadFile(GDriveFile file, Activity activity, ResultListener<InputStream, IOException> resultListener) {
        new DownloadFileTask(file, activity, resultListener).execute();
    }

    public void uploadFile(String fileName, String mimeType, GDriveFile dir, InputStream stream, Activity activity, ResultListener<GDriveFile, Exception> resultListener) {
        new UploadFileTask(fileName, mimeType, dir, stream, activity, resultListener);
    }

    public void overwriteFile(GDriveFile file, InputStream stream, Activity activity, ResultListener<GDriveFile, IOException> resultListener) {
        new OverwriteFileTask(file, stream, activity, resultListener).execute();
    }

    public void rename(String newName, GDriveFile file, final Activity activity, ResultListener<GDriveFile, IOException> resultListener){
        new RenameFileTask(newName, file, activity, resultListener).execute();
    }

    public static class AccountNotPickedException extends Exception {
    }

    public void setRequestCodeForGooglePlayServices(int requestCodeForGooglePlayServices) {
        this.requestCodeForGooglePlayServices = requestCodeForGooglePlayServices;
    }

    public void setRequestCodeForAuthorization(int requestCodeForAuthorization) {
        this.requestCodeForAuthorization = requestCodeForAuthorization;
    }

    public void setRequestCodeForAccountPicker(int requestCodeForAccountPicker) {
        this.requestCodeForAccountPicker = requestCodeForAccountPicker;
    }

    public int getRequestCodeForGooglePlayServices() {
        return requestCodeForGooglePlayServices;
    }

    public int getRequestCodeForAuthorization() {
        return requestCodeForAuthorization;
    }

    public int getRequestCodeForAccountPicker() {
        return requestCodeForAccountPicker;
    }

    public abstract class DriveAsyncTask extends AsyncTask<Void, Void, Boolean> {
        private Activity activity;
        private IOException unrecoverableException;

        public DriveAsyncTask(Activity activity) {
            this.activity = activity;
        }

        @Override
        protected Boolean doInBackground(Void... ignored) {
            try {
                doInBackground();
                return true;
            } catch (GooglePlayServicesAvailabilityIOException e) {
                showGooglePlayServicesAvailabilityErrorDialog(activity, e.getConnectionStatusCode());
            } catch (UserRecoverableAuthIOException e) {
                activity.startActivityForResult(e.getIntent(), requestCodeForAuthorization);
            } catch (IOException e) {
                unrecoverableException = e;
            }
            return false;
        }

        protected IOException getUnrecoverableException() {
            return unrecoverableException;
        }

        abstract protected void doInBackground() throws IOException;
    }

    class FetchFileInfoTask extends DriveAsyncTask {
        private String fileId;
        private boolean listFolder;
        private GDriveFile result;
        private ResultListener<GDriveFile, IOException> listener;

        public FetchFileInfoTask(String fileId, boolean listFolder, Activity activity, ResultListener<GDriveFile, IOException> resultListener) {
            super(activity);
            this.listener = resultListener;
            this.fileId = fileId;
            this.listFolder = listFolder;
        }

        public FetchFileInfoTask setParent(GDriveFile parent) {
            return this;
        }

        @Override
        protected void doInBackground() throws IOException {
            Drive.Files.Get get = drive.files().get(fileId);
            File file = get.execute();
            List<GDriveFile> content = null;
            if (listFolder && Utils.isFolder(file)) {
                Drive.Files.List list = drive.files().list();
                list.setQ(String.format("'%s' in parents and trashed = false", file.getId()));
                FileList fileList = list.execute();
                content = new ArrayList<>();
                for (File f : fileList.getItems()) {
                    content.add(new GDriveFile(f, file.getId(), null));
                }
            }
            result = new GDriveFile(file, null, content);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if (success)
                listener.taskDone(result);
            else
                listener.taskFailed(getUnrecoverableException());
        }
    }

    class DownloadFileTask extends DriveAsyncTask {
        private GDriveFile file;
        private ResultListener<InputStream, IOException> resultListener;
        private InputStream result;

        public DownloadFileTask(GDriveFile file, Activity activity, ResultListener<InputStream, IOException> resultListener) {
            super(activity);
            this.resultListener = resultListener;
            this.file = file;
        }

        @Override
        protected void doInBackground() throws IOException {
            if (file.getDownloadUrl() == null || file.getDownloadUrl().isEmpty()) {
                throw new IOException("File is empty");
            }
            HttpResponse resp = drive.getRequestFactory().buildGetRequest(new GenericUrl(file.getDownloadUrl())).execute();
            result = resp.getContent();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if (success)
                resultListener.taskDone(result);
            else
                resultListener.taskFailed(getUnrecoverableException());
        }
    }


    private class UploadFileTask extends DriveAsyncTask {
        private String fileName;
        private GDriveFile dir;
        private InputStream stream;
        private String mimeType;
        private ResultListener<GDriveFile, Exception> resultListener;
        private GDriveFile result;

        public UploadFileTask(String fileName, String mimeType, GDriveFile dir, InputStream stream, Activity activity, ResultListener<GDriveFile, Exception> resultListener) {
            super(activity);
            this.fileName = fileName;
            this.dir = dir;
            this.stream = stream;
            this.resultListener = resultListener;
            this.mimeType = mimeType;
        }

        @Override
        protected void doInBackground() throws IOException {
            File body = new File();
            body.setTitle(fileName);

            if (dir != null)
                body.setParents(
                        Arrays.asList(new ParentReference().setId(dir.getId()))
                );
            File file;
            if (stream != null) {
                InputStreamContent content = new InputStreamContent(mimeType, stream);
                file = drive.files().insert(body, content).execute();
            } else {
                file = drive.files().insert(body).execute();
            }
            result = new GDriveFile(file, null, null);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if (success) {
                resultListener.taskDone(result);
            } else {
                resultListener.taskFailed(getUnrecoverableException());
            }
        }
    }

    private class OverwriteFileTask extends DriveAsyncTask {
        private GDriveFile file;
        private InputStream stream;
        private ResultListener<GDriveFile, IOException> resultListener;
        private GDriveFile result;

        public OverwriteFileTask(GDriveFile file, InputStream stream, Activity activity, ResultListener<GDriveFile, IOException> resultListener) {
            super(activity);
            this.file = file;
            this.stream = stream;
            this.resultListener = resultListener;
        }

        @Override
        protected void doInBackground() throws IOException {
            File body = new File();
            body.setTitle(file.getName());
            if (file.getParentId() != null)
                body.setParents(
                        Arrays.asList(new ParentReference().setId(file.getParentId()))
                );
            InputStreamContent content = new InputStreamContent(file.getMimeType(), stream);
            result = new GDriveFile(drive.files().update(file.getId(), body, content).execute(), null, null);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if (success) {
                resultListener.taskDone(result);
            } else {
                resultListener.taskFailed(getUnrecoverableException());
            }
        }
    }

    private class RenameFileTask extends DriveAsyncTask{
        private String newName;
        private GDriveFile file;
        private GDriveFile result;
        private ResultListener<GDriveFile, IOException> resultListener;

        private RenameFileTask(String newName, GDriveFile file, Activity activity, ResultListener<GDriveFile, IOException> resultListener) {
            super(activity);
            this.newName = newName;
            this.file = file;
            this.resultListener = resultListener;
        }

        @Override
        protected void doInBackground() throws IOException {
            File body = new File();
            body.setTitle(newName);
            if (file.getParentId() != null)
                body.setParents(
                        Arrays.asList(new ParentReference().setId(file.getParentId()))
                );
            result = new GDriveFile(drive.files().update(file.getId(), body).execute(), null, null);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if(success){
                resultListener.taskDone(result);
            }else
                resultListener.taskFailed(getUnrecoverableException());
        }
    }
}
