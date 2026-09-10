# Explore With Me

## Структура проекта

Многомодульный Maven-проект:

```
.
├── stat-service
│   ├── stat-server   # HTTP-сервис статистики
│   ├── stat-client   # HTTP-клиент для работы со stats-service
│   └── stat-dto      # общие DTO для сервера и клиента
├── main-service      # основной сервис
└── docker-compose.yml
```

### Модули

- **stat-server**  
  REST API для сохранения и получения статистики

- **stat-client**  
  HTTP-клиент для взаимодействия с сервисом статистики

- **stat-dto**  
  Общие классы запросов/ответов (`HitDto`, `StatDto`)

- **main-service**  
  Основной сервис


## Запуск через Docker

```bash
docker-compose up
```

Поднимаются:

- `stats-server` — порт 9090
- `stats-db` (PostgreSQL - БД сервиса статистики)
- `ewm-service` — порт 8080 (основной сервис)
- `ewm-db` (PostgreSQL - БД основного сервиса)

## Locations Feature (Этап 3)

## Общее описание

Добавлена новая функциональность `Locations`, позволяющая:

- хранить справочник мест;
- привязывать место к событию для точного поиска событий по местам проведения;
- выполнять поиск событий по месту;
- выполнять поиск событий в радиусе от места.

---

## Справочник мест Place

Сущность называется Place, т.к. класс Location уже существует.

### Поля

- `id`
- `name`
- `lat`
- `lon`
---

### Связь Event и Place

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "place_id")
private Place place;
```

### База данных

Добавлено поле:

```sql
place_id BIGINT
```

Добавлен внешний ключ:

```sql
FOREIGN KEY (place_id)
REFERENCES places(id)
ON DELETE SET NULL
```

При удалении места связь у событий автоматически очищается.

---

## Структура API Places

### Public API

Получение списка мест:

```http
GET /places
```

Получение места по id:

```http
GET /places/{placeId}
```

### Admin API

Создание места:

```http
POST /admin/places
```

Изменение места:

```http
PUT /admin/places/{placeId}
```

Удаление места:

```http
DELETE /admin/places/{placeId}
```

---

## Привязка / отвязка места к событию

Сделаны отдельные эндпоинты на привязку/отвязку места, а не обновление через PATCH события, т.к. требуется возможность явно сбрасывать привязку в `null`.
При этом общий подход обновления через update dto в приложении предполагает, что `null`-поля в dto игнорируются и не обновляют объект.


### Private API

Привязка места:

```http
PUT /users/{userId}/events/{eventId}/place/{placeId}
```

Отвязка места:

```http
DELETE /users/{userId}/events/{eventId}/place
```

Для private API изменение разрешено только для событий в статусах:

- `PENDING`
- `CANCELED`

### Admin API

Привязка места:

```http
PUT /admin/events/{eventId}/place/{placeId}
```

Отвязка места:

```http
DELETE /admin/events/{eventId}/place
```

Для admin API ограничение по статусу отсутствует.

---

## Поиск событий по месту

Добавлены параметры поиска:

- `placeId`
- `radius`

### Логика поиска

#### Поиск по месту

```http
GET /events?placeId=1
GET /admin/events?placeId=1
```

Выполняется точное сравнение:

```text
event.place = place
```

#### Поиск по месту и радиусу

```http
GET /events?placeId=1&radius=2
GET /admin/events?placeId=1&radius=2
```

Выполняется поиск событий в радиусе от координат места.

---

## Поддержка поиска

Поиск по месту добавлен в:

- Public events API
- Admin events API

---

## DTO

Добавлен новый DTO:

```text
ShortPlaceDto
```

### Поля

- `id`
- `name`

### Использование

`ShortPlaceDto` добавлен в:

```text
EventFullDto
```

Поле:

```text
place
```

`EventShortDto` не изменялся.

---

## Swagger / OpenAPI

Документация обновлена.

Все новые элементы помечены тегом:

```text
[NEW FEATURE: Locations]
```
