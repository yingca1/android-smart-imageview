package me.caiying.asiv;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.util.Log;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Logger {
    public static final boolean DEBUG_MEMO_CACHE = true;
    public static final boolean DEBUG_DISK_CACHE = true;
    public static final String DEBUG_MEMO_CACHE_TAG = "memo-cache";
    public static final String DEBUG_MEMO_INFO_TAG = "memo-cache";

    public static void d(String tag, String message) {
        if(tag.equals(DEBUG_MEMO_CACHE_TAG) && !DEBUG_MEMO_CACHE)
            return;

        android.util.Log.d(tag, message);
    }

    public static void memoInfo(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        Log.i(DEBUG_MEMO_INFO_TAG, " memoryInfo.availMem " + memoryInfo.availMem + "\n" );
        Log.i(DEBUG_MEMO_INFO_TAG, " memoryInfo.lowMemory " + memoryInfo.lowMemory + "\n" );
        Log.i(DEBUG_MEMO_INFO_TAG, " memoryInfo.threshold " + memoryInfo.threshold + "\n" );

//        List<RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
//
//        Map<Integer, String> pidMap = new TreeMap<Integer, String>();
//        for (RunningAppProcessInfo runningAppProcessInfo : runningAppProcesses) {
//            pidMap.put(runningAppProcessInfo.pid, runningAppProcessInfo.processName);
//        }
//
//        Collection<Integer> keys = pidMap.keySet();
//        for(int key : keys) {
//            int pids[] = new int[1];
//            pids[0] = key;
//            android.os.Debug.MemoryInfo[] memoryInfoArray = activityManager.getProcessMemoryInfo(pids);
//            for(android.os.Debug.MemoryInfo pidMemoryInfo: memoryInfoArray) {
//                Log.i(DEBUG_MEMO_INFO_TAG, String.format("** MEMINFO in pid %d [%s] **\n",pids[0],pidMap.get(pids[0])));
//                Log.i(DEBUG_MEMO_INFO_TAG, " pidMemoryInfo.getTotalPrivateDirty(): " + pidMemoryInfo.getTotalPrivateDirty() + "\n");
//                Log.i(DEBUG_MEMO_INFO_TAG, " pidMemoryInfo.getTotalPss(): " + pidMemoryInfo.getTotalPss() + "\n");
//                Log.i(DEBUG_MEMO_INFO_TAG, " pidMemoryInfo.getTotalSharedDirty(): " + pidMemoryInfo.getTotalSharedDirty() + "\n");
//            }
//        }
    }
}
