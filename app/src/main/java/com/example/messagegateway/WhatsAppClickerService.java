package com.example.messagegateway;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

public class WhatsAppClickerService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        List<AccessibilityNodeInfo> sendById = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send");
        if (sendById != null && !sendById.isEmpty()) {
            sendById.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            return;
        }

        List<AccessibilityNodeInfo> sendByText = rootNode.findAccessibilityNodeInfosByText("Send");
        if (sendByText != null) {
            for (AccessibilityNodeInfo node : sendByText) {
                if (node.isClickable()) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    break;
                }
            }
        }
    }

    @Override
    public void onInterrupt() {}
}