package org.example.model;

/**
 * Унифицированный контейнер ответа API.
 *
 * <p>Используется для передачи результатов выполнения операций
 * (например, авторизации, регистрации, обработки команд) в
 * стандартизированном виде.</p>
 *
 * <p>Ответ может находиться в одном из двух состояний:</p>
 * <ul>
 *     <li><b>success = true</b> — операция выполнена успешно, данные находятся в поле {@code data};</li>
 *     <li><b>success = false</b> — произошла ошибка, информация об ошибке находится в поле {@code error}.</li>
 * </ul>
 *
 * <p>Класс является обобщённым (generic) и может содержать любые типы данных,
 * необходимые для ответа.</p>
 *
 * @param <T> тип полезной нагрузки успешного ответа
 */
public class ApiResponse<T> {

    /**
     * Флаг успешности операции.
     * {@code true} — операция выполнена успешно,
     * {@code false} — произошла ошибка.
     */
    private final boolean success;

    /**
     * Полезная нагрузка ответа.
     * Заполняется только в случае успешного выполнения операции.
     */
    private final T data;

    /**
     * Информация об ошибке.
     * Заполняется только в случае неуспешного выполнения операции.
     */
    private final ErrorResponse error;

    /**
     * Приватный конструктор.
     * Используется фабричными методами {@link #ok(Object)} и {@link #fail(String, String)}.
     *
     * @param success флаг успешности
     * @param data    данные ответа
     * @param error   объект ошибки
     */
    private ApiResponse(boolean success, T data, ErrorResponse error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    /**
     * Создаёт успешный ответ API.
     *
     * @param data данные, возвращаемые клиенту
     * @param <T>  тип данных
     * @return успешный {@link ApiResponse} с заполненным полем {@code data}
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /**
     * Создаёт ответ API с ошибкой.
     *
     * @param code    машинно-читаемый код ошибки
     * @param message человеко-читаемое описание ошибки
     * @param <T>     тип данных (не используется при ошибке)
     * @return {@link ApiResponse} с заполненным полем {@code error}
     */
    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorResponse(code, message));
    }

    /**
     * Возвращает флаг успешности операции.
     *
     * @return {@code true}, если операция выполнена успешно
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Возвращает данные успешного ответа.
     *
     * @return данные или {@code null}, если операция завершилась ошибкой
     */
    public T getData() {
        return data;
    }

    /**
     * Возвращает информацию об ошибке.
     *
     * @return объект {@link ErrorResponse} или {@code null}, если операция успешна
     */
    public ErrorResponse getError() {
        return error;
    }
}