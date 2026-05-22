
# Search Engine

Локальный поисковый движок по сайтам с поддержкой морфологического анализа и многопоточного обхода страниц. Приложение индексирует заданные в конфигурации ресурсы и позволяет мгновенно находить наиболее релевантные страницы по ключевым словам.

### 🛠 Stack & Technologies

| Category | Technology | Description |
| :--- | :--- | :--- |
| **Language** | ![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white) | JDK 17+ & Multithreading (ForkJoin) |
| **Framework** | ![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white) | Spring Boot 3, Data JPA, Thymeleaf |
| **Database** | ![MySQL](https://img.shields.io/badge/mysql-%23005C84.svg?style=for-the-badge&logo=mysql&logoColor=white) | Реляционная база данных для хранения индексов |
| **Build Tool** | ![Apache Maven](https://img.shields.io/badge/Apache%20Maven-C71A36?style=for-the-badge&logo=Apache%20Maven&logoColor=white) | Управление зависимостями и жизненным циклом |
| **Library** | ![Jsoup](https://img.shields.io/badge/Jsoup-000000?style=for-the-badge&logo=jsoup&logoColor=white) | Парсинг контента и извлечение ссылок |
| **Analysis** | ![Lucene](https://img.shields.io/badge/Lucene-434343?style=for-the-badge&logo=apache&logoColor=white) | Морфологический анализ и лемматизация |

---

## Architecture


Проект разделен на три логических слоя согласно принципам чистого кода:

1. **Presentation (Controllers)**: Обработка HTTP-запросов и взаимодействие с фронтендом.
2. **Business (Services)**: 
    * `IndexingService`: Рекурсивный обход страниц через `ForkJoinPool`.
    * `LemmaService`: Очистка текста от HTML и извлечение базовых форм слов (лемм).
3. **Data Access (Repositories)**: Работа с БД через Spring Data JPA.

## API Reference

Все запросы к API начинаются с префикса `/api`. Ответы возвращаются в формате JSON.

| Method | Endpoint | Description | Status Codes |
| :--- | :--- | :--- | :--- |
| `GET` | `/startIndexing` | Запуск полной индексации всех сайтов | `200`, `401` |
| `GET` | `/stopIndexing` | Остановка текущего процесса индексации | `200`, `401` |
| `POST` | `/indexPage` | Индексация или обновление одной страницы | `200`, `400` |
| `GET` | `/statistics` | Получение общей статистики по сайтам | `200` |
| `GET` | `/search` | Поиск по проиндексированным страницам | `200`, `400` |

### Search Parameters (`GET /api/search`)

| Parameter | Type | Required | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `query` | `String` | **Yes** | — | Поисковый запрос |
| `site` | `String` | No | All | Фильтр по конкретному URL сайта |
| `offset` | `Integer` | No | `0` | Смещение для пагинации |
| `limit` | `Integer` | No | `10` | Количество результатов на странице |

## Installation
### Prerequisites
* **JDK 17** or higher
* **MySQL Server 8.0+**
* **Maven 3.8+**
### 1. **Клонируйте репозиторий:**
```bash
  git clone [https://github.com/cocojumbo27/search-engine.git](https://github.com/cocojumbo27/search-engine.git)
  cd search-engine
```
### 2. **Database Configuration**
First, ensure your **MySQL Server** is running. Then, execute the following SQL command using your preferred client (MySQL Workbench, Terminal, or IntelliJ Database Tool) to create the project schema:
```sql
CREATE DATABASE search_engine;
```
### 3. **Сборка и запуск**
Выполните следующие команды в корневой папке проекта:
```bash
mvn clean install
java -jar target/searchengine-1.0.0.jar
```
## Authors

- [@Tarantism](https://www.github.com/tarantism7)

