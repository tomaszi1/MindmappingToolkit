package pl.edu.agh.mindmap.gdrive;


import android.app.Activity;

import java.io.IOException;

import pl.edu.agh.mindmap.ResultListener;

public class GDriveBrowser {
    private final GDriveHandler handler;
    private String rootId;
    private GDriveFile currentDir;

    public GDriveBrowser(GDriveHandler handler) {
        this.handler = handler;
    }

    public boolean isInRootDir() {
        return rootId != null && rootId.equals(currentDir.getId());
    }

    public void goToRootDir(Activity activity, final ResultListener<GDriveFile, IOException> resultListener) {
        ResultListener<GDriveFile, IOException> listener = new ResultListener<GDriveFile, IOException>() {
            @Override
            public void taskDone(GDriveFile result) {
                currentDir = result;
                resultListener.taskDone(result);
                rootId = result.getId();
            }

            @Override
            public void taskFailed(IOException exception) {
                resultListener.taskFailed(exception);
            }
        };
        handler.fetchFileMetadata("root", true, activity, listener);
    }

    public void changeDir(final GDriveFile dir, Activity activity, final ResultListener<GDriveFile, IOException> resultListener) {
        if (dir == null || !dir.isFolder())
            throw new IllegalArgumentException("Not a directory");
        ResultListener<GDriveFile, IOException> listener = new ResultListener<GDriveFile, IOException>() {
            @Override
            public void taskDone(GDriveFile result) {
                currentDir = result;
                resultListener.taskDone(result);
            }

            @Override
            public void taskFailed(IOException exception) {
                resultListener.taskFailed(exception);
            }
        };
        handler.fetchFileMetadata(dir.getId(), true, activity, listener);
    }

    public void goToParentDir(Activity activity, ResultListener<GDriveFile, IOException> resultListener) {
        if (currentDir == null || currentDir.getParentId() == null)
            throw new IllegalStateException("Folder has no parent");
        GDriveFile parent = new GDriveFile(currentDir.getParentId(), true);
        changeDir(parent, activity, resultListener);
    }

    public void createFolder(String name, final Activity activity, ResultListener<GDriveFile, Exception> resultListener) {
        handler.uploadFile(name, Utils.FOLDER_MIME_TYPE, currentDir, null, activity, resultListener);
    }
}
