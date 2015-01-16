package pl.edu.agh.mindmap;


import org.xmind.core.Core;
import org.xmind.core.CoreException;
import org.xmind.core.IWorkbook;
import org.xmind.core.io.ByteArrayStorage;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;

public class Utils {
    public static IWorkbook cloneWorkbook(IWorkbook workbook) throws IOException, CoreException {
        if (workbook.getTempStorage() == null)
            workbook.setTempStorage(new ByteArrayStorage());
        workbook.saveTemp();
        return Core.getWorkbookBuilder().loadFromStorage(workbook.getTempStorage());
    }

    public static boolean safeEquals(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null;
        }
        return o1.equals(o2);
    }


    public static void checkNotNull(Object... objs) {
        for (Object o : objs) {
            if (o == null)
                throw new IllegalArgumentException("Null passed");
        }
    }

    public static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null)
                closeable.close();
        } catch (IOException e) {
            // silent
        }
    }
}
