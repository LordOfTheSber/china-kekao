# TASK-021: Stroke data coverage check

Команда проверяет покрытие данных порядка черт для иероглифов уровней HSK 1-6,
проставляет флаг `hanzi.has_stroke_data` и печатает отчёт в лог.

## Подготовка

1. Убедитесь, что в локальной БД есть импортированные иероглифы.
2. Подготовьте папку с JSON-файлами stroke-data, где имя файла — сам иероглиф.

## Запуск

```bash
cd backend
SPRING_PROFILES_ACTIVE=stroke-coverage \
KEKAO_STROKE_DATA_DIR=/absolute/path/to/stroke-data \
./mvnw spring-boot:run
```

По умолчанию сканируется диапазон HSK `1..6`.

Дополнительные параметры:

- `KEKAO_STROKE_MIN_HSK` (по умолчанию `1`)
- `KEKAO_STROKE_MAX_HSK` (по умолчанию `6`)
- `KEKAO_STROKE_MAX_MISSING_IN_LOG` (по умолчанию `50`)

## Результат

- В таблице `hanzi` у всех записей HSK 1-6 обновляется `has_stroke_data`.
- В лог выводится summary: covered / missing / coverage %.
- Для отсутствующих символов выводится preview списка.
