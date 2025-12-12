package org.example.model;

/**
 * DTO-класс, описывающий информацию об ошибке.
 *
 * <p>Используется в составе {@link ApiResponse} для передачи
 * структурированных ошибок клиенту.</p>
 *
 * <p>Ошибка представляется в виде пары:</p>
 * <ul>
 *     <li><b>code</b> — машинно-читаемый код ошибки (используется клиентом для обработки);</li>
 *     <li><b>message</b> — человеко-читаемое описание ошибки.</li>
 * </ul>
 *
 * <p>Класс не содержит логики и используется исключительно
 * как контейнер данных.</p>
 */
public class ErrorResponse {

    /**
     * Машинно-читаемый код ошибки.
     */
    private final String code;

    /**
     * Человеко-читаемое описание ошибки.
     */
    private final String message;

    /**
     * Создаёт объект ошибки.
     *
     * @param code    код ошибки
     * @param message описание ошибки
     */
    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * Возвращает код ошибки.
     *
     * @return код ошибки
     */
    public String getCode() {
        return code;
    }

    /**
     * Возвращает описание ошибки.
     *
     * @return сообщение об ошибке
     */
    public String getMessage() {
        return message;
    }
}