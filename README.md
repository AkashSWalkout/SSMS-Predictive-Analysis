# SSMS Predictive Analysis Module



## Architecture

- **Backend**: Spring Boot (Java 17) — `ssms-predictive-module/`
- **Frontend**: Vite + vanilla JS + Chart.js — `predictive-dashboard/`
- **Database**: MySQL — persists all predictions, analyses, and batch uploads
- **AI Provider**: OpenAI-compatible API (configurable)

The built frontend is served from the backend's `src/main/resources/static/`, so the entire app runs on a single port (`9091`).

---


# Terminal 1 — Backend
cd ssms-predictive-module
mvn spring-boot:run

# Terminal 2 — Frontend
cd predictive-dashboard
npm install
npm run dev
```
Open `http://localhost:5173`

---

## Database Setup (MySQL)

This module uses **MySQL** — compatible with the main SSMS project database.

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:mysql://localhost:3306/ssms_predictive?createDatabaseIfNotExist=true` | JDBC URL |
| `DB_USERNAME` | `root` | MySQL username |
| `DB_PASSWORD` | `root` | MySQL password |

**Tables auto-created by Hibernate (`ddl-auto=update`):**
- `prediction_reports` — student prediction results
- `prediction_batch_uploads` — CSV/Excel upload tracking
- `report_card_analyses` — AI vision/file analysis results

---


---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/predictive/analyse/student` | Analyse single student |
| POST | `/api/predictive/analyse/bulk` | Analyse multiple students |
| POST | `/api/predictive/upload` | Upload CSV/Excel & analyse |
| POST | `/api/predictive/analyze-image` | Analyse report card image |
| POST | `/api/predictive/analyze-file` | Analyse any student data file |
| GET | `/api/predictive/upload/template` | Download CSV template |
| GET | `/api/predictive/student/{id}` | Student prediction history |
| GET | `/api/predictive/report/{id}` | Single prediction report |
| GET | `/api/predictive/dashboard` | Dashboard aggregation data |
| GET | `/api/predictive/charts/*` | Chart data (risk, category, class, trend) |

---

