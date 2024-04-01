package slayer.accessibility.service.flutter_accessibility_service;

public class AccessibilityNodeInfoData {
    private String className;
    private String text;
    // 可以添加更多你需要的字段

    // 构造器
    public AccessibilityNodeInfoData(String className, String text) {
        this.className = className;
        this.text = text;
    }

    // Getters and Setters
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

}
