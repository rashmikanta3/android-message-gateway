package com.example.messagegateway;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

public class WhatsAppClickerService extends AccessibilityService {
    private static final String TAG = "WhatsAppClicker";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isClicking = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        CharSequence pkg = event.getPackageName();
        if (pkg == null) return;

        String pkgName = pkg.toString();
        if (!pkgName.equals("com.whatsapp") && !pkgName.equals("com.whatsapp.w4b")) {
            return;
        }

        if (isClicking) return;

        isClicking = true;
        // Wait 1.2s to ensure WhatsApp completes loading the text draft and renders the send button
        handler.postDelayed(() -> {
            try {
                AccessibilityNodeInfo root = getRootInActiveWindow();
                if (root != null) {
                    boolean clicked = tryClickSendButton(root);
                    if (!clicked) {
                        // Fallback recursive search through entire view tree
                        searchAndClick(root);
                    }
                    root.recycle();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error clicking send", e);
            } finally {
                isClicking = false;
            }
        }, 1200);
    }

    private boolean tryClickSendButton(AccessibilityNodeInfo root) {
        // 1. Check known view IDs
        String[] viewIds = {
            "com.whatsapp:id/send",
            "com.whatsapp.w4b:id/send",
            "com.whatsapp:id/send_btn",
            "com.whatsapp.w4b:id/send_btn"
        };

        for (String id : viewIds) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(id);
            if (nodes != null) {
                for (AccessibilityNodeInfo node : nodes) {
                    if (clickNodeOrParent(node)) {
                        Log.d(TAG, "Clicked via ID: " + id);
                        return true;
                    }
                }
            }
        }

        // 2. Check by text or common descriptions
        String[] targets = {"Send", "send"};
        for (String target : targets) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(target);
            if (nodes != null) {
                for (AccessibilityNodeInfo node : nodes) {
                    if (clickNodeOrParent(node)) {
                        Log.d(TAG, "Clicked via text: " + target);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean searchAndClick(AccessibilityNodeInfo node) {
        if (node == null) return false;

        CharSequence desc = node.getContentDescription();
        if (desc != null) {
            String d = desc.toString().trim().toLowerCase();
            if (d.contains("send") || d.contains("भेजें")) {
                if (clickNodeOrParent(node)) {
                    Log.d(TAG, "Clicked via content-description: " + desc);
                    return true;
                }
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (searchAndClick(child)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean clickNodeOrParent(AccessibilityNodeInfo node) {
        if (node == null) return false;
        if (node.isClickable() && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true;
        }
        AccessibilityNodeInfo parent = node.getParent();
        if (parent != null) {
            if (parent.isClickable() && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true;
            }
            AccessibilityNodeInfo grandParent = parent.getParent();
            if (grandParent != null && grandParent.isClickable()) {
                return grandParent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
        return false;
    }

    @Override
    public void onInterrupt() {}
}
