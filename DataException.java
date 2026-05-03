package org.example.exception;

/**
 * Ném khi dữ liệu đầu vào không hợp lệ:
 * - Null / blank field
 * - Định dạng sai
 * - Vi phạm ràng buộc dữ liệu
 */
public class DataException extends RuntimeException {

    private final String fieldName;

    public DataException(String fieldName, String message) {
        super("Lỗi dữ liệu tại field '%s': %s".formatted(fieldName, message));
        this.fieldName = fieldName;
    }

    public DataException(String message) {
        super(message);
        this.fieldName = "unknown";
    }

    public String getFieldName() { return fieldName; }
}