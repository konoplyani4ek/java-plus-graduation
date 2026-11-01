Explore With Me

Платформа для публикации мероприятий и поиска компании для участия в них. Проект построен как набор микросервисов на Spring Cloud: каждый домен (пользователи, события, заявки, категории, подборки, места) живёт в отдельном сервисе со своей базой данных, а общение между ними идёт через Feign-клиенты с отказоустойчивостью на Resilience4j.

Содержание
Технологии
Архитектура
Структура репозитория
Сервисы и порты
Маршрутизация через Gateway
Сервис статистики
Межсервисное взаимодействие и отказоустойчивость
Функциональность Locations (места)
Запуск
Сборка и качество кода
API и документация
Дорожная карта: рекомендательная система
Технологии
Область	Стек
Язык / платформа	Java 21, Maven (многомодульный проект)
Фреймворк	Spring Boot 3.3.0, Spring Cloud 2023.0.3
Инфраструктура	Eureka (discovery), Spring Cloud Config (native), Spring Cloud Gateway
Взаимодействие	OpenFeign (+ feign-hc5 для PATCH), Spring Cloud LoadBalancer
Отказоустойчивость	Resilience4j 2.2.0 (CircuitBreaker, Retry)
Данные	PostgreSQL 16.1, Spring Data JPA, Spring JDBC (stats), H2 для тестов
Прочее	Lombok, Checkstyle, Docker / Docker Compose, Actuator
Архитектура
                          ┌──────────────────┐
        клиент ─────────► │  gateway-server  │ :8080
                          └────────┬─────────┘
                                   │ lb://
     ┌──────────┬───────────┬──────┴─────┬────────────┬──────────────┬──────────────┐
     ▼          ▼           ▼            ▼            ▼              ▼              
user-service request-   event-service category-   compilation-   location-
   :8081    service      :8083        service      service        service
            :8082          │           :8084        :8085          :8086
                           │
                           └──► stats-server :9090 (просмотры)

 discovery-server (Eureka) :8761  ◄── регистрация всех сервисов
 config-server              ◄── конфигурация (classpath:/config/)

Принципы:

База на сервис. Прямых обращений в чужие БД нет. Вместо JPA-связей между доменами хранятся только идентификаторы (initiator_id, category_id, place_id, event_id, requester_id), а данные соседей запрашиваются через Feign.
Внутренний API. Эндпоинты /internal/** — приватный контракт между сервисами. Через Gateway они не публикуются.
Модули *-dto / *-client / *-server. DTO и Feign-клиент вынесены в отдельные артефакты, чтобы сервисы зависели друг от друга только через контракт.
Batch-запросы. Для списков событий инициаторы, категории и количества подтверждённых заявок запрашиваются одним вызовом, а не по одному (без проблемы N+1).
Структура репозитория
.
├── pom.xml                      # родительский POM
├── docker-compose.yml
├── checkstyle.xml, suppressions.xml, lombok.config
├── ewm-main-service-spec.json   # OpenAPI основного API
├── ewm-stats-service-spec.json  # OpenAPI сервиса статистики
├── infra/
│   ├── discovery-server/        # Eureka Server
│   ├── config-server/           # Spring Cloud Config (native, config/*.properties)
│   └── gateway-server/          # Spring Cloud Gateway
├── stat/
│   ├── stat-dto/                # HitDto, StatDto
│   ├── stat-client/             # клиент сервиса статистики (через Eureka)
│   └── stats-server/            # сервис статистики
└── core/
    ├── user/                    # user-dto, user-client, user-server
    ├── event/                   # event-dto, event-client, event-server
    ├── request/                 # request-dto, request-client, request-server
    ├── category/                # category-dto, category-client, category-server
    ├── compilation/             # compilation-server
    └── place/                   # place-dto, place-client, place-server
Сервисы и порты

Порты указаны для запуска через Docker Compose. Локально сервисы стартуют на случайном порту (server.port=0) и находят друг друга через Eureka.

Сервис	Назначение	Порт	БД (порт на хосте)
discovery-server	Eureka	8761	—
config-server	Централизованная конфигурация	случайный	—
gateway-server	Единая точка входа	8080	—
user-service	Пользователи	8081	ewm-users (6543)
request-service	Заявки на участие	8082	ewm-requests (6544)
event-service	События	8083	ewm-events (6545)
category-service	Категории	8084	ewm-category (6546)
compilation-service	Подборки событий	8085	ewm-compilation (6547)
location-service	Справочник мест	8086	ewm-location (6548)
stats-server	Статистика просмотров	9090	ewm-stats (6541)
Маршрутизация через Gateway

Внешний API доступен на http://localhost:8080:

Путь	Сервис
/admin/users/**	user-service
/users/{userId}/requests/**	request-service
/events/**, /admin/events/**, /users/{userId}/events/**	event-service
/categories/**, /admin/categories/**	category-service
/places/**, /admin/places/**	location-service
/compilations/**, /admin/compilations/**	compilation-service
Сервис статистики

Сохраняет обращения к эндпоинтам и отдаёт агрегированную статистику.

POST /hit — сохранить факт обращения (app, uri, ip, timestamp), ответ 201.
GET /stats?start=&end=&uris=&unique= — статистика просмотров за период; даты в формате yyyy-MM-dd HH:mm:ss, unique=true учитывает только уникальные IP.

Клиент stat-client находит stats-server через Eureka, использует повторные попытки (3 попытки с нарастающей задержкой) и настраиваемый таймаут (stat-client.timeout-ms). event-service отправляет hit при просмотре публичных эндпоинтов событий и получает от сервиса количество просмотров. Если статистика недоступна, события всё равно возвращаются (просмотры в этом случае — null).

Межсервисное взаимодействие и отказоустойчивость

Вызовы соседей обёрнуты в отдельные бины-шлюзы (UserGateway, CategoryGateway, PlaceGateway, RequestGateway, EventGateway) с аннотациями Resilience4j @CircuitBreaker и @Retry. Шлюзы вынесены в отдельные классы намеренно: аннотации работают через Spring AOP-прокси и не срабатывают при вызове метода внутри того же класса.

Стратегия при недоступности зависимости зависит от типа операции:

Операция	Поведение
Чтение для отображения (инициатор, категория, место, число заявок)	Деградация: заглушка / null / 0, ответ не падает
Запись и валидация (создание события, подача заявки, подтверждение заявок, удаление категории)	Fail-closed: 503 Service Unavailable
Бизнес-ответы соседа (404, 409)	Пробрасываются как есть, не считаются сбоем для circuit breaker

Параметры circuit breaker и retry задаются в application.properties каждого сервиса, таймауты Feign — через feign.client.config.default.*.

Функциональность Locations (места)

Справочник мест вынесен в location-service (поля: id, name, lat, lon). Событие хранит только place_id.

Public API

GET /places — список мест
GET /places/{placeId} — место по id

Admin API

POST /admin/places — создать место
PUT /admin/places/{placeId} — изменить место
DELETE /admin/places/{placeId} — удалить место

Привязка места к событию выполняется отдельными эндпоинтами, а не через PATCH события: общий подход к update-DTO игнорирует null-поля, поэтому явно сбросить привязку через PATCH нельзя.

Private: PUT /users/{userId}/events/{eventId}/place/{placeId} и DELETE /users/{userId}/events/{eventId}/place (только для событий в статусах PENDING и CANCELED).
Admin: PUT /admin/events/{eventId}/place/{placeId} и DELETE /admin/events/{eventId}/place (без ограничения по статусу).

Поиск по месту — параметры placeId и radius в GET /events и GET /admin/events:

?placeId=1 — события, привязанные к месту;
?placeId=1&radius=2 — события в радиусе (км) от координат места; радиус без места — ошибка 400.

Расчёт расстояния выполняется SQL-функцией distance(...) (PL/pgSQL), создаваемой в schema.sql event-service, поэтому для поиска по радиусу нужен PostgreSQL.

Запуск
Требования
JDK 21
Maven 3.9+
Docker и Docker Compose
Через Docker Compose

Сначала соберите jar-файлы (Dockerfile каждого сервиса копирует target/*.jar):

bash
mvn clean package -DskipTests
docker compose up --build

Поднимутся инфраструктурные сервисы, все микросервисы и их базы данных PostgreSQL. Gateway стартует после того, как healthcheck пройдут все core-сервисы. После запуска:

API: http://localhost:8080
Eureka: http://localhost:8761
Статистика: http://localhost:9090
Локально (без Docker)
Поднимите PostgreSQL и создайте базы ewm-stats, ewm-users, ewm-requests, ewm-events, ewm-category, ewm-compilation, ewm-location. Параметры подключения по умолчанию (dbuser / 12345, localhost:5432) заданы в application.properties сервисов и переопределяются переменными окружения SPRING_DATASOURCE_*.
Запускайте сервисы в таком порядке: discovery-server → config-server → остальные сервисы → gateway-server.

Таблицы создаются автоматически из schema.sql каждого сервиса (spring.sql.init.mode=always).

Сборка и качество кода
bash
mvn clean verify
Checkstyle подключён в родительском POM (фаза compile, failOnViolation=true), конфигурация — checkstyle.xml.
Тесты запускаются с профилем test (H2 в режиме PostgreSQL).
В CI используется workflow .github/workflows/api-tests.yml с Postman-тестами (отчёт строится по шаблону dashboard-template.hbs).
API и документация

Спецификации OpenAPI лежат в корне репозитория:

ewm-main-service-spec.json — основной API (события, категории, подборки, пользователи, заявки);
ewm-stats-service-spec.json — API сервиса статистики.

Их можно открыть в Swagger Editor.

Дорожная карта: рекомендательная система

Следующий этап — замена простого сервиса статистики на рекомендательную систему на базе Apache Kafka и gRPC из трёх микросервисов:

Сервис	Задача
Collector	Принимает действия пользователей (просмотр, заявка, лайк) по gRPC и пишет их в топик stats.user-actions.v1 в формате Avro
Aggregator	Читает действия пользователей, пересчитывает косинусное сходство мероприятий и пишет результат в stats.events-similarity.v1
Analyzer	Хранит историю взаимодействий и коэффициенты сходства, по gRPC отдаёт рекомендации, похожие мероприятия и суммы взаимодействий
