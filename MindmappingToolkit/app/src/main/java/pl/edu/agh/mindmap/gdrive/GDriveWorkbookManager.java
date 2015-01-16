package pl.edu.agh.mindmap.gdrive;

import android.app.Activity;
import android.os.AsyncTask;

import org.xmind.core.Core;
import org.xmind.core.CoreException;
import org.xmind.core.IWorkbook;
import org.xmind.core.io.ByteArrayStorage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import pl.edu.agh.mindmap.ResultListener;

public class GDriveWorkbookManager {
    public static final String WORKBOOK_MIME_TYPE = "application/x-zip";
    private final GDriveHandler handler;
    private final IWorkbook workbook;
    private GDriveFile file;

    GDriveWorkbookManager(IWorkbook workbook, GDriveHandler handler, GDriveFile file) {
        this.workbook = workbook;
        this.handler = handler;
        this.file = file;
    }

    public static void downloadWorkbook(final GDriveFile file, Activity activity, final GDriveHandler handler,  final ResultListener<GDriveWorkbookManager, Exception> resultListener){
        handler.downloadFile(file, activity, new ResultListener<InputStream, IOException>() {
            @Override
            public void taskDone(InputStream result) {
                new WorkbookLoader(result, file, resultListener, handler).execute();
            }

            @Override
            public void taskFailed(IOException exception) {
                resultListener.taskFailed(exception);
            }
        });
    }

    public static void uploadWorkbook(final IWorkbook workbook, final String fileName,final GDriveFile dir, final Activity activity, final GDriveHandler handler, final ResultListener<GDriveWorkbookManager, Exception> resultListener){
        new WorkbookSaver(workbook, new ResultListener<InputStream, Exception>() {
            @Override
            public void taskDone(InputStream result) {
                handler.uploadFile(fileName, WORKBOOK_MIME_TYPE, dir, result, activity, new ResultListener<GDriveFile, Exception>() {
                    @Override
                    public void taskDone(GDriveFile result) {
                        resultListener.taskDone(new GDriveWorkbookManager(workbook, handler, result));
                    }

                    @Override
                    public void taskFailed(Exception exception) {
                        resultListener.taskFailed(exception);
                    }
                });
            }

            @Override
            public void taskFailed(Exception exception) {
                resultListener.taskFailed(exception);
            }
        }).execute();
    }

    public void uploadWithOverwrite(final Activity activity, final ResultListener<Void, Exception> resultListener){
        new WorkbookSaver(workbook, new ResultListener<InputStream, Exception>() {
            @Override
            public void taskDone(final InputStream result) {
                handler.overwriteFile(file, result, activity, new ResultListener<GDriveFile, IOException>() {
                    @Override
                    public void taskDone(GDriveFile result) {
                        file = result;
                    }

                    @Override
                    public void taskFailed(IOException exception) {
                        resultListener.taskFailed(exception);
                    }
                });
            }

            @Override
            public void taskFailed(Exception exception) {
                resultListener.taskFailed(exception);
            }
        });
    }

    public void fileHasNewVersion(final Activity activity, final ResultListener<Boolean, IOException> resultListener){
        handler.fetchFileMetadata(file.getId(), false, activity, new ResultListener<GDriveFile, IOException>() {
            @Override
            public void taskDone(GDriveFile result) {
                resultListener.taskDone(file.getRevision().equals(result.getRevision()));
            }

            @Override
            public void taskFailed(IOException exception) {
                resultListener.taskFailed(exception);
            }
        });
    }

    public void downloadNewVersion(final Activity activity, final ResultListener<GDriveWorkbookManager, Exception> resultListener){
        handler.downloadFile(file, activity, new ResultListener<InputStream, IOException>() {
            @Override
            public void taskDone(InputStream result) {
                new WorkbookLoader(result, file, resultListener, handler).execute();
            }

            @Override
            public void taskFailed(IOException exception) {
                resultListener.taskFailed(exception);
            }
        });
    }

    public GDriveFile getFile() {
        return file;
    }

    public IWorkbook getWorkbook() {
        return workbook;
    }

    private static class WorkbookLoader extends AsyncTask<Void, Void, GDriveWorkbookManager>{

        private ResultListener<GDriveWorkbookManager, Exception> resultListener;
        private InputStream stream;
        private Exception exception;
        private GDriveFile file;
        private GDriveHandler handler;

        public WorkbookLoader(InputStream stream, GDriveFile file, ResultListener<GDriveWorkbookManager, Exception> resultListener, GDriveHandler handler) {
            this.resultListener = resultListener;
            this.stream = stream;
            this.file = file;
            this.handler = handler;
        }

        @Override
        protected GDriveWorkbookManager doInBackground(Void... ignored) {
            try {
                IWorkbook workbook = Core.getWorkbookBuilder().loadFromStream(stream, new ByteArrayStorage());
                return new GDriveWorkbookManager(workbook, handler, file);
            } catch (IOException | CoreException e) {
                exception = e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(GDriveWorkbookManager result) {
            super.onPostExecute(result);
            if(result!=null){
                resultListener.taskDone(result);
            }else{
                resultListener.taskFailed(exception);
            }
        }
    }

    private static class WorkbookSaver extends AsyncTask<Void, Void, InputStream> {
        private IWorkbook workbook;
        private ResultListener<InputStream, Exception> resultListener;
        private Exception exception;

        private WorkbookSaver(IWorkbook workbook, ResultListener<InputStream, Exception> resultListener) {
            this.workbook = workbook;
            this.resultListener = resultListener;
        }

        @Override
        protected InputStream doInBackground(Void... params) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try {
                workbook.save(os);
                byte[] bytes = os.toByteArray();
                return new ByteArrayInputStream(bytes);
            } catch (IOException | CoreException e) {
                exception = e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(InputStream stream) {
            super.onPostExecute(stream);
            if(stream!=null)
                resultListener.taskDone(stream);
            else
                resultListener.taskFailed(exception);
        }
    }


}
