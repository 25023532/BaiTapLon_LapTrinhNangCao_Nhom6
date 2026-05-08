package org.example.exception;

/**
 * Ném ra khi có lỗi liên quan đến dữ liệu:
 * - Dữ liệu null hoặc rỗng
 * - Dữ liệu không hợp lệ
 * - Lỗi đọc/ghi dữ liệu
 */
public class DataException extends Exception {

    private String field;       // trường dữ liệu bị lỗi
    private Object invalidValue; // giá trị không hợp lệ

    public DataException(String message) {
        super(message);
    }

    public DataException(String message, String field) {
        super(message);
        this.field = field;
    }

    public DataException(String message, String field, Object invalidValue) {
        super(message);
        this.field = field;
        this.invalidValue = invalidValue;
    }

    public DataException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getField() {
        return field;
    }

    public Object getInvalidValue() {
        return invalidValue;
    }

    @Override
    public String toString() {
        return "DataException{" +
                "field='" + field + '\'' +
                ", invalidValue=" + invalidValue +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}