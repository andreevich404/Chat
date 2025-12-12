package org.example.repository;

/**
 * Кастомное исключение уровня доступа к данным (Database Layer).
 *
 * <p>Используется для инкапсуляции ошибок, возникающих при работе с базой данных,
 * таких как {@link java.sql.SQLException}, и передачи их на уровень сервисов
 * в виде unchecked-исключения.</p>
 *
 * <p>Основные цели использования:</p>
 * <ul>
 *     <li>изолировать JDBC-исключения от бизнес-логики;</li>
 *     <li>избежать пробрасывания {@code SQLException} по всему коду;</li>
 *     <li>обеспечить единый тип исключений для слоя репозиториев.</li>
 * </ul>
 *
 * <p>Логирование таких исключений должно выполняться в месте их возникновения
 * (например, в JDBC-репозитории), чтобы исключение логировалось ровно один раз.</p>
 */
public class DatabaseException extends RuntimeException {

    /**
     * Создаёт новое исключение базы данных.
     *
     * @param message описание ошибки
     * @param cause   исходное исключение (например, {@link java.sql.SQLException})
     */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}