
# Search Engine

A local search engine for websites with support for morphological analysis and multithreaded page crawling.
The application indexes resources specified in the configuration and allows users to instantly find the most relevant pages based on keywords.
### 🛠 Stack & Technologies

| Category | Technology | Description |
| :--- | :--- | :--- |
| **Language** | ![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white) | JDK 17+ & Multithreading (ForkJoin) |
| **Framework** | ![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white) | Spring Boot 3, Data JPA, Thymeleaf |
| **Database** | ![MySQL](https://img.shields.io/badge/mysql-%23005C84.svg?style=for-the-badge&logo=mysql&logoColor=white) | Relational database for storing search indexes |
| **Build Tool** | ![Apache Maven](https://img.shields.io/badge/Apache%20Maven-C71A36?style=for-the-badge&logo=Apache%20Maven&logoColor=white) | 	Dependency and project lifecycle management |
| **Library** | ![Jsoup](https://img.shields.io/badge/Jsoup-000000?style=for-the-badge&logo=jsoup&logoColor=white) | HTML parsing, content processing, and link extraction |
| **Analysis** | ![Lucene](https://img.shields.io/badge/Lucene-434343?style=for-the-badge&logo=apache&logoColor=white) | Morphological analysis and lemmatization |

---

## Architecture
The project is divided into three logical layers following clean code principles:

1. **Presentation (Controllers)**: Handles HTTP requests and communication with the frontend.
2. **Business (Services)**: 
    * `IndexingService`: Recursively crawls web pages using `ForkJoinPool`.
    * `LemmaService`: Cleans HTML content and extracts the base forms (lemmas) of words.
3. **Data Access (Repositories)**: Handles database operations using Spring Data JPA.

## API Reference

All API requests use the `/api` prefix. Responses are returned in JSON format.

| Method | Endpoint | Description | Status Codes |
| :--- | :--- | :--- | :--- |
| `GET` | `/startIndexing` | Starts full indexing of all configured websites | `200`, `401` |
| `GET` | `/stopIndexing` | Stops the current indexing process | `200`, `401` |
| `POST` | `/indexPage` | 	Indexes or updates a single page | `200`, `400` |
| `GET` | `/statistics` | 	Returns general statistics for all websites | `200` |
| `GET` | `/search` | Searches through indexed pages | `200`, `400` |

### Search Parameters (`GET /api/search`)

| Parameter | Type | Required | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `query` | `String` | **Yes** | — | Search query |
| `site` | `String` | No | All | Filter results by a specific website URL |
| `offset` | `Integer` | No | `0` | Pagination offset |
| `limit` | `Integer` | No | `10` | Number of results per page |

## Installation
### Prerequisites
* **JDK 17** or higher
* **MySQL Server 8.0+**
* **Maven 3.8+**
### 1. **Clone the repository:**
```bash
  git clone https://github.com/Tarantism7/search_engine
  cd search-engine
```
### 2. **Database Configuration**
First, ensure your **MySQL Server** is running. Then, execute the following SQL command using your preferred client (MySQL Workbench, Terminal, or IntelliJ Database Tool) to create the project schema:
```sql
CREATE DATABASE search_engine;
```
### 3. **Build and run**
Run the following commands from the project root directory:
```bash
mvn clean install
java -jar target/searchengine-1.0.0.jar
```
## Authors

- [@Tarantism](https://www.github.com/tarantism7)

